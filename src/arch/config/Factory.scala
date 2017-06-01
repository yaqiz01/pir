package pir.plasticine.config
                          
import pir._
import pir.plasticine.graph._
import pir.plasticine.main._
import pir.plasticine.util._
import pir.codegen.Logger

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._
import pir.util.enums._

// Common configuration generator 
class ConfigFactory(implicit spade:Spade) extends Logger {

    override lazy val stream = newStream("factory.log", spade)

  // input <== output: input can be configured to output
  // input <== outputs: input can be configured to 1 of the outputs
  
  def forwardStages(cu:ComputeUnit) = cu match {
    case cu:MemoryComputeUnit => cu.wastages :+ cu.rastages.head
    case cu:OuterComputeUnit => Nil
    case cu:ComputeUnit => cu.fustages.head :: Nil 
  }

  /* Generate connections relates to register mapping of a cu */
  def genMapping(cu:ComputeUnit) = {
    implicit val spade:Spade = cu.spade
    val spademeta: SpadeMetadata = spade
    import spademeta._
    /* Register Constrain */
    // All Pipeline Registers (PipeReg) connect to its previous stage ('stage.get(reg)':Pipeline
    // Register 'reg' at stage 'stage')
    for (i <- 1 until cu.stages.size) {
      cu.regs.foreach { reg => cu.stages(i).get(reg).in <== cu.stages(i-1).get(reg).out }
    }

    // Bus input is forwarded to 1 register in empty stage
    val viRegs = cu.regs.filter(_.is(VecInReg))
    assert(cu.vins.size == cu.vbufs.size, s"cu:${cu} vins:${cu.vins.size} vbufs:${cu.vbufs.size}")
    assert(cu.vbufs.size == viRegs.size)
    (cu.vbufs, viRegs).zipped.foreach { case (vbuf, reg) =>
      forwardStages(cu).foreach { s => s.get(reg).in <== vbuf.readPort }
    }

    cu match {
      case cu:MemoryComputeUnit =>
        cu.vouts.foreach { _.ic <== cu.sram.readPort }
      case cu:ComputeUnit =>
        val voRegs = cu.regs.filter(_.is(VecOutReg))
        assert(cu.vouts.size == voRegs.size, s"cu:${cu} vouts:${cu.vouts.size} voRegs:${voRegs.size}")
        (cu.vouts, voRegs).zipped.foreach { case (vo, reg) => vo.ic <== cu.stages.last.get(reg).out }
    }

    // One to one
    val siRegs = cu.regs.filter(_.is(ScalarInReg))
    (cu.sbufs, siRegs).zipped.foreach { case (sbuf, reg) =>
      forwardStages(cu).foreach { s => s.get(reg).in <-- sbuf.readPort } // broadcast
    }

    // Xbar
    val soRegs = cu.regs.filter(_.is(ScalarOutReg))
    cu.souts.foreach { sout => soRegs.foreach { soReg => sout.ic <== (cu.stages.last.get(soReg).out, 0) } }

    // Counters can be forwarde to empty stage, writeAddr and readAddr stages 
    (cu.ctrs, cu.regs.filter(_.is(CounterReg))).zipped.foreach { case (c, reg) => 
      forwardStages(cu).foreach { s => s.get(reg).in <== c.out }
    }
    cu match {
      case cu:MemoryComputeUnit =>
        cu.srams.foreach { case sram => 
          (cu.wastages ++ cu.rastages).foreach { s =>
            sram.readAddr <== (s.fu.out, 0)
            sram.writeAddr <== (s.fu.out, 0)
          }
        }
      case _ =>
    }
  }

  def connectData(mc:MemoryController):Unit = {
    implicit val spade:Spade = mc.spade
    // Xbar
    mc.sins.foreach { sin => mc.sbufs.foreach { sbuf => sbuf.writePort <== sin.ic } }
    // One to one
    (mc.vins, mc.vbufs).zipped.foreach { case (vi, vbuf) => vbuf.writePort <== vi.ic }
  }

