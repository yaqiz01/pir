package prism
package graph

import scala.collection.mutable

trait MetadataIR extends Serializable { self =>

  val metadata = mutable.Map[String, Metadata[_]]()
  
  case class Metadata[T](name:String, default:Option[T] = None) extends MetadataLike[T] {
    value = default
    metadata += name -> this

    override def toString = s"$self.$name"
    def getSelf:self.type = self
    def apply(value:T):self.type = { :=(value); self }
    def get:T = value.getOrElse(throw PIRException(s"$self.$name is empty"))
  }
  object Metadata {
    def apply[T](name:String):Metadata[T] = Metadata[T](name, None)
    def apply[T](name:String, default:T):Metadata[T] = Metadata[T](name, Some(default))
  }

  def mirrorMetas(from:MetadataIR):self.type = {
    metadata.foreach { case (name, tometa) =>
      from.metadata.get(name).foreach { frommeta =>
        tometa.mirror(from, self, frommeta)
      }
    }
    self
  }

  def getMeta[T](name:String, default:Option[T]=None):Metadata[T] = {
    metadata.getOrElseUpdate(name, Metadata[T](name, default)).asInstanceOf[Metadata[T]]
  }

}

abstract class MetadataLike[T] extends Serializable {
  val name:String
  val default:Option[T]
  var value:Option[T] = None

  def check(v:T) = if (value.nonEmpty && Some(v) != value && value != default) throw PIRException(s"$this already has value $value, but reupdate to $v")
  def :=(v:T) = { check(v); value = Some(v) }
  def update(v:Any) = :=(v.asInstanceOf[T])
  def v:Option[T] = value
  def get:T
  def nonEmpty = v.nonEmpty
  def isEmpty = v.isEmpty
  def map[A](func:T => A):Option[A] = value.map(func)
  def getOrElseUpdate(v: => T):T = {
    if (value.isEmpty) :=(v)
    get
  }
  def reset = value = default
  def mirror(from:MetadataIR, to:MetadataIR, frommeta:MetadataLike[_]):Unit = mirror(frommeta)
  def mirror(frommeta:MetadataLike[_]) = frommeta.value.foreach { v => update(v) }
}