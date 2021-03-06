package mx.cinvestav.events

import cats.implicits._
import cats.effect.IO
import mx.cinvestav.Declarations.NodeContext
import mx.cinvestav.commons.events.ServiceReplicator.StartedService
import mx.cinvestav.commons.events.{EventX, Get, Put}

import java.util.UUID

object Events {

  case class AddedService(
                           serialNumber:Long,
                           nodeId:String,
                           serviceId:String,
                           ipAddress:String,
                           hostname:String,
                           port:Int,
                           totalStorageCapacity:Long,
                           cacheSize:Int,
                           cachePolicy:String,
                           timestamp:Long,
                           serviceTimeNanos:Long,
                           eventType:String ="ADDED_SERVICE",
                           eventId:String= UUID.randomUUID().toString,
                           monotonicTimestamp:Long = 0L,
                           correlationId:String = ""
                         ) extends EventX
  case class RemovedService(
                           serialNumber:Long,
                           nodeId:String,
                           serviceId:String,
                           timestamp:Long,
                           serviceTimeNanos:Long,
                           eventType:String ="REMOVED_SERVICE",
                           eventId:String= UUID.randomUUID().toString,
                           monotonicTimestamp:Long = 0L,
                           correlationId:String = ""
                         ) extends EventX


  def onlyAddedService(events:List[EventX]): List[EventX] = events.filter{
    case _:AddedService => true
    case _ => false
  }
  def onlyStartedService(events:List[EventX]): List[EventX] = events.filter{
    case _:StartedService => true
    case _ => false
  }
  def sequentialMonotonic(lastSerialNumber:Int,events:List[EventX]): IO[List[EventX]] = for {
    transformeEvents <- events.zipWithIndex.traverse{
      case (event,index)=>
        for {
          now      <- IO.monotonic.map(_.toNanos)
          newEvent = event match {
            case x:AddedService => x.copy(
              monotonicTimestamp = now,
              serialNumber = lastSerialNumber+index+1)
            case x:RemovedService => x.copy(
              monotonicTimestamp = now,
              serialNumber = lastSerialNumber+index+1)
            case _ => event
          }
        } yield newEvent
    }
  } yield transformeEvents

  def saveEvents(events:List[EventX])(implicit ctx:NodeContext) = for {
    currentState     <- ctx.state.get
    currentEvents    = currentState.events
    lastSerialNumber = currentEvents.length
    transformeEvents <- sequentialMonotonic(lastSerialNumber,events=events)
    _                <- ctx.state.update{ s=>
      s.copy(events =  s.events ++ transformeEvents )
    }
  } yield()

  def orderAndFilterEventsMonotonic(events:List[EventX]):List[EventX] =
    filterEventsMonotonic(events.sortBy(_.monotonicTimestamp))
  //    Events.filterEvents(EventXOps.OrderOps.byTimestamp(events=events).reverse)


  def filterEventsMonotonic(events:List[EventX]): List[EventX] = events.foldLeft(List.empty[EventX]){
    case ((events,e))=> e match {
      case rn:RemovedService => events.filterNot {
        case an: AddedService => an.monotonicTimestamp < rn.monotonicTimestamp && an.nodeId == rn.nodeId
        case an: Put=> an.monotonicTimestamp < rn.monotonicTimestamp && an.nodeId == rn.nodeId && an.nodeId == rn.nodeId
        case an: Get => an.monotonicTimestamp < rn.monotonicTimestamp && an.nodeId == rn.nodeId && an.nodeId == rn.nodeId
        case _ => false
      }
      //      case _:AddedNode | _:Put | _:Get | _:Replicated | _:Missed | _:SetDownloads => events :+ e
      case _ => events :+ e
    }
  }

}
