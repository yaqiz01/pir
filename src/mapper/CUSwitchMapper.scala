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

class CUSwitchMapper(soMapper:ScalarOutMapper)(implicit val design:Design) extends Mapper {
  type R = PCL
  type N = CL
  type V = CLMap.V
  val typeStr = "CUSwitchMapper"

  def finPass(m:M):M = m

  val resMap:MMap[N, List[R]] = MMap.empty

  type Edge = (POB, PIB)

  def search(pdep:PCL, pcls:List[PCL], m:M, hop:Int) = {
    val pathMap:MMap[PCU, List[Edge]] = MMap.empty
  }

  def advance(pne:PNE, hop:Int) = {
    pne.vouts.foreach { vout =>
      vout.fanOuts.foreach { vin =>

      }
    }
  }

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
      val ins = cu match {
        case cl:TT => cu.sins // Assume tile transfer vin internallly connected
        case _ => cu.vins ++ cu.sins
      }
      m
    } match {
      case Success(m) => dprintln(s"$cu -> $pcu (succeeded)"); m
      case Failure(e) => dprintln(s"$cu -> $pcu (failed)"); throw e
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
