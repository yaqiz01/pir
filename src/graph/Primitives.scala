package pir.graph

import scala.collection.mutable.Set
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.Map
import scala.math.max
import pir.Design
import pir.graph._


trait Primitive extends Node {
  var ctrler:Controller = _
  def updateCtrler(c:Controller) = ctrler = c
} 
/** Counter node. Represents a chain of counters where each counter counts upto a certain max value. When
 *  the topmost counter reaches its maximum value, the entire counter chain ''saturates'' and does not
 *  wrap around.
 *  @param maxNStride: An iterable [[scala.Seq]] tuple of zero or more values.
 *  Each tuple represents the (max, stride) for one level in the loop.
 *  Maximum values and strides are specified in the order of topmost to bottommost counter.
 */
case class CounterChain(name:Option[String])(implicit design: Design) extends Primitive {
  override val typeStr = "CC"
  /* Fields */
  var counters:List[Counter] = Nil 
  /* Pointers */
  var dep:Option[CounterChain] = None
  var copy:Option[CounterChain] = None
  toUpdate = true

  override def updateCtrler(c:Controller) = {super.updateCtrler(c); counters.foreach(_.updateCtrler(c))} 
  def apply(i: Int)(implicit design: Design):Counter = {
    if (counters.size == 0) {
      // Speculatively create counters base on need and check index out of bound during update
      this.counters = (0 to i).map { j => Counter() }.toList
    }
    counters(i)
  }
  def copy(cp:CounterChain):Unit = {
    // Check whether speculative wire allocation was correct
    assert(counters.size <= cp.counters.size, 
      s"Accessed counter ${counters.size-1} of ${this} is out of bound")
    val addiCtrs = (counters.size until cp.counters.size).map {i => Counter()}
    counters = counters ++ addiCtrs
    counters.zipWithIndex.foreach { case(c,i) => 
      c.copy(cp.counters(i))
      c.ctrler = this.ctrler
    }
    this.copy = Some(cp)
    toUpdate = false
  }
  def update(bds: Seq[(Port, Port, Port)]):Unit = {
    counters = bds.zipWithIndex.map {case ((mi, ma, s),i) => Counter(mi, ma, s)}.toList
    this.copy = None 
    toUpdate = false
  }
}
object CounterChain {
  def apply(bds: (Port, Port, Port)*)(implicit design: Design):CounterChain =
    {val c = CounterChain(None); c.update(bds); c}
  def apply(name:String, bds: (Port, Port, Port)*)(implicit design: Design):CounterChain =
    {val c = CounterChain(Some(name)); c.update(bds); c}
  def copy(from:String, name:String) (implicit design: Design):CounterChain = {
    val cc = CounterChain(Some(s"${from}_${name}_copy"))
    def updateFunc(cp:Node) = cc.copy(cp.asInstanceOf[CounterChain])
    design.updateLater(s"${from}_${name}", updateFunc)
    cc
  }
  def copy(from:Controller, name:String) (implicit design: Design):CounterChain = {
    copy(from.toString, name)
  }
}

case class Counter(val name:Option[String])(implicit design: Design) extends Primitive {
  override val typeStr = "Ctr"
  /* Fields */
  var min:Port = _
  var max:Port = _
  var step:Port = _
  val out:Port = Port(this, {s"${this}.out"}) 
  toUpdate = true

  def update(mi:Port, ma:Port, s:Port):Unit = {
    min = mi
    max  = ma
    step = s
    toUpdate = false
  }
  def copy(c:Counter) = {
    assert(min==null, 
      s"Overriding existing counter ${this} with min ${min}")
    assert(max==null, 
      s"Overriding existing counter ${this} with min ${max}")
    assert(step==null, 
      s"Overriding existing counter ${this} with min ${step}")
    def copyPort(p:Port):Port = {
      if (p.isConst) {
        p
      } else p.src match {
        case s@ScalarIn(n, scalar) => s.writer match {
          case Right(w) => new ScalarIn(n, scalar, w).out 
          case Left(w) => new ScalarIn(n, scalar, w).out
        }
        case _ => throw new Exception(s"Don't know how to copy port")
      }
    }
    update(copyPort(c.min), copyPort(c.max), copyPort(c.step))
  } 
}
object Counter{
  def apply(min:Port, max:Port, step:Port)(implicit design: Design):Counter =
    { val c = Counter(None); c.update(min, max, step); c }
  def apply(name:String, min:Port, max:Port, step:Port)(implicit design: Design):Counter =
    { val c = Counter(Some(name)); c.update(min, max, step); c }
  def apply()(implicit design: Design):Counter = Counter(None)
}

