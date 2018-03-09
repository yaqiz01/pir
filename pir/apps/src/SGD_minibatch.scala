import pir._
import pir.node._
import arch._
import prism.enums._

object SGD_minibatch extends PIRApp {
  def main(implicit design:PIRDesign) = {
    import design.pirmeta._
    val x2767 = top.argFringe.argIn(init=0).name("x2767").ctrl(top) // ArgInNew(Const(0))
    val x2768 = top.argFringe.argIn(init=0).name("x2768").ctrl(top) // ArgInNew(Const(0))
    val x2769 = top.argFringe.argIn(init=0.0).name("x2769").ctrl(top) // ArgInNew(Const(0))
    val x2773 = ReadMem(x2768).name("x2773").ctrl(top) // RegRead(x2768)
    val x2774 = DRAM().name("x2774").ctrl(top) // x2774 = DRAMNew(ArrayBuffer(x2773, Const(16)),Const(0))
    val x2775 = ReadMem(x2768).name("x2775").ctrl(top) // RegRead(x2768)
    val x2776 = DRAM().name("x2776").ctrl(top) // x2776 = DRAMNew(ArrayBuffer(x2775),Const(0))
    val x2777 = DRAM().name("x2777").ctrl(top) // x2777 = DRAMNew(ArrayBuffer(Const(16)),Const(0))
    val x2942 = UnitController(style=SeqPipe, level=OuterControl).name("x2942").ctrl(top) // Hwblock(Block(Const(())),false)
    val x2789_d0_b0 = SRAM(size=4, banking=Strided(banks=4, stride=1)).name("x2789_d0_b0").ctrl(x2942) // x2789 = SRAMNew(ArrayBuffer(Const(16)))
    isAccum(x2789_d0_b0) = false
    val x2790_d0_b0 = SRAM(size=4, banking=Strided(banks=4, stride=1)).name("x2790_d0_b0").ctrl(x2942) // x2790 = SRAMNew(ArrayBuffer(Const(16)))
    isAccum(x2790_d0_b0) = false
    val x2790_d1_b0 = SRAM(size=16, banking=NoBanking()).name("x2790_d1_b0").ctrl(x2942) // x2790 = SRAMNew(ArrayBuffer(Const(16)))
    isAccum(x2790_d1_b0) = true
    val x2790_d2_b0 = SRAM(size=4, banking=Strided(banks=4, stride=1)).name("x2790_d2_b0").ctrl(x2942) // x2790 = SRAMNew(ArrayBuffer(Const(16)))
    isAccum(x2790_d2_b0) = false
    val x2791_d0_b0 = SRAM(size=64, banking=Strided(banks=4, stride=16)).name("x2791_d0_b0").ctrl(x2942) // x2791 = SRAMNew(ArrayBuffer(Const(16), Const(16)))
    isAccum(x2791_d0_b0) = false
    val x2791_d1_b0 = SRAM(size=64, banking=Strided(banks=4, stride=1)).name("x2791_d1_b0").ctrl(x2942) // x2791 = SRAMNew(ArrayBuffer(Const(16), Const(16)))
    isAccum(x2791_d1_b0) = false
    val x2792 = Counter(min=Const(0), max=Const(16), step=Const(1), par=1).name("x2792").ctrl(x2942) // CounterNew(Const(0),Const(16),Const(1),Const(1))
    val x2793 = CounterChain(List(x2792)).name("x2793").ctrl(x2942) // CounterChainNew(List(x2792))
    val x2795 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2793).name("x2795").ctrl(x2942) // UnrolledForeach(List(Const(true)),x2793,Block(Const(())),List(List(b1747)),List(List(b1748)))
    val b1747 = CounterIter(x2792, None).ctrl(x2795).name("b1747")
    val b1748 = DummyOp().ctrl(x2795).name("b1748")
    val x2794 = StoreBanks(List(x2790_d0_b0, x2790_d1_b0, x2790_d2_b0), List(b1747), Const(0.0)).name("x2794").ctrl(x2795) // ParSRAMStore(x2790,List(List(b1747)),List(Const(0)),List(b1748))
    val x2796 = ReadMem(x2767).name("x2796").ctrl(x2942) // RegRead(x2767)
    val x2797 = Counter(min=Const(0), max=x2796, step=Const(1), par=1).name("x2797").ctrl(x2942) // CounterNew(Const(0),x2796,Const(1),Const(1))
    val x2798 = CounterChain(List(x2797)).name("x2798").ctrl(x2942) // CounterChainNew(List(x2797))
    val x2918 = LoopController(style=SeqPipe, level=OuterControl, cchain=x2798).name("x2918").ctrl(x2942) // UnrolledForeach(List(Const(true)),x2798,Block(Const(())),List(List(b1754)),List(List(b1755)))
    val b1754 = CounterIter(x2797, Some(0)).ctrl(x2918).name("b1754")
    val b1755 = DummyOp().ctrl(x2918).name("b1755")
    val x2799 = ReadMem(x2768).name("x2799").ctrl(x2918) // RegRead(x2768)
    val x2800 = Counter(min=Const(0), max=x2799, step=Const(16), par=1).name("x2800").ctrl(x2918) // CounterNew(Const(0),x2799,Const(16),Const(1))
    val x2801 = CounterChain(List(x2800)).name("x2801").ctrl(x2918) // CounterChainNew(List(x2800))
    val x2917 = LoopController(style=SeqPipe, level=OuterControl, cchain=x2801).name("x2917").ctrl(x2918) // UnrolledForeach(List(b1755),x2801,Block(Const(())),List(List(b1759)),List(List(b1760)))
    val b1759 = CounterIter(x2800, Some(0)).ctrl(x2917).name("b1759")
    val b1760 = DummyOp().ctrl(x2917).name("b1760")
    val x2803 = UnitController(style=SeqPipe, level=InnerControl).name("x2803").ctrl(x2917) // UnitPipe(List(b1760, b1755),Block(Const(())))
    val x2802 = OpDef(op=FixAdd, inputs=List(b1759, Const(16))).name("x2802").ctrl(x2803) // FixAdd(b1759,Const(16))
    val x2825 = UnitController(style=StreamPipe, level=OuterControl).name("x2825").ctrl(x2917) // UnitPipe(List(b1760, b1755),Block(Const(())))
    val b2968 = StreamOut(field="offset").name("b2968").ctrl(x2825) // x2804 = StreamOutNew(BurstCmdBus)
    val b2969 = StreamOut(field="size").name("b2969").ctrl(x2825) // x2804 = StreamOutNew(BurstCmdBus)
    val x2805 = StreamIn(field="data").name("x2805").ctrl(x2825) // x2805 = StreamInNew(BurstDataBus())
    val x2815 = UnitController(style=SeqPipe, level=InnerControl).name("x2815").ctrl(x2825) // UnitPipe(List(b1760, b1755),Block(x2814))
    val x2806 = b1759 // FixConvert(b1759,TRUE,_32,_0)
    val x2807 = OpDef(op=FixSla, inputs=List(x2806, Const(2))).name("x2807").ctrl(x2815) // FixLsh(x2806,Const(2))
    val x2808 = x2807 // FixConvert(x2807,TRUE,_64,_0)
    val x2809 = top.argFringe.dramAddress(x2776).name("x2809").ctrl(x2815) // GetDRAMAddress(x2776)
    val x2810 = OpDef(op=FixAdd, inputs=List(x2808, x2809)).name("x2810").ctrl(x2815) // FixAdd(x2808,x2809)
    val x2812_x2811 = x2810 // FixConvert(x2810,TRUE,_64,_0)
    // x2812 = SimpleStruct(ArrayBuffer((offset,x2811), (size,Const(64)), (isLoad,Const(true))))
    val x2813 = OpDef(op=BitAnd, inputs=List(b1760, b1755)).name("x2813").ctrl(x2815) // And(b1760,b1755)
    val b2970_b2968 = WriteMem(b2968, x2812_x2811).name("b2970_b2968").ctrl(x2815) // StreamWrite(x2804,x2812,x2813)
    val b2971_b2969 = WriteMem(b2969, Const(64)).name("b2971_b2969").ctrl(x2815) // StreamWrite(x2804,x2812,x2813)
    val x2816 = FringeContainer(x2776,b2968,b2969,x2805).name("x2816").ctrl(x2825) // FringeDenseLoad(x2776,x2804,x2805)
    val x2817 = Counter(min=Const(0), max=Const(16), step=Const(1), par=4).name("x2817").ctrl(x2825) // CounterNew(Const(0),Const(16),Const(1),Const(4))
    val x2818 = CounterChain(List(x2817)).name("x2818").ctrl(x2825) // CounterChainNew(List(x2817))
    val x2824 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2818).name("x2824").ctrl(x2825) // UnrolledForeach(List(b1760, b1755),x2818,Block(Const(())),List(List(b1778)),List(List(b1779)))
    val b1778 = CounterIter(x2817, None).ctrl(x2824).name("b1778")
    val b1779 = DummyOp().ctrl(x2824).name("b1779")
    val x2819 = OpDef(op=BitAnd, inputs=List(b1779, b1760)).name("x2819").ctrl(x2824) // And(b1779,b1760)
    val x2820 = OpDef(op=BitAnd, inputs=List(x2819, b1755)).name("x2820").ctrl(x2824) // And(x2819,b1755)
    val x2821_x2821 = ReadMem(x2805).name("x2821").ctrl(x2824) // ParStreamRead(x2805,List(x2820))
    val x2822_x2822 = x2821_x2821 // x2822 = VectorApply(x2821,0)
    val x2823 = StoreBanks(List(x2789_d0_b0), List(b1778), x2822_x2822).name("x2823").ctrl(x2824) // ParSRAMStore(x2789,List(List(b1778)),List(x2822),List(x2820))
    val x2826 = Counter(min=Const(0), max=Const(16), step=Const(1), par=1).name("x2826").ctrl(x2917) // CounterNew(Const(0),Const(16),Const(1),Const(1))
    val x2827 = CounterChain(List(x2826)).name("x2827").ctrl(x2917) // CounterChainNew(List(x2826))
    val x2855 = LoopController(style=StreamPipe, level=OuterControl, cchain=x2827).name("x2855").ctrl(x2917) // UnrolledForeach(List(b1760, b1755),x2827,Block(Const(())),List(List(b1789)),List(List(b1790)))
    val b1789 = CounterIter(x2826, Some(0)).ctrl(x2855).name("b1789")
    val b1790 = DummyOp().ctrl(x2855).name("b1790")
    val b2972 = StreamOut(field="offset").name("b2972").ctrl(x2855) // x2828 = StreamOutNew(BurstCmdBus)
    val b2973 = StreamOut(field="size").name("b2973").ctrl(x2855) // x2828 = StreamOutNew(BurstCmdBus)
    val x2829 = StreamIn(field="data").name("x2829").ctrl(x2855) // x2829 = StreamInNew(BurstDataBus())
    val x2844 = UnitController(style=SeqPipe, level=InnerControl).name("x2844").ctrl(x2855) // UnitPipe(List(b1790, b1760, b1755),Block(x2843))
    val x2830 = OpDef(op=FixAdd, inputs=List(b1759, b1789)).name("x2830").ctrl(x2844) // FixAdd(b1759,b1789)
    val x2831 = x2830 // FixConvert(x2830,TRUE,_32,_0)
    val x2832 = OpDef(op=FixSla, inputs=List(x2831, Const(4))).name("x2832").ctrl(x2844) // FixLsh(x2831,Const(4))
    val x2833 = Const(0) // FixConvert(Const(0),TRUE,_32,_0)
    val x2834 = OpDef(op=FixAdd, inputs=List(x2832, x2833)).name("x2834").ctrl(x2844) // FixAdd(x2832,x2833)
    val x2835 = OpDef(op=FixSla, inputs=List(x2834, Const(2))).name("x2835").ctrl(x2844) // FixLsh(x2834,Const(2))
    val x2836 = x2835 // FixConvert(x2835,TRUE,_64,_0)
    val x2837 = top.argFringe.dramAddress(x2774).name("x2837").ctrl(x2844) // GetDRAMAddress(x2774)
    val x2838 = OpDef(op=FixAdd, inputs=List(x2836, x2837)).name("x2838").ctrl(x2844) // FixAdd(x2836,x2837)
    val x2840_x2839 = x2838 // FixConvert(x2838,TRUE,_64,_0)
    // x2840 = SimpleStruct(ArrayBuffer((offset,x2839), (size,Const(64)), (isLoad,Const(true))))
    val x2841 = OpDef(op=BitAnd, inputs=List(b1790, b1760)).name("x2841").ctrl(x2844) // And(b1790,b1760)
    val x2842 = OpDef(op=BitAnd, inputs=List(x2841, b1755)).name("x2842").ctrl(x2844) // And(x2841,b1755)
    val b2974_b2972 = WriteMem(b2972, x2840_x2839).name("b2974_b2972").ctrl(x2844) // StreamWrite(x2828,x2840,x2842)
    val b2975_b2973 = WriteMem(b2973, Const(64)).name("b2975_b2973").ctrl(x2844) // StreamWrite(x2828,x2840,x2842)
    val x2845 = FringeContainer(x2774,b2972,b2973,x2829).name("x2845").ctrl(x2855) // FringeDenseLoad(x2774,x2828,x2829)
    val x2846 = Counter(min=Const(0), max=Const(16), step=Const(1), par=1).name("x2846").ctrl(x2855) // CounterNew(Const(0),Const(16),Const(1),Const(1))
    val x2847 = CounterChain(List(x2846)).name("x2847").ctrl(x2855) // CounterChainNew(List(x2846))
    val x2854 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2847).name("x2854").ctrl(x2855) // UnrolledForeach(List(b1790, b1760, b1755),x2847,Block(Const(())),List(List(b1811)),List(List(b1812)))
    val b1811 = CounterIter(x2846, None).ctrl(x2854).name("b1811")
    val b1812 = DummyOp().ctrl(x2854).name("b1812")
    val x2848 = OpDef(op=BitAnd, inputs=List(b1812, b1790)).name("x2848").ctrl(x2854) // And(b1812,b1790)
    val x2849 = OpDef(op=BitAnd, inputs=List(b1760, b1755)).name("x2849").ctrl(x2854) // And(b1760,b1755)
    val x2850 = OpDef(op=BitAnd, inputs=List(x2848, x2849)).name("x2850").ctrl(x2854) // And(x2848,x2849)
    val x2851_x2851 = ReadMem(x2829).name("x2851").ctrl(x2854) // ParStreamRead(x2829,List(x2850))
    val x2852_x2852 = x2851_x2851 // x2852 = VectorApply(x2851,0)
    val x2853 = StoreBanks(List(x2791_d0_b0, x2791_d1_b0), List(b1789, b1811), x2852_x2852).name("x2853").ctrl(x2854) // ParSRAMStore(x2791,List(List(b1789, b1811)),List(x2852),List(x2850))
    val x2856_d0_b0 = SRAM(size=4, banking=Strided(banks=4, stride=1)).name("x2856_d0_b0").ctrl(x2917) // x2856 = SRAMNew(ArrayBuffer(Const(16)))
    isAccum(x2856_d0_b0) = false
    val x2857 = Counter(min=Const(0), max=Const(16), step=Const(1), par=1).name("x2857").ctrl(x2917) // CounterNew(Const(0),Const(16),Const(1),Const(1))
    val x2858 = CounterChain(List(x2857)).name("x2858").ctrl(x2917) // CounterChainNew(List(x2857))
    val x2885 = LoopController(style=SeqPipe, level=OuterControl, cchain=x2858).name("x2885").ctrl(x2917) // UnrolledForeach(List(b1760, b1755),x2858,Block(Const(())),List(List(b1824)),List(List(b1825)))
    val b1824 = CounterIter(x2857, Some(0)).ctrl(x2885).name("b1824")
    val b1825 = DummyOp().ctrl(x2885).name("b1825")
    val x2859_d0 = Reg(init=0.0).name("x2859_d0").ctrl(x2885) // x2859 = RegNew(Const(0))
    isAccum(x2859_d0) = false
    val x2859_d1 = Reg(init=0.0).name("x2859_d1").ctrl(x2885) // x2859 = RegNew(Const(0))
    isAccum(x2859_d1) = true
    val x2860 = Counter(min=Const(0), max=Const(16), step=Const(1), par=4).name("x2860").ctrl(x2885) // CounterNew(Const(0),Const(16),Const(1),Const(4))
    val x2861 = CounterChain(List(x2860)).name("x2861").ctrl(x2885) // CounterChainNew(List(x2860))
    val x2877 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2861).name("x2877").ctrl(x2885) // UnrolledReduce(List(b1825, b1760, b1755),x2861,x2859,Block((x2859) => Const(())),List(List(b1829)),List(List(b1830)))
    val b1829 = CounterIter(x2860, None).ctrl(x2877).name("b1829")
    val b1830 = DummyOp().ctrl(x2877).name("b1830")
    val x2862 = OpDef(op=BitAnd, inputs=List(b1830, b1825)).name("x2862").ctrl(x2877) // And(b1830,b1825)
    val x2863 = OpDef(op=BitAnd, inputs=List(b1760, b1755)).name("x2863").ctrl(x2877) // And(b1760,b1755)
    val x2864 = OpDef(op=BitAnd, inputs=List(x2862, x2863)).name("x2864").ctrl(x2877) // And(x2862,x2863)
    val x2865 = LoadBanks(List(x2791_d1_b0), List(b1824, b1829)).name("x2865").ctrl(x2877) // ParSRAMLoad(x2791,List(List(b1824, b1829)),List(x2864))
    val x2866 = x2865 // x2866 = VectorApply(x2865,0)
    val x2867 = LoadBanks(List(x2790_d2_b0), List(b1829)).name("x2867").ctrl(x2877) // ParSRAMLoad(x2790,List(List(b1829)),List(x2864))
    val x2868 = x2867 // x2868 = VectorApply(x2867,0)
    val x2869 = x2868 // FixConvert(x2868,TRUE,_16,_16)
    val x2870 = OpDef(op=FixMul, inputs=List(x2866, x2869)).name("x2870").ctrl(x2877) // FixMul(x2866,x2869)
    val x2871 = ReadMem(x2859_d1).name("x2871").ctrl(x2877) // RegRead(x2859)
    val x2872 = OpDef(op=FixEql, inputs=List(b1829, Const(0))).name("x2872").ctrl(x2877) // FixEql(b1829,Const(0))
    val x2873 = ReduceAccumOp(op=FixAdd, input=x2870, accum=x2871).name("x2873").ctrl(x2877) // FixAdd(x2870,x2871)
    val x2874 = OpDef(op=BitAnd, inputs=List(b1825, b1760)).name("x2874").ctrl(x2877) // And(b1825,b1760)
    val x2875 = OpDef(op=BitAnd, inputs=List(x2874, b1755)).name("x2875").ctrl(x2877) // And(x2874,b1755)
    val x2876_x2859_d0 = WriteMem(x2859_d0, x2873).name("x2876_x2859_d0").ctrl(x2877) // RegWrite(x2859,x2873,x2875)
    val x2876_x2859_d1 = WriteMem(x2859_d1, x2873).name("x2876_x2859_d1").ctrl(x2877) // RegWrite(x2859,x2873,x2875)
    val x2884 = UnitController(style=SeqPipe, level=InnerControl).name("x2884").ctrl(x2885) // UnitPipe(List(b1825, b1760, b1755),Block(Const(())))
    val x2878 = OpDef(op=BitAnd, inputs=List(b1825, b1760)).name("x2878").ctrl(x2884) // And(b1825,b1760)
    val x2879 = OpDef(op=BitAnd, inputs=List(x2878, b1755)).name("x2879").ctrl(x2884) // And(x2878,b1755)
    val x2880 = LoadBanks(List(x2789_d0_b0), List(b1824)).name("x2880").ctrl(x2884) // SRAMLoad(x2789,ArrayBuffer(Const(16)),List(b1824),Const(0),x2879)
    val x2881 = ReadMem(x2859_d0).name("x2881").ctrl(x2884) // RegRead(x2859)
    val x2882 = OpDef(op=FixSub, inputs=List(x2880, x2881)).name("x2882").ctrl(x2884) // FixSub(x2880,x2881)
    val x2883 = StoreBanks(List(x2856_d0_b0), List(b1824), x2882).name("x2883").ctrl(x2884) // SRAMStore(x2856,ArrayBuffer(Const(16)),List(b1824),Const(0),x2882,x2879)
    val x2886 = Counter(min=Const(0), max=Const(16), step=Const(1), par=1).name("x2886").ctrl(x2917) // CounterNew(Const(0),Const(16),Const(1),Const(1))
    val x2887 = CounterChain(List(x2886)).name("x2887").ctrl(x2917) // CounterChainNew(List(x2886))
    val x2916 = LoopController(style=SeqPipe, level=OuterControl, cchain=x2887).name("x2916").ctrl(x2917) // UnrolledForeach(List(b1760, b1755),x2887,Block(Const(())),List(List(b1857)),List(List(b1858)))
    val b1857 = CounterIter(x2886, Some(0)).ctrl(x2916).name("b1857")
    val b1858 = DummyOp().ctrl(x2916).name("b1858")
    val x2888_d0 = Reg(init=0.0).name("x2888_d0").ctrl(x2916) // x2888 = RegNew(Const(0))
    isAccum(x2888_d0) = false
    val x2888_d1 = Reg(init=0.0).name("x2888_d1").ctrl(x2916) // x2888 = RegNew(Const(0))
    isAccum(x2888_d1) = true
    val x2889 = Counter(min=Const(0), max=Const(16), step=Const(1), par=4).name("x2889").ctrl(x2916) // CounterNew(Const(0),Const(16),Const(1),Const(4))
    val x2890 = CounterChain(List(x2889)).name("x2890").ctrl(x2916) // CounterChainNew(List(x2889))
    val x2905 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2890).name("x2905").ctrl(x2916) // UnrolledReduce(List(b1858, b1760, b1755),x2890,x2888,Block((x2888) => Const(())),List(List(b1862)),List(List(b1863)))
    val b1862 = CounterIter(x2889, None).ctrl(x2905).name("b1862")
    val b1863 = DummyOp().ctrl(x2905).name("b1863")
    val x2891 = OpDef(op=BitAnd, inputs=List(b1863, b1858)).name("x2891").ctrl(x2905) // And(b1863,b1858)
    val x2892 = OpDef(op=BitAnd, inputs=List(b1760, b1755)).name("x2892").ctrl(x2905) // And(b1760,b1755)
    val x2893 = OpDef(op=BitAnd, inputs=List(x2891, x2892)).name("x2893").ctrl(x2905) // And(x2891,x2892)
    val x2894 = LoadBanks(List(x2791_d0_b0), List(b1862, b1857)).name("x2894").ctrl(x2905) // ParSRAMLoad(x2791,List(List(b1862, b1857)),List(x2893))
    val x2895 = x2894 // x2895 = VectorApply(x2894,0)
    val x2896 = LoadBanks(List(x2856_d0_b0), List(b1862)).name("x2896").ctrl(x2905) // ParSRAMLoad(x2856,List(List(b1862)),List(x2893))
    val x2897 = x2896 // x2897 = VectorApply(x2896,0)
    val x2898 = OpDef(op=FixMul, inputs=List(x2895, x2897)).name("x2898").ctrl(x2905) // FixMul(x2895,x2897)
    val x2899 = ReadMem(x2888_d1).name("x2899").ctrl(x2905) // RegRead(x2888)
    val x2900 = OpDef(op=FixEql, inputs=List(b1862, Const(0))).name("x2900").ctrl(x2905) // FixEql(b1862,Const(0))
    val x2901 = ReduceAccumOp(op=FixAdd, input=x2898, accum=x2899).name("x2901").ctrl(x2905) // FixAdd(x2898,x2899)
    val x2902 = OpDef(op=BitAnd, inputs=List(b1858, b1760)).name("x2902").ctrl(x2905) // And(b1858,b1760)
    val x2903 = OpDef(op=BitAnd, inputs=List(x2902, b1755)).name("x2903").ctrl(x2905) // And(x2902,b1755)
    val x2904_x2888_d0 = WriteMem(x2888_d0, x2901).name("x2904_x2888_d0").ctrl(x2905) // RegWrite(x2888,x2901,x2903)
    val x2904_x2888_d1 = WriteMem(x2888_d1, x2901).name("x2904_x2888_d1").ctrl(x2905) // RegWrite(x2888,x2901,x2903)
    val x2915 = UnitController(style=SeqPipe, level=InnerControl).name("x2915").ctrl(x2916) // UnitPipe(List(b1858, b1760, b1755),Block(Const(())))
    val x2906 = OpDef(op=BitAnd, inputs=List(b1858, b1760)).name("x2906").ctrl(x2915) // And(b1858,b1760)
    val x2907 = OpDef(op=BitAnd, inputs=List(x2906, b1755)).name("x2907").ctrl(x2915) // And(x2906,b1755)
    val x2908 = LoadBanks(List(x2790_d1_b0), List(b1857)).name("x2908").ctrl(x2915) // SRAMLoad(x2790,ArrayBuffer(Const(16)),List(b1857),Const(0),x2907)
    val x2909 = ReadMem(x2888_d0).name("x2909").ctrl(x2915) // RegRead(x2888)
    val x2910 = x2909 // FixConvert(x2909,TRUE,_16,_16)
    val x2911 = ReadMem(x2769).name("x2911").ctrl(x2915) // RegRead(x2769)
    val x2912 = OpDef(op=FixMul, inputs=List(x2910, x2911)).name("x2912").ctrl(x2915) // FixMul(x2910,x2911)
    val x2913 = OpDef(op=FixAdd, inputs=List(x2908, x2912)).name("x2913").ctrl(x2915) // FixAdd(x2908,x2912)
    val x2914 = StoreBanks(List(x2790_d0_b0, x2790_d1_b0, x2790_d2_b0), List(b1857), x2913).name("x2914").ctrl(x2915) // SRAMStore(x2790,ArrayBuffer(Const(16)),List(b1857),Const(0),x2913,x2907)
    val x2941 = UnitController(style=StreamPipe, level=OuterControl).name("x2941").ctrl(x2942) // UnitPipe(List(Const(true)),Block(Const(())))
    val b2976 = StreamOut(field="offset").name("b2976").ctrl(x2941) // x2919 = StreamOutNew(BurstCmdBus)
    val b2977 = StreamOut(field="size").name("b2977").ctrl(x2941) // x2919 = StreamOutNew(BurstCmdBus)
    val x2920 = StreamOut(field="data").name("x2920").ctrl(x2941) // x2920 = StreamOutNew(BurstFullDataBus())
    val x2921 = StreamIn(field="ack").name("x2921").ctrl(x2941) // x2921 = StreamInNew(BurstAckBus)
    val x2930 = UnitController(style=SeqPipe, level=InnerControl).name("x2930").ctrl(x2941) // UnitPipe(List(Const(true)),Block(x2929))
    val x2922 = Const(0) // FixConvert(Const(0),TRUE,_32,_0)
    val x2923 = OpDef(op=FixSla, inputs=List(x2922, Const(2))).name("x2923").ctrl(x2930) // FixLsh(x2922,Const(2))
    val x2924 = x2923 // FixConvert(x2923,TRUE,_64,_0)
    val x2925 = top.argFringe.dramAddress(x2777).name("x2925").ctrl(x2930) // GetDRAMAddress(x2777)
    val x2926 = OpDef(op=FixAdd, inputs=List(x2924, x2925)).name("x2926").ctrl(x2930) // FixAdd(x2924,x2925)
    val x2928_x2927 = x2926 // FixConvert(x2926,TRUE,_64,_0)
    // x2928 = SimpleStruct(ArrayBuffer((offset,x2927), (size,Const(64)), (isLoad,Const(false))))
    val b2978_b2976 = WriteMem(b2976, x2928_x2927).name("b2978_b2976").ctrl(x2930) // StreamWrite(x2919,x2928,Const(true))
    val b2979_b2977 = WriteMem(b2977, Const(64)).name("b2979_b2977").ctrl(x2930) // StreamWrite(x2919,x2928,Const(true))
    val x2931 = Counter(min=Const(0), max=Const(16), step=Const(1), par=4).name("x2931").ctrl(x2941) // CounterNew(Const(0),Const(16),Const(1),Const(4))
    val x2932 = CounterChain(List(x2931)).name("x2932").ctrl(x2941) // CounterChainNew(List(x2931))
    val x2937 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2932).name("x2937").ctrl(x2941) // UnrolledForeach(List(Const(true)),x2932,Block(Const(())),List(List(b1906)),List(List(b1907)))
    val b1906 = CounterIter(x2931, None).ctrl(x2937).name("b1906")
    val b1907 = DummyOp().ctrl(x2937).name("b1907")
    val x2933 = LoadBanks(List(x2790_d0_b0), List(b1906)).name("x2933").ctrl(x2937) // ParSRAMLoad(x2790,List(List(b1906)),List(b1907))
    val x2935_x2934 = x2933 // x2934 = VectorApply(x2933,0)
    // x2935 = SimpleStruct(ArrayBuffer((_1,x2934), (_2,Const(true))))
    val x2936_x2920 = WriteMem(x2920, x2935_x2934).name("x2936_x2920").ctrl(x2937) // ParStreamWrite(x2920,List(x2935),List(b1907))
    val x2938 = FringeContainer(x2777,b2976,b2977,x2920,x2921).name("x2938").ctrl(x2941) // FringeDenseStore(x2777,x2919,x2920,x2921)
    val x2940 = UnitController(style=SeqPipe, level=InnerControl).name("x2940").ctrl(x2941) // UnitPipe(List(Const(true)),Block(Const(())))
    val x2939_x2939 = ReadMem(x2921).name("x2939").ctrl(x2940) // StreamRead(x2921,Const(true))
    
  }
}
