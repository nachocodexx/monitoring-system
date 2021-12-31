package mx.cinvestav

import cats.effect.{ExitCode, IO, IOApp}
//
import mx.cinvestav.Declarations.{NodeContext, NodeState}
import mx.cinvestav.config.DefaultConfig
import mx.cinvestav.server.HttpServer
//
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
//
import pureconfig._
import pureconfig.generic.auto._
//
import scala.concurrent.duration._
import language.postfixOps

import scala.concurrent.ExecutionContext.global
object Main extends IOApp {
  implicit val config: DefaultConfig = ConfigSource.default.loadOrThrow[DefaultConfig]
  implicit val unsafeLogger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  def initContext(client:Client[IO]): IO[NodeContext] = for {
    _          <- Logger[IO].debug(s"-> MONITORING SYSTEM[${config.nodeId}]")
    _initState = NodeState(events = Nil)
    _          <- Logger[IO].debug(_initState.toString)
    state      <- IO.ref(_initState)
    ctx        = NodeContext(config=config,logger=unsafeLogger,state=state,client=client)
  } yield ctx


  override def run(args: List[String]): IO[ExitCode] =for {
    (client,finalizer) <- BlazeClientBuilder[IO](global).resource.allocated
    implicit0(ctx:NodeContext) <- initContext(client)
    _ <- Daemon(period = ctx.config.delayMs milliseconds).compile.drain.start
    _ <- HttpServer().run()
    _ <- finalizer
  } yield ExitCode.Success

}