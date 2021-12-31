package mx.cinvestav.server

import cats.data.Kleisli
import cats.implicits._
import cats.effect._
import fs2.Stream
import mx.cinvestav.Helpers
import mx.cinvestav.commons.types.Monitoring.NodeInfo
//
import mx.cinvestav.Declarations.NodeContext
import mx.cinvestav.Declarations.Implicits._
import mx.cinvestav.events.Events
import mx.cinvestav.events.Events.AddedService
//
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
//
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
//
import scala.concurrent.ExecutionContext.global

class HttpServer()(implicit ctx:NodeContext) {

  def apiBaseRouteName = s"/api/v${ctx.config.apiVersion}"

  def httpApp: Kleisli[IO, Request[IO], Response[IO]] = Router[IO](
    s"$apiBaseRouteName" -> HttpRoutes.of[IO]{
      case req@POST -> Root / "nodes"/"add" => for {
        currentState <- ctx.state.get
        _            <- ctx.logger.debug("ADDED_NODE")
        addedService <- req.as[AddedService]
        _            <- ctx.logger.debug(addedService.asJson.toString)
        _            <- Events.saveEvents(events= addedService::Nil)
        res          <- NoContent()
      } yield res
      case req@GET -> Root / "pool"/"info"  => for {

        responses <- Helpers.getNodesInfos()
//        currentState <- ctx.state.get
//        events  = Events.orderAndFilterEventsMonotonic(events=currentState.events)
//        addedServices = Events.onlyAddedService(events=events)
//        uris = addedServices.map(_.asInstanceOf[AddedService]).map{x=>
//          val hostname   = x.hostname
//          val port       = x.port
//          val apiVersion = ctx.config.apiVersion
//          Uri.unsafeFromString(s"http://$hostname:$port/api/v$apiVersion/info")
//        }
//        requests = uris.map{ u=>Request[IO](
//            method = Method.GET,
//            uri    = u,
//            headers = Headers.empty
//          )}
//        responses <- Stream.emits(requests).flatMap(r=>
//          ctx.client.stream(r).evalMap{
//            _.as[NodeInfo].handleErrorWith(e=>
//              ctx.logger.error(e.getMessage) *> NodeInfo.empty.pure[IO]
//            )
//          }
//        ).compile.to(List).onError{ e=>
//          ctx.logger.error(e.getMessage)
//        }

        _         <- ctx.logger.debug("INFO_RESPONSES")
        info = Json.obj(
          "poolId" -> ctx.config.poolId.asJson,
          "infos" -> responses.asJson
        )
        res <- Ok(info)
      }  yield res
    }
  ).orNotFound
  def run(): IO[Unit] =
    BlazeServerBuilder[IO](global)
      .bindHttp(ctx.config.port,ctx.config.host)
      .withHttpApp(httpApp = httpApp)
      .serve
      .compile
      .drain
}

object HttpServer {
  def apply()(implicit ctx:NodeContext) = new HttpServer()
}
