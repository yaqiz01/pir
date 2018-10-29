package pir
package pass

import pir.node._
import prism.graph._

class BufferInsertion(implicit compiler:PIR) extends PIRTraversal with SiblingFirstTraversal with UnitTraversal with MemoryAnalyzer {
  val forward = false

  override def visitNode(n:N) = n match {
    case n:Context => bufferInput(n)
    case n => super.visitNode(n)
  }

}

trait MemoryAnalyzer extends PIRPass with Transformer {

  def escape(dep:N, scope:N) = dep match {
    case dep:Memory => false 
    case dep:BufferWrite => false
    case dep if dep.isDescendentOf(scope) => false
    case dep => true
  }

  def bufferInput(ctx:Context):Unit = dbgblk(s"bufferInput($ctx)"){
    ctx.descendents.foreach { deped =>
      deped.localDeps.foreach { dep => 
        if (escape(dep, ctx)) bufferInput(dep.to[PIRNode], deped.to[PIRNode])
      }
    }
  }

  def bufferInput(deped:PIRNode):Unit = {
    val depedCtx = deped.collectUp[Context]().head
    deped.localDeps.foreach { dep =>
      if (escape(dep, depedCtx)) bufferInput(dep.to[PIRNode], deped)
    }
  }

  private def bufferInput(dep:PIRNode, deped:PIRNode):Unit = dbgblk(s"bufferInput(dep=$dep, deped=$deped)"){
    val depCtx = dep.collectUp[Context]().head
    val depedCtx = deped.collectUp[Context]().head
    val (enq, deq) = compEnqDeq(dep.ctrl.get, deped.ctrl.get, false, depCtx, depedCtx)
    val write = assertOneOrLess(depCtx.collectDown[BufferWrite]().filter { write =>
      write.data.traceTo(dep) &&
      write.en.traceTo(enq)
    }, s"bufferWrite from $dep with en=$enq in $depCtx").getOrElse {
      within(depCtx, dep.ctrl.get) {
        val write = BufferWrite().data(dep).en(enq)
        dbg(s"create $write.data($dep).en($enq)")
        bufferInput(write)
        write
      }
    }
    val read = assertOneOrLess(depedCtx.collectDown[BufferRead]().filter { read =>
      read.in.traceTo(write) &&
      read.en.traceTo(deq)
    }, s"bufferRead from $write with en=$deq in $depedCtx").getOrElse {
      within(depedCtx, deped.ctrl.get) {
        val read = BufferRead(isFIFO=dep.ctrl.get == deped.ctrl.get).in(write).en(deq)
        dbg(s"create $read.in($write).en($deq)")
        bufferInput(read)
        read
      }
    }
    swapConnection(deped, dep.to[PIRNode].output.get, read.out)
  }

  def compEnqDeq(a:ControlTree, b:ControlTree, isFIFO:Boolean, actx:Context, bctx:Context):(PIRNode, PIRNode) = 
  dbgblk(s"compEnqDeq($a, $b, isFIFO=$isFIFO, actx=$actx, bctx=$bctx)"){
    if (a == b || isFIFO) {
      (ctrlValid(a, actx), ctrlValid(b, bctx))
    } else if (a.isAncestorOf(b)) {
      val bAncesstors = (b::b.ancestors)
      val idx = bAncesstors.indexOf(a)
      val ctrl = bAncesstors(idx-1).to[ControlTree]
      (ctrlValid(a, actx), ctrlDone(ctrl, bctx))
    } else if (b.isAncestorOf(a)) {
      compEnqDeq(b,a,isFIFO,bctx, actx) 
    } else {
      val lca = leastCommonAncesstor(a,b).get
      val aAncesstors = (a::a.ancestors)
      val bAncesstors = (b::b.ancestors)
      val aidx = aAncesstors.indexOf(lca)
      val bidx = bAncesstors.indexOf(lca)
      val actrl = aAncesstors(aidx-1).to[ControlTree]
      val bctrl = bAncesstors(bidx-1).to[ControlTree]
      (ctrlDone(actrl, actx), ctrlDone(bctrl, bctx))
    }
  }

  def ctrlValid(ctrl:ControlTree, ctx:Context):PIRNode = {
    // Centralized controller
    assertOne(ctrl.pnodes.get.collect { case ctrl:Controller => ctrl }, 
      s"controller for $ctrl").valid
    // Distributed controller
    //ctx.collectDown[Controller]().filter { _.ctrl.get == ctrl }.headOption.getOrElse {
    //}.valid
  }

  def ctrlDone(ctrl:ControlTree, ctx:Context):PIRNode = ctrl match {
    case ctrl if ctrl == pirTop.hostInCtrl => pirTop.hostInDone
    case ctrl if ctrl == pirTop.hostOutCtrl => pirTop.hostOutDone
    case ctrl => 
      // Centralized controller
      assertOne(ctrl.pnodes.get.collect { case ctrl:Controller => ctrl }, 
        s"controller for $ctrl").done
      // Distributed controller
    //ctx.collectDown[Controller]().filter { _.ctrl.get == ctrl }.headOption.getOrElse {
    //}.done
  }

}
