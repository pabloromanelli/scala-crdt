package object dot {

  type Dot[ID] = (ID, Long)

  implicit class dotFields[ID](val dot: Dot[ID]) extends AnyVal {
    def id: ID = dot._1

    def version: Long = dot._2
  }

  implicit class kernelResult[ID, V](val r: (DeltaKernel[ID, V], Kernel[ID, V])) extends AnyVal {
    def delta: DeltaKernel[ID, V] = r._1

    def result: Kernel[ID, V] = r._2
  }

}
