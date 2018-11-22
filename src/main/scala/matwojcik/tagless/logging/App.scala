package matwojcik.tagless.logging
import java.util.concurrent.ForkJoinPool

import cats.Applicative
import cats.data.StateT
import cats.effect.{Async, ContextShift, ExitCode, IO, IOApp}
import cats.mtl.implicits._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future}

object App extends IOApp with StrictLogging {

  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(5))
  import Logging._ // TODO would be better to not require that import

  override def run(args: List[String]): IO[ExitCode] =
    Program.program[StateT[IO, Option[Trace], ?]].runA(None).map { r =>
      logger.info("End log")
      r
    }

  implicit def someDefer[F[_]: DeferFuture: Applicative]: DeferFuture[StateT[F, Option[Trace], ?]] =
    new DeferFuture[StateT[F, Option[Trace], ?]] {
      override def defer[A](f: => Future[A]): StateT[F, Option[Trace], A] = StateT.liftF(DeferFuture[F].defer[A](f))
    }
}

object Impure extends StrictLogging {

  def function: Unit =
    logger.debug("Impure logging")

  def futureFunction(implicit ec: ExecutionContext) = Future { logger.debug("Logging in future") }
}

object Program {

  def program[F[_]: Async: Logging: Tracing: DeferFuture](implicit CS: ContextShift[F], ec: ExecutionContext): F[ExitCode] =
    for {
      _      <- Tracing[F].startNewTrace
      _      <- Async[F].delay(Impure.function)
      v      <- Async[F].pure("value")
      _      <- Logging[F].info(s"log1: $v")
      _      <- Async[F].delay(Impure.function)
      _      <- DeferFuture[F].defer(Impure.futureFunction)
      _      <- Logging[F].info("log2")
      _      <- Async[F].delay(println("some println"))
      _      <- Tracing[F].startNewTrace
      _      <- Logging[F].debug("log3")
      result <- Async[F].pure(ExitCode.Success)
    } yield {
      result
    }

}
