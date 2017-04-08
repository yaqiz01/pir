package pir.mapper
import pir._
import pir.codegen.{Printer}
import pir.util.typealias._
import pir.util.maps._

import pir.pass.PIRMapping
import pir.exceptions._
import scala.collection.immutable.Set
import scala.collection.immutable.HashMap
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._

case class PIRMap(clmap:CLMap, vimap:VIMap, vomap:VOMap, 
  smmap:SMMap, ctmap:CTMap,
  fimap:FIMap, rcmap:RCMap, stmap:STMap, 
  ipmap:IPMap, opmap:OPMap,
  ucmap:UCMap, pmmap:PMMap, rtmap:RTMap,
  xbmap:XBMap) {
  
  //stmap.pirMap = this
  //ipmap.pirMap = this
  //opmap.pirMap = this
  //ctmap.pirMap = this
  //smmap.pirMap = this
  //ucmap.pirMap = this
  //pmmap.pirMap = this

  def set(cp:CLMap):PIRMap = PIRMap(cp   , vimap, vomap, smmap, ctmap, fimap, rcmap, stmap, ipmap, opmap, ucmap, pmmap, rtmap, xbmap)
  def set(cp:VIMap):PIRMap = PIRMap(clmap, cp   , vomap, smmap, ctmap, fimap, rcmap, stmap, ipmap, opmap, ucmap, pmmap, rtmap, xbmap)
  def set(cp:VOMap):PIRMap = PIRMap(clmap, vimap, cp   , smmap, ctmap, fimap, rcmap, stmap, ipmap, opmap, ucmap, pmmap, rtmap, xbmap)
  def set(cp:SMMap):PIRMap = PIRMap(clmap, vimap, vomap, cp   , ctmap, fimap, rcmap, stmap, ipmap, opmap, ucmap, pmmap, rtmap, xbmap)
  def set(cp:CTMap):PIRMap = PIRMap(clmap, vimap, vomap, smmap, cp   , fimap, rcmap, stmap, ipmap, opmap, ucmap, pmmap, rtmap, xbmap)
  def set(cp:FIMap):PIRMap = PIRMap(clmap, vimap, vomap, smmap, ctmap, cp   , rcmap, stmap, ipmap, opmap, ucmap, pmmap, rtmap, xbmap)
  def set(cp:RCMap):PIRMap = PIRMap(clmap, vimap, vomap, smmap, ctmap, fimap, cp   , stmap, ipmap, opmap, ucmap, pmmap, rtmap, xbmap)
  def set(cp:STMap):PIRMap = PIRMap(clmap, vimap, vomap, smmap, ctmap, fimap, rcmap, cp   , ipmap, opmap, ucmap, pmmap, rtmap, xbmap)
  def set(cp:IPMap):PIRMap = PIRMap(clmap, vimap, vomap, smmap, ctmap, fimap, rcmap, stmap, cp   , opmap, ucmap, pmmap, rtmap, xbmap)
  def set(cp:OPMap):PIRMap = PIRMap(clmap, vimap, vomap, smmap, ctmap, fimap, rcmap, stmap, ipmap, cp   , ucmap, pmmap, rtmap, xbmap)
  def set(cp:UCMap):PIRMap = PIRMap(clmap, vimap, vomap, smmap, ctmap, fimap, rcmap, stmap, ipmap, opmap, cp   , pmmap, rtmap, xbmap)
  def set(cp:PMMap):PIRMap = PIRMap(clmap, vimap, vomap, smmap, ctmap, fimap, rcmap, stmap, ipmap, opmap, ucmap, cp   , rtmap, xbmap)
  def set(cp:RTMap):PIRMap = PIRMap(clmap, vimap, vomap, smmap, ctmap, fimap, rcmap, stmap, ipmap, opmap, ucmap, pmmap, cp   , xbmap)
  def set(cp:XBMap):PIRMap = PIRMap(clmap, vimap, vomap, smmap, ctmap, fimap, rcmap, stmap, ipmap, opmap, ucmap, pmmap, rtmap, cp   )

  def setCL(k:CLMap.K, v:CLMap.V):PIRMap = set(clmap + ((k, v)))
  def setVI(k:VIMap.K, v:VIMap.V):PIRMap = set(vimap + ((k, v)))
  def setVO(k:VOMap.K, v:VOMap.V):PIRMap = set(vomap + ((k, v)))
  def setSM(k:SMMap.K, v:SMMap.V):PIRMap = set(smmap + ((k, v)))
  def setCt(k:CTMap.K, v:CTMap.V):PIRMap = set(ctmap + ((k, v)))
  def setFI(k:FIMap.K, v:FIMap.V):PIRMap = set(fimap + ((k, v)))
  def setRC(k:RCMap.K, v:RCMap.V):PIRMap = set(rcmap + ((k, v)))
  def setST(k:STMap.K, v:STMap.V):PIRMap = set(stmap + ((k, v)))
  def setIP(k:IPMap.K, v:IPMap.V):PIRMap = set(ipmap + ((k, v)))
  def setOP(k:OPMap.K, v:OPMap.V):PIRMap = set(opmap + ((k, v)))
  def setUC(k:UCMap.K, v:UCMap.V):PIRMap = set(ucmap + ((k, v)))
  def setPM(k:PMMap.K, v:PMMap.V):PIRMap = set(pmmap + ((k, v)))
  def setRT(k:RTMap.K, v:RTMap.V):PIRMap = set(rtmap + ((k, v)))
  def setXB(k:XBMap.K, v:XBMap.V):PIRMap = set(xbmap + ((k, v)))

  def quote(n:Any)(implicit design:Design) = n match {
    case n:Node => pir.util.quote(n)
    case n:PNode => pir.plasticine.util.quote(n)(design.arch)
    case n => s"$n"
  }

  def printMap(implicit p:Printer, design:Design):Unit = {
    fimap.printMap(quote _)
    design.top.ctrlers.foreach { cl => 
      cl match {
        case cl:CL =>
          if (clmap.map.contains(cl)) {
            val pcl = clmap.map(cl)
            p.emitBlock( s"$cl -> ${quote(pcl)}" ) {
              vimap.printMap(quote _, cl.vins)
              cl match {
                case cu:CU =>
                  val pcu = clmap.map(cu).asInstanceOf[PCU]
                  cu match {
                    case icl:ICL => smmap.printMap(quote _, icl.mems)
                    case _ =>
                  }
                  rcmap.printMap(quote _, rcmap.keys.filter(k => k.ctrler==cu).toList)
                  //stmap.printPMap(pcu.stages)
                  stmap.printMap(quote _, stmap.keys.filter(k => k.ctrler==cu).toList)
                case _ =>
              }
            }
          } else {
            p.emitln(s"$cl <- failed")
          }
        case _ =>
          p.emitln(s"$cl <- no mapping")
      }
    }
  }
  def printPMap(implicit p:Printer, design:Design):Unit = {
    fimap.printMap(quote _)
    design.arch.ctrlers.foreach { pcl => 
      if (clmap.pmap.contains(pcl)) {
        val cl = clmap.pmap(pcl)
        p.emitBlock( s"$pcl <- $cl" ) {
          vimap.printMap(quote _, cl.vins)
          pcl match {
            case pcu:PCU =>
              val cu = clmap.pmap(pcu).asInstanceOf[CU]
              smmap.printPMap(pcu.srams)
              ctmap.printPMap(pcu.ctrs)
              rcmap.printMap(quote _, rcmap.keys.filter(k => k.ctrler==cu).toList)
              stmap.printPMap(pcu.stages)
              //pmmap.printPMap(pcu.ctrlBox.luts)
            case _ =>
          }
        }
      } else {
        p.emitln(s"$pcl <- no mapping")
      }
    }
  }
}
object PIRMap {
  def empty:PIRMap = 
    PIRMap(CLMap.empty, VIMap.empty, VOMap.empty, SMMap.empty, CTMap.empty, 
           FIMap.empty,
           RCMap.empty, STMap.empty, IPMap.empty, OPMap.empty,
           UCMap.empty, PMMap.empty, RTMap.empty, XBMap.empty)
}

