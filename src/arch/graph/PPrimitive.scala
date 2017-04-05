package pir.plasticine.graph

import pir.graph._
import pir.util.enums._
import pir.plasticine.main._
import pir.plasticine.util._

import scala.language.reflectiveCalls
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.collection.mutable.Set

trait Primitive extends Module {
  def ctrler:Controller
}

/** Physical SRAM 
 *  @param numPort: number of banks. Usually equals to number of lanes in CU */
case class SRAM()(implicit spade:Spade, val ctrler:ComputeUnit) extends Primitive {
  import spademeta._
  override val typeStr = "sram"
  override def toString =s"${super.toString}${indexOf.get(this).fold(""){idx=>s"[$idx]"}}"
  val readPort = Output(Word(), this, s"${this}.rp")
  val writePort = Input(Word(), this, s"${this}.wp")
  val readAddr = Input(Word(), this, s"${this}.ra")
  val writeAddr = Input(Word(), this, s"${this}.wa")
}

/** Physical Counter  */
case class Counter()(implicit spade:Spade, val ctrler:ComputeUnit) extends Primitive {
  import spademeta._
  override val typeStr = "ctr"
  override def toString =s"${super.toString}${indexOf.get(this).fold(""){idx=>s"[$idx]"}}"
  val min = Input(Word(), this, s"${this}.min")
  val max = Input(Word(), this, s"${this}.max")
  val step = Input(Word(), this, s"${this}.step")
  val out = Output(Word(), this, s"${this}.out")
  val en = Input(Bit(), this, s"${this}.en")
  val done = Output(Bit(), this, s"${this}.done")
  def isDep(c:Counter) = en.canConnect(c.done)
}

/* Phyiscal pipeline register */
case class PipeReg(stage:Stage, reg:ArchReg)(implicit spade:Spade, val ctrler:Controller) extends Primitive {
  import spademeta._
  override val typeStr = "pr"
  override def toString = s"pr(${quote(stage)},${quote(reg)})"
  val in = Input(Word(), this, s"$this.i")
  val out = Output(Word(), this, s"${this}.out")
}

/* Scalar Buffer between the bus inputs/outputs and first/last stage */
class LocalBuffer(implicit spade:Spade, val ctrler:Controller) extends Primitive {
  import spademeta._
  val in:Input[Word, LocalBuffer] = Input(Word(), this, s"${this}.i") 
  val out:Output[Word, LocalBuffer] = Output(Word(), this, s"${this}.o")
} 

trait ScalarBuffer extends LocalBuffer
trait VectorBuffer extends LocalBuffer
/* Scalar buffer between bus input and the empty stage. (Is an IR but doesn't physically 
 * exist). Input connects to 1 out port of the InBus */
case class ScalarIn()(implicit spade:Spade, ctrler:Controller) extends ScalarBuffer {
  import spademeta._
  override val typeStr = "si"
} 
/* Scalar buffer between the last stage and the bus output. Output connects to 1 in port 
 * of the OutBus */
class ScalarOut()(implicit spade:Spade, ctrler:Controller) extends ScalarBuffer {
  import spademeta._
  override val typeStr = "so"
}
object ScalarOut {
  def apply()(implicit spade:Spade, ctrler:Controller):ScalarOut = new ScalarOut()
}
/* Vector buffer between bus input and the empty stage. (Is an IR but doesn't physically 
 * exist). Input connects to 1 out port of the InBus */
case class VectorIn()(implicit spade:Spade, ctrler:Controller) extends VectorBuffer { override val typeStr = "vi" } 
case class VectorOut()(implicit spade:Spade, ctrler:Controller) extends VectorBuffer { override val typeStr = "vo" } 
/* VectorOut of TileTransfer CU, whos AddrOut has dedicated scalar network that goes to
 * Memory Controller */
trait AddrOut extends ScalarOut {
  override val typeStr = "ado"
}
object AddrOut {
  def apply()(implicit spade:Spade, ctrler:Controller) = new AddrOut {}
}

/* Function unit. 
 * @param numOprds number of operands
 * @param ops List of supported ops
 * @param stage which stage the FU locates
 * */
case class FuncUnit(numOprds:Int, ops:List[Op], stage:Stage)(implicit spade:Spade, val ctrler:Controller) extends Primitive {
  import spademeta._
  override val typeStr = "fu"
  val operands = List.fill(numOprds) (Input(Word(), this, s"$this.oprd${id}")) 
  val out = Output(Word(), this, s"$this.out")
}

/*
 * Phyical stage. 1 column of FU and Pipeline Register block accross lanes. 
 * @param reg Logical registers in current register block
 * */
