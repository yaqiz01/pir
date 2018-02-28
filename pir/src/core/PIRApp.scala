package pir

import pir.node._
import pir.util._

import spade._
import arch._

import prism._
import prism.util._

import scala.language.implicitConversions
import scala.reflect.runtime.universe
import scala.collection.mutable.ListBuffer
import java.io._

trait PIRApp extends PIR {
  override def name:String = this.getClass().getSimpleName().replace("$","")
  
  //def dramDefault = arch.top.dram.dramDefault

  //def setDram(start:Int, array:Iterable[AnyVal]) = {
    //array.zipWithIndex.foreach { case (a, i) => dramDefault(start + i) = a }
  //}

  var args:Array[String] = _ 

  override def setArgs(args: Array[String]):Unit = {
    super.setArgs(args)
    this.args = args
  }

  def parseArgIns() = {
    args.foreach { 
      case arg if arg.contains("=") =>
        val k::v::_ = arg.split("=").toList
        //top.argIns.filter {_.name==Some(k)}.foreach { argIn =>
          //argIn.bound(toValue(v))
        //}
      case arg =>
    }
  }

  def load = PIRConfig.loadDesign
  def save = PIRConfig.saveDesign

  val designPath = s"${outDir}${File.separator}${name}.pir"

  override def loadDesign = {
    super.loadDesign
    arch = getArch(PIRConfig.arch)
    arch.initDesign
    info(s"Configuring spade $arch ...")
  }

  def newDesign = {
    top = new PIRDesign()
    main(top)
    endInfo(s"Finishing graph construction for ${this}")
    arch = getArch(PIRConfig.arch)
    arch.initDesign
    info(s"Configuring spade $arch ...")
  }

  def getArch(name:String) = {
    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    val module = runtimeMirror.staticModule("arch." + name)
    val obj = runtimeMirror.reflectModule(module)
    obj.instance.asInstanceOf[Spade]
  }

  def main(implicit top:PIRDesign): Any 

}

