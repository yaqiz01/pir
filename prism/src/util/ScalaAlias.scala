package prism.util

trait ScalaAlias {
  type ClassTag[T] = scala.reflect.ClassTag[T]
  def classTag[T](implicit ctag: ClassTag[T]) = scala.reflect.classTag[T]
  type TypeTag[T] = scala.reflect.runtime.universe.TypeTag[T]
  def typeTag[T](implicit ctag: TypeTag[T]) = scala.reflect.runtime.universe.typeTag[T]
  def typeOf[T](implicit ctag: TypeTag[T]) = scala.reflect.runtime.universe.typeOf[T]

  val implicitConversions = scala.language.implicitConversions
  val postfixOps = scala.language.postfixOps
  implicit val hgherKinds = scala.language.higherKinds
  implicit val reflectiveCalls = scala.language.reflectiveCalls
  implicit val existentials = scala.language.existentials

  type ListBuffer[T] = scala.collection.mutable.ListBuffer[T]
  val ListBuffer = scala.collection.mutable.ListBuffer
}
