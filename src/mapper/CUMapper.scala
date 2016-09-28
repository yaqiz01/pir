package pir.graph.mapper
import pir.graph._
import pir._
import pir.typealias._
import pir.codegen.Printer
import pir.graph.traversal.{PIRMapping, MapPrinter}

import scala.collection.immutable.Set
import scala.collection.immutable.HashMap
import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Success, Failure}

class CUMapper(soMapper:ScalarOutMapper, viMapper:VecInMapper)(implicit val design:Design) extends Mapper {
  type R = PCL
  type N = CL
  type V = CLMap.V
  val typeStr = "CUMapper"
  override def debug = Config.debugCUMapper

  def finPass(m:M):M = m

  val resMap:MMap[N, List[R]] = MMap.empty

  def resFunc(cu:N, m:M, remainRes:List[R]):List[R] = {
    resMap(cu).filter { pcu => !m.clmap.pmap.contains(pcu) }
  }

  def mapCU(cu:N, pcu:R, pirMap:M):M = {
    if (cu.isInstanceOf[TT]) assert(pcu.isInstanceOf[PTT], s"$cu, $pcu") 
    val cmap = pirMap.setCL(cu, pcu) 
    /* Map CU */
    Try {
      soMapper.map(cu, cmap)
    }.map { m =>
      viMapper.map(cu, m)
    } match {
      case Success(m) => m
      case Failure(e) => throw e
    }
  }

  val cons = List(mapCU _)

  def mapCUs(pcus:List[PCU], cus:List[ICL], pirMap:M, finPass:M => M):M = {
    CUMapper.qualifyCheck(pcus, cus, resMap)
    // Bind nodes to resources
    bind(pcus, cus, pirMap, cons, resFunc _, finPass)
  }

  def map(m:M):M = {
    dprintln(s"Datapath placement & routing ")
    Try{
      mapCU(design.top, design.arch.top, m) 
    } match {
      case Success(mp) => mapCUs(design.arch.cus, design.top.innerCUs, mp, finPass _)
      case Failure(e) => throw e
    }
  }
}
object CUMapper {
  def check(qualify:Boolean, curr:Boolean):Boolean = { qualify && curr }
  def check(qualify:Boolean, numNode:Int, numRes:Int):Boolean = {
    check(qualify, numNode <= numRes)
  }

  def check(conds:ListBuffer[_]):Boolean = {
    conds.foldLeft(true) { case (qualify, cond) =>
      cond match {
        case cond:Boolean => qualify && cond
        case (nn:Int, nr:Int) => qualify && (nn <= nr)
        case (info:String, nn:Int, nr:Int) => 
          val cond = (nn <= nr)
          //if (!cond) dprintln(s"$info: pass:$cond numNodes:$nn numRes:$nr")
          qualify && cond
        case (ns:Iterable[_], rs:Iterable[_]) => qualify && (ns.size <= rs.size)
        case (info:String, ns:Iterable[_], rs:Iterable[_]) => 
          val cond = (ns.size <= rs.size)
          //if (!cond) dprintln(s"$info: pass:$cond numNodes:${ns.size} numRes:${rs.size}")
          qualify && cond
        case c => throw PIRException(s"Unknown checking format: $cond")
      }
    }
  }

  /* 
   * Filter qualified resource. Create a mapping between cus and qualified pcus for each cu
   * */
  def qualifyCheck(pcus:List[PCU], cus:List[ICL], map:MMap[CL, List[PCL]])(implicit mapper:Mapper):Unit = {
    cus.foreach { cu => 
      map += cu -> pcus.filter { pcu =>
        // println(s"$cu -> $pcu: ----")
        val cons = ListBuffer[Any]()
        cons += cu.isInstanceOf[TT] == pcu.isInstanceOf[PTT]
        cons += (("reg"	      , cu.infGraph, pcu.regs))
        cons += (("sram"	    , cu.srams, pcu.srams))
        cons += (("ctr"	      , cu.cchains.flatMap(_.counters), pcu.ctrs))
        cons += (("sin"	      , cu.sins, pcu.sins))
        cons += (("sout"	    , cu.souts, pcu.souts))
        cons += (("vin"	      , cu.vins, pcu.vins))
        cons += (("vout"	    , cu.vouts, pcu.vouts))
        cons += (("stage"	    , cu.stages, pcu.stages))
        cons += (("tokOut"	  , cu.ctrlOuts, pcu.ctrlBox.ctrlOuts))
        cons += (("tokIn"	    , cu.ctrlIns, pcu.ctrlBox.ctrlIns))
        cons += (("udc"	      , cu.udcounters, pcu.ctrlBox.udcs))
        cons += (("enLut"	    , cu.enLUTs, pcu.ctrlBox.enLUTs))
        cons += (("tokDownLut", cu.tokDownLUTs.size, 1)) // TODO
        cons += (("tokOutLut" , cu.tokOutLUTs, pcu.ctrlBox.tokOutLUTs))
        check(cons)
      }
      mapper.dprintln(s"qualified resource: $cu -> ${map(cu)}")
    }
  }
}

case class OutOfPTT(nres:Int, nnode:Int) (implicit val mapper:Mapper, design:Design) extends OutOfResource {
  override val msg = s"Not enough TileTransfers in ${design.arch} to map application."
} 
case class OutOfPCU(nres:Int, nnode:Int) (implicit val mapper:Mapper, design:Design) extends OutOfResource {
  override val msg = s"Not enough ComputeUnits in ${design.arch} to map application."
} 
