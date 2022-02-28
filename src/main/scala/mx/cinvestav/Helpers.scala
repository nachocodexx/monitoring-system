package mx.cinvestav

import cats.implicits._
import cats.effect.IO
import fs2.Stream
import mx.cinvestav.Declarations.NodeContext
import mx.cinvestav.commons.types.Monitoring.NodeInfo
import mx.cinvestav.events.Events
import mx.cinvestav.events.Events.AddedService
import mx.cinvestav.commons.Implicits._
//
import org.http4s.{Headers, Method, Request, Uri}
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
//
import io.circe._
import io.circe.generic.auto._
import mx.cinvestav.Declarations.Implicits.nodeInfoDecoder

object Helpers {

  def getNodesInfos(addedServices:List[AddedService])(implicit ctx:NodeContext): IO[List[NodeInfo]] = {
    for {
      _              <- IO.unit
      uris           = addedServices.map{ x=>
        val hostname   = x.hostname
        val port       = x.port
        val apiVersion = ctx.config.apiVersion
        Uri.unsafeFromString(s"http://$hostname:$port/api/v$apiVersion/info")
      }
      requests = uris.map{ u=>Request[IO](
        method = Method.GET,
        uri    = u,
        headers = Headers.empty
      )}
      responses <- Stream.emits(requests).flatMap(r=>

        Stream.eval(ctx.logger.debug(s"GET_INFO_v2 ${r.uri}"))*>ctx.client.stream(r).evalMap{ x=>
          for {
            info <- x.as[NodeInfo]
              .handleErrorWith(e=>
                ctx.logger.error(e.getMessage) *> NodeInfo.empty.pure[IO]
              )
            _ <- ctx.logger.debug(info.toString)
          } yield info
        }.handleErrorWith{ e=>
          ctx.logger.error(e.getMessage).pureS *> Stream.emit(NodeInfo.empty)
        }

      ).compile.to(List).onError{ e=> ctx.logger.error(e.getMessage)}

    } yield responses
    }

  }
