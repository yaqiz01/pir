package pir.util

import pir._
import pir.graph._
import pir.util.maps._
import scala.collection.mutable.Map

trait PIRMetadata extends { self:Design =>
  object indexOf extends MOneToOneMap {
    type K = Node
    type V = Int
  }

  object vecOf extends MOneToOneMap {
    type K = Node
    type V = VectorIO[_]
  }

  object forRead extends MOneToOneMap {
    type K = Node
    type V = Boolean
    override def apply(k:K):V = {
      super.get(k).getOrElse(false)
    }
  }

  object forWrite extends MOneToOneMap {
    type K = Node
    type V = Boolean
    override def apply(k:K):V = {
      super.get(k).getOrElse(false)
    }
  }

  object isHead extends MOneToOneMap {
    type K = Controller
    type V = Boolean
  }

  object isLast extends MOneToOneMap {
    type K = Controller
    type V = Boolean
  }

  object isStreaming extends MOneToOneMap {
    type K = Controller
    type V = Boolean
  }

  object isPipelining extends MOneToOneMap {
    type K = Controller
    type V = Boolean
  }

  /* Number of children stages on the critical path */
  object lengthOf extends MOneToOneMap {
    type K = Controller
    type V = Int 
  }

  // Including current CU. From current to top
  object ancestorsOf extends MOneToOneMap {
    type K = Controller
    type V = List[Controller]
  }

  // Including current CU. From current to leaf children 
  object descendentsOf extends MOneToOneMap {
    type K = Controller
    type V = List[Controller]
  }

  object writerOf extends MOneToOneMap {
    type K = OnChipMem 
    type V = Controller
  }

  object readerOf extends MOneToOneMap {
    type K = OnChipMem 
    type V = Controller
  }

  object readCChainsOf extends MOneToOneMap {
    type K = ComputeUnit
    type V = List[CounterChain]
  }

  object writeCChainsOf extends MOneToOneMap {
    type K = ComputeUnit 
    type V = List[CounterChain]
  }

  object compCChainsOf extends MOneToOneMap {
    type K = ComputeUnit 
    type V = List[CounterChain]
  }
}

