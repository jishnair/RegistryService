package com.jishnair.actor

import akka.actor.{Actor, ActorLogging, Props}

object Microservice {
  def props(name: String): Props = Props(new Microservice(name))

  final case class RequestGreeting(requestId: Long)

  final case class RespondGreeting(requestId: Long, message: String)

}

class Microservice(name: String) extends Actor with ActorLogging {

  import Microservice._

  override def preStart(): Unit = log.info("Microservice instance {}-{} started",name, self.path.name)

  override def postStop(): Unit = log.info("Microservice instance {}-{} stopped", name, self.path.name)

  override def receive: Receive = {

    case RequestGreeting(id) =>
      log.info("recieved msg:")
      sender() ! RespondGreeting(id, s"Greetings from $name")
  }
}
