import pir._
import pir.node._
import arch._
import pirc.enums._

object Differentiator extends PIRApp {
  def main(top:Top) = {
    val x2142 = top.argIn(init=0).name("x2142").ctrl(top) // ArgInNew(Const(0))
    val x2149 = ReadMem(x2142).name("x2149").ctrl(top) // RegRead(x2142)
    val x2150 = DRAM().name("x2150").ctrl(top) // x2150 = DRAMNew(ArrayBuffer(x2149),Const(0))
    val x2152 = ReadMem(x2142).name("x2152").ctrl(top) // RegRead(x2142)
    val x2153 = DRAM().name("x2153").ctrl(top) // x2153 = DRAMNew(ArrayBuffer(x2152),Const(0))
    val x2262 = UnitController(style=SeqPipe, level=OuterControl).name("x2262").ctrl(top) // Hwblock(Block(Const(())),false)
    val x2154_d0_b0 = RegFile(sizes=List(1, 16), inits=None).name("x2154_d0_b0").ctrl(x2262) // x2154 = RegFileNew(ArrayBuffer(Const(1), Const(16)),None) banking:NoBanking()
    val x2154_d1_b0 = RegFile(sizes=List(1, 16), inits=None).name("x2154_d1_b0").ctrl(x2262) // x2154 = RegFileNew(ArrayBuffer(Const(1), Const(16)),None) banking:NoBanking()
    val x2155_d0_b0 = SRAM(size=64, banking=NoBanking()).name("x2155_d0_b0").ctrl(x2262) // x2155 = SRAMNew(ArrayBuffer(Const(64)))
    val x2156_d0_b0 = SRAM(size=64, banking=NoBanking()).name("x2156_d0_b0").ctrl(x2262) // x2156 = SRAMNew(ArrayBuffer(Const(64)))
    val x2157 = Counter(min=Const(0).ctrl(x2262), max=Const(16).ctrl(x2262), step=Const(1).ctrl(x2262), par=1).name("x2157").ctrl(x2262) // CounterNew(Const(0),Const(16),Const(1),Const(1))
    val x2158 = CounterChain(List(x2157)).name("x2158").ctrl(x2262) // CounterChainNew(List(x2157))
    val x2160 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2158).name("x2160").ctrl(x2262) // UnrolledForeach(List(Const(true)),x2158,Block(Const(())),List(List(b1385)),List(List(b1386)))
    val b1385 = CounterIter(x2157, None).ctrl(x2160).name("b1385")
    val b1386 = DummyOp().ctrl(x2160).name("b1386")
    val x2159 = StoreBanks(List(x2156_d0_b0), List(b1385), Const(0)).name("x2159").ctrl(x2160) // ParSRAMStore(x2156,List(List(b1385)),List(Const(0)),List(b1386))
    val x2161 = ReadMem(x2142).name("x2161").ctrl(x2262) // RegRead(x2142)
    val x2162 = Counter(min=Const(0).ctrl(x2262), max=x2161, step=Const(64).ctrl(x2262), par=1).name("x2162").ctrl(x2262) // CounterNew(Const(0),x2161,Const(64),Const(1))
    val x2163 = CounterChain(List(x2162)).name("x2163").ctrl(x2262) // CounterChainNew(List(x2162))
    val x2261 = LoopController(style=MetaPipe, level=OuterControl, cchain=x2163).name("x2261").ctrl(x2262) // UnrolledForeach(List(Const(true)),x2163,Block(Const(())),List(List(b1392)),List(List(b1393)))
    val b1392 = CounterIter(x2162, Some(0)).ctrl(x2261).name("b1392")
    val b1393 = DummyOp().ctrl(x2261).name("b1393")
    val x2165 = UnitController(style=SeqPipe, level=InnerControl).name("x2165").ctrl(x2261) // UnitPipe(List(b1393),Block(Const(())))
    val x2164 = OpDef(op=FixAdd, inputs=List(b1392, Const(64).ctrl(x2165))).name("x2164").ctrl(x2165) // FixAdd(b1392,Const(64))
    val x2185 = UnitController(style=StreamPipe, level=OuterControl).name("x2185").ctrl(x2261) // UnitPipe(List(b1393),Block(Const(())))
    val b2291 = StreamOut(field="offset").name("b2291").ctrl(x2185) // x2166 = StreamOutNew(BurstCmdBus)
    val b2292 = StreamOut(field="size").name("b2292").ctrl(x2185) // x2166 = StreamOutNew(BurstCmdBus)
    val b2293 = StreamIn(field="data").name("b2293").ctrl(x2185) // x2167 = StreamInNew(BurstDataBus())
    val x2176 = UnitController(style=SeqPipe, level=InnerControl).name("x2176").ctrl(x2185) // UnitPipe(List(b1393),Block(x2175))
    val x2168 = OpDef(op=FixConvert, inputs=List(b1392)).name("x2168").ctrl(x2176) // FixConvert(b1392,TRUE,_32,_0)
    val x2169 = OpDef(op=FixSla, inputs=List(x2168, Const(2).ctrl(x2176))).name("x2169").ctrl(x2176) // FixLsh(x2168,Const(2))
    val x2170 = OpDef(op=FixConvert, inputs=List(x2169)).name("x2170").ctrl(x2176) // FixConvert(x2169,TRUE,_64,_0)
    val x2171 = top.dramAddress(x2150).name("x2171").ctrl(x2176) // GetDRAMAddress(x2150)
    val x2172 = OpDef(op=FixAdd, inputs=List(x2170, x2171)).name("x2172").ctrl(x2176) // FixAdd(x2170,x2171)
    val x2173 = OpDef(op=FixConvert, inputs=List(x2172)).name("x2173").ctrl(x2176) // FixConvert(x2172,TRUE,_64,_0)
    // x2174 = SimpleStruct(ArrayBuffer((offset,x2173), (size,Const(256)), (isLoad,Const(true))))
    val b2294 = WriteMems(List(b2291), x2173).name("b2294").ctrl(x2176) // StreamWrite(x2166,x2174,b1393)
    val b2295 = WriteMems(List(b2292), Const(256)).name("b2295").ctrl(x2176) // StreamWrite(x2166,x2174,b1393)
    val x2177 = FringeContainer(x2150,b2291,b2292,b2293).name("x2177").ctrl(x2185) // FringeDenseLoad(x2150,x2166,x2167)
    val x2178 = Counter(min=Const(0).ctrl(x2185), max=Const(64).ctrl(x2185), step=Const(1).ctrl(x2185), par=1).name("x2178").ctrl(x2185) // CounterNew(Const(0),Const(64),Const(1),Const(1))
    val x2179 = CounterChain(List(x2178)).name("x2179").ctrl(x2185) // CounterChainNew(List(x2178))
    val x2184 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2179).name("x2184").ctrl(x2185) // UnrolledForeach(List(b1393),x2179,Block(Const(())),List(List(b1410)),List(List(b1411)))
    val b1410 = CounterIter(x2178, None).ctrl(x2184).name("b1410")
    val b1411 = DummyOp().ctrl(x2184).name("b1411")
    val x2180 = OpDef(op=BitAnd, inputs=List(b1411, b1393)).name("x2180").ctrl(x2184) // And(b1411,b1393)
    val b2296 = ReadMem(b2293).name("b2296").ctrl(x2184) // ParStreamRead(x2167,List(x2180))
    val b2297 = b2296 // x2182 = VectorApply(x2181,0)
    val x2183 = StoreBanks(List(x2155_d0_b0), List(b1410), b2297).name("x2183").ctrl(x2184) // ParSRAMStore(x2155,List(List(b1410)),List(x2182),List(x2180))
    val x2186 = Counter(min=Const(0).ctrl(x2261), max=Const(64).ctrl(x2261), step=Const(1).ctrl(x2261), par=1).name("x2186").ctrl(x2261) // CounterNew(Const(0),Const(64),Const(1),Const(1))
    val x2187 = CounterChain(List(x2186)).name("x2187").ctrl(x2261) // CounterChainNew(List(x2186))
    val x2236 = LoopController(style=MetaPipe, level=OuterControl, cchain=x2187).name("x2236").ctrl(x2261) // UnrolledForeach(List(b1393),x2187,Block(Const(())),List(List(b1420)),List(List(b1421)))
    val b1420 = CounterIter(x2186, Some(0)).ctrl(x2236).name("b1420")
    val b1421 = DummyOp().ctrl(x2236).name("b1421")
    val x2188_d0 = Reg(init=0.0).name("x2188_d0").ctrl(x2236) // x2188 = RegNew(Const(0))
    val x2188_d1 = Reg(init=0.0).name("x2188_d1").ctrl(x2236) // x2188 = RegNew(Const(0))
    val x2192 = UnitController(style=SeqPipe, level=InnerControl).name("x2192").ctrl(x2236) // UnitPipe(List(b1421, b1393),Block(Const(())))
    val x2189 = OpDef(op=BitAnd, inputs=List(b1421, b1393)).name("x2189").ctrl(x2192) // And(b1421,b1393)
    val x2190 = LoadBanks(List(x2155_d0_b0), List(b1420)).name("x2190").ctrl(x2192) // SRAMLoad(x2155,ArrayBuffer(Const(64)),List(b1420),Const(0),x2189)
    val x2191 = StoreBanks(List(x2154_d0_b0, x2154_d1_b0), List(Const(0), Const(0)), x2190).name("x2191").ctrl(x2192) // RegFileShiftIn(x2154,List(Const(0), Const(0)),1,x2190,x2189)
    val x2193 = Counter(min=Const(0).ctrl(x2236), max=Const(8).ctrl(x2236), step=Const(1).ctrl(x2236), par=1).name("x2193").ctrl(x2236) // CounterNew(Const(0),Const(8),Const(1),Const(1))
    val x2194 = CounterChain(List(x2193)).name("x2194").ctrl(x2236) // CounterChainNew(List(x2193))
    val x2204 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2194).name("x2204").ctrl(x2236) // UnrolledReduce(List(b1421, b1393),x2194,x2188,Block((x2188) => Const(())),List(List(b1429)),List(List(b1430)))
    val b1429 = CounterIter(x2193, None).ctrl(x2204).name("b1429")
    val b1430 = DummyOp().ctrl(x2204).name("b1430")
    val x2195 = LoadBanks(List(x2154_d1_b0), List(Const(0), b1429)).name("x2195").ctrl(x2204) // ParRegFileLoad(x2154,List(List(Const(0), b1429)),List(Const(true)))
    val x2196 = x2195 // x2196 = VectorApply(x2195,0)
    val x2197 = OpDef(op=BitAnd, inputs=List(b1430, b1421)).name("x2197").ctrl(x2204) // And(b1430,b1421)
    val x2198 = OpDef(op=BitAnd, inputs=List(x2197, b1393)).name("x2198").ctrl(x2204) // And(x2197,b1393)
    val x2199 = ReadMem(x2188_d1).name("x2199").ctrl(x2204) // RegRead(x2188)
    val x2200 = OpDef(op=FixEql, inputs=List(b1429, Const(0).ctrl(x2204))).name("x2200").ctrl(x2204) // FixEql(b1429,Const(0))
    val x2201 = OpDef(op=FixAdd, inputs=List(x2196, x2199)).name("x2201").ctrl(x2204) // FixAdd(x2196,x2199)
    val x2202 = OpDef(op=BitAnd, inputs=List(b1421, b1393)).name("x2202").ctrl(x2204) // And(b1421,b1393)
    val x2203 = WriteMems(List(x2188_d0, x2188_d1), x2201).name("x2203").ctrl(x2204) // RegWrite(x2188,x2201,x2202)
    val x2205 = Reg(init=0.0).name("x2205").ctrl(x2236) // x2205 = RegNew(Const(0))
    val x2206_d0 = Reg(init=0.0).name("x2206_d0").ctrl(x2236) // x2206 = RegNew(Const(0))
    val x2206_d1 = Reg(init=0.0).name("x2206_d1").ctrl(x2236) // x2206 = RegNew(Const(0))
    val x2211 = UnitController(style=SeqPipe, level=InnerControl).name("x2211").ctrl(x2236) // UnitPipe(List(b1421, b1393),Block(Const(())))
    val x2207 = ReadMem(x2188_d0).name("x2207").ctrl(x2211) // RegRead(x2188)
    val x2208 = OpDef(op=FixSra, inputs=List(x2207, Const(4).ctrl(x2211))).name("x2208").ctrl(x2211) // FixRsh(x2207,Const(4))
    val x2209 = OpDef(op=BitAnd, inputs=List(b1421, b1393)).name("x2209").ctrl(x2211) // And(b1421,b1393)
    val x2210 = WriteMems(List(x2205), x2208).name("x2210").ctrl(x2211) // RegWrite(x2205,x2208,x2209)
    val x2212 = Counter(min=Const(0).ctrl(x2236), max=Const(8).ctrl(x2236), step=Const(1).ctrl(x2236), par=1).name("x2212").ctrl(x2236) // CounterNew(Const(0),Const(8),Const(1),Const(1))
    val x2213 = CounterChain(List(x2212)).name("x2213").ctrl(x2236) // CounterChainNew(List(x2212))
    val x2224 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2213).name("x2224").ctrl(x2236) // UnrolledReduce(List(b1421, b1393),x2213,x2206,Block((x2206) => Const(())),List(List(b1450)),List(List(b1451)))
    val b1450 = CounterIter(x2212, None).ctrl(x2224).name("b1450")
    val b1451 = DummyOp().ctrl(x2224).name("b1451")
    val x2214 = OpDef(op=FixAdd, inputs=List(b1450, Const(8).ctrl(x2224))).name("x2214").ctrl(x2224) // FixAdd(b1450,Const(8))
    val x2215 = LoadBanks(List(x2154_d0_b0), List(Const(0), x2214)).name("x2215").ctrl(x2224) // ParRegFileLoad(x2154,List(List(Const(0), x2214)),List(Const(true)))
    val x2216 = x2215 // x2216 = VectorApply(x2215,0)
    val x2217 = OpDef(op=BitAnd, inputs=List(b1451, b1421)).name("x2217").ctrl(x2224) // And(b1451,b1421)
    val x2218 = OpDef(op=BitAnd, inputs=List(x2217, b1393)).name("x2218").ctrl(x2224) // And(x2217,b1393)
    val x2219 = ReadMem(x2206_d1).name("x2219").ctrl(x2224) // RegRead(x2206)
    val x2220 = OpDef(op=FixEql, inputs=List(b1450, Const(0).ctrl(x2224))).name("x2220").ctrl(x2224) // FixEql(b1450,Const(0))
    val x2221 = OpDef(op=FixAdd, inputs=List(x2216, x2219)).name("x2221").ctrl(x2224) // FixAdd(x2216,x2219)
    val x2222 = OpDef(op=BitAnd, inputs=List(b1421, b1393)).name("x2222").ctrl(x2224) // And(b1421,b1393)
    val x2223 = WriteMems(List(x2206_d0, x2206_d1), x2221).name("x2223").ctrl(x2224) // RegWrite(x2206,x2221,x2222)
    val x2235 = UnitController(style=SeqPipe, level=InnerControl).name("x2235").ctrl(x2236) // UnitPipe(List(b1421, b1393),Block(Const(())))
    val x2225 = ReadMem(x2206_d0).name("x2225").ctrl(x2235) // RegRead(x2206)
    val x2226 = OpDef(op=FixSra, inputs=List(x2225, Const(4).ctrl(x2235))).name("x2226").ctrl(x2235) // FixRsh(x2225,Const(4))
    val x2227 = ReadMem(x2205).name("x2227").ctrl(x2235) // RegRead(x2205)
    val x2228 = OpDef(op=FixSub, inputs=List(x2227, x2226)).name("x2228").ctrl(x2235) // FixSub(x2227,x2226)
    val x2229 = OpDef(op=FixSra, inputs=List(x2228, Const(3).ctrl(x2235))).name("x2229").ctrl(x2235) // FixRsh(x2228,Const(3))
    val x2230 = OpDef(op=FixAdd, inputs=List(b1420, b1392)).name("x2230").ctrl(x2235) // FixAdd(b1420,b1392)
    val x2231 = OpDef(op=FixLt, inputs=List(x2230, Const(16).ctrl(x2235))).name("x2231").ctrl(x2235) // FixLt(x2230,Const(16))
    val x2232 = OpDef(op=MuxOp, inputs=List(x2231, Const(0).ctrl(x2235), x2229)).name("x2232").ctrl(x2235) // Mux(x2231,Const(0),x2229)
    val x2233 = OpDef(op=BitAnd, inputs=List(b1421, b1393)).name("x2233").ctrl(x2235) // And(b1421,b1393)
    val x2234 = StoreBanks(List(x2156_d0_b0), List(b1420), x2232).name("x2234").ctrl(x2235) // SRAMStore(x2156,ArrayBuffer(Const(64)),List(b1420),Const(0),x2232,x2233)
    val x2260 = UnitController(style=StreamPipe, level=OuterControl).name("x2260").ctrl(x2261) // UnitPipe(List(b1393),Block(Const(())))
    val b2298 = StreamOut(field="offset").name("b2298").ctrl(x2260) // x2237 = StreamOutNew(BurstCmdBus)
    val b2299 = StreamOut(field="size").name("b2299").ctrl(x2260) // x2237 = StreamOutNew(BurstCmdBus)
    val b2300 = StreamOut(field="data").name("b2300").ctrl(x2260) // x2238 = StreamOutNew(BurstFullDataBus())
    val b2301 = StreamIn(field="ack").name("b2301").ctrl(x2260) // x2239 = StreamInNew(BurstAckBus)
    val x2248 = UnitController(style=SeqPipe, level=InnerControl).name("x2248").ctrl(x2260) // UnitPipe(List(b1393),Block(x2247))
    val x2240 = OpDef(op=FixConvert, inputs=List(b1392)).name("x2240").ctrl(x2248) // FixConvert(b1392,TRUE,_32,_0)
    val x2241 = OpDef(op=FixSla, inputs=List(x2240, Const(2).ctrl(x2248))).name("x2241").ctrl(x2248) // FixLsh(x2240,Const(2))
    val x2242 = OpDef(op=FixConvert, inputs=List(x2241)).name("x2242").ctrl(x2248) // FixConvert(x2241,TRUE,_64,_0)
    val x2243 = top.dramAddress(x2153).name("x2243").ctrl(x2248) // GetDRAMAddress(x2153)
    val x2244 = OpDef(op=FixAdd, inputs=List(x2242, x2243)).name("x2244").ctrl(x2248) // FixAdd(x2242,x2243)
    val x2245 = OpDef(op=FixConvert, inputs=List(x2244)).name("x2245").ctrl(x2248) // FixConvert(x2244,TRUE,_64,_0)
    // x2246 = SimpleStruct(ArrayBuffer((offset,x2245), (size,Const(256)), (isLoad,Const(false))))
    val b2302 = WriteMems(List(b2298), x2245).name("b2302").ctrl(x2248) // StreamWrite(x2237,x2246,b1393)
    val b2303 = WriteMems(List(b2299), Const(256)).name("b2303").ctrl(x2248) // StreamWrite(x2237,x2246,b1393)
    val x2249 = Counter(min=Const(0).ctrl(x2260), max=Const(64).ctrl(x2260), step=Const(1).ctrl(x2260), par=1).name("x2249").ctrl(x2260) // CounterNew(Const(0),Const(64),Const(1),Const(1))
    val x2250 = CounterChain(List(x2249)).name("x2250").ctrl(x2260) // CounterChainNew(List(x2249))
    val x2256 = LoopController(style=InnerPipe, level=InnerControl, cchain=x2250).name("x2256").ctrl(x2260) // UnrolledForeach(List(b1393),x2250,Block(Const(())),List(List(b1489)),List(List(b1490)))
    val b1489 = CounterIter(x2249, None).ctrl(x2256).name("b1489")
    val b1490 = DummyOp().ctrl(x2256).name("b1490")
    val x2251 = OpDef(op=BitAnd, inputs=List(b1490, b1393)).name("x2251").ctrl(x2256) // And(b1490,b1393)
    val x2252 = LoadBanks(List(x2156_d0_b0), List(b1489)).name("x2252").ctrl(x2256) // ParSRAMLoad(x2156,List(List(b1489)),List(x2251))
    val x2253 = x2252 // x2253 = VectorApply(x2252,0)
    // x2254 = SimpleStruct(ArrayBuffer((_1,x2253), (_2,Const(true))))
    val b2304 = WriteMems(List(b2300), x2253).name("b2304").ctrl(x2256) // ParStreamWrite(x2238,List(x2254),List(x2251))
    val x2257 = FringeContainer(x2153,b2298,b2299,b2300,b2301).name("x2257").ctrl(x2260) // FringeDenseStore(x2153,x2237,x2238,x2239)
    val x2259 = UnitController(style=SeqPipe, level=InnerControl).name("x2259").ctrl(x2260) // UnitPipe(List(b1393),Block(Const(())))
    val b2305 = ReadMem(b2301).name("b2305").ctrl(x2259) // StreamRead(x2239,b1393)
    
  }
}