/** SRAM 
 *  @param nameStr: user defined name of SRAM 
 *  @param Size: size of SRAM in all dimensions 
 */
case class SRAM(name: Option[String], size: Int, writer:Controller)(implicit design: Design) 
  extends Primitive {
  override val typeStr = "SRAM"

  var readAddr: Port = _
  var writeAddr: Port = _
  val readPort: Port = Port(this, s"${this}.rp") 

  toUpdate = true
  def update (ra:Port, wa:Port) = {
    this.readAddr = ra
    this.writeAddr = wa
    toUpdate = false
  }
  def load = readPort
}
object SRAM {
  def apply(size:Int, write:Controller)(implicit design: Design): SRAM
    = SRAM(None, size, write)
  def apply(name:String, size:Int, write:Controller)(implicit design: Design): SRAM
    = SRAM(Some(name), size, write)
  def apply(size:Int, write:Controller, readAddr:Port, writeAddr:Port)(implicit design: Design): SRAM
    = { val s = SRAM(None, size, write); s.update(readAddr, writeAddr); s } 
  def apply(name:String, size:Int, write:Controller, readAddr:Port, writeAddr:Port)(implicit design: Design): SRAM
    = { val s = SRAM(Some(name), size, write); s.update(readAddr, writeAddr); s } 
}

case class ScalarIn(name: Option[String], scalar:Scalar)(implicit design: Design) 
  extends Primitive  {
  override val typeStr = "ScalarIn"
  var writer:Either[String,Controller] = _
  toUpdate = true
  val out:Port = Port(this, {s"${this}.out"}) 

  def this(n: Option[String], scalar:Scalar, w:Controller)(implicit design: Design) = {
    this(n, scalar)
    update(w)
  }
  
  def this(n: Option[String], scalar:Scalar, w:String)(implicit design: Design) = {
    this(n, scalar)
    design.updateLater(s"${w}", update _)
  }

  def update(n:Node) = {writer = Right(n.asInstanceOf[Controller]); toUpdate = false}
}
object ScalarIn {
  def apply(scalar:Scalar, writer:String)(implicit design:Design):ScalarIn = { 
    new ScalarIn(None, scalar, writer)
  }
  def apply(scalar:ArgIn)(implicit design:Design):ScalarIn = { 
    ScalarIn(scalar, "Top")
  }
  def apply(scalar:Scalar, writer:Controller)(implicit design:Design):ScalarIn = 
    new ScalarIn(None, scalar, writer)
  def apply(name:String, scalar:Scalar, writer:Controller)(implicit design:Design):ScalarIn = 
    new ScalarIn(Some(name), scalar, writer)
}

case class ScalarOut(name: Option[String], scalar:Scalar)(implicit design: Design) extends Primitive {
  override val typeStr = "ScalarOut"
}
object ScalarOut {
  //TODO check argout
  def apply(scalar:Scalar)(implicit design:Design):ScalarOut = 
    ScalarOut(None, scalar)
  def apply(name:String, scalar:Scalar)(implicit design:Design):ScalarOut = 
    ScalarOut(Some(name), scalar)
}

