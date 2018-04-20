package pir.pass

import pir.node._

class AccessLowering(implicit compiler:PIR) extends PIRTransformer {
  import pirmeta._

  override def runPass =  {
    val accesses = compiler.top.collectDown[LocalAccess]()
    accesses.foreach(lowerAccess)
  }

  def lowerAccess(n:N):Unit = {
    n match {
      case Def(n:LocalLoad, LocalLoad(banks, Some(addrs))) =>
        dbgblk(s"Lowering ${qdef(n)}") {
          val accessCU = globalOf(n).get 
          val bankCUs = banks.map { bank => bank -> globalOf(bank).get }.toMap
          val addrCUs = if (banks.size>1 || false /*TODO: if stages are only counters*/) {
            val addrCU = CUContainer().setParent(compiler.top).ctrl(ctrlOf(n))
            banks.map { _ -> addrCU }.toMap
          } else {
            bankCUs
          }
          val bankAccesses = banks.map { bank =>
            // Remote read address calculation
            val maddrs = addrs.map { addr => 
              mirror(addr, Some(addrCUs(bank)))
            }
            val bankCU = bankCUs(bank)
            val access = LoadMem(bank, maddrs).setParent(bankCU)
            dbg(s"add ${qtype(access)} in ${qtype(bankCU)}")
            pirmeta.mirror(n, access)
            retime(access, accessCU).asInstanceOf[LocalLoad]
          }
          val access = if (bankAccesses.size > 1) {
            val sb = SelectBanks(bankAccesses).setParent(accessCU)
            dbg(s"add ${qtype(sb)} in ${qtype(accessCU)}")
            sb
          } else bankAccesses.head
          swapNode(n,access)
        }
      case Def(n:LocalStore, LocalStore(banks, Some(addrs), data)) =>
        dbgblk(s"Lowering ${qdef(n)}") {
          banks.foreach { bank =>
            // Local write address calculation
            val bankCU = globalOf(bank).get 
            val dataLoad = retime(data, bankCU)
            val addrLoad = addrs.map { addr => retime(addr, bankCU) }
            dbg(s"disconnect ${qtype(n)} from ${qtype(bank)}")
            val access = StoreMem(bank, addrLoad, dataLoad).setParent(bankCU)
            dbg(s"add ${qtype(access)} in ${qtype(bankCU)}")
            swapNode(n,access, at=Some(List(bank)))
            access
          }
        }
      case Def(n:LocalLoad, LocalLoad(mem::Nil, None)) =>
        dbgblk(s"Lowering ${qdef(n)}") {
          val memCU = globalOf(mem).get
          val accessCU = globalOf(n).get 
          swapParent(n, memCU)
          val depeds = n.depeds.toList
          val raccess = retime(n, accessCU)
          swapNode(n,raccess,at=Some(depeds))
        }
      case Def(n:LocalStore, LocalStore(mem::Nil, None, data)) =>
        dbgblk(s"Lowering ${qdef(n)}") {
          val memCU = globalOf(mem).get
          swapParent(n, memCU)
        }
      case n =>
    }
  }

}
