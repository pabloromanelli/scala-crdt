package dot

import dot.Context.{ContextInvalidStateException, ContextVersionOverflowException}

import scala.annotation.tailrec
import scala.collection.immutable.{HashMap, SortedSet}
import scala.collection.mutable
import scala.language.higherKinds

/**
  * Causal context
  *
  * @param causalContext latest compacted versions (can only be incremented by the local id or by compacting the dot cloud into it).
  * @param dotCloud      versions that could or not be contiguous to the ones defined on the context (will move to the context after compaction).
  */
trait ContextLike[ID, C <: ContextLike[ID, C]] {
  self: C =>

  def causalContext: HashMap[ID, Long]

  def dotCloud: HashMap[ID, SortedSet[Long]]

  /**
    * Known node ids (by the context or the dot cloud)
    */
  def ids: Set[ID] = causalContext.keySet.union(dotCloud.keySet)

  /**
    * True if an equal or greater version is included in the context or
    * the exact version is included on the dot cloud
    */
  def contains(dot: Dot[ID]): Boolean = {
    (causalContext.getOrElse(dot.id, 0L) >= dot.version) ||
      dotCloud.getOrElse(dot.id, SortedSet.empty[Long]).contains(dot.version)
  }

  /**
    * Move all the consecutive dots on the cloud into the context.
    *
    * Properties:
    * - the first element of the cloud MUST be the following of the one in the context (considering empty context as 0)
    * - all the contiguous versions on the cloud can be compacted using the latest contiguous version
    * - the remaining versions must be kept on the cloud after the compaction
    */
  def compact: C = {
    /**
      * Shrinks the versions set until the head is no longer the successor of the current version.
      *
      * @return the greatest sequential version and the rest of the versions
      */
    @tailrec
    def compact(current: Long, remainingVersions: SortedSet[Long]): (Long, SortedSet[Long]) = {
      if (remainingVersions.isEmpty || remainingVersions.head > current + 1L) {
        // can't continue compacting (head is not the successor)
        (current, remainingVersions)
      } else {
        // head is the successor or is already included on the current version (head <= current + 1)
        // increment current version or discard because is already included
        compact(remainingVersions.head.max(current), remainingVersions.tail)
      }
    }

    dotCloud.foldLeft(this) { case (compactedContext, (id, versions)) =>
      val currentVersion = causalContext.getOrElse(id, 0L)
      val (compactedVersion, rest) = compact(currentVersion, versions)
      if (compactedVersion == currentVersion && rest == versions) {
        // no compaction was possible
        compactedContext
      } else {
        compactedContext.copy(
          compactedContext.causalContext.updated(id, compactedVersion),
          compactedContext.dotCloud.updated(id, rest)
        )
      }
    }
  }

  def insertDot(dot: Dot[ID], compact: Boolean = false): C = {
    insertDot(Iterable(dot), compact)
  }

  /**
    * Inserts all dots and compacts the context.
    */
  def insertDot(dots: Traversable[Dot[ID]], compact: Boolean): C = {
    val result = copy(dotCloud = mergeDotClouds(dotCloud, groupByAsHashMap(dots)))
    if (compact) result.compact else result
  }

  private def groupByAsHashMap(iterable: Traversable[(ID, Long)]): HashMap[ID, SortedSet[Long]] = {
    val m = mutable.Map.empty[ID, mutable.Builder[Long, SortedSet[Long]]]
    for ((key, value) <- iterable) {
      val bldr = m.getOrElseUpdate(key, SortedSet.newBuilder[Long])
      bldr += value
    }
    val b = HashMap.newBuilder[ID, SortedSet[Long]]
    for ((k, v) <- m)
      b += ((k, v.result))

    b.result
  }

  /**
    * Join both clouds doing a union of versions on key collisions
    */
  private def mergeDotClouds(a: HashMap[ID, SortedSet[Long]], b: HashMap[ID, SortedSet[Long]]): HashMap[ID, SortedSet[Long]] = {
    a.merged(b) {
      case ((id, versionsA), (_, versionsB)) => id -> versionsA.union(versionsB)
    }
  }

  /**
    * Keep the maximum version when the id exists in both
    */
  private def mergeCausalContexts(a: HashMap[ID, Long], b: HashMap[ID, Long]): HashMap[ID, Long] = {
    a.merged(b) {
      case ((id, versionA), (_, versionB)) => id -> versionA.max(versionB)
    }
  }

  /**
    * Keep the max value of each context id and union the dot clouds
    */
  def merge(other: C): C = {
    if (this == other) {
      self
    } else {
      copy(
        mergeCausalContexts(causalContext, other.causalContext),
        mergeDotClouds(dotCloud, other.dotCloud)
      ).compact
    }
  }

  def copy(causalContext: HashMap[ID, Long] = causalContext,
           dotCloud: HashMap[ID, SortedSet[Long]] = dotCloud): C

}

case class DeltaContext[ID](causalContext: HashMap[ID, Long] = HashMap[ID, Long](),
                            dotCloud: HashMap[ID, SortedSet[Long]] = HashMap[ID, SortedSet[Long]]())
  extends ContextLike[ID, DeltaContext[ID]] {

  override def copy(causalContext: HashMap[ID, Long],
                    dotCloud: HashMap[ID, SortedSet[Long]]): DeltaContext[ID] =
    DeltaContext[ID](causalContext, dotCloud)

}

case class Context[ID](nodeId: ID,
                       causalContext: HashMap[ID, Long] = HashMap[ID, Long](),
                       dotCloud: HashMap[ID, SortedSet[Long]] = HashMap[ID, SortedSet[Long]]())
  extends ContextLike[ID, Context[ID]] {

  /**
    * Returns the next dot for the node id.
    * It doesn't alters the causal context (you must use add to do that).
    * Should be always globally unique.
    */
  def nextDot: Dot[ID] = {
    val currentVersion = causalContext.getOrElse(nodeId, 0L)

    if (currentVersion == Long.MaxValue)
      throw new ContextVersionOverflowException(this)
    if (dotCloud.getOrElse(nodeId, SortedSet.empty).nonEmpty)
      throw new ContextInvalidStateException(this)

    val nextVersion = currentVersion + 1L
    nodeId -> nextVersion
  }

  override def copy(causalContext: HashMap[ID, Long],
                    dotCloud: HashMap[ID, SortedSet[Long]]): Context[ID] =
    Context[ID](nodeId, causalContext, dotCloud)

  override def toString: String = {
    val cc = causalContext.map { case (k, v) => k -> s"($v)" }
    val dc = dotCloud.map { case (k, v) => k -> v.mkString("{", ",", "}") }
    cc.merged(dc) {
      case ((id, v), (_, dots)) => id -> (v + dots)
    }
      .map { case (k, v) => k.toString + v }
      .mkString(s"Context($nodeId,\n", "\n", "\n)")
  }

}

object Context {

  class ContextInvalidStateException[ID](context: Context[ID])
    extends Exception(s"Invalid state detected: there are dots for the local id. Check if more than one client is using the same node id. Context: $context")

  class ContextVersionOverflowException[ID](context: Context[ID])
    extends Exception(s"Max version reached for ${context.nodeId} (${context.causalContext.getOrElse(context.nodeId, 0L)}), do a global sync and clear the context. Context: $context")

}