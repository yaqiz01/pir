package pir.test

import pir._
import pir.graph.{Counter => Ctr, CounterChain, ComputeUnit => CU}
import pir.plasticine.graph.{Counter => PCtr, Const => PConst, Top => PTop}
import pir.PIRMisc._
import plasticine.config._
import pir.graph.mapper._
import pir.graph.traversal._

import org.scalatest._
import scala.language.reflectiveCalls

object Current extends Tag("Current")

class CtrMapperTest extends UnitTest { self =>

  "CtrMapper Test1" should "success" in {
    new Design {
      implicit val ctrler:CU = null
      // Nodes
      val cc1 = CounterChain(0 until 1 by 2)
      val cc0 = CounterChain(0 until 1 by 2, 3 until 4 by 5)
      val ctrs = cc0.counters ++ cc1.counters
      // PNodes
      override val arch = new Spade {
        val numLanes = 0
        val rcus = Nil
        val ttcus = Nil
        val top = PTop(Nil, Nil, Nil, Nil)
        val wordWidth = 32
        val numCtrs = 3
        val pctrs = List.tabulate(numCtrs) { ic => 
          val c = PCtr(ic) 
          c.min <== const.out
          c.max <== const.out
          c.step <== const.out
          c
        }
        for (i <- 1 until numCtrs) { pctrs(i).en <== pctrs(i-1).done } 
        for (i <- 0 until numCtrs by 2) { pctrs(i).en <== top.clk }
      }
      // Mapping
      val mapper = new CtrMapper()
      val mapping = mapper.map(ctrs, arch.pctrs, PIRMap.empty, (m:PIRMap) => m)
      // Printer
      new CtrDotPrinter("TestPCtr1.dot").print(arch.pctrs, ctrs, mapping)
    }
  }

  "CtrMapper Test2" should "fail" in {
    new Design {
      implicit val ctrler:CU = null
      // Nodes
      val cc1 = CounterChain(0 until 1 by 2)
      val cc0 = CounterChain(0 until 1 by 2, 3 until 4 by 5)
      val ctrs = cc0.counters ++ cc1.counters
      // PNodes
      override val arch = new Spade {
        val numLanes = 0
        val rcus = Nil
        val ttcus = Nil
        val top = PTop(Nil, Nil, Nil, Nil)
        val wordWidth = 32
        val numCtrs = 3
        val pctrs = List.tabulate(numCtrs) { ic => 
          val c = PCtr(ic) 
          c.min <== const.out
          c.max <== const.out
          c.step <== const.out
          c
        }
        for (i <- 1 until numCtrs) { pctrs(i).en <== pctrs(i-1).done } 
        for (i <- 0 until numCtrs by 3) { pctrs(i).en <== top.clk }
      }
      // Mapping
      intercept[PIRException] {
        val mapper = new CtrMapper()
        val mapping = mapper.map(ctrs, arch.pctrs, PIRMap.empty, (m:PIRMap) => m)
      // Printer
        new CtrDotPrinter("TestPCtr2.dot").print(arch.pctrs, ctrs, mapping)
      }
    }
  }

  "CtrMapper Test3" should "success" taggedAs(Current) in {
    new Design {
      implicit val ctrler:CU = null
      // Nodes
      val cc1 = CounterChain(0 until 1 by 2)
      val cc0 = CounterChain(0 until 1 by 2, 3 until 4 by 5)
      val ctrs = cc0.counters ++ cc1.counters
      // PNodes
      override val arch = new Spade {
        val numLanes = 0
        val rcus = Nil
        val ttcus = Nil
        val top = PTop(Nil, Nil, Nil, Nil)
        val wordWidth = 32
        val numCtrs = 3
        val pctrs = List.tabulate(numCtrs) { ic => 
          val c = PCtr(ic) 
          c.min <== const.out
          c.max <== const.out
          c.step <== const.out
          c
        }
        pctrs(1).en <== pctrs(0).done
        for (i <- 0 until numCtrs by 1) { pctrs(i).en <== top.clk }
      }
      // Mapping
      val mapper = new CtrMapper()
      val mapping = mapper.map(ctrs, arch.pctrs, PIRMap.empty, (m:PIRMap) => m)
      // Printer
      new CtrDotPrinter("TestPCtr1.dot").print(arch.pctrs, ctrs, mapping)
      val ctmap = mapping.ctmap
      assert(ctmap(cc1.inner)==arch.pctrs(2))
      assert(ctmap(cc0.inner)==arch.pctrs(0))
      assert(ctmap(cc0.outer)==arch.pctrs(1))
    }
  }

}