case class CLMap(map:CLMap.M, pmap:CLMap.IM) extends IBiOneToOneMap {
  type K = CLMap.K
  type V = CLMap.V
  override type M = CLMap.M
  override type IM = CLMap.IM
  override def + (rec:(K,V)) = { super.check(rec); CLMap(map + rec, pmap + rec.swap) }
  def apply(k:CU):PCU = { map(k).asCU }
}
object CLMap extends IBiOneToOneObj {
  type K = CL
  type V = PNE
  def empty:CLMap = CLMap(Map.empty, Map.empty)
}

/* A mapping between a Input (VecIn or ScalarIn) with PInBus */
case class VIMap(map:VIMap.M, pmap:VIMap.IM) extends IBiManyToOneMap {
  type K = VIMap.K
  type V = VIMap.V
  override type M = VIMap.M
  override type IM = VIMap.IM
  override def + (rec:(K,V)) = { 
    super.check(rec)
    val set:Set[K] = (pmap.getOrElse(rec._2, Set.empty) + rec._1)
    val v:V = rec._2
    val npmap:IM = pmap + ((v, set))
    VIMap(map + rec, npmap)
  }
  def apply(n:VI):PGI[PModule] = { map(n).asGlobal[PModule] }
  def apply(n:SI):PGI[PModule] = { map(n).asGlobal[PModule] }
}
object VIMap extends IBiManyToOneObj {
  type K = Node //InPort or VecIn
  type V = PI[_<:PModule]
  def empty:VIMap = VIMap(Map.empty, Map.empty)
}

