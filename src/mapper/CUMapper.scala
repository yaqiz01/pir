package pir.mapper
import pir.graph._
import pir.{Design, Config}
import pir.typealias._
import pir.misc._
import pir.codegen.Printer
import pir.graph.traversal.{CUCtrlDotPrinter, CUVectorDotPrinter, MapPrinter, PIRMapping}
import pir.plasticine.main._

import scala.collection.immutable.Set
import scala.collection.immutable.HashMap
import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Success, Failure}

class CUMapper(implicit ds:Design) extends Mapper {
  def design:Design = ds 
  def typeStr = "CUMapper"
  implicit def spade:Spade = ds.arch

  override implicit val mapper:CUMapper = this

  override val exceptLimit = 200

  val routers = ListBuffer[Router]()
  routers += new VectorRouter()
  routers += new ScalarRouter()
  routers += new ControlRouter()

  def finPass(m:M):M = m
  override def debug = Config.debugCUMapper

  lazy val resMap:Map[CL, List[PNE]] = qualifyCheck

  def check(info:String, cond:Any):(Boolean,String) = {
      cond match {
        case cond:Boolean => 
          //if (!cond) dprintln(s"$info: pass:$cond numNodes:$nn numRes:$nr")
          (cond, s"$info: false")
        case (nn:Int, nr:Int) => val (pass,_) = check(info, (nn <= nr))
          (pass, s"$info: numNode:$nn numRes:$nr")
        case (ns:Iterable[_], rs:Iterable[_]) => check(info, (ns.size, rs.size))
        case c => throw PIRException(s"Unknown checking format: $cond")
      }
  }

  def check(conds:List[(String, Any)], failureInfo:ListBuffer[String]):Boolean = {
    conds.foldLeft(true) { case (qualify, cond) => 
      val (info, c) = cond
      val (pass, fi) = check(info, c) 
      if (!pass) failureInfo += fi 
      qualify && pass 
    }
  }

  /* 
   * Filter qualified resource. Create a mapping between cus and qualified pcus for each cu
   * */
  def qualifyCheck:Map[CL, List[PNE]] = {
    val pnes = design.arch.pnes
    val cls = design.top.ctrlers
    val mcs = cls.collect { case mc:MC => mc }
    val pmcs = design.arch.mcs 
    val scus = mcs.filter { _.mctpe.isDense }.map { mc => mc.mcfifos("offset").writer.ctrler.asInstanceOf[StreamPipeline] }
    val pscus = design.arch.scus 
    val mcus = cls.collect { case mp:MP => mp }
    val pmcus = design.arch.mcus 
    val ocus = cls.collect { case ocu:OCL => ocu }
    val pocus = design.arch.ocus 
    val rcus = cls.collect { case pcu:CU => pcu }.diff(scus).diff(mcs).diff(ocus).diff(mcus)
    val pcus = design.arch.pcus 
    if (mcs.size > pmcs.size) throw OutOfPMC(pmcs, mcs)
    if (ocus.size > pocus.size) throw OutOfOCU(pocus, ocus)
    if (mcus.size > pmcus.size) throw OutOfMCU(pmcus, mcus)
    if (scus.size > pscus.size) throw OutOfSCU(pscus, scus)
    if (rcus.size > pcus.size) throw OutOfPCU(pcus, rcus)
    val map = MMap[CL, List[PNE]]()
    map += design.top -> List(design.arch.top)
    mcs.foreach { cl => map += cl -> pmcs }
    ocus.foreach { cl => map += cl -> pocus }
    mcus.foreach { cl => map += cl -> pmcus }
    scus.foreach { cl => map += cl -> pscus }
    rcus.foreach { cl => map += cl -> pcus }
    cls.foreach { cl => 
      val failureInfo = MMap[PNE, ListBuffer[String]]()
      map += cl -> map(cl).filter { pne =>
        val cons = ListBuffer[(String, Any)]()
        cl match {
          case top:Top =>
            //cons += (("sin"	      , (cl.sins.size, pne.vins.size))) //TODO
          case mc:MC =>
          case scu:SP =>
          case cu:ICL  =>
            val pcu = pne.asInstanceOf[PCU]
            cons += (("reg"	      , (cu.infGraph, pcu.regs)))
            cons += (("ctr"	      , (cu.cchains.flatMap(_.counters), pcu.ctrs)))
            cons += (("stage"	    , (cu.stages, pcu.stages)))
            cons += (("udc"	      , (cu.udcounters, pcu.ctrlBox.udcs)))
            cons += (("sin"	      , (cl.sins, pcu.sins)))
            cons += (("sout"	    , (cl.souts, pcu.souts)))
            cons += (("vin"	      , (cl.vins.filter(_.isConnected), pcu.vins.filter(_.fanIns.size>0))))
            cons += (("vout"	    , (cl.vouts.filter(_.isConnected), pcu.vouts.filter(_.fanOuts.size>0))))
            cons += (("cin"	      , (cl.ctrlIns.filter(_.isConnected).map(_.from).toSet, pcu.cins.filter(_.fanIns.size>0))))
            cons += (("cout"	    , (cl.ctrlOuts.filter(_.isConnected), pcu.couts.filter(_.fanOuts.size>0))))
            cu match {
              case mc:MemoryController => 
              case _ => 
                cons += (("onchipmem"	, (cu.mems, pcu.srams)))
            }
          case cu:OCL =>
            val pocu = pne.asInstanceOf[POCU]
            cons += (("sin"	      , (cl.sins, pocu.scalarIO.ins)))
            cons += (("cin"	      , (cl.ctrlIns.filter(_.isConnected).map(_.from).toSet, pocu.ctrlIO.ins.filter(_.fanIns.size>0))))
            cons += (("cout"	    , (cl.ctrlOuts.filter(_.isConnected), pocu.ctrlIO.outs.filter(_.fanOuts.size>0))))
        }
        failureInfo += pne -> ListBuffer[String]()
        check(cons.toList, failureInfo(pne))
      }
      if (map(cl).size==0) {
        val info = failureInfo.map{ case (pne, info) => s"$pne: [${info.mkString(",")}] \n"}.mkString(",")
        println(s"info:${info}")
        throw CUOutOfSize(cl, info)
      }
      mapper.dprintln(s"qualified resource: $cl -> [${map(cl).map{ pcl => quote(pcl)}.reduce(_ + "," + _)}]")
    }
    map.toMap
  }