case class Stage(name:Option[String], pipeline:Pipeline)(implicit design: Design) extends Primitive {
  override val typeStr = "ScalarOut"
  var operands:List[Port] = _
  var op:Op = _
  var result:Port = _
} 
object Stage {
  /* No Sugar API */
  def apply(stage:Stage, opds:List[Port], o:Op, r:Port, prm:Pipeline)
    (implicit design: Design):Unit= {
    stage.operands = opds
    stage.op = o
    stage.result = r 
    prm.addStage(stage)
  }
  /* Sugar API */
  def apply(stage:Stage, op1:Port, op:Op, result:Port)
           (implicit prm:Pipeline, design: Design):Unit =
    Stage(stage, List(op1), op, result, prm)
  def apply(stage:Stage, op1:Port, op2:Port, op:Op, result:Port)
           (implicit prm:Pipeline, design: Design):Unit = 
    Stage(stage, List(op1, op2), op, result, prm)
  def apply(stage:Stage, op1:Port, op2:Port, op3:Port, op:Op, result:Port)
           (implicit prm:Pipeline, design: Design):Unit =
    Stage(stage, List(op1, op2, op3), op, result, prm)
  //TODO
  def reduce(stage:Stage, op:Op) (implicit prm:Pipeline, design: Design):Unit = {
    Stage(stage, List(prm.reduce(stage).read), op, prm.reduce(stage).read, prm)
  }

}
object Stages {
  def apply(n:Int) (implicit prm:Pipeline, design: Design):List[Stage] = {
    List.tabulate(n) {i => 
      val s = Stage(None, prm)
      prm.stageUses += (s -> Set[PipeReg]())
      prm.stageDefs += (s -> Set[PipeReg]())
      prm.stagePRs += (s -> HashMap[Int, PipeReg]())
      s
    }
  }
}

