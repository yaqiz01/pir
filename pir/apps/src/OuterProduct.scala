import pir._
import pir.node._
import arch._
import prism.enums._

object OuterProduct extends PIRApp {
  def main(implicit design:PIRDesign) = {
    import design.pirmeta._
    val x1767 = ArgIn(init=0).name("x1767").ctrl(top) // ArgInNew(Const(0))
    boundOf(x1767) = 1024
    val x1768_d0 = ArgIn(init=0).name("x1768_d0").ctrl(top) // ArgInNew(Const(0))
    boundOf(x1768_d0) = 1024
    val x1771 = ReadMem(x1767).name("x1771").ctrl(top) // RegRead(x1767)
    val x1772 = DRAM().name("x1772").ctrl(top) // x1772 = DRAMNew(ArrayBuffer(x1771),Const(0))
    val x1773 = ReadMem(x1768_d0).name("x1773").ctrl(top) // RegRead(x1768)
    val x1774 = DRAM().name("x1774").ctrl(top) // x1774 = DRAMNew(ArrayBuffer(x1773),Const(0))
    val x1775 = ReadMem(x1768_d0).name("x1775").ctrl(top) // RegRead(x1768)
    val x1776 = ReadMem(x1767).name("x1776").ctrl(top) // RegRead(x1767)
    val x1777 = DRAM().name("x1777").ctrl(top) // x1777 = DRAMNew(ArrayBuffer(x1776, x1775),Const(0))
    val x1889 = UnitController(style=SeqPipe, level=OuterControl).name("x1889").ctrl(top) // Hwblock(Block(Const(())),false)
    val x1780 = ReadMem(x1768_d0).name("x1780").ctrl(x1889) // RegRead(x1768)
    val x1781 = Counter(min=Const(0), max=x1780, step=Const(16), par=1).name("x1781").ctrl(x1889) // CounterNew(Const(0),x1780,Const(16),Const(1))
    val x1782 = ReadMem(x1767).name("x1782").ctrl(x1889) // RegRead(x1767)
    val x1783 = Counter(min=Const(0), max=x1782, step=Const(32), par=1).name("x1783").ctrl(x1889) // CounterNew(Const(0),x1782,Const(32),Const(1))
    val x1784 = CounterChain(List(x1783,x1781)).name("x1784").ctrl(x1889) // CounterChainNew(List(x1783, x1781))
    val x1888 = LoopController(style=MetaPipe, level=OuterControl, cchain=x1784).name("x1888").ctrl(x1889) // UnrolledForeach(List(Const(true)),x1784,Block(Const(())),List(List(b1045), List(b1046)),List(List(b1047), List(b1048)))
    val b1045 = CounterIter(x1783, Some(0)).ctrl(x1888).name("b1045")
    val b1047 = DummyOp().ctrl(x1888).name("b1047")
    val b1046 = CounterIter(x1781, Some(0)).ctrl(x1888).name("b1046")
    val b1048 = DummyOp().ctrl(x1888).name("b1048")
    val x1785_d0_b0 = SRAM(size=2, banking=Strided(banks=16, stride=1)).name("x1785_d0_b0").ctrl(x1888) // x1785 = SRAMNew(ArrayBuffer(Const(32)))
    isAccum(x1785_d0_b0) = false
    bufferDepthOf(x1785_d0_b0) = 2
    val x1786_d0_b0 = SRAM(size=1, banking=Strided(banks=16, stride=1)).name("x1786_d0_b0").ctrl(x1888) // x1786 = SRAMNew(ArrayBuffer(Const(16)))
    isAccum(x1786_d0_b0) = false
    bufferDepthOf(x1786_d0_b0) = 2
    val x1787_d0_b0 = SRAM(size=32, banking=Strided(banks=16, stride=1)).name("x1787_d0_b0").ctrl(x1888) // x1787 = SRAMNew(ArrayBuffer(Const(32), Const(16)))
    isAccum(x1787_d0_b0) = false
    bufferDepthOf(x1787_d0_b0) = 3
    val x1789 = UnitController(style=SeqPipe, level=InnerControl).name("x1789").ctrl(x1888) // UnitPipe(List(b1047, b1048),Block(Const(())))
    val x1788 = OpDef(op=FixAdd, inputs=List(b1045, Const(32))).name("x1788").ctrl(x1789) // FixAdd(b1045,Const(32))
    val x1811 = UnitController(style=StreamPipe, level=OuterControl).name("x1811").ctrl(x1888) // UnitPipe(List(b1047, b1048),Block(Const(())))
    val b1906 = StreamOut(field="offset").name("b1906").ctrl(x1811) // x1790 = StreamOutNew(BurstCmdBus)
    val b1907 = StreamOut(field="size").name("b1907").ctrl(x1811) // x1790 = StreamOutNew(BurstCmdBus)
    val x1791 = StreamIn(field="data").name("x1791").ctrl(x1811) // x1791 = StreamInNew(BurstDataBus())
    val x1801 = UnitController(style=SeqPipe, level=InnerControl).name("x1801").ctrl(x1811) // UnitPipe(List(b1047, b1048),Block(x1800))
    val x1792 = b1045 // FixConvert(b1045,TRUE,_32,_0)
    val x1793 = OpDef(op=FixSla, inputs=List(x1792, Const(2))).name("x1793").ctrl(x1801) // FixLsh(x1792,Const(2))
    val x1794 = x1793 // FixConvert(x1793,TRUE,_64,_0)
    val x1795 = DramAddress(x1772).name("x1795").ctrl(x1801) // GetDRAMAddress(x1772)
    val x1796 = OpDef(op=FixAdd, inputs=List(x1794, x1795)).name("x1796").ctrl(x1801) // FixAdd(x1794,x1795)
    val x1798_x1797 = x1796 // FixConvert(x1796,TRUE,_64,_0)
    // x1798 = SimpleStruct(ArrayBuffer((offset,x1797), (size,Const(128)), (isLoad,Const(true))))
    val x1799 = OpDef(op=BitAnd, inputs=List(b1047, b1048)).name("x1799").ctrl(x1801) // And(b1047,b1048)
    val b1908_b1906 = WriteMem(b1906, x1798_x1797).name("b1908_b1906").ctrl(x1801) // StreamWrite(x1790,x1798,x1799)
    val b1909_b1907 = WriteMem(b1907, Const(128)).name("b1909_b1907").ctrl(x1801) // StreamWrite(x1790,x1798,x1799)
    val x1802 = FringeDenseLoad(dram=List(x1772), cmdStream=List(b1906, b1907), dataStream=List(x1791)).name("x1802").ctrl(x1811) // FringeDenseLoad(x1772,x1790,x1791)
    val x1803 = Counter(min=Const(0), max=Const(32), step=Const(1), par=16).name("x1803").ctrl(x1811) // CounterNew(Const(0),Const(32),Const(1),Const(16))
    val x1804 = CounterChain(List(x1803)).name("x1804").ctrl(x1811) // CounterChainNew(List(x1803))
    val x1810 = LoopController(style=InnerPipe, level=InnerControl, cchain=x1804).name("x1810").ctrl(x1811) // UnrolledForeach(List(b1047, b1048),x1804,Block(Const(())),List(List(b1069)),List(List(b1070)))
    val b1069 = CounterIter(x1803, None).ctrl(x1810).name("b1069")
    val b1070 = DummyOp().ctrl(x1810).name("b1070")
    val x1805 = OpDef(op=BitAnd, inputs=List(b1070, b1047)).name("x1805").ctrl(x1810) // And(b1070,b1047)
    val x1806 = OpDef(op=BitAnd, inputs=List(x1805, b1048)).name("x1806").ctrl(x1810) // And(x1805,b1048)
    val x1807_x1807 = ReadMem(x1791).name("x1807").ctrl(x1810) // ParStreamRead(x1791,List(x1806))
    val x1808_x1808 = x1807_x1807 // x1808 = VectorApply(x1807,0)
    val x1809 = StoreBanks(List(x1785_d0_b0), List(b1069), x1808_x1808).name("x1809").ctrl(x1810) // ParSRAMStore(x1785,List(List(b1069)),List(x1808),List(x1806))
    val x1813 = UnitController(style=SeqPipe, level=InnerControl).name("x1813").ctrl(x1888) // UnitPipe(List(b1047, b1048),Block(Const(())))
    val x1812 = OpDef(op=FixAdd, inputs=List(b1046, Const(16))).name("x1812").ctrl(x1813) // FixAdd(b1046,Const(16))
    val x1835 = UnitController(style=StreamPipe, level=OuterControl).name("x1835").ctrl(x1888) // UnitPipe(List(b1047, b1048),Block(Const(())))
    val b1910 = StreamOut(field="offset").name("b1910").ctrl(x1835) // x1814 = StreamOutNew(BurstCmdBus)
    val b1911 = StreamOut(field="size").name("b1911").ctrl(x1835) // x1814 = StreamOutNew(BurstCmdBus)
    val x1815 = StreamIn(field="data").name("x1815").ctrl(x1835) // x1815 = StreamInNew(BurstDataBus())
    val x1825 = UnitController(style=SeqPipe, level=InnerControl).name("x1825").ctrl(x1835) // UnitPipe(List(b1047, b1048),Block(x1824))
    val x1816 = b1046 // FixConvert(b1046,TRUE,_32,_0)
    val x1817 = OpDef(op=FixSla, inputs=List(x1816, Const(2))).name("x1817").ctrl(x1825) // FixLsh(x1816,Const(2))
    val x1818 = x1817 // FixConvert(x1817,TRUE,_64,_0)
    val x1819 = DramAddress(x1774).name("x1819").ctrl(x1825) // GetDRAMAddress(x1774)
    val x1820 = OpDef(op=FixAdd, inputs=List(x1818, x1819)).name("x1820").ctrl(x1825) // FixAdd(x1818,x1819)
    val x1822_x1821 = x1820 // FixConvert(x1820,TRUE,_64,_0)
    // x1822 = SimpleStruct(ArrayBuffer((offset,x1821), (size,Const(64)), (isLoad,Const(true))))
    val x1823 = OpDef(op=BitAnd, inputs=List(b1047, b1048)).name("x1823").ctrl(x1825) // And(b1047,b1048)
    val b1912_b1910 = WriteMem(b1910, x1822_x1821).name("b1912_b1910").ctrl(x1825) // StreamWrite(x1814,x1822,x1823)
    val b1913_b1911 = WriteMem(b1911, Const(64)).name("b1913_b1911").ctrl(x1825) // StreamWrite(x1814,x1822,x1823)
    val x1826 = FringeDenseLoad(dram=List(x1774), cmdStream=List(b1910, b1911), dataStream=List(x1815)).name("x1826").ctrl(x1835) // FringeDenseLoad(x1774,x1814,x1815)
    val x1827 = Counter(min=Const(0), max=Const(16), step=Const(1), par=16).name("x1827").ctrl(x1835) // CounterNew(Const(0),Const(16),Const(1),Const(16))
    val x1828 = CounterChain(List(x1827)).name("x1828").ctrl(x1835) // CounterChainNew(List(x1827))
    val x1834 = LoopController(style=InnerPipe, level=InnerControl, cchain=x1828).name("x1834").ctrl(x1835) // UnrolledForeach(List(b1047, b1048),x1828,Block(Const(())),List(List(b1095)),List(List(b1096)))
    val b1095 = CounterIter(x1827, None).ctrl(x1834).name("b1095")
    val b1096 = DummyOp().ctrl(x1834).name("b1096")
    val x1829 = OpDef(op=BitAnd, inputs=List(b1096, b1047)).name("x1829").ctrl(x1834) // And(b1096,b1047)
    val x1830 = OpDef(op=BitAnd, inputs=List(x1829, b1048)).name("x1830").ctrl(x1834) // And(x1829,b1048)
    val x1831_x1831 = ReadMem(x1815).name("x1831").ctrl(x1834) // ParStreamRead(x1815,List(x1830))
    val x1832_x1832 = x1831_x1831 // x1832 = VectorApply(x1831,0)
    val x1833 = StoreBanks(List(x1786_d0_b0), List(b1095), x1832_x1832).name("x1833").ctrl(x1834) // ParSRAMStore(x1786,List(List(b1095)),List(x1832),List(x1830))
    val x1836 = Counter(min=Const(0), max=Const(16), step=Const(1), par=16).name("x1836").ctrl(x1888) // CounterNew(Const(0),Const(16),Const(1),Const(16))
    val x1837 = Counter(min=Const(0), max=Const(32), step=Const(1), par=1).name("x1837").ctrl(x1888) // CounterNew(Const(0),Const(32),Const(1),Const(1))
    val x1838 = CounterChain(List(x1837,x1836)).name("x1838").ctrl(x1888) // CounterChainNew(List(x1837, x1836))
    val x1847 = LoopController(style=InnerPipe, level=InnerControl, cchain=x1838).name("x1847").ctrl(x1888) // UnrolledForeach(List(b1047, b1048),x1838,Block(Const(())),List(List(b1108), List(b1109)),List(List(b1110), List(b1111)))
    val b1108 = CounterIter(x1837, Some(0)).ctrl(x1847).name("b1108")
    val b1110 = DummyOp().ctrl(x1847).name("b1110")
    val b1109 = CounterIter(x1836, None).ctrl(x1847).name("b1109")
    val b1111 = DummyOp().ctrl(x1847).name("b1111")
    val x1839 = OpDef(op=BitAnd, inputs=List(b1110, b1111)).name("x1839").ctrl(x1847) // And(b1110,b1111)
    val x1840 = OpDef(op=BitAnd, inputs=List(b1047, b1048)).name("x1840").ctrl(x1847) // And(b1047,b1048)
    val x1841 = OpDef(op=BitAnd, inputs=List(x1839, x1840)).name("x1841").ctrl(x1847) // And(x1839,x1840)
    val x1842 = LoadBanks(List(x1785_d0_b0), List(b1108)).name("x1842").ctrl(x1847) // SRAMLoad(x1785,ArrayBuffer(Const(32)),List(b1108),Const(0),x1841)
    val x1843 = LoadBanks(List(x1786_d0_b0), List(b1109)).name("x1843").ctrl(x1847) // ParSRAMLoad(x1786,List(List(b1109)),List(x1841))
    val x1844 = x1843 // x1844 = VectorApply(x1843,0)
    val x1845 = OpDef(op=FixMul, inputs=List(x1842, x1844)).name("x1845").ctrl(x1847) // FixMul(x1842,x1844)
    val x1846 = StoreBanks(List(x1787_d0_b0), List(b1108, b1109), x1845).name("x1846").ctrl(x1847) // ParSRAMStore(x1787,List(List(b1108, b1109)),List(x1845),List(x1841))
    val x1850 = UnitController(style=SeqPipe, level=InnerControl).name("x1850").ctrl(x1888) // UnitPipe(List(b1047, b1048),Block(Const(())))
    val x1848 = OpDef(op=FixAdd, inputs=List(b1045, Const(32))).name("x1848").ctrl(x1850) // FixAdd(b1045,Const(32))
    val x1849 = OpDef(op=FixAdd, inputs=List(b1046, Const(16))).name("x1849").ctrl(x1850) // FixAdd(b1046,Const(16))
    val x1851 = Counter(min=Const(0), max=Const(32), step=Const(1), par=1).name("x1851").ctrl(x1888) // CounterNew(Const(0),Const(32),Const(1),Const(1))
    val x1852 = CounterChain(List(x1851)).name("x1852").ctrl(x1888) // CounterChainNew(List(x1851))
    val x1887 = LoopController(style=StreamPipe, level=OuterControl, cchain=x1852).name("x1887").ctrl(x1888) // UnrolledForeach(List(b1047, b1048),x1852,Block(Const(())),List(List(b1126)),List(List(b1127)))
    val b1126 = CounterIter(x1851, Some(0)).ctrl(x1887).name("b1126")
    val b1127 = DummyOp().ctrl(x1887).name("b1127")
    val b1914 = StreamOut(field="offset").name("b1914").ctrl(x1887) // x1853 = StreamOutNew(BurstCmdBus)
    val b1915 = StreamOut(field="size").name("b1915").ctrl(x1887) // x1853 = StreamOutNew(BurstCmdBus)
    val x1854 = StreamOut(field="data").name("x1854").ctrl(x1887) // x1854 = StreamOutNew(BurstFullDataBus())
    val x1855 = StreamIn(field="ack").name("x1855").ctrl(x1887) // x1855 = StreamInNew(BurstAckBus)
    val x1871 = UnitController(style=SeqPipe, level=InnerControl).name("x1871").ctrl(x1887) // UnitPipe(List(b1127, b1047, b1048),Block(x1870))
    val x1856 = OpDef(op=FixAdd, inputs=List(b1045, b1126)).name("x1856").ctrl(x1871) // FixAdd(b1045,b1126)
    val x1857 = x1856 // FixConvert(x1856,TRUE,_32,_0)
    val x1858 = ReadMem(x1768_d0).name("x1858").ctrl(x1871) // RegRead(x1768)
    val x1859 = OpDef(op=FixMul, inputs=List(x1857, x1858)).name("x1859").ctrl(x1871) // FixMul(x1857,x1858)
    val x1860 = b1046 // FixConvert(b1046,TRUE,_32,_0)
    val x1861 = OpDef(op=FixAdd, inputs=List(x1859, x1860)).name("x1861").ctrl(x1871) // FixAdd(x1859,x1860)
    val x1862 = OpDef(op=FixSla, inputs=List(x1861, Const(2))).name("x1862").ctrl(x1871) // FixLsh(x1861,Const(2))
    val x1863 = x1862 // FixConvert(x1862,TRUE,_64,_0)
    val x1864 = DramAddress(x1777).name("x1864").ctrl(x1871) // GetDRAMAddress(x1777)
    val x1865 = OpDef(op=FixAdd, inputs=List(x1863, x1864)).name("x1865").ctrl(x1871) // FixAdd(x1863,x1864)
    val x1867_x1866 = x1865 // FixConvert(x1865,TRUE,_64,_0)
    // x1867 = SimpleStruct(ArrayBuffer((offset,x1866), (size,Const(64)), (isLoad,Const(false))))
    val x1868 = OpDef(op=BitAnd, inputs=List(b1127, b1047)).name("x1868").ctrl(x1871) // And(b1127,b1047)
    val x1869 = OpDef(op=BitAnd, inputs=List(x1868, b1048)).name("x1869").ctrl(x1871) // And(x1868,b1048)
    val b1916_b1914 = WriteMem(b1914, x1867_x1866).name("b1916_b1914").ctrl(x1871) // StreamWrite(x1853,x1867,x1869)
    val b1917_b1915 = WriteMem(b1915, Const(64)).name("b1917_b1915").ctrl(x1871) // StreamWrite(x1853,x1867,x1869)
    val x1872 = Counter(min=Const(0), max=Const(16), step=Const(1), par=16).name("x1872").ctrl(x1887) // CounterNew(Const(0),Const(16),Const(1),Const(16))
    val x1873 = CounterChain(List(x1872)).name("x1873").ctrl(x1887) // CounterChainNew(List(x1872))
    val x1881 = LoopController(style=InnerPipe, level=InnerControl, cchain=x1873).name("x1881").ctrl(x1887) // UnrolledForeach(List(b1127, b1047, b1048),x1873,Block(Const(())),List(List(b1149)),List(List(b1150)))
    val b1149 = CounterIter(x1872, None).ctrl(x1881).name("b1149")
    val b1150 = DummyOp().ctrl(x1881).name("b1150")
    val x1874 = OpDef(op=BitAnd, inputs=List(b1150, b1127)).name("x1874").ctrl(x1881) // And(b1150,b1127)
    val x1875 = OpDef(op=BitAnd, inputs=List(b1047, b1048)).name("x1875").ctrl(x1881) // And(b1047,b1048)
    val x1876 = OpDef(op=BitAnd, inputs=List(x1874, x1875)).name("x1876").ctrl(x1881) // And(x1874,x1875)
    val x1877 = LoadBanks(List(x1787_d0_b0), List(b1126, b1149)).name("x1877").ctrl(x1881) // ParSRAMLoad(x1787,List(List(b1126, b1149)),List(x1876))
    val x1879_x1878 = x1877 // x1878 = VectorApply(x1877,0)
    // x1879 = SimpleStruct(ArrayBuffer((_1,x1878), (_2,Const(true))))
    val x1880_x1854 = WriteMem(x1854, x1879_x1878).name("x1880_x1854").ctrl(x1881) // ParStreamWrite(x1854,List(x1879),List(x1876))
    val x1882 = FringeDenseStore(dram=List(x1777), cmdStream=List(b1914, b1915), dataStream=List(x1854), ackStream=List(x1855)).name("x1882").ctrl(x1887) // FringeDenseStore(x1777,x1853,x1854,x1855)
    val x1886 = UnitController(style=SeqPipe, level=InnerControl).name("x1886").ctrl(x1887) // UnitPipe(List(b1127, b1047, b1048),Block(Const(())))
    val x1883 = OpDef(op=BitAnd, inputs=List(b1127, b1047)).name("x1883").ctrl(x1886) // And(b1127,b1047)
    val x1884 = OpDef(op=BitAnd, inputs=List(x1883, b1048)).name("x1884").ctrl(x1886) // And(x1883,b1048)
    val x1885_x1885 = ReadMem(x1855).name("x1885").ctrl(x1886) // StreamRead(x1855,x1884)
    
  }
}
