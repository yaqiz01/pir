package pir.pass
import pir.graph._
import pir._
import pir.util._
import pir.exceptions._
import pir.util.misc._
import pir.codegen.Logger

import scala.collection.mutable._

class Optimizer(implicit design: Design) extends Pass with Logger {
  def shouldRun = true
  import pirmeta._
  override lazy val stream = newStream(s"Optimizer.log")

  addPass(canRun=design.memoryAnalyzer.hasRun || !Config.ctrl) {
    // No longer need info by dummy CUs
    design.top.compUnits.foreach { cu =>
      if (cu.children.isEmpty && cu.stages.isEmpty && cu.mems.isEmpty && cu.sins.isEmpty && cu.vins.isEmpty) {
        dprintln(s"Find dummy CU $cu")
        design.removeNode(cu)
      }
    }
    dprintln(s"ctrlers:${design.top.ctrlers.mkString(",")}")
  } 

}