case class Pipeline(name:Option[String])(implicit design: Design) extends Primitive {
  override val typeStr = "Pipeline"
  var regId = 0
  private def newTemp = {val temp = regId; regId +=1; temp}

  /* Fields */
  val stages = ListBuffer[Stage]()
  override def updateCtrler(c:Controller) ={super.updateCtrler(c);stages.foreach(_.updateCtrler(c))} 

  /* Register Mapping */
  val reduceReg = newTemp
  val vecIn = newTemp
  val vecOut = newTemp
  val scalarIns = HashMap[Scalar, Int]() 
  val scalarOuts = HashMap[Scalar, Int]() 
  val loadRegs  = HashMap[SRAM, Int]()
  val storeRegs  = HashMap[SRAM, Int]()
  val ctrRegs   = HashMap[Counter, Int]()
  val tempRegs  = ListBuffer[Int]()

  val stageUses = HashMap[Stage, Set[PipeReg]]()
  val stageDefs = HashMap[Stage, Set[PipeReg]]()
  val stagePRs  = HashMap[Stage, HashMap[Int,PipeReg]]()
  def reset     = { regId = 0; loadRegs.clear; storeRegs.clear; ctrRegs.clear; stageUses.clear; stageDefs.clear }

  def addStage(s:Stage):Unit = {
    stages += s
    s.operands.foreach { opd => opd.src match {
        case pr:PipeReg => addUse(pr)
        case _ =>
      } 
    }
    s.result.src match {
      case pr:PipeReg => addDef(pr)
      case _ =>
    }
  }
  private def addUse(p:PipeReg) = stageUses(p.stage) += p
  private def addDef(p:PipeReg) = stageDefs(p.stage) += p

 /** Create a pipeline register for a stage corresponding to 
  *  the register that loads from the sram
  * @param stage: Stage for the pipeline register 
  * @param s: sram to load from 
  */
 def load(stage:Stage, s:SRAM):PipeReg = {
    if (!loadRegs.contains(s)) loadRegs += (s -> newTemp)
    val prs = stagePRs(stage); val rid = loadRegs(s)
    if (!prs.contains(rid)) prs += (rid -> new PipeReg(stage, rid) with LoadPR)
    prs(rid)
  }
 /** Create a pipeline register for a stage corresponding to 
  *  the register that stores to the sram
  * @param stage: Stage for the pipeline register 
  * @param s: sram to load from 
  */
  def stores(stage:Stage, s:SRAM):PipeReg = {
    if (!storeRegs.contains(s)) storeRegs += (s -> newTemp)
    val prs = stagePRs(stage); val rid = storeRegs(s)
    if (!prs.contains(rid)) prs += (rid -> new PipeReg(stage, rid) with StorePR)
    prs(rid)
  }
 /** Create a pipeline register for a stage corresponding to 
  *  the register that connects to the counter 
  * @param stage: Stage for the pipeline register 
  * @param c: counter 
  */
  def ctr(stage:Stage, c:Counter):PipeReg = {
    if (!ctrRegs.contains(c)) ctrRegs += (c -> newTemp)
    val prs = stagePRs(stage); val rid = ctrRegs(c)
    if (!prs.contains(rid)) prs += (rid -> new PipeReg(stage, rid) with CtrPR)
    prs(rid)
  }
 /** Create a pipeline register for a stage corresponding to 
  *  the register that connects to the reduction network 
  * @param stage: Stage for the pipeline register 
  */
  def reduce(stage:Stage):PipeReg = {
    val prs = stagePRs(stage); val rid = reduceReg
    if (!prs.contains(rid)) prs += (rid -> new PipeReg(stage, rid) with ReducePR)
    prs(rid)
  }
 /** Create a pipeline register for a stage corresponding to 
  *  the register that connects to 1 scalarIn buffer 
  * @param stage: Stage for the pipeline register 
  */
  //def scalarIn(stage:Stage):PipeReg = {
  //  val rid = newTemp; scalarIns += rid 
  //  val prs = stagePRs(stage)
  //  if (!prs.contains(rid)) prs += (rid -> new PipeReg(stage, rid) with ScalarInPR)
  //  prs(rid)
  //}
 /** Create a pipeline register for a stage corresponding to 
  *  the register that connects to the scalarIn buffer with register rid
  * @param stage: Stage for the pipeline register 
  * @param rid: reg rid of scalar input 
  */
  def scalarIn(stage:Stage, s:Scalar):PipeReg = {
    val rid = if (!scalarIns.contains(s)) { val i = newTemp; scalarIns += (s -> i); i }
              else scalarIns(s)
    val prs = stagePRs(stage)
    if (!prs.contains(rid)) 
      prs += (rid -> new { override val scalar = s} with PipeReg(stage, rid) with ScalarInPR)
    prs(rid)
  }
 /** Create a pipeline register for a stage corresponding to 
  *  the register that connects to 1 scalarOut buffer 
  * @param stage: Stage for the pipeline register 
  */
  //def scalarOut(stage:Stage):PipeReg = {
  //  val rid = newTemp; scalarOuts += rid 
  //  val prs = stagePRs(stage)
  //  if (!prs.contains(rid)) prs += (rid -> new PipeReg(stage, rid) with ScalarOutPR)
  //  prs(rid)
  //}
 /** Create a pipeline register for a stage corresponding to 
  *  the register that connects to the scalarOut buffer with register rid
  * @param stage: Stage for the pipeline register 
  * @param rid: reg rid of scalar input 
  */
  def scalarOut(stage:Stage, s:Scalar):PipeReg = {
    val rid = if (!scalarOuts.contains(s)) { val i = newTemp; scalarOuts += (s -> i); i} 
              else scalarOuts(s)
    val prs = stagePRs(stage)
    if (!prs.contains(rid)) 
      prs += (rid -> new { override val scalar = s } with PipeReg(stage, rid) with ScalarOutPR)
    prs(rid)
  }
 /** Create a pipeline register for a stage corresponding to 
  *  the register that directly connects to CU input ports in streaming communication 
  * @param stage: Stage for the pipeline register 
  */
  def vecIn(stage:Stage):PipeReg = {
    val prs = stagePRs(stage); val rid = vecIn
    if (!prs.contains(rid)) prs += (rid -> new PipeReg(stage, rid) with VecInPR)
    prs(rid)
  }
 /** Create a pipeline register for a stage corresponding to 
  *  the register that directly connects to CU output ports 
  * @param stage: Stage for the pipeline register 
  */
  def vecOut(stage:Stage):PipeReg = {
    val prs = stagePRs(stage); val rid = vecOut 
    if (!prs.contains(rid)) prs += (rid -> new PipeReg(stage, rid) with VecOutPR)
    prs(rid)
  }
  def temp = newTemp
 /** Get the pipeline register for stage with rid 
  * @param stage: Stage for the pipeline register 
  */
  def temp(stage:Stage, rid:Int):PipeReg = {
    val prs = stagePRs(stage)
    if (!prs.contains(rid)) prs += (rid -> new PipeReg(stage, rid) with TempPR)
    prs(rid)
  }
 /** Allocate a new pipeline register in the stage 
  * @param stage: Stage for the pipeline register 
  */
  def temp(stage:Stage):PipeReg = {
    val prs = stagePRs(stage); val rid = newTemp
    if (!prs.contains(rid)) prs += (rid -> new PipeReg(stage, rid) with TempPR)
    prs(rid)
  }

}