/* A mapping between a Input (VecIn or ScalarIn) with PInBus */
case class VOMap(map:VOMap.M, pmap:VOMap.IM) extends IBiOneToManyMap {
  type K = VOMap.K
  type V = VOMap.V
  override type M = VOMap.M
  override type IM = VOMap.IM
  override def + (rec:(K,V)) = { 
    super.check(rec)
    val os:Set[V] = map.getOrElse(rec._1, Set.empty)
    val set:Set[V] = os + rec._2
    val newmap = map + (rec._1 -> set)
    VOMap(newmap, pmap + rec.swap)
  }
  def apply(n:VO):Set[PGO[PModule]] = { map(n).map(_.asGlobal[PModule]) }
  def apply(n:SO):Set[PGO[PModule]] = { map(n).map(_.asGlobal[PModule]) }
}
object VOMap extends IBiOneToManyObj {
  type K = Node //OutPort or VecOut
  type V = PO[_<:PModule]
  def empty:VOMap = VOMap(Map.empty, Map.empty)
}

/* A Map between PIR Counter to Spade Counter */
case class SMMap(map:SMMap.M, pmap:SMMap.IM) extends IBiOneToOneMap {
  type K = SMMap.K
  type V = SMMap.V
  override type M = SMMap.M
  override type IM = SMMap.IM
  override def + (rec:(K,V)) = { super.check(rec); SMMap(map + rec, pmap + rec.swap) }
  def apply(n:SRAM):PSRAM = { map(n).asSRAM }
}
object SMMap extends IBiOneToOneObj {
  type K = OCM
  type V = POCM
  def empty:SMMap = SMMap(Map.empty, Map.empty)
}

/* A Map between PIR Counter to Spade Counter */
case class CTMap(map:CTMap.M, pmap:CTMap.IM) extends IBiOneToOneMap {
  type K = CTMap.K
  type V = CTMap.V
  override type M = CTMap.M
  override type IM = CTMap.IM
  override def + (rec:(K,V)) = { super.check(rec); CTMap(map + rec, pmap + rec.swap) }
}
object CTMap extends IBiOneToOneObj {
  type K = Ctr
  type V = PCtr
  def empty:CTMap = CTMap(Map.empty, Map.empty)
}

