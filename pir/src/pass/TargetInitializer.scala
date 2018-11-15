package pir
package pass

import pir.node._
import prism.graph._
import scala.collection.mutable

import spade.param._
import spade.node._
import pir.mapper._

class TargetInitializer(implicit compiler:PIR) extends PIRPass with DefaultParamLoader with BaseFactory {

  def getOpt[T](name:String):Option[T] = config.getOption[T](name)

  override def runPass = {
    compiler.states.spadeParam = loadParam
    compiler.states.spadeTop = create[spade.node.Top](spadeParam)
    compiler.states.topMap = Right(TopMap())
  }

}
