package tsec

import tsec.common.TSecError

import scala.reflect.ClassTag

package object authorization {

  /** A simple trait to allow array instantiation from a type into an unboxed primitive array.
    *
    * We simply want arrays for the sake of speed when iterating through them, thus
    * we are doing this.
    */
  sealed trait AuthPrimitive[A] {
    def unBoxedFromRepr[I](getRepr: I => A, array: Array[I]): Array[A]
    def unboxedSet(set: Set[A]): Array[A]
  }

  implicit val IntPrimitive: AuthPrimitive[Int] = new AuthPrimitive[Int] {
    def unBoxedFromRepr[I](getRepr: (I) => Int, array: Array[I]): Array[Int] = {
      val arrayLen = array.length
      val target   = new Array[Int](arrayLen)
      var i        = 0
      while (i < arrayLen) {
        target(i) = getRepr(array(i))
        i += 1
      }
      target
    }

    def unboxedSet(set: Set[Int]): Array[Int] = {
      var i     = 0
      val array = new Array[Int](set.size)
      set.foreach { e =>
        array(i) = e
        i += 1
      }
      array
    }
  }

  implicit val LongPrimitive: AuthPrimitive[Long] = new AuthPrimitive[Long] {
    def unBoxedFromRepr[I](getRepr: (I) => Long, array: Array[I]): Array[Long] = {
      val arrayLen = array.length
      val target   = new Array[Long](arrayLen)
      var i        = 0
      while (i < arrayLen) {
        target(i) = getRepr(array(i))
        i += 1
      }
      target
    }

    def unboxedSet(set: Set[Long]): Array[Long] = {
      var i     = 0
      val array = new Array[Long](set.size)
      set.foreach { e =>
        array(i) = e
        i += 1
      }
      array
    }
  }

  implicit val StringPrimitive: AuthPrimitive[String] = new AuthPrimitive[String] {
    def unBoxedFromRepr[I](getRepr: (I) => String, array: Array[I]): Array[String] = {
      val arrayLen = array.length
      val target   = new Array[String](arrayLen)
      var i        = 0
      while (i < arrayLen) {
        target(i) = getRepr(array(i))
        i += 1
      }
      target
    }

    def unboxedSet(set: Set[String]): Array[String] = {
      var i     = 0
      val array = new Array[String](set.size)
      set.foreach { e =>
        array(i) = e
        i += 1
      }
      array
    }
  }

  implicit val BytePrimitive: AuthPrimitive[Byte] = new AuthPrimitive[Byte] {
    def unBoxedFromRepr[I](getRepr: (I) => Byte, array: Array[I]): Array[Byte] = {
      val arrayLen = array.length
      val target   = new Array[Byte](arrayLen)
      var i        = 0
      while (i < arrayLen) {
        target(i) = getRepr(array(i))
        i += 1
      }
      target
    }

    def unboxedSet(set: Set[Byte]): Array[Byte] = {
      var i     = 0
      val array = new Array[Byte](set.size)
      set.foreach { e =>
        array(i) = e
        i += 1
      }
      array
    }
  }

  type AuthGroup[G] = AuthGroup.Type[G]
  
  object AuthGroup {
    type Type[A] <: Array[A]

    private def unsafeApply[G: ClassTag](array: Array[G]) = array.asInstanceOf[AuthGroup[G]]

    def apply[G: ClassTag](values: G*): AuthGroup[G]    = values.toSet.toArray.asInstanceOf[AuthGroup[G]]
    def fromSet[G: ClassTag](set: Set[G]): AuthGroup[G] = set.toArray.asInstanceOf[AuthGroup[G]]
    def unsafeFromArray[G: ClassTag](array: Array[G]): AuthGroup[G] = {
      val arrayLen = array.length
      val newArr   = new Array[G](arrayLen)
      System.arraycopy(array, 0, newArr, 0, arrayLen)
      newArr.asInstanceOf[AuthGroup[G]]
    }
    def fromSeq[G: ClassTag](seq: Seq[G]): AuthGroup[G]       = unsafeApply[G](seq.distinct.toArray)
    def unsafeFromSeq[G: ClassTag](seq: Seq[G]): AuthGroup[G] = unsafeApply(seq.toArray)
    def empty[G: ClassTag]: AuthGroup[G]           = unsafeApply[G](Array.empty[G])
  }

  /** A simple typeclass that allows us to propagate information that is required for authorization */
  trait AuthorizationInfo[F[_], Role, U] {
    def fetchInfo(u: U): F[Role]
  }

  trait DynamicAuthGroup[F[_], Grp] {
    def fetchGroupInfo: F[AuthGroup[Grp]]
  }

  type InvalidAuthLevel = InvalidAuthLevelError.type

  final object InvalidAuthLevelError extends TSecError {
    val cause: String = "The minimum auth level is zero."
  }

}
