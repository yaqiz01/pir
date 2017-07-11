
import pir._
import pir.test._
import pir.util.misc._
import pir.util.enums._
import pir.util._
import pir.graph._
import pir.exceptions.PIRException
import plasticine.main._
import pir.codegen.Logger

import org.scalatest._
import scala.language.reflectiveCalls

class AppTests extends UnitTest { self =>

  def logDRAM(app:PIRApp, dram:Array[Option[AnyVal]]) = {
    val logger = new Logger {
      override lazy val stream = newStream("dram.log")(app)
    }
    dram.zipWithIndex.foreach { case (data, addr) => logger.dprintln(s"DRAM[$addr] = $data") }
    logger.close
  }

  def checkDram(start:Int, gold:List[AnyVal])(dram:Array[Option[AnyVal]]):Boolean = {
    var correct = true
    val N = gold.size
    for (i <- 0 until N) {
      val addr = start + i
      val dramVal = dram(addr)
      val goldVal = Some(gold(i))
      if (dramVal!=goldVal) {
        errmsg(s"DRAM($addr)($dramVal) != gold($i)($goldVal)")
        correct = false
      }
    }
    correct
  }

  def test(
      app:PIRApp, 
      args:String="", 
      argOuts:String="", 
      checkDram: Option[Array[Option[AnyVal]] => Boolean]=None, 
      timeOut:Int=60,
      debug:Boolean=false
    ) = {
    s"$app [$args] ($argOuts)" should "success" in { 
      Config.simulate = true
      Config.simulationTimeOut = timeOut
      Config.debug = debug
      Config.waveform = debug 
      Config.verbose = debug 
      Config.codegen = false
      try {
        if (app.pirMapping.hasRun) {
          app.setArgs(args.split(" "))
          app.simulator.reset
          app.simulator.run
        } else {
          app.main(args)
        }
        assert(!app.simulator.timeOut)
        if (argOuts != "") {
          argOuts.split(" ").foreach { aos =>
            val aon::aov::_ = aos.split("=").toList
            val ao = app.top.sins.filter { _.scalar.name==Some(aon) }.head
            val pao = app.mapping.get.vimap(ao)
            assert(pao.values(0).asBus.head.value==Some(toValue(aov)), s"ArgOut result incorrect")
          }
        }
        checkDram.foreach { checkDram =>
          val dram = app.arch.dram.getValue
          logDRAM(app, dram)
          assert(checkDram(dram), s"DRAM result incorrect")
        }
      } catch {
        case e:Exception => errmsg(s"$e"); throw e
      }
    }
  }

  def testDotProduct(app:PIRApp, startA:Int, startB:Int, N:Int, debug:Boolean=false) = {
    val a = (startA until startA+N).toList
    val b = (startB until startB+N).toList
    val gold = a.zip(b).map{ case (aa,bb) => aa * bb }.sum
    val default = Config.simulationTimeOut
    test(
      app, 
      args=s"x1019=$N x1037=${startA*4} x1056=${startB*4}", 
      argOuts=s"x1026_x1096=$gold", 
      timeOut=100, 
      debug=debug
    )
  }

  def testTPCHQ6(app:PIRApp, startA:Int, startB:Int, startC:Int, startD:Int, N:Int, debug:Boolean=false) = {
    val a = (startA until startA+N).toList
    val b = (startB until startB+N).toList
    val c = (startC until startC+N).toList
    val d = (startD until startD+N).toList
    val minDate = 0
    val maxDate = 9999
    val MIN_DISC = 0
    val MAX_DISC = 9999
    val gold = ((a,b,c).zipped,d).zipped.map{ case ((quant,disct,date),price) => 
      val valid = (date > minDate) && (date < maxDate) && (disct >= MIN_DISC) && (disct <= MAX_DISC) && (quant<24)
      if (valid) price * disct else 0
    }.sum.toFloat
    test(
      app, 
      args=s"x1563=$N x1607=${startA*4} x1626=${startB*4} x1588=${startC*4} x1645=${startD*4}", 
      argOuts=s"x1573_x1699=$gold",
      timeOut=100 * (N / 32),
      debug=debug
    )
  }

