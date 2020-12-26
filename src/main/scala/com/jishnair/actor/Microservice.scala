package com.jishnair.actor

import akka.actor.{Actor, ActorLogging, Props}

object Microservice {
  def props(name: String): Props = Props(new Microservice(name))

  final case class RequestGreeting(requestId: Long)

  final case class RespondGreeting(requestId: Long, message: String)

  final case class RequestHealthCheck(requestId: Long)

  final case class RespondHealthCheck(requestId: Long)

}

class Microservice(name: String) extends Actor with ActorLogging {

  import Microservice._

  override def preStart(): Unit = log.info("Microservice  {} started", name)

  override def postStop(): Unit = log.info("Microservice  {} stopped", name)

  override def receive: Receive = {

    case RequestGreeting(id) =>
      sender() ! RespondGreeting(id, s"Greetings from $name")
    case RequestHealthCheck(id) =>
     // log.info("Received health check {}", self.path.name)
      sender() ! RespondHealthCheck(id)
  }
}