  def place(cl:CL, pne:PNE, m:M):M = {
    log((s"Try $cl -> ${quote(pne)}", false)) {
      routers.foldLeft(m.setCL(cl, pne)) { case (pm, router) =>
        router.route(cl, pm)
      }
    }
  }

  def resFunc(cl:CL, m:M, triedRes:List[PNE]):List[PNE] = {
    implicit val spade:Spade = design.arch
    log((s"$cl resFunc:", false)) {
      dprintln(s"--triedRes:[${triedRes.mkString(",")}]")
      var pnes = resMap(cl).filterNot( pne => triedRes.contains(pne) || m.clmap.pmap.contains(pne) )
      dprintln(s"--not mapped and tried:[${pnes.mkString(",")}]")
      cl match {
        case cl:MC if cl.mctpe.isDense => 
          val sp = cl.mcfifos("offset").writer.ctrler
          if (m.clmap.contains(sp)) {
            pnes = pnes.filter{ pne => pne.coord == m.clmap(sp).coord }
          }
        case sp:SP =>
          val mcs = sp.writtenFIFO.filter{ _.isOfsFIFO }
          if (!mcs.isEmpty && m.clmap.contains(mcs.head.ctrler)) {
            pnes = pnes.filter{ pne => pne.coord == m.clmap(mcs.head.ctrler).coord }
          }
        case _ =>
      }
      dprintln(s"--mc filtered:[${pnes.mkString(",")}]")
      pnes = routers.foldLeft(pnes) { case (pnes, router) =>
        router.filterPNE(cl, pnes, m)
      }
      dprintln(s"--routers filtered:[${pnes.mkString(",")}] ")
      if (pnes.size==0) throw MappingException(this, m, s"No pcu available for $cl")
      else pnes
    }
  }

  def map(m:M):M = {
    dprintln(s"Datapath placement & routing ")
    val nodes = design.top.topoSort
    val reses = design.arch.pnes
    info(s"Number of cus:${nodes.size}")
    bind(
      allNodes = nodes,
      initMap = m,
      constrain = place _,
      resFunc = resFunc _,
      finPass = finPass _ 
    )
  }

}

case class CUOutOfSize(cl:CL, info:String) (implicit val mapper:CUMapper, design:Design) extends MappingException(PIRMap.empty) {
  override val msg = s"cannot map ${cl} due to resource constrains\n${info}"
} 
case class OutOfPMC(pnodes:List[PMC], nodes:List[MC]) (implicit val mapper:CUMapper, design:Design) extends OutOfResource(PIRMap.empty) {
  override val msg = s"Not enough MemoryController in ${design.arch} to map application."
} 
case class OutOfPCU(pnodes:List[PCU], nodes:List[CU]) (implicit val mapper:CUMapper, design:Design) extends OutOfResource(PIRMap.empty) {
  override val msg = s"Not enough ComputeUnits in ${design.arch} to map application."
} 
case class OutOfSCU(pnodes:List[PSCU], nodes:List[SP]) (implicit val mapper:CUMapper, design:Design) extends OutOfResource(PIRMap.empty) {
  override val msg = s"Not enough ScalarComputeUnits in ${design.arch} to map application."
} 
case class OutOfMCU(pnodes:List[PMCU], nodes:List[MP]) (implicit val mapper:CUMapper, design:Design) extends OutOfResource(PIRMap.empty) {
  override val msg = s"Not enough MemoryComputeUnits in ${design.arch} to map application."
} 
case class OutOfOCU(pnodes:List[POCU], nodes:List[OCL]) (implicit val mapper:CUMapper, design:Design) extends OutOfResource(PIRMap.empty) {
  override val msg = s"Not enough OuterComputeUnit in ${design.arch} to map application."
} 
