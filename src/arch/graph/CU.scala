package pir.plasticine.graph

import pir.util.enums._
import pir.util.misc._
import pir.plasticine.main._
import pir.plasticine.config.ConfigFactory
import pir.plasticine.simulation._
import pir.plasticine.util._
import pir.exceptions._

import scala.language.reflectiveCalls
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.Set

trait ComputeUnitParam extends ControllerParam {
  val numRegs:Int
  val numCtrs:Int
  val numSRAMs:Int
  val sramSize:Int
  val numUDCs:Int
  val numLanes:Int
}
/*
 * ComputeUnit
 * */
abstract class ComputeUnit(param:ComputeUnitParam)(implicit spade:Spade) extends Controller(param) {
  import spademeta._
  import param._
  //override implicit val ctrler:ComputeUnit = this 
  val regs:List[ArchReg] = List.tabulate(numRegs) { ir => ArchReg().index(ir) }
  val srams:List[SRAM] = List.tabulate(numSRAMs) { i => SRAM(sramSize).index(i) }
  val ctrs:List[Counter] = List.tabulate(numCtrs) { i => Counter().index(i) }
  //var sbufs:List[ScalarMem] = Nil // in Controller
  def mems:List[OnChipMem] = srams ++ sbufs ++ vbufs

  lazy val ctrlBox:CtrlBox = new InnerCtrlBox(numUDCs)
  def vout = vouts.head

  protected val _regstages:ListBuffer[FUStage] = ListBuffer.empty  // Regular Stages
  protected val _rdstages:ListBuffer[ReduceStage] = ListBuffer.empty // Reduction Stages
  protected val _fustages:ListBuffer[FUStage] = ListBuffer.empty // Function Unit Stages
  protected val _stages:ListBuffer[Stage] = ListBuffer.empty // All stages

  def regstages:List[FUStage] = _regstages.toList // Regular Stages
  def rdstages:List[ReduceStage] = _rdstages.toList // Reduction Stages
  def fustages:List[FUStage] = _fustages.toList // Function Unit Stages
  def stages:List[Stage] = _stages.toList // All stages

  def addRegstages(stages:List[FUStage]) = { _regstages ++= stages; _fustages ++= stages; addStages(stages) }
  def addRdstages(stages:List[ReduceStage]) = { _rdstages ++= stages; _fustages ++= stages; addStages(stages) }

  protected def addStages(sts:List[Stage]) = {
    sts.zipWithIndex.foreach { case (stage, i) =>
      stage.index(stages.size)
      if (stages.nonEmpty) {
        stage.prev = Some(stages.last)
        stage.prev.get.next = Some(stage)
      }
      _stages += stage
    }
  }

  def addRegstages(numStage:Int, numOprds:Int, ops:List[Op]):this.type = { 
    addRegstages(List.fill(numStage) { FUStage(numOprds=numOprds, regs, ops) }); this // Regular stages
  }
  def addRdstages(numStage:Int, numOprds:Int, ops:List[Op]):this.type = {
    addRdstages(List.fill(numStage) { ReduceStage(numOprds=numOprds, regs, ops)}); this // Reduction stage 
  } 

  def color(range:Range, color:RegColor):this.type = { range.foreach { i => regs(i).color(color) }; this }
  def color(i:Int, color:RegColor):this.type = { regs(i).color(color); this }

  override def register(implicit sim:Simulator):Unit = {
    import sim.util._
    // Add delay to output if input is from doneXBar
    clmap.pmap.get(this).foreach { cu =>
      ctrlBox match {
        case cb:InnerCtrlBox =>
        case _ =>
      }
      val enable = ctrlBox match {
        case cb:MemoryCtrlBox => Some(cb.readDelay.out.v)
        case cb:InnerCtrlBox => Some(cb.enDelay.out.v)
        case _ => None
      }
      val outs:List[GlobalOutput[Bus, ComputeUnit]] = (souts++vouts)
      outs.foreach { out =>
        fimap.get(out.ic).fold {
          if (out.ic.fanIns.size==1) {
            out.ic.v.set { v =>
              v <<= out.ic.fanIns.head.v
              v.valid <<= enable.get
            }
          }
        } { pout => 
          out.ic.v.set { v =>
            v <<= pout.v
            v.valid <<= enable.get
          }
        }
      }
    }
    super.register
  }

}

