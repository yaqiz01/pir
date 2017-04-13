package pir.mapper
import pir._
import pir.util.typealias._
import pir.pass.{PIRMapping}
import pir.codegen.{CtrDotPrinter}
import pir.util._
import pir.exceptions._
import pir.graph.Const
import pir.plasticine.util._

import scala.collection.immutable.Set
import scala.collection.immutable.HashMap
import scala.collection.immutable.Map
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Success, Failure}

class CtrMapper(implicit val design:Design) extends Mapper with LocalRouter {
  type R = PCtr
  type N = Ctr
  import spademeta._
  val typeStr = "CtrMapper"
  override def debug = Config.debugCtrMapper
  override val exceptLimit = 200
  
  def finPass(cu:CU)(m:M):M = m

  /*Make sure counters that are chained are next to each other and the counter is order such that
   * inner counter */
  def sortCChains(cchains:List[CC]):List[Ctr] = {
    cchains.filter(_.inner.isInner).flatMap { cc =>
      val ctrs = ListBuffer[Ctr]()
      var cur = cc.inner
      while (!cur.isOuter) {
        ctrs += cur
        cur = cur.next
      }
      ctrs += cur
      ctrs.toList
    }
  }

  def map(cu:CL, pirMap:M):M = {
    cu match {
      case cu:CU => map(cu, pirMap)
      case cu => pirMap
    }
  }

  def map(cu:CU, pirMap:M):M = {
    log((cu, false)) {
      // Mapping inner counter first converges faster
      val pcu = pirMap.clmap(cu).asInstanceOf[PCU]
      val ctrs = sortCChains(cu.cchains) //++ cu.mems.collect{case f:FOW => f.dummyCtr}
      val pctrs = pcu.ctrs
      bind(
        allNodes=ctrs,
        initMap=pirMap,
        constrain=mapCtr _, 
        resFunc=resFunc(pctrs) _, 
        finPass=finPass(cu) _
      )
    }
  }

  def resFunc(allRes:List[R])(n:N, m:M, triedRes:List[R]):List[R] = {
    //if (n.id==507){
      //new CtrDotPrinter().print(allRes, m)
    //}
    val remainRes = allRes.diff(triedRes).filter( pc => !m.ctmap.pmap.contains(pc))
    val ptop = design.arch.top
    val enCtrs = n.en.from.src match {
      case dep:Ctr if n.ctrler.inner == dep.ctrler.inner => // Counter in the same CU
        m.ctmap.get(dep).fold(remainRes) { pdep =>
          pdep.done.fanOuts.map{ fo => fo.src }.collect{ case pc:R => pc }.toList
        }
      // Inner most counter or copied inner most counter whose enable is routed fron network
      case _ => remainRes.filter(pc => isInnerCounter(pc))
    }
    val doneCtrs = n.done.to.map { done =>
      done.src match {
        case deped:Ctr if n.ctrler.inner==deped.ctrler.inner =>
          m.ctmap.get(deped).fold(remainRes) { pdeped =>
            pdeped.en.fanIns.map{ fi => fi.src}.collect{case pc:R => pc}.toList
          }
        case _ => remainRes
      }
    }.reduceOption{ _ intersect _ }.getOrElse(remainRes)

    val resPool = enCtrs intersect doneCtrs
    //if (resPool.size==0) {
      //new CtrDotPrinter().print(allRes, m)
      //println(s"here")
    //}
    resPool
  }

  def mapCtr(n:N, p:R, mp:M):M = {
    var map = mp
    map = mapInPort(n.min, p.min, map)
    map = mapInPort(n.max, p.max, map)
    map = mapInPort(n.step, p.step, map)
    map = map.setCT(n,p).setOP(n.out, p.out)
    dprintln(s"mapping $n -> ${map.ctmap(n)}")
    map
  }

}