  def testOuterProduct(app:PIRApp, startA:Int, startB:Int, startC:Int, N:Int, debug:Boolean=false) = {
    val a = (startA until startA+N).toList
    val b = (startB until startB+N).toList

    val gold = List.tabulate(a.size, b.size) { case (i, j) =>
      a(i) * b(j)
    }.flatten

    test(
      app, 
      args=s"x1203=$N x1204=$N x1226=${startA*4} x1247=${startB*4} x1284=${startC*4}", 
      checkDram = Some(checkDram(startC, gold) _),
      timeOut=200,
      debug=debug
    )
  }

  def testMatMult_inner(app:PIRApp, N:Int, M:Int, P:Int, startA:Int, startB:Int, startC:Int, debug:Boolean=false) = {
    val a = Array.tabulate(M, P){ case (i, j) => i*P + j }
    val b = Array.tabulate(P, N){ case (i, j) => i*N + j }

    val gold = List.tabulate(M, N){ case (i,j) =>
      val bCol = b.map{ row => row(j) }
      a(i).zip(bCol).map{ case (aa, bb) => aa * bb }.reduce{_+_}
    }.flatten

    test(
      app, 
      args=s"N=$N M=$M P=$P a_addr=${startA*4} b_addr=${startB*4} c_addr=${startC*4}", 
      checkDram = Some(checkDram(startC, gold) _),
      timeOut=200,
      debug=debug
    )
  }

  //intercept[PIRException] {

  // Apps 
  //"OuterProduct" should "success" in { OuterProduct.main(Array("OuterProduct")) }
  //"BlackScholes" should "success" in { BlackScholes.main(Array("BlackScholes")) }
  //"TPCHQ6" should "success" in { TPCHQ6.main(Array("TPCHQ6")) }
  //"MatMult_inner" should "success" in { MatMult_inner.main(Array("MatMult_inner")) }
  //"GDA" should "success" in { GDA.main(Array("GDA")) }
  //"LogReg" should "success" in { LogReg.main(Array("LogReg")) }
  
  //test(InOutArg_cb, args="x222=4", argOuts="x223_x227=8.0", debug=false)
  //test(SRAMReadWrite_cb, argOuts="x1026_x1096=10416", timeOut=60, debug=false)
  //test(SimpleSequential_cb, args="x343=2 x342=10", argOuts="x344_x356=2false0", debug=false)
  //test(SimpleSequential_cb, args="x343=1 x342=10", argOuts="x344_x356=10", debug=false)
  //test(SimpleReduce_cb, args="x350=10", argOuts="x351_x365=1200", debug=false)
  //testDotProduct(DotProductSeq_cb, startA=0, startB=16, N=32, debug=false)
  //testDotProduct(DotProductSeq_cb, startA=0, startB=16, N=64, debug=false)
  //testDotProduct(DotProductMeta_cb, startA=0, startB=16, N=32, debug=false)
  //testDotProduct(DotProductMeta_cb, startA=0, startB=16, N=64, debug=false)
  //testTPCHQ6(TPCHQ6_cb, startA=0, startB=10, startC=20, startD=30, N=32, debug=false)
  //testTPCHQ6(TPCHQ6_cb, startA=0, startB=10, startC=20, startD=30, N=64, debug=false)
  //testOuterProduct(OuterProduct_cb, startA=0, startB=100, startC=200, N=16, debug=false)
  testMatMult_inner(MatMult_inner, N=16, M=16, P=16, startA=0, startB=20, startC=40, debug=true)
  
  //test(InOutArg, args="x222=4", argOuts="x223_x227=8.0", debug=false)
  //test(SRAMReadWrite, argOuts="x1026_x1096=10416", timeOut=60, debug=false)
}

