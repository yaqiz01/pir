package pir.graph

import scala.collection.mutable.Set
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.math.max
import pir._
import pir.graph._
import pir.util._

/** Base class for all PIR nodes. 
  * @param name: optional user name for a node 
  * @param typeStr: Consice name for a type of node for printing purpose 
  */
abstract class Node (implicit val design: Design) { 
  val pirmeta:PIRMetadata = design
  import pirmeta._

  val name:Option[String]
  val typeStr:String
	design.addNode(this)
  val id : Int = design.nextId // Unique id for each node
  def toUpdate:Boolean = false // Whether fields of the node is not yet defined, 
                               // which would be updated later. Debug purpose only 

  override def equals(that: Any) = that match {
    case n: Node => super.equals(that) && id == n.id
    case _ => super.equals(that)
  }

  override def toString = s"${typeStr}${id}${name.fold("")(n => s"_${n}")}" 
  def bound(b:AnyVal):this.type = { boundOf(this) = b; this }
}

trait Module extends Node {
  implicit val self:Module = this
  val _ins = ListBuffer[InPort]()
  def ins:List[InPort] = _ins.toList
  val _outs = ListBuffer[OutPort]()
  def outs:List[OutPort] = _outs.toList
  def addIO(io:Port) = io match {
    case input:InPort => _ins += input
    case output:OutPort => _outs += output
  }
  def ios:List[Port] = ins ++ outs

  def isConst:Boolean = this.isInstanceOf[Const[_]]

  def asSRAM:SRAM = this.asInstanceOf[SRAM]
}
