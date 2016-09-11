package pir.graph.traversal
import pir.graph._
import pir._
import pir.PIRMisc._
import pir.graph.mapper.PIRException

import scala.collection.mutable.Set
import scala.collection.mutable.HashMap

class IRCheck(implicit val design: Design) extends Traversal{
  override def traverse:Unit = {
    design.allNodes.foreach{ n => 
      if (n.toUpdate) {
        val printer = new PIRPrinter()
        printer.run
        printer.close
        throw PIRException(s"Node ${n} contains unupdated field/fields! Refer to ${printer.getPath} for more information")
      }
      n match {
        case c:ComputeUnit =>
          c.readers.foreach { r =>
            r match {
              case t:Top =>
              case c:ComputeUnit =>
                if (!c.isInstanceOf[InnerController]) 
                  throw PIRException(s"${n} have non inner pipe reader: ${r}")
              case _ =>
            }
          }
          c.writers.foreach { w =>
            w match {
              case t:Top =>
              case c:ComputeUnit =>
                if (!c.isInstanceOf[InnerController]) 
                  throw PIRException(s"${n} have non inner pipe writer: ${w}")
              case _ =>
            }
          }
        case c =>
      }
    }
  } 
  override def finPass = {
    info("Finishing checking mutable fields")
  }

}