/* A mapping between a scalar value and its writer's (OutBus, Index of Scalar Port in the Bus) */
case class RCMap(map:RCMap.M) extends IOneToOneMap {
  type K = RCMap.K
  type V = RCMap.V
  override type M = RCMap.M
  override def + (rec:(K,V)) = { super.check(rec); RCMap(map + rec) }
}
object RCMap extends IOneToOneObj {
  type K = Reg 
  type V = PReg 
  def empty:RCMap = RCMap(Map.empty)
}
/* A mapping between Stage and PStage */
case class STMap(map:STMap.M, pmap:STMap.IM) extends IBiOneToOneMap {
  type K = STMap.K
  type V = STMap.V
  override type M = STMap.M
  override type IM = STMap.IM
  override def + (rec:(K,V)) = { super.check(rec); STMap(map + rec, pmap + rec.swap) }
}
object STMap extends IBiOneToOneObj {
  type K = ST 
  type V = PST
  def empty:STMap = STMap(Map.empty, Map.empty)
}
/* FanIn map: a mapping between a PInput and the POutput it connects to */
case class FIMap(map:FIMap.M) extends IOneToOneMap {
  type K = FIMap.K
  type V = FIMap.V
  override type M = FIMap.M
  override def + (rec:(K,V)) = { super.check(rec); FIMap(map + rec) }
}
object FIMap extends IOneToOneObj {
  type K = PI[_<:PModule]
  type V = PO[_<:PModule]
  def empty:FIMap = FIMap(Map.empty)
}
/* XbarMap: mapping between output and input of xbar */
case class XBMap(map:XBMap.M) extends IOneToOneMap {
  type K = XBMap.K
  type V = XBMap.V
  override type M = XBMap.M
  override def + (rec:(K,V)) = { super.check(rec); XBMap(map + rec) }
}
object XBMap extends IOneToOneObj {
  type K = PIO[_<:PModule]
  type V = PIO[_<:PModule]
  def empty:XBMap = XBMap(Map.empty)
}
/* A mapping between InPort and PInPort */
case class IPMap(map:IPMap.M, pmap:IPMap.IM) extends IBiOneToOneMap {
  type K = IPMap.K
  type V = IPMap.V
  override type M = IPMap.M
  override type IM = IPMap.IM
  override def + (rec:(K,V)) = { super.check(rec); IPMap(map + rec, pmap + rec.swap) }
  //def printInPort(ip:IP)(implicit p:Printer, design:Design) = {
    //if (map.contains(ip)) {
      //val pip = map(ip)
      //val fimap = pirMap.fimap
      //if (fimap.contains(pip)) {
        //val pop = fimap(pip)
        //p.emitln(s"${ip} -> ${pip} <- ${pop}")
      //} else {
        //p.emitln(s"${ip} -> ${pip} <- failed")
      //}
    //} else {
      //p.emitln(s"${ip} -> failed")
    //}
  //}
  //def printInPort(pip:PI)(implicit p:Printer, design:Design) = {
    //if (pmap.contains(pip)) {
      //val ip = pmap(pip)
      //val fimap = pirMap.fimap
      //if (fimap.contains(pip)) {
        //val pop = fimap(pip)
        //p.emitln(s"${pip}(${ip}) <- ${pop}")
      //} else {
        //p.emitln(s"${pip}(${ip}) <- failed")
      //}
    //} //else {
      ////p.emitln(s"${pip} -> no map")
    ////}
  //}
}
object IPMap extends IBiOneToOneObj {
  type K = IP 
  type V = PI[_<:PModule]
  def empty:IPMap = IPMap(Map.empty, Map.empty)
}
/* A mapping between OutPort and the POutPort */
case class OPMap(map:OPMap.M, pmap:OPMap.IM) extends IBiOneToOneMap {
  type K = OPMap.K
  type V = OPMap.V
  override type M = OPMap.M
  override type IM = OPMap.IM
  override def + (rec:(K,V)):OPMap = { super.check(rec); OPMap(map + rec, pmap + rec.swap) }
  def printOutPort(op:OP)(implicit p:Printer, design:Design) = {
    if (map.contains(op)) {
      p.emitln(s"${op} -> ${map(op)}")
    } else {
      p.emitln(s"${op} -> failed")
    }
  }
  def printOutPort(pop:PO[_<:PModule])(implicit p:Printer, design:Design) = {
    if (pmap.contains(pop)) {
      p.emitln(s"${pop} <- ${pmap(pop)}")
    }// else {
      //p.emitln(s"${pop} <- no map")
    //}
  }
}
object OPMap extends IBiOneToOneObj {
  type K = OP
  type V = PO[_<:PModule]
  def empty:OPMap = OPMap(Map.empty, Map.empty)
}

case class UCMap(map:UCMap.M, pmap:UCMap.IM) extends IBiOneToOneMap {
  type K = UCMap.K
  type V = UCMap.V
  override type M = UCMap.M
  override type IM = UCMap.IM
  override def + (rec:(K,V)) = { super.check(rec); UCMap(map + rec, pmap + rec.swap) }
}
object UCMap extends IBiOneToOneObj {
  type K = UC
  type V = PUC
  def empty:UCMap = UCMap(Map.empty, Map.empty)
}

/* Primitive Node Mapping */
case class PMMap(map:PMMap.M, pmap:PMMap.IM) extends IBiOneToOneMap {
  type K = PMMap.K
  type V = PMMap.V
  override type M = PMMap.M
  override type IM = PMMap.IM
  override def + (rec:(K,V)) = { super.check(rec); PMMap(map + rec, pmap + rec.swap) }
  def apply(k:LUT):PLUT = { map(k).asInstanceOf[PLUT] }
  def apply(k:Const):PConst = { map(k).asInstanceOf[PConst] }
}
object PMMap extends IBiOneToOneObj {
  type K = Node
  type V = PNode
  def empty:PMMap = PMMap(Map.empty, Map.empty)
}

case class RTMap(map:RTMap.M) extends IOneToOneMap {
  type K = RTMap.K
  type V = RTMap.V
  override type M = RTMap.M
  override def + (rec:(K,V)) = { super.check(rec); RTMap(map + rec) }
}
object RTMap extends IOneToOneObj {
  type K = Node //InPort or VecIn
  type V = Int 
  def empty:RTMap = RTMap(Map.empty)
}
