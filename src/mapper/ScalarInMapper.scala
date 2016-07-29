package pir.graph.mapper
import pir._
import pir.graph.{ComputeUnit => CU, ScalarIn => SI, Top}
import pir.plasticine.graph.{ComputeUnit => PCU, InBus => PVI, ScalarIn => PSI, ScalarOut => PSO}
import pir.graph.traversal.PIRMapping

import scala.collection.immutable.Set
import scala.collection.immutable.HashMap
import scala.collection.immutable.Map

object ScalarInMapper extends Mapper {
  type N = SI
  type R = PSI 
  type V = (PSI, PSO) 

  def printMap(m:MP)(implicit p:Printer) = {
    p.emitBS("scalarInMap")
    m.foreach{ case (k,v) =>
      p.emitln(s"$k -> $v")
    }
    p.emitBE
  }

  private def mapScalarIns(cu:CU, pcu:PCU)(n:N, p:R, cuMap:M)(implicit design: Design):M = {
    //val vmap = cuMap(n.ctrler.asInstanceOf[CU])._2
    //val dep = n.scalar.writers.head
    //val validSouts = dep match {
    //  case c:CU =>
    //    val pvin = vmap(dep)
    //    pvin.outports.filter(op => p.in.isConn(op))
    //  case c:Top =>
    //    design.arch.argIns.filter(ai => p.in.isConn(ai))
    //}
    //if (validSouts.size == 0) throw ScalarInRouting(p, pvin) 
    CUMapper.setSImap(cuMap, cu, CUMapper.getSImap(cuMap, cu) + (n -> (p, null)))
  }

  def map(cu:CU, pcu:PCU, cuMap:CUMapper.M)(implicit design: Design):M = {
    val sin = cu.sins
    val psin = pcu.sins
    simAneal(psin, sin, cuMap, List(mapScalarIns(cu, pcu) _),None,OutOfScalarIn(pcu, _, _))
  }

}

case class OutOfScalarIn(pcu:PCU, nres:Int, nnode:Int)(implicit design:Design) extends OutOfResource {
  override val mapper = ScalarInMapper
  override val msg = s"Not enough Scalar Input Buffer in ${pcu} to map application."
}
case class ScalarInRouting(psi:PSI, pvin:PVI)(implicit design:Design) extends MappingException {
  override val mapper = ScalarInMapper
  override val msg = s"Fail to route ${psi} to ${pvin}"
}
