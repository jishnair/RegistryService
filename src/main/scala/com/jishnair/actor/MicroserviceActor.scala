package com.jishnair.actor

import akka.actor.{Actor, ActorLogging, Props}

object MicroserviceActor {
  def props(name: String): Props = Props(new MicroserviceActor(name))

  final case class RequestGreeting(requestId: Long)

  final case class RespondGreeting(requestId: Long, message: String)

  final case class RequestHealthCheck(requestId: Long)

  final case class RespondHealthCheck(requestId: Long)

}
//Represents a microservice instance
class MicroserviceActor(name: String) extends Actor with ActorLogging {

  import MicroserviceActor._

  override def preStart(): Unit = log.info("Microservice  {} started", name)

  override def postStop(): Unit = log.info("Microservice  {} stopped", name)

  override def receive: Receive = {

    case RequestGreeting(id) =>
      log.info(s"Greetings from $name")
      sender() ! RespondGreeting(id, s"Greetings from $name")
    case RequestHealthCheck(id) =>
      sender() ! RespondHealthCheck(id)
  }
}
