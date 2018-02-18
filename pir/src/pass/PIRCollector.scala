package pir.pass

import pir._
import pir.node._
import prism.traversal._

import scala.reflect._
import prism.codegen.Logging

trait PIRCollector extends GraphCollector {

  def collectUp[M<:PIRNode:ClassTag](n:PIRNode, depth:Int= -1, logger:Option[Logging]=None):List[M] =
    super.collectUp[PIRNode, M](n, depth, logger)

  def collectDown[M<:PIRNode:ClassTag](n:PIRNode, depth:Int= -1, logger:Option[Logging]=None):List[M] = 
    super.collectDown[PIRNode, M](n, depth, logger)

  def collectIn[M<:PIRNode:ClassTag](n:PIRNode, depth:Int=10, logger:Option[Logging]=None):List[M] = 
    super.collectIn[PIRNode, M](n, depth, logger)

  def collectOut[M<:PIRNode:ClassTag](n:PIRNode, depth:Int=10, logger:Option[Logging]=None):List[M] = 
    super.collectOut[PIRNode, M](n, depth, logger)

  def globalOf(n:PIRNode) = {
    collectUp[GlobalContainer](n).headOption
  }

  def contextOf(n:PIRNode) = {
    collectUp[ComputeContext](n).headOption
  }

  def enableOf(n:PIRNode) = {
    n match {
      case n:ComputeContext => collectDown[ContextEnable](n).headOption
      case _ => None
    }
  }

}

