package dot

import org.scalacheck.Arbitrary
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.collection.immutable.SortedSet

class ContextTest extends PropSpec with CommonGenerators with Checkers with PropertyChecks with Matchers {

  property("compacted cloud is a subset of the original cloud") {
    check { v: Context[Int] =>
      val result = v.compact
      v.ids.forall { id =>
        val originalCloud = v.dotCloud.getOrElse(id, SortedSet.empty)
        val resultCloud = result.dotCloud.getOrElse(id, SortedSet.empty)

        resultCloud.subsetOf(originalCloud)
      }
    }
  }

  property("compact removes versions from the cloud lower or equals than the successor of the context version") {
    forAll { v: Context[Int] =>
      val result = v.compact
      v.ids.foreach { id =>
        val contextVersion = result.causalContext.getOrElse(id, 0L)
        val resultCloud = result.dotCloud.getOrElse(id, SortedSet.empty)
        resultCloud.foreach(_ should be > (contextVersion + 1L))
      }
    }
  }

  property("compact removes consecutive versions from the cloud") {
    forAll { v: Context[Int] =>
      val result = v.compact
      v.ids.foreach { id =>
        val originalVersion = v.causalContext.getOrElse(id, 0L)
        val resultCloud = result.dotCloud.getOrElse(id, SortedSet.empty)
        val onlyGreaterCloud = v.dotCloud
          .getOrElse(id, SortedSet.empty)
          .filter(_ > originalVersion)

        if (onlyGreaterCloud.isEmpty) {
          resultCloud shouldBe empty
        } else {
          val versionsToZip = onlyGreaterCloud + originalVersion
          val expectedCloud = onlyGreaterCloud
            .+(originalVersion)
            .zip(versionsToZip.tail)
            .dropWhile {
              case (l, r) => r == l + 1
            }
            .map(_._2)

          expectedCloud shouldEqual resultCloud
        }
      }
    }
  }

  property("compact doesn't create new ids") {
    forAll { v: Context[Int] =>
      v.ids shouldEqual v.compact.ids
    }
  }

  property("compact advances the context version") {
    check { v: Context[Int] =>
      val result = v.compact
      v.ids.forall(id =>
        result.causalContext.getOrElse(id, 0L) >= v.causalContext.getOrElse(id, 0L)
      )
    }
  }

  property("merge is idempotent") {
    check { v: Context[Int] =>
      v.merge(v) == v
    }
  }

  property("merge is idempotent with other contexts") {
    forAll { (v1: Context[Int], v2: Context[Int]) =>
      v1.merge(v2) shouldEqual v1.merge(v2).merge(v2)
    }
  }

  property("merge is associative") {
    check { (v1: Context[Int], v2: Context[Int], v3: Context[Int]) =>
      v1.merge(v2.merge(v3)) == v1.merge(v2).merge(v3)
    }
  }

  property("merge is commutative") {
    forAll { (v1: Context[Int], v2: Context[Int]) =>
      v1.merge(v2) shouldEqual v2.merge(v1)
    }
  }

  def contains(a: Context[Int], b: Context[Int]): Unit = {
    val aa = a.compact
    val bb = b.compact
    bb.causalContext.foreach { case (id, v) =>
      aa.causalContext.getOrElse(id, 0L) should be >= v
    }
    bb.dotCloud.foreach { case (id, vs) =>
      val c = aa.causalContext.getOrElse(id, 0L)
      vs
        .filter(_ > c)
        .diff(aa.dotCloud.getOrElse(id, SortedSet.empty)) shouldBe empty
    }
  }

  property("merged context contains both contexts") {
    forAll { (v1: Context[Int], v2: Context[Int]) =>
      val merged = v1.merge(v2)
      contains(merged, v1)
      contains(merged, v2)
    }
  }

  property("after inserting dots, all of them should be included") {
    implicit val dotArb: Arbitrary[Dot[Int]] = Arbitrary(vTouple)
    check { (v: Context[Int], dots: Traversable[Dot[Int]]) =>
      val result = v.insertDot(dots, compact = false)
      val beforeCompact = dots.forall(result.contains)
      val compacted = result.compact
      val afterCompact = dots.forall(compacted.contains)
      beforeCompact && afterCompact
    }
  }

}
