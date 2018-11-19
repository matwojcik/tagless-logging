package matwojcik.tagless.logging
import cats.Monad
import cats.data.StateT
import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.mtl.implicits._
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.typesafe.scalalogging.StrictLogging

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

}

object Impure extends StrictLogging {

  def function: Unit =
    logger.debug("Impure logging")
}
