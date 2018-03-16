package pir.codegen
import pir._
import pir.util._
import pir.pass._
import pir.node._

import prism._
import prism.util._
import prism.traversal._
import prism.codegen._

import sys.process._
import scala.language.postfixOps
import scala.collection.mutable


class ControllerDotCodegen(val fileName:String)(implicit compiler:PIR) extends PIRPass with ChildFirstTraversal with IRDotCodegen {

  import pirmeta._

  type N = Controller

  //def shape(attr:DotAttr, n:Any) = attr.shape(box)

  override def color(attr:DotAttr, n:Any) = n match {
    case n:SRAM => attr.fillcolor(orange).style(filled)
    case n:RetimingFIFO => attr.fillcolor(gold).style(filled)
    case n:FIFO => attr.fillcolor(gold).style(filled)
    case n:StreamIn => attr.fillcolor(gold).style(filled)
    case n:StreamOut => attr.fillcolor(gold).style(filled)
    case n:Reg => attr.fillcolor(limegreen).style(filled)
    case n:ArgIn => attr.fillcolor(limegreen).style(filled)
    case n:ArgOut => attr.fillcolor(limegreen).style(filled)
    case n:Controller if n.children.nonEmpty => attr.style(dashed)
    case n => super.color(attr, n)
  }

  override def emitSubGraph(n:N)(block: => Unit):Unit = {
    var attr = DotAttr()
    attr = label(attr, n)
    emitSubGraph(n, attr) { 
      emitSingleNode(n)
      block
    }
  }

  override def emitSingleNode(n:N):Unit = {
    ctrlOf.bmap(n).foreach {
      case mem:RetimingFIFO =>
      case mem:Memory => emitSingleNode(mem)
      case _ =>
    }
    super.emitSingleNode(n)
  }

  override def runPass = {
    traverseNode(compiler.top.topController)
  }

  override def emitEdges = {
    val mems = compiler.top.collectDown[Memory]()
    mems.foreach { 
      case mem:ArgIn =>
        readersOf(mem).foreach { reader => emitEdge(mem, ctrlOf(reader)) }
      case mem:ArgOut =>
        writersOf(mem).foreach { writer => emitEdge(ctrlOf(writer), mem) }
      case mem:RetimingFIFO =>
      case mem =>
        readersOf(mem).foreach { reader => emitEdge(mem, ctrlOf(reader)) }
        writersOf(mem).foreach { writer => emitEdge(ctrlOf(writer), mem) }
    }
  }

  override def quote(n:Any):String = qtype(n)

}
