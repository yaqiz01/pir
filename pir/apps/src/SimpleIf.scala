import pir._
import pir.node._
import arch._
import pirc.enums._

object SimpleIf extends PIRApp {
  def main(top:Top) = {
    val x730_d0 = top.argIn(init=0).name("x730_d0").ctrl(top) // ArgInNew(Const(0))
    val x731 = top.argOut(init=0).name("x731").ctrl(top) // ArgOutNew(Const(0))
    val x800 = UnitController(style=SeqPipe, level=OuterControl).name("x800").ctrl(top) // Hwblock(Block(Const(())),false)
    val x733_d0_b0 = SRAM(size=1, banking=NoBanking()).name("x733_d0_b0").ctrl(x800) // x733 = SRAMNew(ArrayBuffer(Const(3)))
    val x733_d0_b1 = SRAM(size=1, banking=NoBanking()).name("x733_d0_b1").ctrl(x800) // x733 = SRAMNew(ArrayBuffer(Const(3)))
    val x733_d0_b2 = SRAM(size=1, banking=NoBanking()).name("x733_d0_b2").ctrl(x800) // x733 = SRAMNew(ArrayBuffer(Const(3)))
    val x734 = Counter(min=Const(0).ctrl(x800), max=Const(3).ctrl(x800), step=Const(1).ctrl(x800), par=3).name("x734").ctrl(x800) // CounterNew(Const(0),Const(3),Const(1),Const(3))
    val x735 = CounterChain(List(x734)).name("x735").ctrl(x800) // CounterChainNew(List(x734))
    val x796 = LoopController(style=MetaPipe, level=OuterControl, cchain=x735).name("x796").ctrl(x800) // UnrolledForeach(List(Const(true)),x735,Block(Const(())),List(List(b347, b348, b349)),List(List(b350, b351, b352)))
    val b347 = CounterIter(x734, Some(0)).ctrl(x796).name("b347")
    val b350 = DummyOp().ctrl(x796).name("b350")
    val b348 = CounterIter(x734, Some(1)).ctrl(x796).name("b348")
    val b351 = DummyOp().ctrl(x796).name("b351")
    val b349 = CounterIter(x734, Some(2)).ctrl(x796).name("b349")
    val b352 = DummyOp().ctrl(x796).name("b352")
    val x736_d0 = Reg(init=false).name("x736_d0").ctrl(x796) // x736 = RegNew(Const(false))
    val x736_d1 = Reg(init=false).name("x736_d1").ctrl(x796) // x736 = RegNew(Const(false))
    val x737_d0 = Reg(init=false).name("x737_d0").ctrl(x796) // x737 = RegNew(Const(false))
    val x737_d1 = Reg(init=false).name("x737_d1").ctrl(x796) // x737 = RegNew(Const(false))
    val x738_d0 = Reg(init=false).name("x738_d0").ctrl(x796) // x738 = RegNew(Const(false))
    val x738_d1 = Reg(init=false).name("x738_d1").ctrl(x796) // x738 = RegNew(Const(false))
    val x739_d0 = Reg(init=false).name("x739_d0").ctrl(x796) // x739 = RegNew(Const(false))
    val x739_d1 = Reg(init=false).name("x739_d1").ctrl(x796) // x739 = RegNew(Const(false))
    val x740_d0 = Reg(init=false).name("x740_d0").ctrl(x796) // x740 = RegNew(Const(false))
    val x740_d1 = Reg(init=false).name("x740_d1").ctrl(x796) // x740 = RegNew(Const(false))
    val x741_d0 = Reg(init=false).name("x741_d0").ctrl(x796) // x741 = RegNew(Const(false))
    val x741_d1 = Reg(init=false).name("x741_d1").ctrl(x796) // x741 = RegNew(Const(false))
    val x747 = UnitController(style=SeqPipe, level=InnerControl).name("x747").ctrl(x796) // UnitPipe(List(b350),Block(Const(())))
    val x742 = ReadMem(x730_d0).name("x742").ctrl(x747) // RegRead(x730)
    val x743 = OpDef(op=FixLeq, inputs=List(Const(42).ctrl(x747), x742.ctrl(x747))).name("x743").ctrl(x747) // FixLeq(Const(42),x742)
    val x744 = OpDef(op=BitNot, inputs=List(x743)).name("x744").ctrl(x747) // Not(x743)
    val x745 = WriteMems(List(x736_d0, x736_d1), x743).name("x745").ctrl(x747) // RegWrite(x736,x743,b350)
    val x746 = WriteMems(List(x739_d0, x739_d1), x744).name("x746").ctrl(x747) // RegWrite(x739,x744,b350)
    val x753 = UnitController(style=SeqPipe, level=InnerControl).name("x753").ctrl(x796) // UnitPipe(List(b351),Block(Const(())))
    val x748 = ReadMem(x730_d0).name("x748").ctrl(x753) // RegRead(x730)
    val x749 = OpDef(op=FixLeq, inputs=List(Const(42).ctrl(x753), x748.ctrl(x753))).name("x749").ctrl(x753) // FixLeq(Const(42),x748)
    val x750 = OpDef(op=BitNot, inputs=List(x749)).name("x750").ctrl(x753) // Not(x749)
    val x751 = WriteMems(List(x737_d0, x737_d1), x749).name("x751").ctrl(x753) // RegWrite(x737,x749,b351)
    val x752 = WriteMems(List(x740_d0, x740_d1), x750).name("x752").ctrl(x753) // RegWrite(x740,x750,b351)
    val x759 = UnitController(style=SeqPipe, level=InnerControl).name("x759").ctrl(x796) // UnitPipe(List(b352),Block(Const(())))
    val x754 = ReadMem(x730_d0).name("x754").ctrl(x759) // RegRead(x730)
    val x755 = OpDef(op=FixLeq, inputs=List(Const(42).ctrl(x759), x754.ctrl(x759))).name("x755").ctrl(x759) // FixLeq(Const(42),x754)
    val x756 = OpDef(op=BitNot, inputs=List(x755)).name("x756").ctrl(x759) // Not(x755)
    val x757 = WriteMems(List(x738_d0, x738_d1), x755).name("x757").ctrl(x759) // RegWrite(x738,x755,b352)
    val x758 = WriteMems(List(x741_d0, x741_d1), x756).name("x758").ctrl(x759) // RegWrite(x741,x756,b352)
    val x760 = ReadMem(x739_d1).name("x760").ctrl(x796) // RegRead(x739)
    val x761 = ReadMem(x736_d1).name("x761").ctrl(x796) // RegRead(x736)
    val x771 = UnitController(style=ForkSwitch, level=OuterControl).name("x771").ctrl(x796) // //TODO Switch(Block(x770),List(x761, x760),List(x765, x770))
    val x765 = UnitController(style=MetaPipe, level=InnerControl).name("x765").ctrl(x771) // //TODO SwitchCase(Block(x764))
    val x762 = ReadMem(x736_d0).name("x762").ctrl(x765) // RegRead(x736)
    val x763 = OpDef(op=BitAnd, inputs=List(x762, b350)).name("x763").ctrl(x765) // And(x762,b350)
    val x764 = StoreBanks(List(x733_d0_b0), List(b347), b347).name("x764").ctrl(x765) // SRAMStore(x733,ArrayBuffer(Const(3)),List(b347),Const(0),b347,x763)
    val x770 = UnitController(style=MetaPipe, level=InnerControl).name("x770").ctrl(x771) // //TODO SwitchCase(Block(x769))
    val x766 = OpDef(op=FixAdd, inputs=List(b347, Const(1).ctrl(x770))).name("x766").ctrl(x770) // FixAdd(b347,Const(1))
    val x767 = ReadMem(x739_d0).name("x767").ctrl(x770) // RegRead(x739)
    val x768 = OpDef(op=BitAnd, inputs=List(x767, b350)).name("x768").ctrl(x770) // And(x767,b350)
    val x769 = StoreBanks(List(x733_d0_b0), List(b347), x766).name("x769").ctrl(x770) // SRAMStore(x733,ArrayBuffer(Const(3)),List(b347),Const(0),x766,x768)
    val x772 = ReadMem(x740_d1).name("x772").ctrl(x796) // RegRead(x740)
    val x773 = ReadMem(x737_d1).name("x773").ctrl(x796) // RegRead(x737)
    val x783 = UnitController(style=ForkSwitch, level=OuterControl).name("x783").ctrl(x796) // //TODO Switch(Block(x782),List(x773, x772),List(x777, x782))
    val x777 = UnitController(style=MetaPipe, level=InnerControl).name("x777").ctrl(x783) // //TODO SwitchCase(Block(x776))
    val x774 = ReadMem(x737_d0).name("x774").ctrl(x777) // RegRead(x737)
    val x775 = OpDef(op=BitAnd, inputs=List(x774, b351)).name("x775").ctrl(x777) // And(x774,b351)
    val x776 = StoreBanks(List(x733_d0_b1), List(b348), b348).name("x776").ctrl(x777) // SRAMStore(x733,ArrayBuffer(Const(3)),List(b348),Const(0),b348,x775)
    val x782 = UnitController(style=MetaPipe, level=InnerControl).name("x782").ctrl(x783) // //TODO SwitchCase(Block(x781))
    val x778 = OpDef(op=FixAdd, inputs=List(b348, Const(1).ctrl(x782))).name("x778").ctrl(x782) // FixAdd(b348,Const(1))
    val x779 = ReadMem(x740_d0).name("x779").ctrl(x782) // RegRead(x740)
    val x780 = OpDef(op=BitAnd, inputs=List(x779, b351)).name("x780").ctrl(x782) // And(x779,b351)
    val x781 = StoreBanks(List(x733_d0_b1), List(b348), x778).name("x781").ctrl(x782) // SRAMStore(x733,ArrayBuffer(Const(3)),List(b348),Const(0),x778,x780)
    val x784 = ReadMem(x741_d1).name("x784").ctrl(x796) // RegRead(x741)
    val x785 = ReadMem(x738_d1).name("x785").ctrl(x796) // RegRead(x738)
    val x795 = UnitController(style=ForkSwitch, level=OuterControl).name("x795").ctrl(x796) // //TODO Switch(Block(x794),List(x785, x784),List(x789, x794))
    val x789 = UnitController(style=MetaPipe, level=InnerControl).name("x789").ctrl(x795) // //TODO SwitchCase(Block(x788))
    val x786 = ReadMem(x738_d0).name("x786").ctrl(x789) // RegRead(x738)
    val x787 = OpDef(op=BitAnd, inputs=List(x786, b352)).name("x787").ctrl(x789) // And(x786,b352)
    val x788 = StoreBanks(List(x733_d0_b2), List(b349), b349).name("x788").ctrl(x789) // SRAMStore(x733,ArrayBuffer(Const(3)),List(b349),Const(0),b349,x787)
    val x794 = UnitController(style=MetaPipe, level=InnerControl).name("x794").ctrl(x795) // //TODO SwitchCase(Block(x793))
    val x790 = OpDef(op=FixAdd, inputs=List(b349, Const(1).ctrl(x794))).name("x790").ctrl(x794) // FixAdd(b349,Const(1))
    val x791 = ReadMem(x741_d0).name("x791").ctrl(x794) // RegRead(x741)
    val x792 = OpDef(op=BitAnd, inputs=List(x791, b352)).name("x792").ctrl(x794) // And(x791,b352)
    val x793 = StoreBanks(List(x733_d0_b2), List(b349), x790).name("x793").ctrl(x794) // SRAMStore(x733,ArrayBuffer(Const(3)),List(b349),Const(0),x790,x792)
    val x799 = UnitController(style=SeqPipe, level=InnerControl).name("x799").ctrl(x800) // UnitPipe(List(Const(true)),Block(Const(())))
    val x797 = LoadBanks(List(x733_d0_b0, x733_d0_b1, x733_d0_b2), List(Const(2))).name("x797").ctrl(x799) // SRAMLoad(x733,ArrayBuffer(Const(3)),List(Const(2)),Const(0),Const(true))
    val x798 = WriteMems(List(x731), x797).name("x798").ctrl(x799) // RegWrite(x731,x797,Const(true))
    
  }
}
