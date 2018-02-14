package pirc

import pirc._
import pirc.util._
import prism.codegen.Logging

import scala.collection.mutable

case class RunPass(pass:Pass, id:Int)(implicit design:Design) {
  def name = s"$pass-$id"
  var hasRun = false
  def reset = { hasRun = false }
  val dependencies = mutable.ListBuffer[RunPass]()
  def dependsOn(deps:Pass*) = {
    deps.foreach { dep =>
      dependencies += dep.runPasses.last
    }
    this
  }
  def unfinishedDependencies = dependencies.filter { !_.hasRun }
  def isDependencyFree = unfinishedDependencies.isEmpty

  def prevRuns = {
    design.runPasses.slice(0, id)
  }

  def run:Unit = {
    if (!pass.shouldRun) return
    if (!isDependencyFree) 
      err(s"Cannot run pass $name due to dependencies=${unfinishedDependencies.map(_.name).mkString(",")} haven't run")

    pass.logger.withOpen(s"$name.log") {
      pass.initPass(this)
      pass.runPass(this)
      pass.finPass(this)
      hasRun = true
    }
  }
}

trait Pass extends Logging {

  def shouldRun:Boolean
  lazy val name = this.getClass.getSimpleName
  override def toString = name
  
  val runPasses = mutable.ListBuffer[RunPass]()

  def reset:Unit = runPasses.foreach(_.reset)

  def newRun(id:Int)(implicit design:Design):RunPass = {
    val runPass = RunPass(this, id)
    runPasses += runPass
    runPass
  }
  
  def initPass(runner:RunPass):Unit = {
    info(s"Running ${runner.name} ...")
    initPass
  }
  def initPass:Unit = {}

  def runPass(runner:RunPass):Unit = runPass
  def runPass:Unit = {}

  def finPass(runner:RunPass):Unit = { finPass; check } 
  def finPass:Unit = {}

  def check:Unit = {}

  def hasRun:Boolean = runPasses.exists(_.hasRun)

  def hasRunAll:Boolean = runPasses.forall(_.hasRun)

  def runCount = runPasses.filter(_.hasRun).size

}
