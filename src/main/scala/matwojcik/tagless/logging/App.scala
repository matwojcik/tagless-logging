package matwojcik.tagless.logging
import cats.Functor
import cats.Monad
import cats.data.IndexedStateT
import cats.data.StateT
import cats.effect.ExitCase
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Sync
import cats.mtl.implicits._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.typesafe.scalalogging.StrictLogging
import org.slf4j.MDC

object App extends IOApp with StrictLogging {
  override def run(args: List[String]): IO[ExitCode] = {
    def program[F[_]: Sync: Logging: Monad: Tracing] =
      for {
        _      <- Tracing[F].startNewTrace
        _      <- Sync[F].delay(Impure.function)
        v      <- Sync[F].pure("value")
        _      <- Logging[F].info(s"log1: $v")
        _      <- Sync[F].delay(Impure.function)
        _      <- Logging[F].info("log2")
        _      <- Sync[F].delay(println("some println"))
        _      <- Tracing[F].startNewTrace
        _      <- Logging[F].debug("log3")
        result <- Sync[F].pure(ExitCode.Success)
      } yield {
        result
      }

    program[StateT[IO, Option[Trace], ?]].runA(None).map { r =>
      logger.info("End log")
      r
    }

  }

  type Dupa[A] = StateT[IO, Option[Trace], A]
  implicit def dupaMonad: Monad[Dupa] = new Monad[Dupa] with StrictLogging {
    override def pure[A](x: A): Dupa[A] = IndexedStateT.pure(x)
    override def flatMap[A, B](fa: Dupa[A])(f: A => Dupa[B]): Dupa[B] = {

      def r(a: A): Dupa[B] =
        IndexedStateT.catsDataMonadForIndexedStateT[IO, Option[Trace]].flatMap(Tracing[Dupa].currentTrace) { trace =>
          trace.foreach(t => MDC.put("X-TraceId", t.id.value))
          logger.info("flat mapping")
          val result = f(a)
//          MDC.remove("X-TraceId")
          result.map { a =>
            MDC.remove("X-TraceId")
            a
          }
        }

      IndexedStateT.catsDataMonadForIndexedStateT[IO, Option[Trace]].flatMap(fa) {
        r
      }
    }
    override def tailRecM[A, B](a: A)(f: A => Dupa[Either[A, B]]): Dupa[B] =
      IndexedStateT.catsDataMonadForIndexedStateT[IO, Option[Trace]].tailRecM(a)(f)
  }

  implicit def dupaFunctor: Functor[Dupa] = new Functor[Dupa] {
    override def map[A, B](fa: Dupa[A])(f: A => B): Dupa[B] = {
      println("mapping")
      IndexedStateT.catsDataMonadForIndexedStateT[IO, Option[Trace]].map(fa)(f)
    }
  }

  implicit def dupaSync: Sync[Dupa] = new Sync[Dupa] with StrictLogging {
    override def suspend[A](thunk: => Dupa[A]): Dupa[A] = cats.effect.Sync.catsStateTSync[IO, Option[Trace]].suspend(thunk)
    override def bracketCase[A, B](acquire: Dupa[A])(use: A => Dupa[B])(release: (A, ExitCase[Throwable]) => Dupa[Unit]): Dupa[B] =
      cats.effect.Sync.catsStateTSync[IO, Option[Trace]].bracketCase(acquire)(use)(release)
    override def raiseError[A](e: Throwable): Dupa[A] = cats.effect.Sync.catsStateTSync[IO, Option[Trace]].raiseError(e)
    override def handleErrorWith[A](fa: Dupa[A])(f: Throwable => Dupa[A]): Dupa[A] =
      cats.effect.Sync.catsStateTSync[IO, Option[Trace]].handleErrorWith(fa)(f)
    override def pure[A](x: A): Dupa[A] = cats.effect.Sync.catsStateTSync[IO, Option[Trace]].pure(x)
    override def flatMap[A, B](fa: Dupa[A])(f: A => Dupa[B]): Dupa[B] = {

      def r(a: A): Dupa[B] =
        cats.effect.Sync.catsStateTSync[IO, Option[Trace]].flatMap(Tracing[Dupa].currentTrace) { trace =>
          trace.foreach(t => MDC.put("X-TraceId", t.id.value))
          logger.info("flat mapping")
          val result = f(a)
          //          MDC.remove("X-TraceId")
          result.map { a =>
            MDC.remove("X-TraceId")
            a
          }
        }

      cats.effect.Sync.catsStateTSync[IO, Option[Trace]].flatMap(fa) {
        r
      }
    }
    override def tailRecM[A, B](a: A)(f: A => Dupa[Either[A, B]]): Dupa[B] =
      cats.effect.Sync.catsStateTSync[IO, Option[Trace]].tailRecM(a)(f)
  }
}

object Impure extends StrictLogging {

  def function: Unit =
    logger.debug("Impure logging")
}
