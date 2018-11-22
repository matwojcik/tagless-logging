package matwojcik.tagless.logging
import cats.effect.IO
import cats.tagless.finalAlg

import scala.concurrent.Future

@finalAlg
trait DeferFuture[F[_]] {
  def defer[A](f: => Future[A]): F[A]
}

object DeferFuture {
  implicit val ioDeferFuture: DeferFuture[IO] = new DeferFuture[IO] { override def defer[A](f: => Future[A]): IO[A] = IO.fromFuture(IO(f)) }
}
