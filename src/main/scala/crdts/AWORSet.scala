package crdts

import dot.Kernel

import scala.collection.immutable.{AbstractSet, SetOps}
import scala.collection.{IterableFactory, StrictOptimizedIterableOps, mutable}

/**
  * Unordered add win set
  */
class AWORSet[A](kernel: Kernel[String, A])
  extends AbstractSet[A]
    with SetOps[A, AWORSet, AWORSet[A]]
    with StrictOptimizedIterableOps[A, AWORSet, AWORSet[A]] {

  override val iterableFactory: IterableFactory[IterableCC] =
    AWORSet.createFactory(kernel.context.nodeId)

  override def incl(elem: A): AWORSet[A] = ???

  override def excl(elem: A): AWORSet[A] = ???

  override def contains(elem: A): Boolean = ???

  override def iterator: Iterator[A] = ???

}

object AWORSet {
  /**
    * Iterable factory can't be static, it depends on the node id.
    */
  def createFactory(kernel: Kernel[String, _]): IterableFactory[AWORSet] =
    new IterableFactory[AWORSet] {

      override def empty[A]: AWORSet[A] = new AWORSet[A](kernel)

      override def from[A](source: IterableOnce[A]): AWORSet[A] = ???

      override def newBuilder[A]: mutable.Builder[A, AWORSet[A]] = ???

    }
}