package pir.pass

import pir._
import pir.node._

import pirc._

import scala.collection.mutable
import scala.reflect._
import pirc.util._

class DeadCodeElimination(implicit design:PIR) extends PIRTransformer with BFSBottomUpTopologicalTraversal {
  import pirmeta._

  type T = Map[N, Boolean]

  override def shouldRun = true

  val forward = false

  override def runPass =  {
    // Mark dead code
    val deathMap = traverseNode(design.newTop, Map.empty)
    // Remove dead code
    deathMap.foreach { 
      case (n, true) =>
        dbg(s"eliminate ${qdef(n)} from parent=${n.parent}")
        val neighbors = n.neighbors
        removeNode(n)
        neighbors.foreach { nb =>
          dbg(s"neighbor=$nb, neighbor.neighbors=${nb.neighbors}")
          assert(!nb.neighbors.asInstanceOf[Set[N]].contains(n))
        }
        pirmeta.removeAll(n)
      case (n, false) => removeUnusedIOs(n)
    }
  }

  def markDeath(deathMap:T, n:N) = {
    val isDead = n match {
      case n:ArgOut => false
      case n:StreamOut => false
      case n:Counter => false
        //if (!design.controlPropogator.hasRunAll) false
        //else if (ctrlOf(n).isOuterControl) false //TODO: after ControlAllocation this can be eliminated if n.depeds is empty
        //else n.depeds.forall(d => deathMap.getOrElse(d, false))
      case n => depFunc(n).forall(d => deathMap.getOrElse(d, false))
    }
    if (isDead) dbgs(s"Mark $n as dead code")
    deathMap + (n -> isDead)
  }

  override def visitNode(n:N, prev:T):T = {
    super.visitNode(n, markDeath(prev, n))
  }

  override def check = {
    val cus = collectDown[GlobalContainer](design.newTop)
    cus.foreach { cu =>
      val mems = collectDown[Memory](cu)
      mems.foreach { mem =>
        mem match {
          case mem:ArgIn =>
          case mem:StreamIn =>
          case mem if mem.writers.isEmpty =>
            warn(s"${qtype(mem)} in $cu does not have writer")
          case _ =>
        }
        mem match {
          case mem:ArgOut =>
          case mem:StreamOut =>
          case mem:StreamIn if mem.field == "ack" =>
          case mem if mem.readers.isEmpty =>
            warn(s"${qtype(mem)} in $cu does not have reader")
          case _ =>
        }
      }
    }
  }

}


