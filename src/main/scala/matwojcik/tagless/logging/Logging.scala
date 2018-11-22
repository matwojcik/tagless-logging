package matwojcik.tagless.logging
import cats.Functor
import cats.effect.{Async, ExitCase, Sync}
import cats.syntax.functor._
import cats.tagless.finalAlg
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.MDC

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


  implicit def tracingAsync[F[_]: Tracing](implicit underlyingAsync: Async[F]): Async[F] = new Async[F] {



    import cats.syntax.flatMap._

    override def suspend[A](thunk: => F[A]): F[A] = underlyingAsync.suspend(thunk)
    override def bracketCase[A, B](acquire: F[A])(use: A => F[B])(release: (A, ExitCase[Throwable]) => F[Unit]): F[B] =
      underlyingAsync.bracketCase(acquire)(use)(release)
    override def raiseError[A](e: Throwable): F[A] = underlyingAsync.raiseError(e)
    override def handleErrorWith[A](fa: F[A])(f: Throwable => F[A]): F[A] =
      underlyingAsync.handleErrorWith(fa)(f)
    override def pure[A](x: A): F[A] = underlyingAsync.pure(x)
    override def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B] = {

      underlyingAsync.flatMap(fa) { a =>
        underlyingAsync.flatMap(Tracing[F].currentTrace) { trace =>
          underlyingAsync.guarantee(underlyingAsync.flatMap(underlyingAsync.delay(trace.foreach(t => MDC.put("X-TraceId", t.id.value))))(_ => f(a)))(underlyingAsync.delay {
            MDC.remove("X-TraceId")
          })
        }
      }

    }
    override def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B] =
      underlyingAsync.tailRecM(a)(f)
    override def async[A](k: (Either[Throwable, A] => Unit) => Unit): F[A] =
      underlyingAsync.async(k)
    override def asyncF[A](k: (Either[Throwable, A] => Unit) => F[Unit]): F[A] = underlyingAsync.asyncF(k)
  }
}