class Stage(regs:List[ArchReg])(implicit spade:Spade, val ctrler:Controller) extends Primitive {
  import spademeta._
  val funcUnit:Option[FuncUnit] = None
  val _prs = Map[ArchReg, PipeReg]() // Mapping between logical register and physical register
  regs.foreach { reg => _prs += (reg -> PipeReg(this, reg)) }
  def prs:List[PipeReg] = regs.map { r => _prs(r) }
  def get(reg:ArchReg):PipeReg = _prs(reg)
  var pre:Option[Stage] = None // changed in addStage in PController
  var next:Option[Stage] = None 
  def isLast = next.isEmpty
  def isHead = pre.isEmpty
  def before(s:Stage) = indexOf(this) < indexOf(s)
  def after(s:Stage) = indexOf(this) > indexOf(s)
  override val typeStr = "st"
}
/* Dummy stage that only has register block */
case class EmptyStage(regs:List[ArchReg])(implicit spade:Spade, ctrler:Controller) extends Stage(regs) {
  override val typeStr = "etst"
}
/* Stage with Function unit */
trait FUStage extends Stage {
  def fu:FuncUnit = funcUnit.get 
}
object FUStage {
  def apply(numOprds:Int, regs:List[ArchReg], ops:List[Op])(implicit spade:Spade, ctrler:Controller):FUStage = 
    new Stage(regs) with FUStage { override val funcUnit = Some(FuncUnit(numOprds, ops, this)) }
}
/* Reduction stage */
trait ReduceStage extends FUStage {
  override val typeStr = "rdst"
}
object ReduceStage {
  /*
   * Create a list of reduction stages
   * @param numOprds number of operand
   * @param regs list of logical registers in the stage
   * @param ops reduction operations
   * */
  def apply(numOprds:Int, regs:List[ArchReg], ops:List[Op])(implicit spade:Spade, ctrler:Controller):ReduceStage = 
    new Stage(regs) with ReduceStage { override val funcUnit = Some(FuncUnit(numOprds, ops, this)) }
}
/* WriteAddr calculation stage */
trait WAStage extends FUStage {
  override val typeStr = "wast"
}
object WAStage {
  def apply(numOprds:Int, regs:List[ArchReg], ops:List[Op])(implicit spade:Spade, ctrler:Controller):WAStage = 
    new Stage(regs) with WAStage { override val funcUnit = Some(FuncUnit(numOprds, ops, this)) }
}

class Const()(implicit spade:Spade) extends Module {
  val out = Output(Word(), this, s"Const")
}
object Const {
  def apply()(implicit spade:Spade) = new Const()
}

abstract class LUT(implicit spade:Spade, val ctrler:Controller) extends Node {
  val numIns:Int
}
case class EnLUT(numIns:Int)(implicit spade:Spade, ctrler:Controller) extends LUT {
  import spademeta._
  override val typeStr = "enlut"
  override def toString =s"${super.toString}${indexOf.get(this).fold(""){idx=>s"[$idx]"}}"
}
case class TokenOutLUT()(implicit spade:Spade, ctrler:Controller) extends LUT{
  import spademeta._
  override val typeStr = "tolut"
  override def toString =s"${super.toString}${indexOf.get(this).fold(""){idx=>s"[$idx]"}}"
  override val numIns = 2 // Token out is a combination of two output
}
case class TokenDownLUT(numIns:Int)(implicit spade:Spade, ctrler:Controller) extends LUT {
  override val typeStr = "tdlut"
}
object TokenDownLUT {
  def apply(idx:Int, numIns:Int)(implicit spade:Spade, ctrler:Controller):TokenDownLUT = 
    TokenDownLUT(numIns).index(idx)
}
case class UDCounter()(implicit spade:Spade, val ctrler:Controller) extends Primitive {
  import spademeta._
  override val typeStr = "udlut"
  override def toString =s"${super.toString}${indexOf.get(this).fold(""){idx=>s"[$idx]"}}"
  //val init = Input(Word(), this, s"${this}.init")
  //val inc = Input(Word(), this, s"${this}.inc")
  //val dec = Input(Word(), this, s"${this}.dec")
  //val out = Output(Word(), this, s"${this}.out")
}
object UDCounter {
  def apply(idx:Int)(implicit spade:Spade, ctrler:Controller):UDCounter = UDCounter().index(idx)
}

class CtrlBox(numUDCs:Int) (implicit spade:Spade, val ctrler:Controller) extends Primitive {
  import spademeta._
  val udcs = List.tabulate(numUDCs) { i => UDCounter(i) }
}

