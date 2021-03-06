package tsec.hashing

import cats.Id
import fs2.Pipe

trait CryptoHashAPI[A] {

  /** The cryptographic hash function, in its
    * raw form
    *
    * It may or may not be pure, depending on the implementation of
    * S (in libsodium it is not pure, since JNI,
    * but in java it is essentially pure)
    *
    * @return
    */
  final def unsafeHash(bytes: Array[Byte])(implicit C: CryptoHasher[Id, A]): CryptoHash[A] =
    C.hash(bytes)

  /** Lift a the cryptographic hash function into an
    * F[_] which captures side effects.
    *
    * The underlying hash function may or may not side effect.
    * @return
    */
  final def hash[F[_]](bytes: Array[Byte])(implicit C: CryptoHasher[F, A]): F[CryptoHash[A]] =
    C.hash(bytes)

  /** A pipe that transforms a byte stream into the stream of its
    * cryptographic hash.
    *
    * Useful for hashes of arbitrary length.
    */
  def hashPipe[F[_]](implicit C: CryptoHasher[F, A]): Pipe[F, Byte, Byte] = C.hashPipe

}
