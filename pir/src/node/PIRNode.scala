package pir
package node

import prism.graph._

import scala.collection.mutable

abstract class PIRNode(implicit env:BuildEnvironment) 
  extends EnvNode[PIRNode] 
  with FieldNode[PIRNode] 
  with DefNode[PIRNode] { self =>
  lazy val Nct = classTag[PIRNode]

  val name = new Metadata[String]("name")
  val sname = new Metadata[String]("sname") {
    override def check(v:String) = {}
  }
  val srcCtx = new Metadata[String]("srcCtx") {
    override def check(v:String) = {}
  }

  val ctrl = new Metadata[ControlTree]("ctrl") {
    override def apply(value:ControlTree) = self match {
      case self:GlobalContainer => getSelf
      case self => super.apply(value)
    }
    override def reset = {
      self match {
        case _:Controller => value.foreach { v => v.ctrler.reset }
        case _ =>
      }
      super.reset
    }
  }

  // Scale is relative rate of a node active to ctx enable
  val scale = new Metadata[Option[Long]]("scale") {
    override def mirror(frommeta:MetadataLike[_]) = {}
  }
  // Count is total number of time a node is active
  val count = new Metadata[Option[Long]]("count")
  // Iter is how many iteration a node run between enabled and done. Independent of what it stacks on
  val iter = new Metadata[Option[Long]]("iter")

  override def asOutput = output

  env.initNode(this)
}

trait PIRNodeUtil extends MemoryUtil with AccessUtil {
  implicit class PIRNodeOp(n:PIRNode) {
    def ctx = n.collectUp[Context]().headOption
    def global = n.collectUp[GlobalContainer]().headOption
    def isUnder[T:ClassTag] = n.ancestors.exists { _.to[T].nonEmpty }
    def traceTo(y:N):Boolean = n match {
      case n if n == y => true
      case n:BufferRead => n.in.T.traceTo(y)
      case n:BufferWrite => n.data.T.traceTo(y)
      case n => false
    }
  }
  implicit class EdgeOp(n:Edge) {
    def traceTo(y:PIRNode):Boolean = 
      n.connected.exists { _.src.as[PIRNode].traceTo(y) }
  }
}

case class ControlTree(schedule:String)(implicit env:Env) extends EnvNode[ControlTree] with FieldNode[ControlTree] { self =>
  lazy val Nct = classTag[ControlTree]

  val ctrler = new Metadata[Controller]("ctrler")

  env.initNode(this)
}
