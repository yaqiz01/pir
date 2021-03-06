package pir.test

import pir._
import pir.util.misc._
import pir.pass._
import pir.spade.config._
import pir.spade.main._
import pir.spade.graph._
import pir.spade.util._
import pir.exceptions._
import pir.codegen._

import org.scalatest._
import scala.language.reflectiveCalls
import scala.util.Random
import scala.collection.mutable.ListBuffer
import sys.process._
import scala.language.postfixOps

class SpadeTest extends UnitTest { self =>

  "SpadeTest" should "success" taggedAs(ARCH) in {
    val design = new PIRApp { self =>
      def main(top:pir.graph.Top): Any = {}
      //arch = SN2x2
      arch = SN16x8_LD
      //arch = new SN(numRows=2, numCols=2, pattern=HalfHalf)
      implicit val spade = arch.asInstanceOf[SwitchNetwork]
      val cu = arch.pcus.head
      info(s"${arch} ${quote(cu)}.vin=${cu.vins.size}")
      info(s"${arch} ${quote(cu)}.vout=${cu.vouts.size}")
      info(s"${arch} ${quote(cu)}.cin=${cu.cins.size}")
      info(s"${arch} ${quote(cu)}.cout=${cu.couts.size}")
      info(s"${arch} ${quote(cu)}.sin=${cu.sins.size}")
      info(s"${arch} ${quote(cu)}.sout=${cu.souts.size}")
      info(s"${arch} ${quote(cu)}.stages=${cu.stages.size}")
      info(s"numLanes=${spade.numLanes}")
      info(s"wordWidth=${spade.wordWidth}")
      emitBlock("regs") {
        cu.stages.headOption.foreach { _.prs.map(_.reg).foreach { reg =>
          info(s"reg=${quote(reg)} colors=[${reg.colors.mkString(",")}]")
        } }
      }
      new CUCtrlDotPrinter().print
      s"out/bin/run -c out/${arch}/CtrlNetwork".replace(".dot", "") !

      new CUScalarDotPrinter().print
      s"out/bin/run -c out/${arch}/ScalNetwork".replace(".dot", "") !

      new CUVectorDotPrinter().print
      s"out/bin/run -c out/${arch}/VecNetwork".replace(".dot", "") !

      //new SpadePrinter().run //this prints architecture in detail but is slow
      //new SpadeNetworkCodegen().run
      //new SpadeParamCodegen().run
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
            if (out.fanOuts.filterNot{_.src.isInstanceOf[Top]}.size>1) {
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

