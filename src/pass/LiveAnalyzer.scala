package pir.pass
import pir.graph._
import pir._
import pir.util._
import pir.exceptions._
import pir.util.misc._
import pir.codegen.Logger

import scala.collection.mutable.Set
import scala.collection.immutable.{Set => ISet}
import scala.collection.mutable.Map

class LiveAnalyzer(implicit design: Design) extends Pass with Logger {
  def shouldRun = true 
  import pirmeta._

  override lazy val stream = newStream(s"LiveAnalyzer.log")

  addPass {
    design.top.innerCUs.foreach { implicit cu =>
      // Uses in sram and counter
      updatesPrim(cu)
      // Write Addr Stages
      stageAnalysis(cu.wtAddrStages)(cu)
      stageAnalysis(cu.rdAddrStages)(cu)
      //val locals = empty::cu.localStages.toList
      stageAnalysis(cu.localStages)(cu)
      // Interference Graph
      infAnalysis(cu)
    }
  } 

  private def stageAnalysis(stages:List[Stage])(implicit cu:ComputeUnit) = {
    updateStages(stages)
    liveness(stages) 
    checkLiveness(stages)
    connectPRs(stages)
    checkConnection(stages)
  }

  private def updatesPrim(implicit cu:ComputeUnit) = {
    collectIn[PipeReg](cu.mems ++ cu.cchains).foreach { case PipeReg(stage, reg) => stage.addLiveOut(reg) }
  }
  
  private def updateStages(stages:List[Stage])(implicit cu:ComputeUnit) = {
    stages.zipWithIndex.foreach { case (s, i) =>
      s.fu.foreach(_.operands.foreach ( opd => addOpd(opd, s, stages) ))
      s.fu.foreach(f => addRes(f.out, i, stages))
    }
  }

  /*
   * If operand connects to:
   * 1. Current stage PipeReg: It's a Use on current stage 
   * 2. Previous stage PipeReg: It's a LiveOut from previous stage
   * 3. MemoryLoad, Counter: Operand is directly fed to ALU, no impact on liveness. But need to
   *    make sure only first stage can do so.
   * 4. Constant: Directly feed to ALU, no impact
   * */
  private def addOpd(port:InPort, stage:Stage, stages:List[Stage]) (implicit cu:ComputeUnit) = {
    (port.from.src , cu) match {
      case (pr@PipeReg(s, reg@(_:CtrPR | _:VecInPR | _:ScalarInPR | _:LoadPR)), cu) => 
        s.addLiveOut(reg) 
        stages.head.addDef(reg)
      case (pr@PipeReg(s, reg@TempPR(Some(init))), cu) => 
        s.addDef(reg)
        s.addLiveOut(reg) 
      case (pr@PipeReg(s, reg@AccumPR(init)), cu) => 
        s.addDef(reg)
        s.addLiveOut(reg) 
      case (pr@PipeReg(s, reg), cu) => 
        s.addLiveOut(reg) 
      case (pm@(_:OnChipMem| _:Counter), cu) if (stage == stages.head) =>
      case (pm:Const[_], cu) =>
      case (pm, cu) => warn(s"trying to read $pm in stage ${quote(stage)} in $cu")
    } 
  }


  private def addRes(res:OutPort, i:Int, stages:List[Stage])(implicit cu:ComputeUnit) = {
    val stage = stages(i)
    res.to.foreach { in =>
      (in, in.src) match {
        case (p:InPort, src@PipeReg(s,reg@(_:WtAddrPR | _:StorePR | _:VecOutPR | _:ScalarOutPR))) =>
          s.addDef(reg)
          stages.last.addLiveOut(reg)
        case (p:InPort, src@PipeReg(s,reg)) =>
          s.addDef(reg)
        case _ =>
      }
    }
  }

  private def liveness(stages:List[Stage]) = {
    stages.reverseIterator.foreach { stage =>
      stage.liveOuts = stage.liveOuts ++ stage.next.map { _.liveIns }.getOrElse(Nil)
      stage.liveIns = stage.liveOuts -- stage.defs
    }
  }

  private def checkLiveness(stages:List[Stage]) = {
    if (stages.nonEmpty) assert(stages.head.liveIns.isEmpty, s"${stages.head.liveIns} is not defined in the first stage=${stages.head}!")
    stages.foreach { s =>
      var diff = (s.liveIns -- s.liveOuts)
      if (diff.size!=0) {
        throw PIRException(s"ctrler: ${s.ctrler}, liveIn is not contained by liveOut! stage:${s} liveIns:${s.liveIns} liveOuts:${s.liveOuts}")
      }
    }
  }

