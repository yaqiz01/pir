package pir.pass
import pir.graph._
import pir.Design
import pir.Config
import pir.exceptions._

import scala.collection.mutable.Set

trait Traversal extends Pass {
  implicit def design: Design

  val visited = Set[Any]()
  override def reset {
    super.reset
    visited.clear()
  }
  
  def traverse:Unit = design.top.ctrlers.foreach(visitNode)

  addPass { traverse }

  def visitNode(node: Node) : Unit = {
    assert(!visited.contains(node), s"Revisiting visited node ${node}! visitedNodes:${visited}")
    visitNodeNoCheck(node)
    if (visited.contains(node)) return
  }
  /* Depth first search traversal on node and their fields */
  def visitNodeNoCheck(node: Node) : Unit = {
    visited += node
    node match {
      case node:Module =>
        node.ins.foreach(visitNode)
        node.outs.foreach(visitNode)
      case node =>
    }
    node match {
      case n:Controller => 
        n.sins.foreach { si => visitNode(si) }
        n.vins.foreach { vi => visitNode(vi) }
        n.souts.foreach { so => visitNode(so) }
        n.vouts.foreach { vo => visitNode(vo) }
        n match {
          case c:Top => 
          case c:ComputeUnit => {
            c.cchains.foreach { cc => visitNode(cc) }
            c.mems.foreach { s => visitNode(s) }
            c.stages.foreach { s => visitNode(s) }
          }
        } 
        visitNode(n.ctrlBox)
      case n:CounterChain => n.counters.foreach(c => visitNode(c))
      case n:Stage =>
        n.prs.foreach(visitNode)
        n.fu.foreach(visitNode)
      case n:CtrlBox =>
        n.tokenBuffers.foreach { case (dep, t) => visitNode(t) }
        n.creditBuffers.foreach { case (deped, c) => visitNode(c) }
        n.delays.foreach(visitNode)
        n.predicateUnits.foreach(visitNode)
        n.andTrees.foreach(visitNode)
      case n:SRAM =>
        visitNode(n.readAddrMux)
        visitNode(n.writeAddrMux)
        visitNode(n.writePortMux)
      case n:OnChipMem =>
        visitNode(n.writePortMux)
      case n =>
    }
  }
}
