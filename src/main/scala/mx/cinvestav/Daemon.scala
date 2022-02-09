package mx.cinvestav

import cats.implicits._
import cats.effect._
import fs2.Stream
import mx.cinvestav.Declarations.NodeContext
import mx.cinvestav.events.Events
import mx.cinvestav.events.Events.AddedService
//
import scala.concurrent.duration._
import language.postfixOps
//
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

object Daemon {

  def apply(period:FiniteDuration)(implicit ctx:NodeContext) = {
    Stream.awakeEvery[IO](period).evalMap{ _ =>
      for {
        currentState  <- ctx.state.get
        events        = Events.orderAndFilterEventsMonotonic(events=currentState.events)
        addedServices = Events.onlyAddedService(events=events).map(_.asInstanceOf[AddedService])
        responses     <- Helpers.getNodesInfos(addedServices = addedServices)
//        _
//        _             <- if(responses.isEmpty)
//          IO.unit
//        else
//          ctx.logger.debug(s"INFOS ${responses.asJson.toString}")
      } yield()
    }
  }

}
