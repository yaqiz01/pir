package pir.plasticine.config
                          
import pir.plasticine.graph._
import pir.plasticine.main._

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._
import pir.graph.enums._

// 4 cu + 2 tt

// Assume no scalarIn and scalarOut buffer are before and after pipeline stages.
// Still have scalarIn and scalarOut as node but make sure # scalarIn and # scalarOut always equal
// to outports and inports of inbus and outbus
object P2P_2CU_2TT extends PointToPointNetwork {

  // input <== output: input can be configured to output
  // input <== outputs: input can be configured to 1 of the outputs
  
  // Inner CU Specs
  override val wordWidth = 32
  override val numLanes = 4
  override val scalarBandwidth = numLanes // BO, how many scalar registers can be read from each bus
  override val numScalarInReg = numLanes // BO, how many scalar registers can be read from each bus
  
  private val numRCUs = 2
  private val numTTs = 2 

  private val numArgIns = scalarBandwidth  // need to be multiple of scalarBandwith 
  private val numArgOuts = scalarBandwidth // need to be multiple of scalarBandwith 

  // Top level controller ~= Host
  override val top = Top(numArgIns, numArgOuts).index(-1)

  override val rcus = List.tabulate(numRCUs) { i =>
    val cu = ConfigFactory.genRCU(numSRAMs = 2, numCtrs = 8, numRegs = 20).index(i)
    ConfigFactory.genMapping(cu, vinsPtr=12, voutPtr=0, sinsPtr=12, soutsPtr=0, ctrsPtr=0, waPtr=1, wpPtr=1, loadsPtr=8, rdPtr=0)
    cu
  } 

  override val ttcus = List.tabulate(numTTs) { i =>
    val cu = ConfigFactory.genTT(numSRAMs = 0, numCtrs = 4, numRegs = 20).index(i+rcus.size)
    ConfigFactory.genMapping(cu, vinsPtr=12, voutPtr=0, sinsPtr=12, soutsPtr=0, ctrsPtr=0, waPtr=1, wpPtr=1, loadsPtr=8, rdPtr=0)
    cu
  }

  /* Network Constrain */ 
  rcus(0).vins(0) <== ttcus(0).vout 
  rcus(0).vins(1) <== ttcus(1).vout
  rcus(1).vins(0) <== rcus(0).vout 
  rcus(1).vins(1) <== rcus(0).vout

  ConfigFactory.genArgIOConnection
}