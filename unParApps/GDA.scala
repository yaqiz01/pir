import pir.graph
import pir.graph._
import pir.graph.enums._
import pir.codegen._
import pir.plasticine.config._
import pir.Design
import pir.misc._
import pir.PIRApp

object GDADesign extends PIRApp {
  def main(args: String*)(top:Top) = {
    val x3640_scalar = Scalar("x3640")
    val x3499_oc = OffChip("x3499")
    val x3500_oc = OffChip("x3500")
    val x3497_oc = OffChip("x3497")
    val x3632_vector = Vector("x3632")
    val x3745_vector = Vector("x3745")
    val x3498_oc = OffChip("x3498")
    val x3738_vector = Vector("x3738")
    val x3746_vector = Vector("x3746")
    val bus_375_vector = Vector("bus_375")
    val x3639_x3751_addr_vector = Vector("x3639_x3751_addr")
    val x3496_oc = OffChip("x3496")
    val x3609_mc = MemoryController(TileLoad, x3499_oc)
    val x3711_mc = MemoryController(TileLoad, x3496_oc)
    val x3567_mc = MemoryController(TileLoad, x3498_oc)
    val x3664_mc = MemoryController(TileLoad, x3497_oc)
    val x3851_mc = MemoryController(TileStore, x3500_oc)
    val x3855 = Sequential(name = "x3855", parent=top, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val x3855_unitcc = CounterChain(name = "x3855_unitcc", (Const("0i"), Const("1i"), Const("1i")))
      var stage: List[Stage] = Nil
    }
    val x3587 = StreamController(name = "x3587", parent=x3855, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val x3587_unitcc = CounterChain(name = "x3587_unitcc", (Const("0i"), Const("1i"), Const("1i")))
      var stage: List[Stage] = Nil
    }
    val x3563_0 = UnitPipeline(name = "x3563_0", parent=x3587, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val tr264 = CU.temp
      val tr263 = CU.temp
      val tr262 = CU.temp
      val tr261 = CU.temp
      val x3563_unitcc = CounterChain(name = "x3563_unitcc", (Const("0i"), Const("1i"), Const("1i")))
      var stage: List[Stage] = Nil
      stage = stage0 +: Stages(6)
      Stage(stage(1), operands=List(Const("48i"), Const("64i")), op=FixMod, results=List(CU.temp(stage(1), tr261)))
      Stage(stage(2), operands=List(Const("48i"), CU.temp(stage(1), tr261)), op=FixSub, results=List(CU.temp(stage(2), tr262)))
      Stage(stage(3), operands=List(CU.temp(stage(2), tr261), Const("0i")), op=FixNeq, results=List(CU.temp(stage(3), tr263)))
      Stage(stage(4), operands=List(CU.temp(stage(3), tr263), Const("64i"), Const("0i")), op=Mux, results=List(CU.temp(stage(4), tr264)))
      Stage(stage(5), operands=List(CU.temp(stage(4), tr262), CU.temp(stage(4), tr264)), op=FixAdd, results=List(CU.scalarOut(stage(5), x3567_mc.len)))
      Stage(stage(6), operands=List(Const("0i")), op=Bypass, results=List(CU.scalarOut(stage(6), x3567_mc.ofs)))
    }
    val x3629 = StreamController(name = "x3629", parent=x3855, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val x3629_unitcc = CounterChain(name = "x3629_unitcc", (Const("0i"), Const("1i"), Const("1i")))
      var stage: List[Stage] = Nil
    }
    val x3605_0 = UnitPipeline(name = "x3605_0", parent=x3629, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val tr285 = CU.temp
      val tr284 = CU.temp
      val tr283 = CU.temp
      val tr282 = CU.temp
      val x3605_unitcc = CounterChain(name = "x3605_unitcc", (Const("0i"), Const("1i"), Const("1i")))
      var stage: List[Stage] = Nil
      stage = stage0 +: Stages(6)
      Stage(stage(1), operands=List(Const("48i"), Const("64i")), op=FixMod, results=List(CU.temp(stage(1), tr282)))
      Stage(stage(2), operands=List(Const("48i"), CU.temp(stage(1), tr282)), op=FixSub, results=List(CU.temp(stage(2), tr283)))
      Stage(stage(3), operands=List(CU.temp(stage(2), tr282), Const("0i")), op=FixNeq, results=List(CU.temp(stage(3), tr284)))
      Stage(stage(4), operands=List(CU.temp(stage(3), tr284), Const("64i"), Const("0i")), op=Mux, results=List(CU.temp(stage(4), tr285)))
      Stage(stage(5), operands=List(CU.temp(stage(4), tr283), CU.temp(stage(4), tr285)), op=FixAdd, results=List(CU.scalarOut(stage(5), x3609_mc.len)))
      Stage(stage(6), operands=List(Const("0i")), op=Bypass, results=List(CU.scalarOut(stage(6), x3609_mc.ofs)))
    }
    val x3831 = MetaPipeline(name = "x3831", parent=x3855, deps=List(x3587, x3629)) { implicit CU => 
      val stage0 = CU.emptyStage
      val ctr3 = (Const("0i").out, Const("3840000i").out, Const("20i").out) // Counter
      val x3634 = CounterChain(name = "x3634", ctr3)
      val ctr5 = (Const("0i").out, Const("48i").out, Const("1i").out) // Counter
      val ctr6 = (Const("0i").out, Const("48i").out, Const("1i").out) // Counter
      val x3637 = CounterChain(name = "x3637", ctr5, ctr6)
      var stage: List[Stage] = Nil
    }
    val x3685 = StreamController(name = "x3685", parent=x3831, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val x3685_unitcc = CounterChain(name = "x3685_unitcc", (Const("0i"), Const("1i"), Const("1i")))
      var stage: List[Stage] = Nil
    }
    val x3660_0 = UnitPipeline(name = "x3660_0", parent=x3685, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val tr310 = CU.temp
      val tr309 = CU.temp
      val tr307 = CU.temp
      val tr306 = CU.temp
      val tr304 = CU.temp
      val tr299 = CU.temp
      val x3660_unitcc = CounterChain(name = "x3660_unitcc", (Const("0i"), Const("1i"), Const("1i")))
      val x3634 = CounterChain.copy(x3831, "x3634")
      var stage: List[Stage] = Nil
      stage = stage0 +: Stages(8)
      Stage(stage(1), operands=List(CU.ctr(stage(0), x3634(0)), Const("64i")), op=FixMod, results=List(CU.temp(stage(1), tr299)))
      Stage(stage(2), operands=List(CU.ctr(stage(1), x3634(0)), CU.temp(stage(1), tr299)), op=FixSub, results=List(CU.scalarOut(stage(2), x3664_mc.ofs)))
      Stage(stage(3), operands=List(CU.temp(stage(2), tr299), Const("20i")), op=FixAdd, results=List(CU.temp(stage(3), tr304)))
      Stage(stage(4), operands=List(CU.temp(stage(3), tr304), Const("64i")), op=FixMod, results=List(CU.temp(stage(4), tr306)))
      Stage(stage(5), operands=List(CU.temp(stage(4), tr304), CU.temp(stage(4), tr306)), op=FixSub, results=List(CU.temp(stage(5), tr307)))
      Stage(stage(6), operands=List(CU.temp(stage(5), tr306), Const("0i")), op=FixNeq, results=List(CU.temp(stage(6), tr309)))
      Stage(stage(7), operands=List(CU.temp(stage(6), tr309), Const("64i"), Const("0i")), op=Mux, results=List(CU.temp(stage(7), tr310)))
      Stage(stage(8), operands=List(CU.temp(stage(7), tr307), CU.temp(stage(7), tr310)), op=FixAdd, results=List(CU.scalarOut(stage(8), x3664_mc.len)))
    }
    val x3732 = StreamController(name = "x3732", parent=x3831, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val ctr11 = (Const("0i").out, Const("20i").out, Const("1i").out) // Counter
      val x3688 = CounterChain(name = "x3688", ctr11)
      var stage: List[Stage] = Nil
    }
    val x3707_0 = UnitPipeline(name = "x3707_0", parent=x3732, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val tr337 = CU.temp
      val tr336 = CU.temp
      val tr334 = CU.temp
      val tr333 = CU.temp
      val tr331 = CU.temp
      val tr327 = CU.temp
      val tr326 = CU.temp
      val tr324 = CU.temp
      val x3707_unitcc = CounterChain(name = "x3707_unitcc", (Const("0i"), Const("1i"), Const("1i")))
      val x3688 = CounterChain.copy(x3732, "x3688")
      val x3634 = CounterChain.copy(x3831, "x3634")
      var stage: List[Stage] = Nil
      stage = stage0 +: Stages(10)
      Stage(stage(1), operands=List(CU.ctr(stage(0), x3634(0)), CU.ctr(stage(0), x3688(0))), op=FixAdd, results=List(CU.temp(stage(1), tr324)))
      Stage(stage(2), operands=List(CU.temp(stage(1), tr324), Const("48i")), op=FixMul, results=List(CU.temp(stage(2), tr326)))
      Stage(stage(3), operands=List(CU.temp(stage(2), tr326), Const("64i")), op=FixMod, results=List(CU.temp(stage(3), tr327)))
      Stage(stage(4), operands=List(CU.temp(stage(3), tr326), CU.temp(stage(3), tr327)), op=FixSub, results=List(CU.scalarOut(stage(4), x3711_mc.ofs)))
      Stage(stage(5), operands=List(CU.temp(stage(4), tr327), Const("48i")), op=FixAdd, results=List(CU.temp(stage(5), tr331)))
      Stage(stage(6), operands=List(CU.temp(stage(5), tr331), Const("64i")), op=FixMod, results=List(CU.temp(stage(6), tr333)))
      Stage(stage(7), operands=List(CU.temp(stage(6), tr331), CU.temp(stage(6), tr333)), op=FixSub, results=List(CU.temp(stage(7), tr334)))
      Stage(stage(8), operands=List(CU.temp(stage(7), tr333), Const("0i")), op=FixNeq, results=List(CU.temp(stage(8), tr336)))
      Stage(stage(9), operands=List(CU.temp(stage(8), tr336), Const("64i"), Const("0i")), op=Mux, results=List(CU.temp(stage(9), tr337)))
      Stage(stage(10), operands=List(CU.temp(stage(9), tr334), CU.temp(stage(9), tr337)), op=FixAdd, results=List(CU.scalarOut(stage(10), x3711_mc.len)))
    }
    val x3735_0 = UnitPipeline(name = "x3735_0", parent=x3831, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val tr351 = CU.temp
      val x3634 = CounterChain.copy(x3831, "x3634")
      val x3735_unitcc = CounterChain(name = "x3735_unitcc", (Const("0i"), Const("1i"), Const("1i")))
      var stage: List[Stage] = Nil
      stage = stage0 +: Stages(2)
      Stage(stage(1), operands=List(Const("3840000i"), CU.ctr(stage(0), x3634(0))), op=FixSub, results=List(CU.temp(stage(1), tr351)))
      Stage(stage(2), operands=List(CU.temp(stage(1), tr351), Const("20i")), op=FixMin, results=List(CU.scalarOut(stage(2), x3640_scalar)))
    }
    val x3812 = MetaPipeline(name = "x3812", parent=x3831, deps=List(x3685, x3732, x3735_0)) { implicit CU => 
      val stage0 = CU.emptyStage
      val ctr9 = (Const("0i").out, Const("48i").out, Const("1i").out) // Counter
      val ctr10 = (Const("0i").out, Const("48i").out, Const("1i").out) // Counter
      val x3744 = CounterChain(name = "x3744", ctr9, ctr10)
      val ctr7 = (Const("0i").out, CU.scalarIn(stage0, x3640_scalar).out, Const("1i").out) // Counter
      val x3741 = CounterChain(name = "x3741", ctr7)
      var stage: List[Stage] = Nil
    }
    val x3774 = StreamController(name = "x3774", parent=x3812, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val ctr13 = (Const("0i").out, Const("48i").out, Const("1i").out) // Counter
      val x3748 = CounterChain(name = "x3748", ctr13)
      var stage: List[Stage] = Nil
    }
    val x3774_0 = StreamPipeline(name = "x3774_0", parent=x3774, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val tr364 = CU.temp
      val x3748 = CounterChain.copy(x3774, "x3748")
      val x3741 = CounterChain.copy(x3812, "x3741")
      var stage: List[Stage] = Nil
      stage = stage0 +: Stages(2)
      Stage(stage(1), operands=List(CU.ctr(stage(0), x3741(0)), Const("48i")), op=FixMul, results=List(CU.temp(stage(1), tr364)))
      Stage(stage(2), operands=List(CU.temp(stage(1), tr364), CU.ctr(stage(1), x3748(0))), op=FixAdd, results=List(CU.vecOut(stage(2), x3639_x3751_addr_vector)))
    }
    val x3774_1 = StreamPipeline(name = "x3774_1", parent=x3774, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val tr374 = CU.temp
      val x3744 = CounterChain.copy(x3812, "x3744")
      val x3748 = CounterChain.copy(x3774, "x3748")
      val x3741 = CounterChain.copy(x3812, "x3741")
      val x3544_x3760 = SemiFIFO(size = 48, banking = Strided(1), buffering = SingleBuffer()).wtPort(x3567_mc.vdata).rdAddr(x3748(0))
      val x3545_x3757 = SemiFIFO(size = 48, banking = Strided(1), buffering = SingleBuffer()).wtPort(x3609_mc.vdata).rdAddr(x3748(0))
      val x3638_x3754 = SemiFIFO(size = 20, banking = Strided(1), buffering = MultiBuffer(2, swapRead = x3744(0))).wtPort(x3664_mc.vdata).rdAddr(x3741(0))
      var stage: List[Stage] = Nil
      stage = stage0 +: Stages(2)
      Stage(stage(1), operands=List(x3638_x3754.load, Const("1i")), op=FixEql, results=List(CU.temp(stage(1), tr374)))
      Stage(stage(2), operands=List(CU.temp(stage(1), tr374), CU.load(stage(1), x3545_x3757), CU.load(stage(1), x3544_x3760)), op=Mux, results=List(CU.vecOut(stage(2), bus_375_vector)))
    }
    val x3774_2 = StreamPipeline(name = "x3774_2", parent=x3774, deps=List(x3774_0, x3774_1)) { implicit CU => 
      val stage0 = CU.emptyStage
      val x3744 = CounterChain.copy(x3812, "x3744")
      val x3748 = CounterChain.copy(x3774, "x3748")
      val x3639_x3751 = SemiFIFO(size = 960, banking = Strided(1), buffering = MultiBuffer(2, swapRead = x3744(0))).wtPort(x3711_mc.vdata)
      val x3639_x3751_addr_fifo = FIFO(size = 4096, banking = Strided(1)).wtPort(x3639_x3751_addr_vector)
      val bus_375_fifo = FIFO(size = 4096, banking = Strided(1)).wtPort(bus_375_vector)
      var stage: List[Stage] = Nil
      stage = stage0 +: Stages(2)
      Stage(stage(1), operands=List(x3639_x3751_addr_fifo.load), op=Bypass, results=List(x3639_x3751.readAddr))
      Stage(stage(2), operands=List(x3639_x3751.load, bus_375_fifo.load), op=FltSub, results=List(CU.vecOut(stage(2), x3745_vector)))
    }
    val x3793_0 = Pipeline(name = "x3793_0", parent=x3812, deps=List(x3774)) { implicit CU => 
      val stage0 = CU.emptyStage
      val ctr21 = (Const("0i").out, Const("48i").out, Const("1i").out) // Counter
      val ctr22 = (Const("0i").out, Const("48i").out, Const("16i").out) // Counter
      val x3777 = CounterChain(name = "x3777", ctr21, ctr22)
      val x3748 = CounterChain.copy(x3774, "x3748")
      val x3745_x3780 = SRAM(size = 48, writeCtr = x3748(0), banking = Strided(1), buffering = MultiBuffer(2, swapRead = x3777(0), swapWrite = x3748(0))).wtPort(x3745_vector).rdAddr(x3777(0)).wtAddr(x3748(0))
      val x3745_x3783 = SRAM(size = 48, writeCtr = x3748(0), banking = Strided(1), buffering = MultiBuffer(2, swapRead = x3777(0), swapWrite = x3748(0))).wtPort(x3745_vector).rdAddr(x3777(1)).wtAddr(x3748(0))
      var stage: List[Stage] = Nil
      stage = stage0 +: Stages(1)
      Stage(stage(1), operands=List(x3745_x3780.load, x3745_x3783.load), op=FltMul, results=List(CU.vecOut(stage(1), x3746_vector)))
    }
    val x3810_0 = Pipeline(name = "x3810_0", parent=x3812, deps=List(x3793_0)) { implicit CU => 
      val stage0 = CU.emptyStage
      val tr408 = CU.temp
      val tr398 = CU.temp
      val tr394 = CU.temp
      val tr392 = CU.temp
      val tr391 = CU.temp
      val x3744 = CounterChain.copy(x3812, "x3744")
      val x3777 = CounterChain.copy(x3793_0, "x3777")
      val ctr25 = (Const("0i").out, Const("48i").out, Const("1i").out) // Counter
      val ctr26 = (Const("0i").out, Const("48i").out, Const("16i").out) // Counter
      val x3746_x3796 = SRAM(size = 2304, writeCtr = x3777(0), banking = Strided(1), buffering = MultiBuffer(2, swapRead = x3744(0), swapWrite = x3777(0))).wtPort(x3746_vector)
      val x3738_x3799 = SRAM(size = 2304, writeCtr = x3744(0), banking = Strided(1), buffering = SingleBuffer())
      val wr410 = CU.wtAddr(x3738_x3799)
      var stage: List[Stage] = Nil
      stage = stage0 +: WAStages(2, List(x3746_x3796))
      Stage(stage(1), operands=List(x3777(0), Const("48i")), op=FixMul, results=List(CU.temp(stage(1), tr391)))
      Stage(stage(2), operands=List(CU.temp(stage(1), tr391), CU.ctr(stage(1), x3777(1))), op=FixAdd, results=List(x3746_x3796.writeAddr, CU.temp(stage(2), tr392)))
      stage = stage0 +: Stages(7)
      Stage(stage(1), operands=List(CU.ctr(stage(0), x3744(0)), Const("48i")), op=FixMul, results=List(CU.temp(stage(1), tr394)))
      Stage(stage(2), operands=List(CU.temp(stage(1), tr394), CU.ctr(stage(1), x3744(1))), op=FixAdd, results=List(x3746_x3796.readAddr))
      Stage(stage(3), operands=List(CU.ctr(stage(2), x3744(0)), Const("48i")), op=FixMul, results=List(CU.temp(stage(3), tr398)))
      Stage(stage(4), operands=List(CU.temp(stage(3), tr398), CU.ctr(stage(3), x3744(1))), op=FixAdd, results=List(x3738_x3799.readAddr))
      Stage(stage(5), operands=List(CU.load(stage(4), x3746_x3796), x3738_x3799.load), op=FltAdd, results=List(CU.vecOut(stage(5), x3738_vector), CU.store(stage(5), x3738_x3799)))
      Stage(stage(6), operands=List(CU.ctr(stage(5), x3744(0)), Const("48i")), op=FixMul, results=List(CU.temp(stage(6), tr408)))
      Stage(stage(7), operands=List(CU.temp(stage(6), tr408), CU.ctr(stage(6), x3744(1))), op=FixAdd, results=List(CU.wtAddr(stage(7), wr410)))
    }
    val x3829_0 = Pipeline(name = "x3829_0", parent=x3831, deps=List(x3812)) { implicit CU => 
      val stage0 = CU.emptyStage
      val tr428 = CU.temp
      val tr420 = CU.temp
      val tr416 = CU.temp
      val tr414 = CU.temp
      val tr413 = CU.temp
      val x3744 = CounterChain.copy(x3812, "x3744")
      val ctr27 = (Const("0i").out, Const("48i").out, Const("1i").out) // Counter
      val ctr28 = (Const("0i").out, Const("48i").out, Const("16i").out) // Counter
      val x3637 = CounterChain(name = "x3637", ctr27, ctr28)
      val x3738_x3815 = SRAM(size = 2304, writeCtr = x3744(0), banking = Strided(1), buffering = MultiBuffer(2, swapRead = x3637(0), swapWrite = x3744(0))).wtPort(x3738_vector)
      val x3632_x3818 = SRAM(size = 2304, writeCtr = x3637(0), banking = Strided(1), buffering = SingleBuffer())
      val wr430 = CU.wtAddr(x3632_x3818)
      var stage: List[Stage] = Nil
      stage = stage0 +: WAStages(2, List(x3738_x3815))
      Stage(stage(1), operands=List(x3744(0), Const("48i")), op=FixMul, results=List(CU.temp(stage(1), tr413)))
      Stage(stage(2), operands=List(CU.temp(stage(1), tr413), CU.ctr(stage(1), x3744(1))), op=FixAdd, results=List(x3738_x3815.writeAddr, CU.temp(stage(2), tr414)))
      stage = stage0 +: Stages(7)
      Stage(stage(1), operands=List(CU.ctr(stage(0), x3637(0)), Const("48i")), op=FixMul, results=List(CU.temp(stage(1), tr416)))
      Stage(stage(2), operands=List(CU.temp(stage(1), tr416), CU.ctr(stage(1), x3637(1))), op=FixAdd, results=List(x3738_x3815.readAddr))
      Stage(stage(3), operands=List(CU.ctr(stage(2), x3637(0)), Const("48i")), op=FixMul, results=List(CU.temp(stage(3), tr420)))
      Stage(stage(4), operands=List(CU.temp(stage(3), tr420), CU.ctr(stage(3), x3637(1))), op=FixAdd, results=List(x3632_x3818.readAddr))
      Stage(stage(5), operands=List(CU.load(stage(4), x3738_x3815), x3632_x3818.load), op=FltAdd, results=List(CU.vecOut(stage(5), x3632_vector), CU.store(stage(5), x3632_x3818)))
      Stage(stage(6), operands=List(CU.ctr(stage(5), x3637(0)), Const("48i")), op=FixMul, results=List(CU.temp(stage(6), tr428)))
      Stage(stage(7), operands=List(CU.temp(stage(6), tr428), CU.ctr(stage(6), x3637(1))), op=FixAdd, results=List(CU.wtAddr(stage(7), wr430)))
    }
    val x3853 = StreamController(name = "x3853", parent=x3855, deps=List(x3831)) { implicit CU => 
      val stage0 = CU.emptyStage
      val ctr29 = (Const("0i").out, Const("48i").out, Const("1i").out) // Counter
      val x3834 = CounterChain(name = "x3834", ctr29)
      var stage: List[Stage] = Nil
    }
    val x3838_0 = UnitPipeline(name = "x3838_0", parent=x3853, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val x3834 = CounterChain.copy(x3853, "x3834")
      val x3838_unitcc = CounterChain(name = "x3838_unitcc", (Const("0i"), Const("1i"), Const("1i")))
      var stage: List[Stage] = Nil
      stage = stage0 +: Stages(2)
      Stage(stage(1), operands=List(CU.ctr(stage(0), x3834(0)), Const("48i")), op=FixMul, results=List(CU.scalarOut(stage(1), x3851_mc.ofs)))
      Stage(stage(2), operands=List(Const("48i")), op=Bypass, results=List(CU.scalarOut(stage(2), x3851_mc.len)))
    }
    val x3849_0 = Pipeline(name = "x3849_0", parent=x3853, deps=List()) { implicit CU => 
      val stage0 = CU.emptyStage
      val tr440 = CU.temp
      val tr438 = CU.temp
      val tr437 = CU.temp
      val x3834 = CounterChain.copy(x3853, "x3834")
      val ctr31 = (Const("0i").out, Const("48i").out, Const("16i").out) // Counter
      val x3840 = CounterChain(name = "x3840", ctr31)
      val x3637 = CounterChain.copy(x3829_0, "x3637")
      val x3632_x3843 = SRAM(size = 2304, writeCtr = x3637(0), banking = Strided(1), buffering = SingleBuffer()).wtPort(x3632_vector)
      var stage: List[Stage] = Nil
      stage = stage0 +: WAStages(2, List(x3632_x3843))
      Stage(stage(1), operands=List(x3637(0), Const("48i")), op=FixMul, results=List(CU.temp(stage(1), tr437)))
      Stage(stage(2), operands=List(CU.temp(stage(1), tr437), CU.ctr(stage(1), x3637(1))), op=FixAdd, results=List(x3632_x3843.writeAddr, CU.temp(stage(2), tr438)))
      stage = stage0 +: Stages(3)
      Stage(stage(1), operands=List(CU.ctr(stage(0), x3834(0)), Const("48i")), op=FixMul, results=List(CU.temp(stage(1), tr440)))
      Stage(stage(2), operands=List(CU.temp(stage(1), tr440), CU.ctr(stage(1), x3840(0))), op=FixAdd, results=List(x3632_x3843.readAddr))
      Stage(stage(3), operands=List(x3632_x3843.load), op=Bypass, results=List(CU.vecOut(stage(3), x3851_mc.vdata)))
    }
    
  }
}