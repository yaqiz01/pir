package prism
package graph

import implicits._

import scala.collection.mutable

/*
 * Product node provide a concise interface to specify IRs in case classes. 
 * Each product node has exactly one output, and fields of the case classes
 * are input nodes. Edges are created while constructing the IR. 
 * While the graph can be mirrored. The connection is not mutable
 * */
trait ProductNode[N] extends Node[N] with DefNode[N] with Product { self:N =>
  implicit val src = this

  def newIn = new Input
  def newOut = new Output
  val out = newOut

  private val efields = mutable.ListBuffer[Input]()
  productIterator.foreach { field => 
    unpack(field) { case x:ProductNode[_] => connectField(x) }
  }

  def connectField[T<:ProductNode[_]](x:T) = { efields += newIn.connect(x.out); x }

  def nfields = efields.toList.map { field =>
    unpack(field) { case x:Edge => x.connected.map { _.src} }
  }

  def trace[T<:Node[_]:ClassTag]:T = assertOne(this.collect[T](visitGlobalOut _), 
    s"$this.trace[${classTag[T]}]")

}
