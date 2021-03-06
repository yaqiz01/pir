package pir.spade.graph

import pir.util.enums._
import pir.util.misc._
import pir.spade.main._
import pir.spade.config.ConfigFactory
import pir.spade.simulation._
import pir.spade.util._
import pir.exceptions._

import scala.language.reflectiveCalls
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.Set

case class OuterComputeUnitParam (
  sbufSize:Int = 16,
  numSins:Int = 5,
  numRegs:Int = 0,
  numStages:Int = 0,
  numCtrs:Int = 6,
  muxSize:Int = 10,
  numUDCs:Int = 15
) extends ComputeUnitParam() {
  val numVins:Int = 0
  val numVouts:Int = 0
  val numSouts:Int = 0
  val vbufSize:Int = 0
  val numSRAMs:Int = 0
  val sramSize:Int = 0
  override lazy val numLanes:Int = 1

  def config(cu:OuterComputeUnit)(implicit spade:Spade) = {
    assert(cu.sins.size >= numSins, s"sins=${cu.sins.size} numSins=${numSins}")
    cu.numScalarBufs(numSins)
    cu.mems.foreach(_.writePortMux.addInputs(muxSize))
    cu.genConnections
  }
}

class OuterComputeUnit(override val param:OuterComputeUnitParam=new OuterComputeUnitParam())(implicit spade:Spade) 
  extends ComputeUnit(param) {
  import spademeta._
  import param._
  override val typeStr = "ocu"
  override def config = param.config(this)

  lazy val ctrlBox:OuterCtrlBox = new OuterCtrlBox()
}
