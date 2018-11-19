package matwojcik.tagless.logging
import java.util.UUID

import cats.mtl.MonadState
import cats.syntax.functor._
import cats.tagless.finalAlg
import cats.Functor
import cats.mtl

@finalAlg
trait Tracing[F[_]] {
  def currentTrace: F[Option[Trace]]
  def startNewTrace: F[Trace]
}

object Tracing {
  type TraceMonadState[F[_]] = MonadState[F, Option[Trace]]
  implicit def instance[F[_]: TraceMonadState: Functor]: Tracing[F] = new Tracing[F] {
    override def currentTrace: F[Option[Trace]] = mtl.MonadState[F, Option[Trace]].get
    override def startNewTrace: F[Trace] = {
      val trace = Trace(Trace.Id(UUID.randomUUID().toString), Span.Id(UUID.randomUUID().toString))
      mtl.MonadState[F, Option[Trace]]
        .set(Some(trace))
        .map(_ => trace)
    }
  }
}
case class Trace(id: Trace.Id, spanId: Span.Id)

object Trace {
  case class Id(value: String) extends AnyVal
}

object Span {
  case class Id(value: String) extends AnyVal
}
