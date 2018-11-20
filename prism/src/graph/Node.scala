package prism
package graph

import scala.collection.mutable

trait Node[N] extends IR { self:N =>

  /*  ------- State -------- */
  implicit def Nct:ClassTag[N]
  private var _parent:Option[Node[_]] = None
  private lazy val _children = ListBuffer[Node[_]]()
  val localEdges = mutable.ListBuffer[Edge]()

  /*  ------- Metadata -------- */
  val pos = Metadata[(Double,Double)]("pos")

  /*  ------- functions -------- */
  // Parent
  def parent:Option[Node[_]] = _parent
  def setParent(p:Node[_]):this.type =  {
    assert(p != this, s"setting parent of $this to be the same as it self $p!")
    _parent match {
      case Some(`p`) => this
      case Some(parent) => err(s"Resetting parent of $this from $parent to $p"); this
      case None =>
        p match {
          case _:N =>
            assert(!this.isAncestorOf(p), s"Setting descendent $p as parent of $this") 
            _parent = Some(p)
            p.addChild(this)
          case _ => 
        }
        this
    }
  }
  def unsetParent:self.type = {
    parent.foreach { p =>
      _parent = None
      p.removeChild(this)
    }
    this
  }
  def resetParent(p:Node[_]):this.type = { unsetParent; setParent(p) }
  def isParentOf(m:Node[_]) = m.parent == Some(this)

  // Children
  def children:List[Node[_]] = _children.toList
  def addChild(cs:Node[_]*):this.type = { 
    cs.foreach { c =>
      assert(c != this, s"Cannot add self as a children node=$this")
      c match {
        case _:N => 
          if (_children.contains(c)) return this
          _children += c
          c.setParent(this)
        case c => 
          throw new Exception(s"Cannot add $c as child of node $this with type $Nct")
      }
    }
    this
  }

  def removeChild(c:Node[_]):Unit = {
    if (!_children.contains(c)) return
    _children -= c
    c.unsetParent
  }
  def isChildOf(p:Node[_]) = p.children.contains(this)

  def siblings:List[Node[_]] = parent.map { _.children.filterNot { _ == this} }.getOrElse(Nil)
  def ancestors:List[Node[_]] = {
    parent.toList.flatMap { parent => parent :: parent.ancestors }
  }
  def isAncestorOf(n:Node[_]):Boolean = n.isDescendentOf(this)
  // Inclusive
  def ancestorSlice(top:Node[_]) = { // from this to top inclusive
    val chain = this :: this.ancestors
    val idx = chain.indexOf(top)
    assert(idx != -1, s"$top is not ancestor of the $this")
    chain.slice(0, idx+1)
  }
  def descendents:List[Node[_]] = children.flatMap { child => child :: child.descendents }
  def isDescendentOf(n:Node[_]):Boolean = parent.fold(false) { p => p == n || p.isDescendentOf(n) }

  def addEdge(io:Edge) = {
    localEdges += io
  }
  def removeEdge(io:Edge) = localEdges -= io
  def localIns = localEdges.collect { case i:Input => i }
  def localOuts = localEdges.collect { case i:Output => i }

  def edges = localEdges ++ descendents.flatMap { _.localEdges }
  def ins = edges.collect { case i:Input => i }
  def outs = edges.collect { case i:Output => i }
  def localDeps:Seq[Node[_]] = { 
    localIns.flatMap { _.connected.map { _.src} }.toSeq.distinct
  }

  def localDepeds:Seq[Node[_]] = {
    localOuts.flatMap { _.connected.map { _.src} }.toSeq.distinct
  }

  def matchLevel(n:Node[_]) = (n :: n.ancestors).filter { _.parent == this.parent }.headOption

  /*
   * A map of external dependent outputs and internal inputs that depends on the external 
   * dependencies
   * */
  def depsFrom:Map[Output, Seq[Input]] = {
    val descendents = this.descendents
    val ins = localIns.toIterator ++ descendents.toIterator.flatMap { _.localIns }
    ins.flatMap { in =>
      in.connected.filterNot { out => descendents.contains(out.src) } 
      .map { out => (out.as[Output], in) } 
    }.toSeq.groupBy { _._1 }.mapValues { _.map { _._2 } }
  }

  def deps:Seq[Node[_]] = depsFrom.keys.map(_.src).toSeq

  /*
   * A map of internal outs to seq of external inputs
   * */
  def depedsTo:Map[Output, Seq[Input]] = {
    val descendents = this.descendents
    val outs = localOuts.toIterator ++ descendents.toIterator.flatMap { _.localOuts }
    outs.flatMap { out =>
      out.connected.filterNot { in => descendents.contains(in.src) } 
      .map { in => (in.as[Input], out) } 
    }.toSeq.groupBy { _._2 }.mapValues { _.map { _._1 } }
  }

  def depeds:Seq[Node[_]] = depedsTo.values.flatten.map { _.src }.toSeq.distinct

  def siblingDeps = deps.flatMap(matchLevel)
  def globalDeps = deps.filter { d => matchLevel(d).isEmpty }
  def siblingDepeds = depeds.flatMap(matchLevel)
  def globalDepeds = depeds.filter { d => matchLevel(d).isEmpty }
  def neighbors = deps ++ depeds

}
