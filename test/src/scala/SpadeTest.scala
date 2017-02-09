package pir.test

import pir._
import pir.misc._
import pir.graph.traversal._
import pir.plasticine.config._
import pir.plasticine.main._
import pir.graph.mapper.PIRException

import org.scalatest._
import scala.language.reflectiveCalls
import scala.util.Random
import scala.collection.mutable.ListBuffer
import sys.process._
import scala.language.postfixOps

class SpadeTest extends UnitTest { self =>

  "SN_2x2" should "success" taggedAs(ARCH) in {
    val design = new Design { self =>
      override val arch = SN_2x2
      new SpadeCodegen().run

      //new CUCtrlDotPrinter().print
      //s"out/bin/run -c out/CtrlNetwork".replace(".dot", "") !

      //new CUScalarDotPrinter().print
      //s"out/bin/run -c out/ScalNetwork".replace(".dot", "") !

      new CUVectorDotPrinter().print
      s"out/bin/run -c out/VecNetwork".replace(".dot", "") !
    }
    design.arch match {
      case sn:SwitchNetwork =>
        sn.sbs.foreach { sb => 
          (sb.vectorIO.ins ++ sb.scalarIO.ins ++ sb.ctrlIO.ins).foreach { in => 
            if (in.fanIns.size>1) { 
              println(s"Switchbox $sb has $in with fanIns > 1 ${in.fanIns}")
              throw PIRException(s"Switchbox $sb has $in with fanIns > 1 ${in.fanIns}")
            }
          }
          (sb.vectorIO.outs ++ sb.scalarIO.outs ++ sb.ctrlIO.outs).foreach { out => 
            if (out.fanOuts.size>1) {
              throw PIRException(s"Switchbox $sb has $out with fanOuts > 1 ${out.fanOuts}")
            }
          }
        }
        sn.cus.foreach { pcu =>
          (pcu.vectorIO.ins ++ pcu.scalarIO.ins ++ pcu.ctrlIO.ins).foreach { in => 
            if (in.fanIns.size>1) 
              throw PIRException(s"ComputeUnit $pcu has $in with fanIns > 1 ${in.fanIns}")
          }
          pcu.ctrlIO.outs.foreach { out => 
            if (out.fanOuts.size>1) 
              throw PIRException(s"ComputeUnit $pcu has $out with fanOuts > 1 ${out.fanOuts}")
          }
        }
      case _ =>
    }
  }

}