  private def connectPRs(stages:List[Stage])(implicit cu:ComputeUnit) = {
    stages.foreach { stage =>
      stage.defs.foreach { reg =>
        val pr = stage.get(reg)
        if (stage == stages.head && !stage.fu.get.writesTo(reg)) {
          reg match {
            case CtrPR(ctr) => pr.in.connect(ctr.out) 
            case LoadPR(mem) => pr.in.connect(mem.readPort) 
            case TempPR(Some(init)) => // Initial value
            case _ => throw PIRException(s"Cannot forward reg type: ${reg}")
          }
        }
      }
      stage.liveIns.foreach { reg =>
        val pr = stage.get(reg)
        val prevStage = stage.prev.get
        pr.in.connect(prevStage.get(reg).out)
      }
      stage.liveOuts.foreach { reg =>
        val pr = stage.get(reg)
        if (stage==stages.last) {
          reg match { 
            case StorePR(sram) => sram.wtPort(pr.out)
            case VecOutPR(vecOut) => vecOut.in.connect(pr.out)
            case ScalarOutPR(scalarOut) => scalarOut.in.connect(pr.out)
            case reg if stage.uses.contains(reg) =>
            //case WtAddrPR(waPort) => waPort.connect(pr.out)
            //case RdAddrPR(raPort) => raPort.connect(pr.out)
            case _ => throw PIRException(s"Unknown live out variable ${reg} in last stage ${stage}!")
          }
        }
      }
    }
  }

  def checkConnection(stages:List[Stage])(implicit cu:ComputeUnit) = {
    stages.foreach { stage =>
      stage.liveIns.foreach { reg =>
        val pr = stage.get(reg)
        assert(pr.in.isConnected, s"liveIn ${pr}.in is not connected in $cu")
      }
      stage.liveOuts.foreach { reg =>
        val pr = stage.get(reg)
        assert(pr.out.isConnected, s"liveOut ${pr}.out is not connected in $cu")
      }
      stage.defs.foreach { reg =>
        val pr = stage.get(reg)
        reg match {
          case reg@TempPR(Some(init)) =>
          case _ => assert(pr.in.isConnected, s"Def ${pr}.in is not connected in $cu")
        }
      }
    }
  }

  // If reg:
  // if liveOut, register is enabled
  // if liveIn, register is passed through from previous reg
  // if defs but not defined by ALU, forwarding value to pipereg
  // assert false on liveOut but not liveIn and no def
  //private def connectPRs(stages:List[Stage])(implicit cu:ComputeUnit) = {
    //for (i <- 0 until stages.size) {
      //val stage = stages(i)
      //stage.liveOuts.foreach { reg =>
        //val pr = stage.get(reg)
        //if (!pr.in.isConnected) {
          //if (stage.defs.contains(reg)) {
            //if (stage == stages.head && !stage.fu.get.writesTo(reg)) {
              //reg match {
                //case CtrPR(ctr) => pr.in.connect(ctr.out) 
                //case LoadPR(mem) => pr.in.connect(mem.readPort) 
                //case TempPR(Some(init)) => // Initial value
                //case _ => throw PIRException(s"Cannot forward reg type: ${reg}")
              //}
            //}
          //} else if (stage.liveIns.contains(reg)) {
            //val pre = stages(i-1)
            //val prePr = pre.get(reg)
            //pr.in.connect(prePr)
          //} else {
            //throw PIRException(s"$pr.in is not connected but it's also not in liveIn and def of $stage in $cu")
          //} 
        //}
        //if (stage==stages.last) { // Last stage
          //if (!pr.out.isConnected) {
            //reg match {
              //case StorePR(sram) => sram.wtPort(pr.out)
              //case VecOutPR(vecOut) => vecOut.in.connect(pr.out)
              //case ScalarOutPR(scalarOut) => scalarOut.in.connect(pr.out)
              ////case WtAddrPR(waPort) => waPort.connect(pr.out)
              ////case RdAddrPR(raPort) => raPort.connect(pr.out)
              //case _ => throw PIRException(s"Unknown live out variable ${reg} in last stage ${stage}!")
            //}
          //}
        //}
      //}
      //stage.prs.foreach { case pr@PipeReg(stage, reg) =>
        //if (!stage.liveIns.contains(reg) && !stage.liveOuts.contains(reg)) {
          //stage.remove(reg)
          //dprintln(s"eliminate unused register $pr in $cu.$stage")
        //}
      //}
    //}
  //}

  private def infAnalysis(cu:ComputeUnit):Unit = {
    val stages = cu.stages
    stages.foreach { s =>
      (s.liveOuts ++ s.defs).foreach { r =>
        if (!cu.infGraph.contains(r)) cu.infGraph += (r -> Set.empty)
        // register doesn't interfere with co-def from the same source
        // e.g. FU writes to 2 registers
        val sameDefLiveOuts = s.liveOuts.filter { lo => s.get(r).in.src == s.get(lo).in.src }
        cu.infGraph(r) ++= (s.liveOuts -- sameDefLiveOuts)
      }
    }
  }

  override def finPass = {
    endInfo("Finishing Liveness Analysis")
  }

}
