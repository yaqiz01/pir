package pir.plasticine.simulation

import pir.plasticine.main._
import pir.plasticine.graph._
import pir.util.enums._
import pir.exceptions._
import pir.util.misc._
import pir.Config
import pir.plasticine.util._

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.collection.mutable.Set
//import scala.reflect.{ClassTag, classTag}
import scala.reflect.runtime.universe._
import scala.language.implicitConversions

trait Val[P<:PortType]{ self:IO[P, Module] =>
  private lazy val _values = ListBuffer[P]() // from current to previous value
  def values = _values.toList
  def setPrev(pv:SingleValue, nv:SingleValue)(implicit sim:Simulator):Unit = {
    pv.next = Some(nv)
    nv.prev = Some(pv)
    pv.set { pv => pv copy nv }
  }
  def setPrev(pv:BusValue, nv:BusValue)(implicit sim:Simulator):Unit = {
    (pv.value, nv.value).zipped.foreach { case (pv, nv) => setPrev(pv, nv) }
    setPrev(pv.valid, nv.valid)
  }
  def setPrev(pv:Value, nv:Value)(implicit sim:Simulator):Unit = {
    (pv, nv) match {
      case (pv:SingleValue, nv:SingleValue) => setPrev(pv, nv)
      case (pv:BusValue, nv:BusValue) => setPrev(pv, nv)
    }
  }
  def vAt(idx:Int)(implicit sim:Simulator):P = {
    assert(tp.io == this, s"$tp.io != ${this}")
    while (values.size<=idx) {
      val v = tp.clone(s"${tp.typeStr}v${values.size}")
      assert(v.io == this)
      values.lastOption.foreach { nv => setPrev(v, nv) }
      _values += v
    }
    values(idx)
  }
  def v(implicit sim:Simulator):P = vAt(0)
  def pv(implicit sim:Simulator):P = vAt(1)
  //def ev(implicit sim:Simulator):P = vAt(0).update
  //def epv(implicit sim:Simulator):P = vAt(1).update
  //def vs:String = s"${value.s}"
  //def pvs:String = s"${prevValue.s}"

  def changed(implicit sim:Simulator):Boolean = v.value != pv.value

  def update(implicit sim:Simulator):Unit = { 
    assert(sim.inSimulation)
    //if (v.isDefined) sim.emitBlock(s"UpdateIO ${sim.quote(this)}") { v.update }
    if (v.isDefined) v.update
  }

  def clearUpdate(implicit sim:Simulator) = {
    values.foreach(_.clearUpdate)
  }

  def := (o: => IO[_<:PortType, Module])(implicit sim:Simulator):Unit = {
    v := o.v
  }

  def :== (o: => IO[_<:PortType, Module])(implicit sim:Simulator):Unit = {
    v := o.pv
  }

}

trait Value extends Node with Evaluation { self:PortType =>
  type V
  def value:V
  def s:String
  def asBit:BitValue = this.asInstanceOf[BitValue]
  def asWord:WordValue = {
    this match {
      case v:WordValue => v
      case _ => throw new Exception(s"${io}'s value cannot be casted to WordValue")
    }
  }
  def asBus:BusValue = this.asInstanceOf[BusValue]
  var parent:Option[Value] = None

