package pir.codegen

import pir._
import pir.exceptions._
import pir.plasticine.main.Spade
import pir.pass.Pass

import java.io.PrintWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import scala.collection.mutable.Stack

trait Logger extends Printer {
  override def emitBSln(s:String):Unit = { super.emitBSln(s); flush }
  override def emitBEln(s:String):Unit = { super.emitBEln(s); flush }
  override def emitln(s:String):Unit = { super.emitln(s); flush } 
  override def emitBlock[T](block: =>T):T = { if (Config.debug) { val res = super.emitBlock(block); flush; res } else { block } }
  override def emitBlock[T](s:String)(block: =>T):T = { if (Config.debug) { val res = super.emitBlock(s)(block); flush; res } else { block } }
  def emitBlock[T](header:String, s:String)(block: =>T):T = { if (Config.debug) { val res = super.emitBlock(promp(Some(header), s))(block); flush; res } else { block } }
  def promp(header:Option[String], s:Any) = s"${header.fold("") { h => s"[$h]"}} $s"
  def dprintln(pred:Boolean, header:Option[String], s:Any):Unit = if (pred) emitln(promp(header, s))
  def dprint(pred:Boolean, header:Option[String], s:Any):Unit = if (pred) emit(promp(header, s))
  def dprintln(pred:Boolean, header:String, s:Any):Unit = dprintln(pred, Some(header), s) 
  def dprint(pred:Boolean, header:String, s:Any):Unit = dprint(pred, Some(header), s) 
  def dprintln(header:String, s:Any):Unit = dprintln(Config.debug, header, s) 
  def dprint(header:String, s:Any):Unit = dprint(Config.debug, header, s) 
  def dprintln(pred:Boolean, s:Any):Unit = dprintln(pred, None, s) 
  def dprint(pred:Boolean, s:Any):Unit = dprintln(pred, None, s)
  def dprintln(s:Any):Unit = dprintln(Config.debug, None, s) 
  def dprint(s:Any):Unit = dprintln(Config.debug, None, s)
  def dbsln(pred:Boolean, header:Option[String], s:Any):Unit = if (pred) emitBSln(promp(header,s) + " ")
  def dbeln(pred:Boolean, header:Option[String], s:Any):Unit = if (pred) emitBEln(" " + promp(header, s))

  def info(s:String) = emitln(s"[pir] ${s}")
  def startInfo(s:String) = emit(s"[pir] ${s}")
  def endInfo(s:String) = { emitln(s" ${s}") }
  def warn(s:Any) = emitln(s"${Console.YELLOW}[warning] ${s}${Console.RESET}")
  def err(s:Any) = { emitln(s"${Console.RED}[error]${s}${Console.RESET}"); throw PIRException(s"$s") }
  def bp(s:Any) = emitln(s"${Console.RED}[break]${s}${Console.RESET}")

  //override def newStream(dp:String, fname:String):FileOutputStream = { 
    //this match {
      //case p:Pass => super.newStream(dp, s"${p.runId}.${fname}")
      //case _ => super.newStream(dp, fname)
    //}
  //}
}