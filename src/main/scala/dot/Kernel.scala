package dot

import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.language.higherKinds

trait KernelLike[ID, V, C <: ContextLike[ID, C], K <: KernelLike[ID, V, C, K]] {
  self: K =>

  type MUTATION = (DeltaKernel[ID, V], K)

  def dottedVals: HashMap[V, Set[Dot[ID]]]

  def context: C

  def copy(dottedVals: HashMap[V, Set[Dot[ID]]] = dottedVals,
           context: C = context): K

  /**
    * Merges both contexts and updates the current dotted values according to the following rules:
    * - Add other kernel's dotted values only using the dots that are not included on this context.
    * - Removes this kernel value dots if they are not included on the other kernel
    * values but included on the other kernel context.
    * - Remove values if they don't have any dot associated.
    *
    * Not having a value on the dotted values but having its dot on the context is a
    * way to advance the causal history without adding a new dot.
    * Because of that, the dots on the values will be always included on the context.
    */
  def merge(other: K): K = {
    if (this == other) {
      self
    } else {
      copy(
        joinDottedVals(other),
        context.merge(other.context)
      )
    }
  }

  private def joinDottedVals(other: K): HashMap[V, Set[Dot[ID]]] = {
    val dotsMapBuilder = mutable.Map.empty[V, mutable.Builder[Dot[ID], Set[Dot[ID]]]]

    def add[A <: K, B <: K](a: A, b: B): Unit = for {
      (v, dots) <- a.dottedVals
      dot <- dots
      if b.dots(v).contains(dot) || !b.context.contains(dot)
    } dotsMapBuilder.getOrElseUpdate(v, Set.newBuilder[Dot[ID]]) += dot

    // filter other dotted values
    add(other, this)
    // filter self dotted values
    add(this, other)

    val mapBuilder = HashMap.newBuilder[V, Set[Dot[ID]]]
    for ((v, dotsBuilder) <- dotsMapBuilder)
      mapBuilder += ((v, dotsBuilder.result))
    mapBuilder.result
  }

  private def dots(value: V) =
    dottedVals.getOrElse(value, Set.empty)

  def add(dot: Dot[ID], value: V): K =
    copy(dottedVals.updated(value, dots(value) + dot),
      context.insertDot(dot, compact = true))

  /**
    * Removed the value and the dots associated with it keeping the causal context intact.
    * If the value is not present, does nothing.
    * The context keeps the causal past.
    * The removed dots are added to the delta.
    */
  def remove(value: V, delta: DeltaKernel[ID, V]): MUTATION = {
    (delta.copy(context = delta.context.insertDot(dots(value), compact = true)),
      copy(dottedVals = dottedVals - value))
  }

  /**
    * Removes all the values keeping the causal context intact.
    * The removed dots are added to the delta.
    */
  def clear(delta: DeltaKernel[ID, V]): MUTATION =
    (delta.copy(context = delta.context.insertDot(dottedVals.values.flatten, compact = true)),
      copy(HashMap.empty, context))

  /**
    * Is true if there are no current values (on creation, after clear or after merge of deletions).
    */
  def isEmpty: Boolean =
    dottedVals.isEmpty

  def values: Set[V] = dottedVals.keySet

}

case class DeltaKernel[ID, V](dottedVals: HashMap[V, Set[Dot[ID]]] = HashMap[V, Set[Dot[ID]]](),
                              context: DeltaContext[ID] = DeltaContext[ID]())
  extends KernelLike[ID, V, DeltaContext[ID], DeltaKernel[ID, V]] {

  override def copy(dottedVals: HashMap[V, Set[(ID, Long)]],
                    context: DeltaContext[ID]): DeltaKernel[ID, V] =
    DeltaKernel(dottedVals, context)

}

case class Kernel[ID, V](dottedVals: HashMap[V, Set[Dot[ID]]] = HashMap[V, Set[Dot[ID]]](),
                         context: Context[ID])
  extends KernelLike[ID, V, Context[ID], Kernel[ID, V]] {

  /**
    * Adds the value using the current context for the node.
    */
  def add(value: V, delta: DeltaKernel[ID, V]): MUTATION = {
    val dot = context.nextDot
    (delta.add(dot, value), add(dot, value))
  }

  override def copy(dottedVals: HashMap[V, Set[(ID, Long)]],
                    context: Context[ID]): Kernel[ID, V] =
    Kernel(dottedVals, context)

}