  var funcHasRan = false
  var func: Option[(this.type => Unit, String)] = None 
  def set(f: this.type => Unit)(implicit sim:Simulator):Unit = {
    if (next.isEmpty) assert(!sim.inSimulation)
    val stackTrace = getStackTrace(1, 20)
    func.foreach { func =>
      var info = s"Reseting func of value $this of io ${io} in ${if (io!=null) s"${io.src}" else "null"}\n"
      info += s"Redefinition at \n${getStackTrace(4, 20)}\n"
      info += s"Originally defined at \n${func._2}"
      warn(info)
    }
    func = Some((f,stackTrace)) 
  }
  def isDefined:Boolean
  def updated:Boolean
  final def update(implicit sim:Simulator):this.type = {
    if (updated || !isDefined) return this
    prevUpdate
    if (updated || !isDefined) return this
    sim.emitBlock(s"UpdateValue #${sim.cycle} ${sim.quote(this)} n${id}", {
      mainUpdate
    }, s"UpdateValue #${sim.cycle} ${sim.quote(this)} n${id} ${value}")
    //postUpdate // allow cyclic update on previous value
    this
  }
  def prevUpdate(implicit sim:Simulator):Unit = { prev.foreach(_.update) }
  def mainUpdate(implicit sim:Simulator):Unit = {
    if (!funcHasRan) {
      funcHasRan = true
      func.foreach { case (f, stackTrace) => 
        try {
          f(this)
        } catch {
          case e:Exception =>
            errmsg(e.toString)
            errmsg(e.getStackTrace.slice(0,5).mkString("\n"))
            errmsg(s"\nStaged trace for $this: ")
            errmsg(stackTrace)
            sys.exit()
        }
      }
    }
  }
  def postUpdate(implicit sim:Simulator):Unit = { next.foreach(_.update) }
  var prev:Option[Value] = None
  var next:Option[Value] = None
  def clearUpdate(implicit sim:Simulator):Unit = {
    funcHasRan = false 
  }
  def := (other:Value)(implicit sim:Simulator):Unit
  def <<= (other:Value)(implicit sim:Simulator):Unit = { 
    sim.dprintln(s"${sim.quote(this)} <<= ${sim.quote(other)}")
    copy(other.update)
  }
  def copy (other:Value):Unit
}

trait SingleValue extends Value { self:PortType =>
  type E <: AnyVal
  type V = Option[E]
  var value:V = None
  def isVOrElse(x:Any)(matchFunc: V => Unit)(unmatchFunc: Any => Unit):Unit
  override def updated = funcHasRan
  override def isDefined:Boolean = func.isDefined && next.fold(true) { _.isDefined }
  override def mainUpdate(implicit sim:Simulator):Unit = { 
    assert(!funcHasRan)
    super.mainUpdate
  }
  override def clearUpdate(implicit sim:Simulator):Unit = {
    if (isDefined && !updated) throw PIRException(s"${this} is not updated at #${sim.cycle}")
    super.clearUpdate
  }
  def := (other:Value)(implicit sim:Simulator):Unit = {
    sim.dprintln(s"${sim.quote(this)} := ${sim.quote(other)}")
    set { _ <<= other }
  }
  def := (other: => Option[AnyVal])(implicit sim:Simulator):Unit = set { _ copy other }
  def <<= (other:Option[AnyVal]):Unit = copy(other) 
  def copy(other:Value):Unit = copy(other.asInstanceOf[SingleValue].value)
  def copy (other:Option[AnyVal]):Unit
}

trait BitValue extends SingleValue { self:Bit =>
  type E = Boolean
  def isVOrElse(x:Any)(matchFunc: V => Unit)(unmatchFunc: Any => Unit) = x match {
    case v:Option[_] if v.fold(true) { _.isInstanceOf[E] } => matchFunc(v.asInstanceOf[V])
    case v => unmatchFunc(v)
  }
  def copy (other:Option[AnyVal]) = value = other.asInstanceOf[V]
  def s:String = value match {
    case Some(true) => "1"
    case Some(false) => "0"
    case None => "x"
  }
  override def equals(that:Any):Boolean = {
    that match {
      case that: Bit => super.equals(that) && (this.value == that.value)
      case that => false
    }
  }

  def isHigh:V = value
  def isLow:V = value.map { v => !v } 
  def setHigh = value = Some(true)
  def setLow = value = Some(false)
  def & (vl:Any)(implicit sim:Simulator):V = eval(BitAnd, this, vl).asInstanceOf[V]
  def | (vl:Any)(implicit sim:Simulator):V = eval(BitOr, this, vl).asInstanceOf[V]
  def == (vl:Any)(implicit sim:Simulator):V = eval(BitXnor, this, vl).asInstanceOf[V]
  def != (vl:Any)(implicit sim:Simulator):V = eval(BitXor, this, vl).asInstanceOf[V]
  def not(implicit sim:Simulator):V = eval(BitNot, this).asInstanceOf[V]
}

