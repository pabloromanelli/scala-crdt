package clocks

trait Clock[SELF <: Clock[_]] extends PartiallyOrdered[SELF] {

  /**
    * Returns the successor of this clock and the provided clock.
    * The resulting clock should be greater or equal (>=) than the two.
    */
  def merge(that: SELF): SELF

}
