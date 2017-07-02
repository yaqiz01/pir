package pir.plasticine

import pir.plasticine.main.Spade
import pir.plasticine.graph._
import pir.plasticine.simulation._
import pir.exceptions.PIRException
import pir.mapper.PIRMap
import pir.Design

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import scala.language.implicitConversions
import scala.reflect.{ClassTag, classTag}

package object util {
  implicit def pr_to_ip(pr:PipeReg):Input[Bus, PipeReg] = pr.in
  implicit def pr_to_op(pr:PipeReg):Output[Bus, PipeReg] = pr.out

  def regsOf(io:IO[_,_]):List[ArchReg] = mappingOf[PipeReg](io).map(_.reg)

  def mappingOf[T](io:IO[_,_])(implicit ev:ClassTag[T]):List[T] = io match {
    case in:Input[_,_] => 
      //println(s"$in.fanIns=[${in.fanIns.map(n => s"($n, ${ev.runtimeClass.isInstance(n)})").mkString("\n")}]")
      in.fanIns.map(_.src).flatMap {
        case n if ev.runtimeClass.isInstance(n) => List(n.asInstanceOf[T])
        case sl:Slice[_] => mappingOf[T](sl.in)
        case bc:BroadCast[_] => mappingOf[T](bc.in)
        case n => Nil
      }
    case out:Output[_,_] =>
      //println(s"$out.fanOuts=[${out.fanOuts.map(n => s"($n, ${ev.runtimeClass.isInstance(n)})").mkString("\n")}]")
      out.fanOuts.map(_.src).flatMap { 
        case n if ev.runtimeClass.isInstance(n) => List(n.asInstanceOf[T])
        case sl:Slice[_] => mappingOf[T](sl.out)
        case bc:BroadCast[_] => mappingOf[T](bc.out)
        case n => Nil
      }
  }
  def stageOf(io:IO[_,_]):Option[Stage] = {
    io.src match {
      case pr:PipeReg => Some(pr.stage) 
      case fu:FuncUnit => Some(fu.stage)
      case _ => None
    }
  }

  def quote(n:Node)(implicit spade:Spade):String = {
    val spademeta: SpadeMetadata = spade
    import spademeta._
    n match {
      case n:NetworkElement => coordOf.get(n).fold(s"$n") { case (x,y) => s"$n[$x,$y]" }
      case n:GlobalIO[_,_] => s"${quote(n.src)}.$n[${n.index}]"
      case n => indexOf.get(n).fold(s"$n"){ i =>s"$n[$i]"}
    }
  }

  def isMapped(node:Node)(implicit mp: PIRMap):Boolean = {
    node match {
      case n:Primitive if !isMapped(n.pne) => return false
      case n =>
    }
    node match {
      case n:Controller => mp.clmap.isMapped(n)
      case n:DRAM => true
      case n:OnChipMem => mp.smmap.isMapped(n)
      case n:Counter => mp.ctmap.isMapped(n)
      case n:Stage => mp.stmap.isMapped(n)
      case n:PipeReg => isMapped(n.in)
      case n:FuncUnit => isMapped(n.stage)
      case n:UDCounter => 
        n.pne.ctrlBox match {
          case cb:MemoryCtrlBox => true
          case cb:CtrlBox => mp.pmmap.isMapped(n)
        }
      case n:Input[_,_] => mp.fimap.contains(n) || n.fanIns.size==1
      case n:Output[_,_] => mp.opmap.pmap.contains(n)
      case n:SwitchBox => n.ios.exists(isMapped)
      case n:CtrlBox => isMapped(n.pne)
      case n:PulserSM => isMapped(n.pne) && !mp.clmap.pmap(n.pne).isSC
      case n:UpDownSM => isMapped(n.pne)
      case n:Const[_] => mp.pmmap.isMapped(n)
      case n:BroadCast[_] => isMapped(n.in) 
      case n:Slice[_] => isMapped(n.in) 
      case n:Delay[_] => isMapped(n.pne)
      case n:AndTree => n.ins.exists(isMapped)
      case n:AndGate => n.ins.exists(isMapped)
      case n => throw PIRException(s"Don't know how to check whether $n is mapped")
    }
  }

  def OCU_MAX_CIN(implicit spade:Spade) = {
    val ocu = spade.ocus.head
    ocu.cins.size
  }

  def zip[T1, T2, T](x1:Option[T1], x2:Option[T2])(lambda:(T1,T2) => T):Option[T] = (x1, x2) match {
    case (Some(x1), Some(x2)) => Some(lambda(x1, x2))
    case _ => None
  }
  def zip[T1, T2, T3, T](x1:Option[T1], x2:Option[T2], x3:Option[T3])(lambda:(T1,T2,T3) => T):Option[T] = (x1, x2, x3) match {
    case (Some(x1), Some(x2), Some(x3)) => Some(lambda(x1, x2, x3))
    case _ => None
  }
  def zip[T1, T2, T3, T4, T](x1:Option[T1], x2:Option[T2], x3:Option[T3], x4:Option[T4])(lambda:(T1,T2,T3,T4) => T):Option[T] = (x1, x2, x3, x4) match {
    case (Some(x1), Some(x2), Some(x3), Some(x4)) => Some(lambda(x1, x2, x3, x4))
    case _ => None
  }
}