trait WordValue extends SingleValue { self:Word =>
  type E = WordTp
  def isVOrElse(x:Any)(matchFunc: V => Unit)(unmatchFunc: Any => Unit) = x match {
    case v:Option[_] if v.fold(true) { _.isInstanceOf[E] } => matchFunc(v.asInstanceOf[V])
    case v => unmatchFunc(v)
  }
  def copy (other:Option[AnyVal]):Unit = value = other.asInstanceOf[V]
  def <<= (other:Int):Unit = <<=(Some(other.toFloat)) 
  def s:String = value match {
    case Some(v) => s"$v"
    case None => "x"
  }
  override def equals(that:Any):Boolean = {
    that match {
      case that: Bit => super.equals(that) && (this.value == that.value)
      case that => false
    }
  }

  def + (vl:Any)(implicit sim:Simulator):Option[AnyVal] = eval(FltAdd, this, vl)
  def - (vl:Any)(implicit sim:Simulator):Option[AnyVal] = eval(FltSub, this, vl)
  def * (vl:Any)(implicit sim:Simulator):Option[AnyVal] = eval(FltMul, this, vl)
  def >= (vl:Any)(implicit sim:Simulator):Option[AnyVal] = eval(FltGeq, this, vl)
  def > (vl:Any)(implicit sim:Simulator):Option[AnyVal] = eval(FltGt, this, vl)
  def < (vl:Any)(implicit sim:Simulator):Option[AnyVal] = eval(FltLt, this, vl)
}

trait BusValue extends Value { self:Bus =>
  type V = List[Value] 
  val value:V = List.tabulate(busWidth) { i => 
    val eval = elemTp.clone(s"${elemTp.typeStr}v[$i]")
    eval.parent = Some(this)
    eval
  }
  lazy val valid = new Bit() { 
    this.io = self.io
    override def toString = s"${self.toString}.valid"
  }
  def s:String = value.map(_.s).mkString
  override def equals(that:Any):Boolean = {
    that match {
      case that: Bit => super.equals(that) && (this.value == that.value)
      case that => false
    }
  }
  def foreach(lambda:(Value, Int) => Unit):Unit =  {
    value.zipWithIndex.foreach { case (e, i) => lambda(e, i) }
  }
  def foreachv(lambda:(Value, Int) => Unit)(vlambda:BitValue => Unit):Unit =  {
    foreach(lambda)
    vlambda(valid)
  }
  def head:Value = value.head

  override def isDefined:Boolean = (value.exists(_.isDefined) || func.isDefined) && next.fold(true){ _.isDefined }
  override def updated = funcHasRan && value.forall(_.updated) 
  override def mainUpdate(implicit sim:Simulator):Unit = {
    super.mainUpdate
    value.foreach(_.update)
    valid.update
  }
  override def clearUpdate(implicit sim:Simulator) = {
    super.clearUpdate
    value.foreach(_.clearUpdate)
    valid.clearUpdate
  }
  override def := (other:Value)(implicit sim:Simulator):Unit = {
    sim.dprintln(s"${sim.quote(this)} := ${sim.quote(other)}")
    other match {
      case other:SingleValue =>
        value.foreach { v => v := other }
      case other:BusValue =>
        (value, other.value).zipped.foreach { case (v, ov) => v := ov }
        valid := other.valid
    }
  }
  override def copy (other:Value) = {
    other match {
      case other:SingleValue =>
        value.foreach { v => v copy other }
      case other:BusValue =>
        (value, other.value).zipped.foreach { case (v, ov) => v copy ov }
        valid copy other.valid
    }
  }
}

