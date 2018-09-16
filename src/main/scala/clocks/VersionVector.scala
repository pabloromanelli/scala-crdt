package clocks

import scala.collection.immutable.HashMap

/**
  * Version vectors provides causal ordering.
  * If a version vector is greater than other, then it is causally after the other (it includes the other, it saw the other).
  * If they are concurrent, no causal relationship can be inferred from them.
  */
case class VersionVector[ID](versions: HashMap[ID, Long]) extends Clock[VersionVector[ID]] {
  /**
    * Returns the successor of this and the provided clock.
    * So, the resulting clock should be:
    * result >= this
    * result >= that
    */
  override def merge(that: VersionVector[ID]): VersionVector[ID] = {
    VersionVector(versions.merged(that.versions) {
      case ((id, versionA), (_, versionB)) => id -> versionA.max(versionB)
    })
  }

  /**
    * Will return the successor of the clock for the node id.
    * The resulting clock should be:
    * result > this
    * result != this
    */
  def +(id: ID): VersionVector[ID] = {
    val currentVersion = versions.getOrElse(id, 0L)
    val nextVersion = currentVersion + 1L
    VersionVector(versions.updated(id, nextVersion))
  }

  /**
    * Compare two version vectors. The outcome will be one of the following:
    * if both are equals => 0
    * if all versions are <= than the other versions and one is strictly < => lower (-1)
    * if all versions are >= than the other versions and one is strictly > => greater (1)
    * In any other case => None (concurrent)
    */
  override def tryCompareTo[B >: VersionVector[ID]](that: B)(implicit evidence$1: B => PartiallyOrdered[B]): Option[Int] = that match {
    case other if this == other => Some(0)
    case x: VersionVector[ID] =>
      val v = x.versions
      val ids: Set[ID] = versions.keySet.union(v.keySet)
      if (ids.forall(id => versions.getOrElse(id, 0L) <= v.getOrElse(id, 0L)))
        Some(-1)
      else if (ids.forall(id => versions.getOrElse(id, 0L) >= v.getOrElse(id, 0L)))
        Some(1)
      else
        None
    case _ => None
  }
}
