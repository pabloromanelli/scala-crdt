package clocks

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

import scala.collection.immutable.HashMap

class VersionVectorTest extends PropSpec with Checkers with PropertyChecks with Matchers {

  implicit val config: PropertyCheckConfiguration =
    PropertyCheckConfiguration(1000, 500D, 0, 1000, 4)

  val maxNodes = 10

  val ids: Gen[Int] = Gen.choose(1, maxNodes)

  /**
    * Limited to avoid long overflow.
    * Starting from 1 because there are no value when the version is 0 on the map.
    */
  val genVersions: Gen[Long] = Gen.chooseNum(1L, Long.MaxValue / 2)

  val vTouple: Gen[(Int, Long)] = for {
    id <- ids
    version <- genVersions
  } yield (id, version)

  val vMap: Gen[HashMap[Int, Long]] =
    Gen.buildableOfN[HashMap[Int, Long], (Int, Long)](maxNodes, vTouple)

  implicit val versionVectors: Arbitrary[VersionVector[Int]] = Arbitrary(vMap.map(VersionVector(_)))

  property("merge always return bigger vectors") {
    check { (v1: VersionVector[Int], v2: VersionVector[Int]) =>
      val result = v1.merge(v2)
      result >= v1 && result >= v2
    }
  }

  property("inc always return bigger vectors") {
    implicit val x: Arbitrary[Int] = Arbitrary(ids)
    check { (v: VersionVector[Int], id: Int) =>
      v < (v + id)
    }
  }

  property("merge itself returns itself") {
    check { v: VersionVector[Int] =>
      v.merge(v) == v
    }
  }

  property("merge is idempotent") {
    check { (v1: VersionVector[Int], v2: VersionVector[Int]) =>
      v1.merge(v2) == v1.merge(v2).merge(v2)
    }
  }

  property("merge is conmutative") {
    check { (v1: VersionVector[Int], v2: VersionVector[Int]) =>
      v1.merge(v2) == v2.merge(v1)
    }
  }

  property("reflexivity") {
    check { v: VersionVector[Int] =>
      v <= v
    }
  }

  // Can't do it like this (too many tests evaluations discarded):
  // property("antisymmetry") {
  //   forAll { (v1: VersionVector[Int], v2: VersionVector[Int]) =>
  //     whenever(v1 <= v2 && v2 <= v1) {
  //       v1 == v2
  //     }
  //   }
  // }

  property("transitivity") {
    // Can't do it like this (too many tests evaluations discarded):
    // forAll { (v1: VersionVector[Int], v2: VersionVector[Int], v3: VersionVector[Int]) =>
    // whenever(v1 <= v2 && v2 <= v3) {
    //   v1 <= v3
    // }
    implicit val x: Arbitrary[Int] = Arbitrary(ids)
    check { (v1: VersionVector[Int], ids1: List[Int], ids2: List[Int]) =>
      val v2 = ids1.foldLeft(v1)(_ + _)
      val v3 = (ids1 ::: ids2).foldLeft(v1)(_ + _)
      (v1 <= v2) && (v2 <= v3) && (v1 <= v3)
    }
  }

  property("concurrent updates on different nodes are concurrent") {
    implicit val x: Arbitrary[Int] = Arbitrary(ids)
    // '==>' is not working, switching to 'forAll' format
    forAll { (v: VersionVector[Int], id1: Int, id2: Int) =>
      whenever(id1 != id2) {
        val v2 = v + id1
        val v3 = v + id2
        v2.tryCompareTo(v3) shouldBe None
        (v2 <= v3) shouldBe false
        (v3 <= v2) shouldBe false
      }
    }
  }

}
