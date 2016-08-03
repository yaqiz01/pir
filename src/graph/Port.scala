package pir.graph

import scala.collection.mutable.Set
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.math.max
import pir.Design
import pir.graph._

class Range (s:Port, e:Port) {
  val start:Port = s
  val end:Port = e
  def by(step:Port) = (start, end, step)
}

/**
 * A type representing a group of wires in pir
 */
trait Port{
  val src:Node

  def width(implicit design:Design) = design.arch.wordWidth
  def by(step:Port)(implicit design:Design) = (Const(0l).out, this, step)
  def until(max:Port) = new Range(this, max)
  def isConst = this.isInstanceOf[Const] 
}
object Port {
  def apply(s:Node) = {
    new {override val src = s} with Port
  }
  def apply(s:Node, toStr: => String) = {
    new {override val src = s} with Port { override def toString = toStr }
  }
}
