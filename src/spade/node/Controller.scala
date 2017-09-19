package pir.spade.node

import pir.util.enums._
import pir.util.misc._
import pir.exceptions._

import pir.spade.main._
import pir.spade.simulation._
import pir.spade.util._

import scala.language.reflectiveCalls
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.Set

trait ControllerParam extends SpadeParam {
  val sbufSize:Int
  val vbufSize:Int
}

class ControllerConfig (
  val outputValid:Map[GlobalOutput[_<:PortType, Module], Output[_<:PortType, Module]]
) extends Configuration

/* Controller */
abstract class Controller(val param:ControllerParam)(implicit spade:Spade) extends Routable with Configurable {

  type CT <: ControllerConfig
  import spademeta._
  import param._

  lazy val scalarIO:ScalarIO[this.type] = ScalarIO(this)
  lazy val vectorIO:VectorIO[this.type] = VectorIO(this)
  lazy val ctrlIO:ControlIO[this.type] = ControlIO(this)

  var vbufs:List[VectorMem] = Nil
  var sbufs:List[ScalarMem] = Nil
  def bufs:List[LocalMem] = sbufs ++ vbufs
  def mems:List[OnChipMem] = sbufs ++ vbufs
  def numScalarBufs(num:Int):this.type = { sbufs = List.tabulate(num)  { i => ScalarMem(sbufSize).index(i) }; this }
  def numScalarBufs:Int = sbufs.size
  def numVecBufs(num:Int):this.type = { vbufs = List.tabulate(num) { i => VectorMem(vbufSize).index(i) }; this }
  def numVecBufs:Int = vbufs.size

  def ctrlBox:CtrlBox
  def config:Unit
}