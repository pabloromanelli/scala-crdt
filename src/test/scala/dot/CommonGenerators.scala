package dot

import org.scalacheck.{Arbitrary, Gen}
import org.scalactic.Equality
import org.scalatest.prop.PropertyChecks

import scala.collection.immutable.{HashMap, SortedSet}

trait CommonGenerators extends PropertyChecks {
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

  // TODO: buildableOfN has a static size! it will almost always return N elements
  val causalContextGen: Gen[HashMap[Int, Long]] =
    Gen.buildableOfN[HashMap[Int, Long], (Int, Long)](maxNodes, vTouple)

  val dotsGen: Gen[SortedSet[Long]] = Gen.buildableOf[SortedSet[Long], Long](genVersions)

  val dotTouple: Gen[(Int, SortedSet[Long])] = for {
    id <- ids
    dots <- dotsGen
  } yield (id, dots)

  /**
    * the cloud should only have non empty values for each id (but not every id needs to be defined)
    * TODO: buildableOfN has a static size! it will almost always return N elements
    */
  val dotCloutGen: Gen[HashMap[Int, SortedSet[Long]]] =
    Gen.buildableOfN[HashMap[Int, SortedSet[Long]], (Int, SortedSet[Long])](maxNodes, dotTouple)
      .map(_.filter { case (k, v) => v.nonEmpty })

  /**
    * A context should have an empty dot cloud for the owner node id
    */
  val contextGen: Gen[Context[Int]] = for {
    id <- ids
    cc <- causalContextGen
    dc <- dotCloutGen
  } yield new Context(id, cc, dc.filter(_._1 != id))

  def versionFromId(context: Context[Int], id: Int): Gen[Long] = {
    val fromContext = context.causalContext.get(id).map(v => Gen.chooseNum(1, v))
    val fromCloud = context.dotCloud.get(id).filter(_.nonEmpty).map(versions => Gen.oneOf(versions.toSeq))
    List(fromContext, fromCloud).collect { case Some(g) => g } match {
      case List(single) => single
      case List(a, b) => Gen.oneOf(a, b)
    }
  }

  def dotFrom(context: Context[Int]): Gen[Dot[Int]] = for {
    id <- Gen.oneOf(context.ids.toSeq)
    version <- versionFromId(context, id)
  } yield (id, version)

  val valuesGen: Gen[Char] = Gen.alphaLowerChar

  def genValues(set: Iterable[Dot[Int]]): Gen[Map[Char, Set[Dot[Int]]]] =
    for {
      chars <- Gen.listOfN(set.size, valuesGen)
    } yield set.zip(chars)
      .groupBy(_._2)
      .mapValues(_.map(_._1).toSet)

  val kernelGen: Gen[Kernel[Int, Char]] = for {
    context <- contextGen
    dots <- Gen.containerOf[Set, Dot[Int]](dotFrom(context))
    values <- genValues(dots)
  } yield Kernel(HashMap(values.toSeq: _*), context)

  // TODO Do I need to provide a shrinking for this arbs or the automatically shrink?
  implicit val contextArb: Arbitrary[Context[Int]] = Arbitrary(contextGen)
  implicit val kernelArb: Arbitrary[Kernel[Int, Char]] = Arbitrary(kernelGen)

  /**
    * Ignore node id when comparing equality
    */
  implicit object ContextEq extends Equality[Context[Int]] {
    override def areEqual(a: Context[Int], b: Any): Boolean = (a, b) match {
      case (Context(_, cc1, dc1), Context(_, cc2, dc2)) => cc1 == cc2 && dc1 == dc2
      case _ => false
    }
  }

}
