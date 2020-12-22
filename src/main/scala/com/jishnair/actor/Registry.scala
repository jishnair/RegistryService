package com.jishnair.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing.BalancingPool

/**
 * Registry creates and track Microservices
 */
object Registry {
  def props: Props= Props(new Registry)

  final case class CreateMicroservice(serviceName: String, replicas: Option[Int])
  case object MicroserviceCreated
  case object MicroserviceAlreadyExists

  final case class RequestMicroserviceList(requestId: Long)
  final case class ReplyMicroserviceList(requestId:Long, response: Set[String])

}

class Registry extends Actor with ActorLogging{
  import Registry._

  //Keep track of the created microservices
  var nameToActorDB = Map.empty[String, ActorRef]
  var actorToNameDB = Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("Registry started")
  override def postStop(): Unit = log.info("Registry stopped")

  override def receive: Receive = {
    case createMsg @ CreateMicroservice(name, _) =>
     //check if a microservice with the given "name" already exist
      nameToActorDB.get(name) match {
      case Some(_) =>
        log.warning("The Microservice with name {} already exists", name)
        sender() ! MicroserviceAlreadyExists

      case None =>
        //Create N number of microservices with using a pool of actors
        val microServiceActor = context.actorOf(Microservice.props(createMsg.serviceName)
          .withRouter( BalancingPool(nrOfInstances = createMsg.replicas.getOrElse(1))), s"${createMsg.serviceName}")
        context.watch(microServiceActor)
        nameToActorDB += name -> microServiceActor
        actorToNameDB += microServiceActor -> name
        sender() ! Registry.MicroserviceCreated

      }

    case RequestMicroserviceList(requestId) =>
      sender() ! nameToActorDB.keySet

    case Terminated(microServiceActor) =>
    val serviceName = actorToNameDB(microServiceActor)
    log.info("Microservice actor {} has been terminated", serviceName)
    actorToNameDB -= microServiceActor
    nameToActorDB -= serviceName
  }
}
