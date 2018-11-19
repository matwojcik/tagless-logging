package matwojcik.tagless.logging
import cats.Functor
import cats.effect.Sync
import cats.syntax.functor._
import cats.tagless.finalAlg
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.MDC
import simulacrum.typeclass

@finalAlg
trait Logging[F[_]] {
  def debug(message: String): F[Unit]
  def info(message: String): F[Unit]
}

object Logging {
  implicit def instance[F[_]: Sync: Tracing: Functor]: Logging[F] = new Logging[F] with StrictLogging {
    override def debug(message: String): F[Unit] = log(logger.debug(message))
    override def info(message: String): F[Unit] = log(logger.info(message))

    private def log(loggingFunction: => Unit) = Tracing[F].currentTrace.map { trace =>
      trace.foreach(t => MDC.put("X-TraceId", t.id.value))
      loggingFunction
      MDC.remove("X-TraceId")
    }
  }
}
