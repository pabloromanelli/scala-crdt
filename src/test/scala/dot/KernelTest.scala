package dot

import org.scalacheck.Arbitrary
import org.scalactic.Equality
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.{Matchers, PropSpec}

class KernelTest extends PropSpec with CommonGenerators with Checkers with PropertyChecks with Matchers {

  /**
    * Ignore context node id when comparing equality
    */
  implicit object KernelEq extends Equality[Kernel[Int, Char]] {
    override def areEqual(a: Kernel[Int, Char], b: Any): Boolean = (a, b) match {
      case (Kernel(_, c1), Kernel(_, c2)) => implicitly[Equality[Context[Int]]].areEqual(c1, c2)
      case _ => false
    }
  }

  val delta = DeltaKernel[Int, Char]()

  property("after adding a value it is present") {
    implicit val idsArb: Arbitrary[Int] = Arbitrary(ids)
    implicit val valuesArb: Arbitrary[Char] = Arbitrary(valuesGen)

    check { (k: Kernel[Int, Char], value: Char) =>
      k.add(value, delta).result.values.contains(value)
    }
  }

  property("after removing a value it is not present") {
    implicit val valuesArb: Arbitrary[Char] = Arbitrary(valuesGen)

    check { (k: Kernel[Int, Char], value: Char) =>
      !k.remove(value, delta).result.values.contains(value)
    }
  }

  property("after adding and removing a value it is not present") {
    implicit val idsArb: Arbitrary[Int] = Arbitrary(ids)
    implicit val valuesArb: Arbitrary[Char] = Arbitrary(valuesGen)

    check { (k: Kernel[Int, Char], value: Char) =>
      !k.add(value, delta).result.remove(value, delta).result.values.contains(value)
    }
  }

  property("after removing and adding a value it is present") {
    implicit val idsArb: Arbitrary[Int] = Arbitrary(ids)
    implicit val valuesArb: Arbitrary[Char] = Arbitrary(valuesGen)

    check { (k: Kernel[Int, Char], value: Char) =>
      k.remove(value, delta).result.add(value, delta).result.values.contains(value)
    }
  }

  property("after inserting values, all of them should be included") {
    implicit val instanceValuesArb: Arbitrary[(Int, Char)] = Arbitrary(for {
      v <- valuesGen
      id <- ids
    } yield (id, v))

    check { (k: Kernel[Int, Char], values: Traversable[(Int, Char)]) =>
      val result = values.foldLeft(k) { case (newKernel, (_, v)) =>
        newKernel.add(v, delta).result
      }
      val resultValues = result.values
      values.map(_._2).forall(resultValues.contains)
    }
  }

  property("clear removes every value") {
    check { k: Kernel[Int, Char] =>
      k.clear(delta).result.values.isEmpty
    }
  }

  // TODO https://proofwiki.org/wiki/Definition:Join_Semilattice
  // TODO http://www.math.chapman.edu/~jipsen/structures/doku.php/semilattices

  property("merge is idempotent") {
    check { k: Kernel[Int, Char] =>
      k.merge(k) == k
    }
  }

  property("merge is idempotent with other kernels") {
    forAll { (k1: Kernel[Int, Char], k2: Kernel[Int, Char]) =>
      k1.merge(k2) shouldEqual k1.merge(k2).merge(k2)
    }
  }

  property("merge is associative") {
    check { (k1: Kernel[Int, Char], k2: Kernel[Int, Char], k3: Kernel[Int, Char]) =>
      k1.merge(k2.merge(k3)) == k1.merge(k2).merge(k3)
    }
  }

  property("merge is commutative") {
    forAll { (k1: Kernel[Int, Char], k2: Kernel[Int, Char]) =>
      k1.merge(k2) shouldEqual k2.merge(k1)
    }
  }

}
