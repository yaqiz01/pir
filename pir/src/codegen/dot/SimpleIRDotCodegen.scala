package pir.codegen
import pir._
import pir.util._
import pir.pass._
import pir.node._

import prism._
import prism.util._
import prism.node._
import prism.traversal._
import prism.codegen._

import sys.process._
import scala.language.postfixOps
import scala.collection.mutable

class SimpleIRDotCodegen(override val fileName:String)(implicit compiler:PIR) extends PIRIRDotCodegen(fileName) {
  override val horizontal:Boolean = false

  override def color(attr:DotAttr, n:Any) = n match {
    case n:FringeContainer => attr.fillcolor("lightseagreen").style(filled)
    case n:CUContainer if isPMU(n) => attr.fillcolor(chartreuse).style(filled)
    case n:CUContainer if isDAG(n) => attr.fillcolor("deeppink").style(filled)
    case n:CUContainer if isSCU(n) => attr.fillcolor("gold").style(filled)
    case n:CUContainer if isOCU(n) => attr.fillcolor("darkorange1").style(filled)
    case n:CUContainer if isPCU(n) => attr.fillcolor("deepskyblue").style(filled)
    case n => super.color(attr,n)
  }

  override def emitNode(n:N) = {
    n match {
      case g:Design => emitSubGraph(n)(super.visitNode(n))
      case g:GlobalContainer => emitSingleNode(n); super.visitNode(n)
      case _ => super.visitNode(n)
    }
  }

  override def emitEdge(from:Edge[N], to:Edge[N], attr:DotAttr):Unit = {
    dbg(s"edge:${from.src}.$from -> ${to.src}.$to")
    (from.src, to.src) match {
      case (from:GlobalOutput, to:GlobalInput) =>
        val fromBundleType = bundleTypeOf(from, logger=Some(this))
        val toBundleType = bundleTypeOf(to, logger=Some(this))
        dbg(s"from:$fromBundleType, to:$toBundleType")
        assert(fromBundleType == toBundleType)
        val style = fromBundleType match {
          case Bit => attr.set("style", "dashed").set("color","red")
          case Word => attr.set("style", "solid")
          case Vector => attr.set("style", "bold").set("color","sienna")
        }
      case _ =>
    }
    super.emitEdge(from, to, attr)
  }

}

