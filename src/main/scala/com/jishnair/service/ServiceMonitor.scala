package com.jishnair.service

import akka.actor.{ Actor, ActorLogging, ActorRef, Props, Terminated }

import scala.concurrent.duration._

//#query-full
//#query-outline
object ServiceMonitor {
  case object CollectionTimeout

/*  def props(
             actorToDeviceId: Map[ActorRef, String],
             requestId: Long,
             requester: ActorRef,
             timeout: FiniteDuration): Props = {
    Props(new ServiceMonitor(actorToDeviceId, requestId, requester, timeout))
  }*/
}

/**
 *



class ServiceMonitor(
                        actorToDeviceId: Map[ActorRef, String],
                        requestId: Long,
                        requester: ActorRef,
                        timeout: FiniteDuration)
  extends Actor
    with ActorLogging {
  import ServiceMonitor._
  import context.dispatcher
  val queryTimeoutTimer = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)

  override def preStart(): Unit = {
    actorToDeviceId.keysIterator.foreach { deviceActor =>
      context.watch(deviceActor)
      deviceActor ! Microservice.ReadTemperature(0)
    }
  }

  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }

  //#query-outline
  //#query-state
  override def receive: Receive =
    waitingForReplies(Map.empty, actorToDeviceId.keySet)

  def waitingForReplies(
                         repliesSoFar: Map[String, Manager.TemperatureReading],
                         stillWaiting: Set[ActorRef]): Receive = {
    case Microservice.RespondGreeting(0, valueOption) =>
      val deviceActor = sender()
      val reading = valueOption match {
        case Some(value) => Manager.Temperature(value)
        case None        => Manager.TemperatureNotAvailable
      }
      receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar)

    case Terminated(deviceActor) =>
      receivedResponse(deviceActor, Manager.DeviceNotAvailable, stillWaiting, repliesSoFar)

    case CollectionTimeout =>
      val timedOutReplies =
        stillWaiting.map { deviceActor =>
          val deviceId = actorToDeviceId(deviceActor)
          deviceId -> Manager.DeviceTimedOut
        }
      requester ! Manager.RespondAllTemperatures(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }
  //#query-state

  //#query-collect-reply
  def receivedResponse(
                        deviceActor: ActorRef,
                        reading: Manager.TemperatureReading,
                        stillWaiting: Set[ActorRef],
                        repliesSoFar: Map[String, Manager.TemperatureReading]): Unit = {
    context.unwatch(deviceActor)
    val deviceId = actorToDeviceId(deviceActor)
    val newStillWaiting = stillWaiting - deviceActor

    val newRepliesSoFar = repliesSoFar + (deviceId -> reading)
    if (newStillWaiting.isEmpty) {
      requester ! Manager.RespondAllTemperatures(requestId, newRepliesSoFar)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }
  //#query-collect-reply

  //#query-outline
}
//#query-outline
//#query-full

 */