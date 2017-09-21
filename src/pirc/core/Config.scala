package pirc

import scala.collection.mutable

trait GlobalConfig {
  protected def getProperty(prop: String, default: String) = {
    val p1 = System.getProperty(prop)
    val p2 = System.getProperty(prop.substring(1))
    if (p1 != null && p2 != null) {
      assert(p1 == p2, "ERROR: conflicting properties")
      p1
    }
    else if (p1 != null) p1 else if (p2 != null) p2 else default
  }

  val optMap = mutable.Map[String, String => Any]()

  def register[T](key:String, default:T)(update:String => Unit) = {
    optMap += key -> update 
    default
  }

  def setOption(opt:String):Unit = {
    val (key, value) = if (opt.contains("=")) {
      val key::value::_ = opt.split("=").toList
      (key, value)
    } else {
      (opt, "true")
    }
    optMap(key)(value)
  }
}
