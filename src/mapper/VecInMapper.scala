package pir.graph.mapper
import pir._
import pir.graph.{Controller => CL, ComputeUnit => CU, TileTransfer => TT, Input => I, VecOut => VO, _}
import pir.plasticine.graph.{Controller => PCL, ComputeUnit => PCU, TileTransfer => PTT, InBus => PIB, OutBus => POB}
import pir.graph.traversal.PIRMapping

import scala.collection.immutable.Set
import scala.collection.immutable.HashMap
import scala.collection.immutable.Map

object VecInMapper extends Mapper {
  type R = PIB
  type N = I

  val finPass:Option[M => M] = None

  def map(cl:CL, pirMap:M):M = {
    val pcl = pirMap.clmap(cl)
   // Assume sin and vin have only one writer
    val cons = List(mapVec(cl, pcl) _) 
    val ins = cl.vins ++ cl.sins
    val pvins = pcl.vins
    val pmap = simAneal(pvins, ins, pirMap, cons, finPass, OutOfVec(cl, pcl, _, _))
    cl.readers.foldLeft(pmap) { case (pm, reader) =>
      if (pirMap.clmap.contains(reader)) {
        val rins = reader.vins ++ reader.sins
        if (rins.exists( rin => !pirMap.vimap.contains(rin) )) map(reader, pm)
        else pm
      } else pm
    }
  }

  def mapVec(cl:CL, pcl:PCL)(n:N, p:R, pirMap:M):M = {
    if (pirMap.vimap.contains(n)) return pirMap
    val dep = n match { // ctrler that writes n
      case n:ScalarIn => n.scalar.writer.ctrler
      case n:VecIn => n.vector.writer.ctrler
    }
    // If reader ctrler dep haven't been placed, postpone mapping
    if (!pirMap.clmap.contains(dep)) return pirMap
    // Get dep's output bus 
    val pdvout:POB = n match {
      case n:VecIn => dep match {
        //case d:MemoryController => Nil //TODO
        case d:CU => pirMap.clmap(d).vouts.head //TODO Assume only 1 vout 
        case _ => throw TODOException(s"Not supported vecout mapping")
      }
      case n:ScalarIn =>
        pirMap.slmap.getOutBus(n.scalar)
    } 

    /* Find vins that connects to the depended ctrler */
    if (p.isConn(pdvout)) {
      val pmap = pirMap.setVI(n, p)
      pmap.setIB(p, pdvout)
    } else {
      throw IntConnct(cl, pcl)
    }
  }
}

case class IntConnct(cl:CL, pcl:PCL)(implicit design:Design) extends MappingException {
  override val mapper = VecInMapper 
  override val msg = s"Fail to map ${cl} on ${pcl} due to interconnection constrain"
}
case class OutOfVec(cl:CL, pcl:PCL, nres:Int, nnode:Int)(implicit design:Design) extends OutOfResource {
  override val mapper = VecInMapper
  override val msg = s"Not enough InBus IO in ${pcl} to map ${cl}."
}
