package pir.mapper
import pir.{Design, Config}
import pir.util.typealias._
import pir.pass.PIRMapping
import pir.graph.{PipeReg => PR, VecInPR, LoadPR}
import pir.spade.graph.{PipeReg => PPR}
import pir.spade.util._
import pir.spade.main._
import pir.exceptions._
import pir.util.PIRMetadata

import scala.collection.mutable.ListBuffer
import scala.collection.immutable.Set
import scala.collection.immutable.HashMap
import scala.collection.immutable.Map

class SramMapper(implicit val design:Design) extends Mapper with LocalRouter {
  type N = SRAM
  type R = PSRAM
  val typeStr = "SramMapper"
  override def debug = Config.debugSramMapper
  import pirmeta.{indexOf => _, _}
  import spademeta._

  def finPass(cu:CL)(m:M):M = m 

  def constrain(n:N, r:R, m:M):M = {
    var mp = m
    val bankSize = r.bankSize(n.banking)
    if (n.size > bankSize) throw MappingException(m, s"$n'size=${n.size} > $r.bankSize=${bankSize}")
    if (n.banks > r.banks) throw MappingException(m, s"$n.banks=${n.banks} > $r.banks=${r.banks}")
    mp = m.setSM(n, r)
    mp = mapOutPort(n.readPort, r.readPort, mp)
    mp = mapInPort(n.writePort, r.writePort, mp)
    mp = mapInPort(n.writeAddr, r.writeAddr, mp)
    mp = mapInPort(n.readAddr, r.readAddr, mp)
    mp = mapMux(n.writePortMux, r.writePortMux, mp)
    mp = mapMux(n.writeAddrMux, r.writeAddrMux, mp)
    mp = mapMux(n.readAddrMux, r.readAddrMux, mp)
    mp
  }

  def map(cu:ICL, pirMap:M):M = {
    log(cu) {
      val srams = cu.srams
      val psrams = pirMap.clmap(cu) match {
        case pcu:PCU => pcu.srams
        case _ => Nil
      }
      bind(
        allRes=psrams,
        allNodes=srams,
        initMap=pirMap,
        constrain=constrain _,
        finPass=(m:M) => m
      )
    }
  }

  def map(cu:CL, pirMap:M):M = {
    cu match {
      case cu:ICL => map(cu, pirMap)
      case cu => finPass(cu)(pirMap)
    }
  }
}

