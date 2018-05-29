package pir
package mapper

import prism.collection.immutable._
import prism.util.Memorization
import scala.collection.mutable

abstract class Constrain(implicit val pass:PIRPass) extends Logging with Memorization with MappingLogger {
  type K
  type V
  type FG <: FactorGraphLike[K,V,FG]
  implicit def fgct:ClassTag[FG]
  override lazy val logger = pass.logger
  override def toString = this.getClass.getSimpleName.replace("$","")
  override def quote(n:Any) = pass.quote(n)
  memorizing = true // TODO: is this actually memorizing?
  def prune(fg:FG):EOption[FG]
  def prune(pmap:PIRMap):EOption[PIRMap] = {
    pmap.flatMap[FG](field => prune(field))
  }
}
trait CostConstrain extends Constrain {
  def fit(cuP:K, cuS:V):Boolean
  def prune(fg:FG):EOption[FG] = {
    fg.filter { case (cuP,cuS) => fit(cuP, cuS) }.left.map {
      case InvalidFactorGraph(fg:FG, k) => CostConstrainFailure(this, fg, k)
    }
  }
}
trait PrefixConstrain extends CostConstrain {
  def prefixKey(cuP:K):Boolean
  def prefixValue(cuS:V):Boolean
  val prefixKeyOf = memorize(prefixKey _)
  val prefixValueOf = memorize(prefixValue _)
  def fit(cuP:K, cuS:V):Boolean = {
    val key = prefixKeyOf(cuP)
    val value = prefixValueOf(cuS)
    val factor = key == value
    if (!factor) dbg(s"$this ${quote(cuP)}:$key != ${quote(cuS)}:$value")
    factor
  }
}
trait QuantityConstrain extends CostConstrain {
  def numPNodes(cuP:K):Int
  def numSnodes(cuS:V):Int
  val numPNodesOf = memorize(numPNodes _)
  val numSnodesOf = memorize(numSnodes _)
  def fit(cuP:K, cuS:V):Boolean = {
    val key = numPNodesOf(cuP)
    val value = numSnodesOf(cuS)
    val factor = key <= value
    if (!factor) dbg(s"$this ${quote(cuP)}:$key != ${quote(cuS)}:$value")
    factor
  }
}
trait ArcConsistencyConstrain extends Constrain {
  def prune(fg:FG):EOption[FG] = {
    flatFold(fg.freeKeys,fg) { case (fg, k) => ac3[K,V,FG](fg, k) }
  }
  def ac3[K,V,FG<:FactorGraphLike[K,V,FG]](fg:FG, k:K):EOption[FG] = {
    flatFold(fg(k),fg) { case (fg, v) =>
      val neighbors = fg.freeKeys(v).filterNot { _ == k }
      val nfg = fg.set(k,v)
      nfg match {
        case Left(_) => fg - (k,v)
        case Right(nfg) =>
          flatFold(neighbors, fg) { case (fg, neighbor) => 
            if (ac3[K,V,FG](nfg, neighbor).isLeft) fg - (k,v) else Right(fg)
          }
      }
    }
  }
}
trait MatchingConstrain extends Constrain {

  //def createCompleteBipartiteSubgraphs(fg:FG) = {
    //val subgraphs = mutable.Map[Set[V], mutable.Set[K]]()
    //fg.freeKeys.foreach { key =>
      //val values = fg.freeValues(key)
      //if (!subgraphs.contains(values)) {
        //subgraphs += (values -> mutable.Set[K]())
      //}
      //subgraphs.foreach { case (vs, ks) =>
        //if (vs.subsetOf(values)) ks += key
      //}
    //}
    //subgraphs
  //}

  def check(fg:FG, ks:Set[K], vs:Set[V]) = {
    val keySize = ks.size
    val valueSize = vs.size
    val fit = keySize <= valueSize
    dbgblk(s"MatchingConstrain: keySize=$keySize <= valueSize=$valueSize = $fit") {
      dbg(s"keys=${quote(ks)}")
      dbg(s"values=${quote(vs)}")
    }
    if (fit) Right(fg) else Left(MatchConstrainFailure(fg, ks.toSet))
  }
  
  def prune(fg:FG):EOption[FG] = {
    log(fg)
    val sets = fg.freeKeys.groupBy { key => fg.freeValues(key) }.map { case (values, keys) =>
      (keys.head, values)
    }
    flatFold(sets, fg) { case (fg, (key, values)) =>
      val keys = values.flatMap { value => fg.freeKeys(value) }.filter { key => 
        fg.freeValues(key).subsetOf(values)
      }
      check(fg, keys, values)
    }
  }

}

case class CostConstrainFailure[FG<:FactorGraphLike[_,_,FG]](constrain:CostConstrain, @transient fg:FG, key:Any) extends MappingFailure
case class MatchConstrainFailure[FG<:FactorGraphLike[_,_,FG]](@transient fg:FG, keys:Set[_]) extends MappingFailure {
  override def toString = s"MatchConstrainFailure(${fg.getClass.getSimpleName}, $keys)"
}