  /* Generate primitive connections within a CU */ 
  def connectData(cu:ComputeUnit):Unit = {
    implicit val spade:Spade = cu.spade
    val spademeta: SpadeMetadata = spade
    import spademeta._
    val top = spade.top

    cu.ctrs.foreach { c => 
      c.min <== Const().out // Counter max, min, step can be constant or scalarIn(specified later)
      c.min <== cu.sbufs.map(_.readPort)
      c.max <== Const().out
      c.max <== cu.sbufs.map(_.readPort)
      c.step <== Const().out
      c.step <== cu.sbufs.map(_.readPort)
    }
    /* Chain counters together */
    for (i <- 1 until cu.ctrs.size) { cu.ctrs(i).en <== cu.ctrs(i-1).done } 
    for (i <- 0 until cu.ctrs.size by 1) { isInnerCounter(cu.ctrs(i)) = true  } // Allow group counter in chain in multiple of 2

    cu.srams.foreach { s =>
      s.readAddr <== (cu.ctrs.map(_.out), 0) // sram read/write addr can be from all counters
      s.writeAddr <== (cu.ctrs.map(_.out), 0)
    } 
    // Xbar
    cu.sins.foreach { sin => cu.sbufs.foreach { sbuf => sbuf.writePort <== sin.ic } }
    // One to one
    (cu.vins, cu.vbufs).zipped.foreach { case (vi, vbuf) => vbuf.writePort <== vi.ic }

    /* FU Constrain  */
    cu.fustages.foreach { stage =>
      // All stage can read from any regs of its own stage, previous stage, and Const
      stage.fu.operands.foreach{ oprd =>
        oprd <-- Const().out // operand is constant
        cu.regs.foreach{ reg =>
          oprd <== stage.get(reg) // operand is from current register block
          stage.prev.foreach { oprd <== _.get(reg) }// operand is forwarded from previous register block
        }
      }
      // All stage can write to all regs of its stage
      cu.regs.foreach{ reg => stage.get(reg) <== stage.fu.out }
    }

    cu match {
      case cu:MemoryComputeUnit =>
        cu.srams.foreach { sram =>
          cu.wastages.foreach { stage => sram.writeAddr <== (stage.fu.out, 0) }
          cu.rastages.foreach { stage => sram.readAddr <== (stage.fu.out, 0) }
          sram.writePort <== cu.vbufs.map(_.readPort)
        }
      case _ =>
    }
    forwardStages(cu).foreach { stage =>
      stage.fu.operands.foreach { oprd => 
        cu.ctrs.foreach{ oprd <== _.out }
        cu.srams.foreach { oprd <== _.readPort }
        cu.vbufs.foreach { oprd <== _.readPort }
        cu.sbufs.foreach { oprd <-- _.readPort }
      }
    }

  }

  def connectAndTree(cu:Controller):Unit = {
    implicit val spade:Spade = cu.spade
    val spademeta: SpadeMetadata = spade
    import spademeta._
    (cu, cu.ctrlBox) match {
      case (cu:ComputeUnit, cb:InnerCtrlBox) => 
        cb.siblingAndTree <== cb.udcs.map(_.out)
        cb.tokenInAndTree <== cu.cins.map(_.ic)
        cb.fifoAndTree <== cu.bufs.map(_.notEmpty) 
      case (cu:OuterComputeUnit, cb:OuterCtrlBox) => 
        cb.childrenAndTree <== cb.udcs.map(_.out)
        cb.siblingAndTree <== cb.udcs.map(_.out)
      case (cu:MemoryComputeUnit, cb:MemoryCtrlBox) => 
        cb.writeFifoAndTree <== cu.bufs.map(_.notEmpty) 
        cb.readFifoAndTree <== cu.bufs.map(_.notEmpty)
      case (mc:MemoryController, cb:CtrlBox) =>
      case (top:Top, cb:TopCtrlBox) =>
    }
  }

