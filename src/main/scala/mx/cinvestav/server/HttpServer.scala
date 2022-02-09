package mx.cinvestav.server

import cats.data.Kleisli
import cats.implicits._
import cats.effect._
import fs2.Stream
import mx.cinvestav.Helpers
import mx.cinvestav.commons.types.Monitoring.{NodeInfo, PoolInfo}
import org.typelevel.ci.CIString
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
      case req@GET -> Root / "stats" => for {
        currentState  <- ctx.state.get
        events        = Events.orderAndFilterEventsMonotonic(events=currentState.events)
        addedServices = Events.onlyAddedService(events=events).map(_.asInstanceOf[AddedService])
        infos         <- Helpers.getNodesInfos(addedServices = addedServices)
        stats         = Json.obj(
          "nodeId" -> ctx.config.nodeId.asJson,
          "delayMs" -> ctx.config.delayMs.asJson,
          "apiVersion" -> ctx.config.apiVersion.asJson,
          "port"-> ctx.config.port.asJson,
          "infos" -> infos.asJson
        )
        res   <- Ok(stats)
      } yield res

      case req@POST -> Root / "nodes"/"add" => for {
        currentState <- ctx.state.get
        _            <- ctx.logger.debug("ADDED_NODE")
        addedService <- req.as[AddedService]
        _            <- ctx.logger.debug(addedService.asJson.toString)
        _            <- Events.saveEvents(events= addedService::Nil)
        res          <- NoContent()
      } yield res

      case req@GET -> Root / "pool"/"info"  => for {
        currentState  <- ctx.state.get
        headers       = req.headers
        nodesKey      = CIString("Nodes")
        maybeNodesIds = headers.get(nodesKey)
        events        = Events.orderAndFilterEventsMonotonic(events=currentState.events)
        addedServices = Events.onlyAddedService(events=events).map(_.asInstanceOf[AddedService])
        poolId        = ctx.config.poolId
        res           <- maybeNodesIds match {
          case Some(nodeIds) => for {
            _              <- IO.unit
            _              <- ctx.logger.debug(s"NODE_IDS $nodeIds")
            _              <- ctx.logger.debug((s"NODE_IDS2 ${addedServices.map(_.nodeId)}"))
            _addedServices = addedServices.filter(x=>nodeIds.toList.contains(x.nodeId))
            responses      <- Helpers.getNodesInfos(addedServices = _addedServices)
            _              <- ctx.logger.debug("INFO_RESPONSES")
            info           = PoolInfo(poolId = poolId,infos=responses)
            res            <- Ok(info.asJson)
          } yield res
          case None => for {
            infos       <- Helpers.getNodesInfos(addedServices = addedServices)
            _           <- ctx.logger.debug("INFO_RESPONSES")
            info        = PoolInfo(poolId = poolId,infos = infos)
            res         <- Ok(info.asJson)
          } yield res
        }
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