trait Reg extends Primitive {
  var in:Option[Port] = None
  val out:Port = Port(this, {s"${this}"}) 
  def read:Port = out
}

/*
 * A Pipeline Register keeping track of which stage (column) and which logical register (row)
 * the PR belongs to
 * @param n Optional user defined name
 * @param regId Register ID the PipeReg mapped to
 **/
case class PipeReg(name:Option[String], stage:Stage, regId:Int)(implicit design: Design) extends Reg{
  override val typeStr = "PR"
  override def toString = s"${super.toString}_${stage.name.getOrElse("")}${regId}"
  def this (stage:Stage, regId:Int)(implicit design:Design) = this(None, stage, regId)
}
trait LoadPR      extends PipeReg {override val typeStr = "PRld"}
trait StorePR     extends PipeReg {override val typeStr = "PRst"}
trait CtrPR       extends PipeReg {override val typeStr = "PRct"}
trait ReducePR    extends PipeReg {override val typeStr = "PRrd"}
trait VecInPR     extends PipeReg {override val typeStr = "PRvi"}
trait VecOutPR    extends PipeReg {override val typeStr = "PRvo"}
trait ScalarInPR  extends PipeReg {override val typeStr = "PRsi"; val scalar:Scalar }
trait ScalarOutPR extends PipeReg {override val typeStr = "PRso"; val scalar:Scalar }
trait TempPR      extends PipeReg {override val typeStr = "PRtp"}

/* Register declared outside CU for communication between two CU. Only a symbol to keep track of
 * the scalar value, not a real register */
case class Scalar(val name:Option[String])(implicit design: Design) extends Node {
  override val typeStr = "Scalar"
  var writer:Controller = _ //TODO: need to keep track of these
  val reader:Set[Controller] = Set[Controller]() 
  toUpdate = false //TODO 
}
object Scalar {
  def apply(name:String)(implicit design: Design):Scalar = Scalar(Some(name)) 
  def apply()(implicit design: Design):Scalar = Scalar(None) 
}

trait ArgIn extends Scalar {
  override val typeStr = "ArgIn"
  toUpdate = false
}
object ArgIn {
  def apply(n:Option[String])(implicit design: Design):ArgIn = new Scalar(n) with ArgIn
  def apply() (implicit design: Design):ArgIn = ArgIn(None)
  def apply(name:String) (implicit design: Design):ArgIn = ArgIn(Some(name))
}

/** Scalar values passed from accelerator to host CPU code via memory-mapped registers.
 *  Represent scalar outputs from the accelerator, and are write-only from accelerator.
 */
trait ArgOut extends Scalar {
  override val typeStr = "ArgOut"
  toUpdate = false
} 
object ArgOut {
  def apply(n:Option[String])(implicit design: Design) = new Scalar(n) with ArgOut
  def apply() (implicit design: Design):ArgOut = ArgOut(None)
  def apply(name:String) (implicit design: Design):ArgOut = ArgOut(Some(name))
}