  def connectCounters(cu:Controller):Unit = {
    implicit val spade:Spade = cu.spade
    val spademeta: SpadeMetadata = spade
    import spademeta._
    (cu, cu.ctrlBox) match {
      case (cu:ComputeUnit, cb:InnerCtrlBox) => 
        cu.ctrs.foreach { cb.doneXbar.in <== _.done }
        cu.ctrs.filter { ctr => isInnerCounter(ctr) }.map(_.en <== cb.en.out)
        cb.en.in <== cb.pipeAndTree.out // 0
        cb.en.in <== cb.streamAndTree.out // 1
      case (cu:OuterComputeUnit, cb:OuterCtrlBox) => 
        cu.ctrs.foreach { cb.doneXbar.in <== _.done }
        cu.ctrs.filter { ctr => isInnerCounter(ctr) }.map(_.en <== cb.en.out)
        cb.en.in <== cb.childrenAndTree.out
      case (cu:MemoryComputeUnit, cb:MemoryCtrlBox) => 
        cu.ctrs.foreach { cb.readDoneXbar.in <== _.done }
        cu.cins.foreach { cb.readDoneXbar.in <== _.ic }
        cu.ctrs.foreach { cb.writeDoneXbar.in <== _.done }
        cu.cins.foreach { cb.writeDoneXbar.in <== _.ic }
        cu.ctrs.filter { ctr => isInnerCounter(ctr) }.map(_.en <== cb.readEn.out)
        cu.ctrs.filter { ctr => isInnerCounter(ctr) }.map(_.en <== cb.writeEn.out)
        cb.readEn.in <== cb.readAndGate.out
        //cb.readEn.in <== cb.tokenInXbar.out
        cb.writeEn.in <== cb.writeFifoAndTree.out
      case (mc:MemoryController, cb:CtrlBox) =>
      case (top:Top, cb:TopCtrlBox) =>
    }
  }

  def connectUDCs(cu:Controller):Unit = {
    implicit val spade:Spade = cu.spade
    val spademeta: SpadeMetadata = spade
    import spademeta._
    (cu, cu.ctrlBox) match {
      case (cu:ComputeUnit, cb:InnerCtrlBox) => 
        cb.udcs.foreach { udc =>
          udc.inc <== cu.cins.map{_.ic}
          udc.dec <== cb.doneXbar.out
        }
      case (cu:OuterComputeUnit, cb:OuterCtrlBox) => 
        cb.udcs.foreach { udc =>
          udc.inc <== cu.cins.map{_.ic}
          udc.dec <== cb.childrenAndTree.out
          udc.dec <== cb.doneXbar.out
        }
      case (cu:MemoryComputeUnit, cb:MemoryCtrlBox) => 
        cb.readUDC.inc <== cu.sram.swapWrite
        cb.readUDC.dec <== cb.readDoneXbar.out
      case (mc:MemoryController, cb:CtrlBox) =>
      case (top:Top, cb:TopCtrlBox) =>
    }
  }

  def connectMemoryControl(cu:Controller):Unit = {
    implicit val spade:Spade = cu.spade
    val spademeta: SpadeMetadata = spade
    import spademeta._
    (cu, cu.ctrlBox) match {
      case (cu:MemoryComputeUnit, cb:MemoryCtrlBox) => 
        cu.bufs.foreach { buf => 
          buf.incReadPtr <== cb.readDoneXbar.out; 
          buf.incReadPtr <== cb.writeDoneXbar.out
          buf.incReadPtr <== cb.readEn.out; 
          buf.incReadPtr <== cb.writeEn.out; 
          buf.incWritePtr <== cb.writeDoneXbar.out
          buf.incWritePtr <== cu.cins.map(_.ic)
        }
        cu.srams.foreach { sram => 
          sram.incReadPtr <== cb.readDoneXbar.out 
          sram.incWritePtr <== cb.writeDoneXbar.out
          sram.writeEn <== cb.writeEn.out
        }
      case (cu:ComputeUnit, cb:InnerCtrlBox) => 
        cu.bufs.foreach { buf =>
          //TODO
          //buf.incReadPtr <== cu.ctrs.map(_.done)
          //buf.incReadPtr <== cu.cins.map(_.ic)
          buf.incReadPtr <== cb.doneXbar.out 
          buf.incReadPtr <== cb.en.out; 
        }
        cu.bufs.foreach { buf => buf.incWritePtr <== cu.cins.map(_.ic) }
      case (cu:OuterComputeUnit, cb:OuterCtrlBox) => 
        cu.bufs.foreach { buf =>
          //TODO
          //buf.incReadPtr <== cu.ctrs.map(_.done)
          //buf.incReadPtr <== cu.cins.map(_.ic)
          buf.incReadPtr <== cb.doneXbar.out 
        }
        cu.bufs.foreach { buf => buf.incWritePtr <== cu.cins.map(_.ic) }
      case (mc:MemoryController, cb:CtrlBox) =>
        mc.sbufs.foreach { buf => buf.incWritePtr <== cu.cins.map(_.ic) }
      case (top:Top, cb:TopCtrlBox) =>
    }
  }

