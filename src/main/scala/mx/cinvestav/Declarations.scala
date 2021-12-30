package mx.cinvestav

import cats.Order
import cats.effect._
import cats.effect.std.Semaphore
import io.circe.{Decoder, Encoder, HCursor}
import mx.cinvestav.commons.events._
import mx.cinvestav.commons.types.NodeX
import mx.cinvestav.config.DefaultConfig
import mx.cinvestav.events.Events.AddedService
import org.http4s.client.Client
//import mx.cinvestav.events.Events.GetInProgress
import io.circe.generic.auto._
import io.circe.syntax._
import org.typelevel.log4cats.Logger

object Declarations {


  object Implicits {
    implicit val nodeXOrder: Order[NodeX] = new Order[NodeX] {
      override def compare(x: NodeX, y: NodeX): Int = Order.compare[Int](x.nodeId.hashCode,y.nodeId.hashCode)
    }

    implicit val eventDecoderX:Decoder[EventX] = (hCursor:HCursor) =>{
      for {
        eventType <- hCursor.get[String]("eventType")
        decoded   <- eventType match {
          case "UPLOADED" => hCursor.as[Uploaded]
          case "EVICTED" => hCursor.as[Evicted]
          case "DOWNLOADED" => hCursor.as[Downloaded]
          case "PUT" => hCursor.as[Put]
          case "GET" => hCursor.as[Get]
          case "DEL" => hCursor.as[Del]
          case "PUSH" => hCursor.as[Push]
          case "MISSED" => hCursor.as[Missed]
          case "ADDED_NODE" => hCursor.as[AddedNode]
          case "REMOVED_NODE" => hCursor.as[RemovedNode]
          case "REPLICATED" => hCursor.as[Replicated]
          case "ADDED_SERVICE" => hCursor.as[AddedService]
        }
      } yield decoded
    }
    implicit val eventXEncoder: Encoder[EventX] = {
      case p: Put => p.asJson
      case g: Get => g.asJson
      case d: Del => d.asJson
      case push:Push => push.asJson
      case x: Uploaded => x.asJson
      case y: Downloaded => y.asJson
      case y: AddedNode => y.asJson
      case rmn: RemovedNode => rmn.asJson
      case x:Evicted => x.asJson
      case r: Replicated => r.asJson
      case m: Missed => m.asJson
      case m: AddedService => m.asJson
//      case sd:Transfered => sd.asJson
//      case sd:GetInProgress => sd.asJson
    }
  }
  case class NodeState(events:List[EventX])
  case class NodeContext(
                          config:DefaultConfig,
                          state: Ref[IO,NodeState],
                          logger:Logger[IO],
                          client:Client[IO]
                        )

}
