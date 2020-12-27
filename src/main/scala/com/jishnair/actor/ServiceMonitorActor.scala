package com.jishnair.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import scala.concurrent.duration._

object ServiceMonitorActor {

  case object CollectionTimeout

  def props(actorToMicroserviceId: Map[ActorRef, String],
            requestId: Long,
            requester: ActorRef,
            timeout: FiniteDuration): Props = {
    Props(new ServiceMonitorActor(actorToMicroserviceId, requestId, requester, timeout))
  }
}

/*
 ServiceMonitor actor pings each Microservices instances and waits for a reply till the timeout.
 If either all Microservices replies or reaches timeout it replies the status to the sender.
 */
class ServiceMonitorActor(actorToMicroserviceId: Map[ActorRef, String],
                          requestId: Long,
                          requester: ActorRef,
                          timeout: FiniteDuration)
  extends Actor with ActorLogging {

  import ServiceMonitorActor._
  import context.dispatcher

  val queryTimeoutTimer = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)

  //Ping all microservices
  override def preStart(): Unit = {
    log.info("service monitor started")
    actorToMicroserviceId.keysIterator.foreach { microserviceActor =>
      context.watch(microserviceActor)
      microserviceActor ! MicroserviceActor.RequestHealthCheck(requestId)
    }
  }

  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }

  override def receive: Receive =
    waitingForReplies(Map.empty, actorToMicroserviceId.keySet)

  def waitingForReplies(repliesSoFar: Map[String, String], stillWaiting: Set[ActorRef]): Receive = {
    case MicroserviceActor.RespondHealthCheck(requestId) =>
      receivedResponse(sender(), "OK", stillWaiting, repliesSoFar)

    case Terminated(microserviceActor) =>
      receivedResponse(microserviceActor, "NotAvailable", stillWaiting, repliesSoFar)

    case CollectionTimeout =>
      val timedOutReplies =
        stillWaiting.map { microserviceActor =>
          val id = actorToMicroserviceId(microserviceActor)
          id -> "TimedOut"
        }
      requester ! RegistryActor.HealthCheckResponse(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }

  def receivedResponse(microserviceActor: ActorRef, status: String, stillWaiting: Set[ActorRef], repliesSoFar: Map[String, String]): Unit = {
    context.unwatch(microserviceActor)

    val microserviceId = actorToMicroserviceId(microserviceActor)
    val newStillWaiting = stillWaiting - microserviceActor
    log.info("health of {} is {}", microserviceId, status)

    val newRepliesSoFar = repliesSoFar + (microserviceId -> status)
    if (newStillWaiting.isEmpty) {
      requester ! RegistryActor.HealthCheckResponse(requestId, newRepliesSoFar)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }
}