  def connectStageControl(cu:Controller):Unit = {
    implicit val spade:Spade = cu.spade
    val spademeta: SpadeMetadata = spade
    import spademeta._
    (cu, cu.ctrlBox) match {
      case (cu:MemoryComputeUnit, cb:MemoryCtrlBox) => 
        //TODO: map enable once read and write stage can be shared
        cu.rastages.foreach { _.prs.foreach { _.en <== cb.readEn.out } }
        cu.wastages.foreach { _.prs.foreach { _.en <== cb.writeEn.out } }
      case (cu:ComputeUnit, cb:InnerCtrlBox) => 
        cu.stages.foreach { _.prs.foreach { _.en <== cb.en.out } }
      case (cu:OuterComputeUnit, cb:OuterCtrlBox) => 
      case (mc:MemoryController, cb:CtrlBox) =>
      case (top:Top, cb:TopCtrlBox) =>
    }
  }

  def connectCtrlIO(cu:Controller):Unit = {
    implicit val spade:Spade = cu.spade
    val spademeta: SpadeMetadata = spade
    import spademeta._
    (cu, cu.ctrlBox) match {
      case (cu:ComputeUnit, cb:InnerCtrlBox) => 
        cb.tokenInXbar.in <== cu.cins.map(_.ic)
        cu.couts.foreach { cout => 
          cout.ic <== cu.sbufs.map(_.notFull)
          cout.ic <== cb.doneXbar.out
          cout.ic <== cb.en.out
        }
      case (cu:OuterComputeUnit, cb:OuterCtrlBox) => 
        cb.pulserSM.done <== cb.doneXbar.out
        cb.pulserSM.en <== cb.childrenAndTree.out
        cb.pulserSM.init <== cb.siblingAndTree.out
        cu.couts.foreach { cout => 
          cout.ic <== cb.doneXbar.out
          cout.ic <== cb.pulserSM.out
          cout.ic <== cb.en.out // en.in is connected to childrenAndTree.out
          cout.ic <== cb.siblingAndTree.out
        }
      case (cu:MemoryComputeUnit, cb:MemoryCtrlBox) => 
        cb.tokenInXbar.in <== cu.cins.map(_.ic)
        cu.couts.foreach { cout => 
          cout.ic <== cu.sbufs.map(_.notFull)
          cout.ic <== cb.writeDoneXbar.out
          cout.ic <== cb.readDoneXbar.out
        }
      case (mc:MemoryController, cb:MCCtrlBox) =>
        mc.couts.foreach { cout =>
          cout.ic <== mc.sbufs.map(_.notFull)
          cout.ic <== cb.rdone
          cout.ic <== cb.wdone
        }
      case (top:Top, cb:TopCtrlBox) =>
        top.couts.foreach { _.ic <== cb.command}
        top.cins.foreach { _.ic ==> cb.status }
    }
  }

  def connectCtrl(cu:Controller):Unit = {
    connectAndTree(cu)
    connectCounters(cu)
    connectUDCs(cu)
    connectMemoryControl(cu)
    connectStageControl(cu)
    connectCtrlIO(cu)
  }

  def genConnections(pne:NetworkElement):Unit = {
    pne match {
      case pne:Top =>
        connectCtrl(pne)
      case pne:ComputeUnit => 
        connectData(pne)
        genMapping(pne)
        connectCtrl(pne)
      case pne:MemoryController =>
        connectData(pne)
        connectCtrl(pne)
      case pne:SwitchBox => 
        pne.connectXbars
    }
  }

}
