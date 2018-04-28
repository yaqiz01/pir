package prism

import scala.collection.mutable

trait Compiler {

  implicit val compiler:this.type = this

  def name = getClass().getSimpleName().replace("$", "")
  override def toString = name
  def outDir = Config.outDir + separator + name

  var _session:Session = _
  lazy val session = _session

  def setSession(sess:Session) = _session = sess

  def reset = { 
    _session = null
    clearLogs(outDir)
  } 

  def handle(e:Exception):Unit

  val sessionPath = s"${outDir}${separator}${name}.sess"

  val configs:List[GlobalConfig]

  def setArgs(inputArgs: Array[String]):Unit = {
    val args = loadArgs(inputArgs)
    info(s"args=[${args.mkString(", ")}]")
    if (args.contains("--help")) {
      configs.foreach(_.printUsage)
      System.exit(0)
    }
    configs.foreach(_.setOption(args.toList))
  }

  type D <: Design
  var design:D = _

  def load:Boolean
  def save:Boolean

  val designPath:String
  def loadDesign = design = loadFromFile[D](designPath)
  def newDesign:Unit
  def saveDesign:Unit = saveToFile(design, designPath)

  def initDesign = if (load) loadDesign else newDesign

  def initSession = {
    try {
      val sess = if (load) loadFromFile[Session](sessionPath) else new Session() // Load session
      setSession(sess)
    } catch {
      case e:SessionRestoreFailure =>
        warn(s"Restore session failed: ${e}. Creating a new session ...")
        setSession(new Session())
      case e:Throwable => throw e
    }
  }

  def runSession = {
    session.run
  }

  def main(args: Array[String]): Unit = {
    try {
      reset
      setArgs(args)
      initDesign
      initSession
      runSession
    } catch { 
      case e:Exception =>
        err(e, exception=false)
        handle(e)
    }
  }
}
