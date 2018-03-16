package pir.mapper

import pir._
import pir.node._

import spade.node._
import spade.network._

import prism._
import prism.util._

trait ResourcePruning { self:PIRPass =>

  val constrains:List[Constrain]
  def initCUMap:CUMap = {
    var cumap = CUMap.empty
    val topP = compiler.top
    val topS = compiler.arch.top
    var pnodes1 = topP.collectDown[GlobalContainer]()
    var snodes1 = topS.collectDown[Routable]()
    val (afgP, pnodes2) = pnodes1.partition(isAFG)
    val (afgS, snodes2) = snodes1.partition { _.isInstanceOf[spade.node.ArgFringe] }
    val (dfgP, pnodes3) = pnodes2.partition(isDFG)
    val (dfgS, snodes3) = snodes2.partition { _.isInstanceOf[spade.node.MC] }
    val restP = pnodes3
    val restS = snodes3
    cumap ++= afgP.toSet[PNode] -> afgS.toSet[SNode]
    cumap ++= dfgP.toSet[PNode] -> dfgS.toSet[SNode]
    cumap ++= restP.toSet[PNode] -> restS.toSet[SNode]
    prune(cumap)
  }
  def prune(cumap:CUMap):CUMap = {
    constrains.foldLeft(cumap) { case (cumap, constrain) => constrain.prune(cumap) }
  }


  trait Constrain {
    def prune(cumap:CUMap):CUMap
  }
  abstract class QuantityConstrain extends Constrain {
    def numPNodes(cuP:CUMap.K):Int
    def numSnodes(cuS:CUMap.V):Int
    def prune(cumap:CUMap):CUMap = {
      cumap.multiplyFactor { case (cuP,cuS) =>
        if (numPNodes(cuP) > numSnodes(cuS)) 0 else 1
      }
    }
  }
  object StageConstrain extends QuantityConstrain {
    def numPNodes(cuP:CUMap.K):Int = cuP.collectDown[StageDef]().size
    def numSnodes(cuS:CUMap.V):Int = cuS.collectDown[Stage]().size
  }
  object ControlFIFOConstrain extends QuantityConstrain {
    def numPNodes(cuP:CUMap.K):Int = cuP.collectDown[pir.node.FIFO]().filter{ fifo => isBit(fifo) }.size
    def numSnodes(cuS:CUMap.V):Int = cuS.collectDown[spade.node.FIFO[_]]().filter(is[Bit]).size
  }
  object ScalarFIFOConstrain extends QuantityConstrain {
    def numPNodes(cuP:CUMap.K):Int = cuP.collectDown[pir.node.FIFO]().filter{ fifo => isScalar(fifo) }.size
    def numSnodes(cuS:CUMap.V):Int = cuS.collectDown[spade.node.FIFO[_]]().filter(is[Word]).size
  }
  object VectorFIFOConstrain extends QuantityConstrain {
    def numPNodes(cuP:CUMap.K):Int = cuP.collectDown[pir.node.FIFO]().filter{ fifo => isVector(fifo) }.size
    def numSnodes(cuS:CUMap.V):Int = cuS.collectDown[spade.node.FIFO[_]]().filter(is[Vector]).size
  }
  object LaneConstrain extends QuantityConstrain {
    def numPNodes(cuP:CUMap.K):Int = parOf(cuP).get
    def numSnodes(cuS:CUMap.V):Int = cuS.collectDown[Stage]().size
  }
}

