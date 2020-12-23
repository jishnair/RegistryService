package com.jishnair.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing.{BalancingPool, BroadcastPool, RoundRobinPool}
import com.jishnair.model.Model

import scala.concurrent.duration.DurationInt
import scala.util.Random

/**
 * Registry creates and track Microservices
 */
object Registry {
  def props: Props = Props(new Registry)

  final case class CreateMicroservice(requestId: Long, serviceName: String, isEntryPoint: Boolean, replicas: Int, dependency: List[String])

  final case class MicroserviceCreated(requestId: Long)

  final case class MicroserviceAlreadyExists(requestId: Long)

  final case class RequestMicroserviceList(requestId: Long)

  final case object DeviceNotAvailable

  final case object DeviceTimedOut

  final case class HealthCheckAllMicroservices(requestId: Long)

  final case class RespondAllHealthCheck(requestId: Long, message: Map[String, String])

}

class Registry extends Actor with ActorLogging {

  import Registry._

  //Keep track of the created microservices
  var nameToActorDB = Map.empty[String, ActorRef]
  var actorToNameDB = Map.empty[ActorRef, String]
  var nameToMicroserviceDB = Map.empty[String, Model.MicroserviceModel]

  override def preStart(): Unit = log.info("Registry started")

  override def postStop(): Unit = log.info("Registry stopped")

  override def receive: Receive = {
    case createMsg@CreateMicroservice(requestId, name, _, _, _) =>
      //check if a microservice with the given "name" already exist
      nameToActorDB.get(name) match {
        case Some(_) =>
          log.warning("The Microservice with name {} already exists", name)
          sender() ! MicroserviceAlreadyExists(requestId)

        case None =>
          //Create N number of microservices with using a pool of actors
          val microServiceActor = context.actorOf(Microservice.props(createMsg.serviceName)
            .withRouter(RoundRobinPool(nrOfInstances = createMsg.replicas)), s"${createMsg.serviceName}")
          context.watch(microServiceActor)
          nameToActorDB += name -> microServiceActor
          actorToNameDB += microServiceActor -> name
          nameToMicroserviceDB += name -> Model.MicroserviceModel(name, generateRandomId(), createMsg.isEntryPoint, createMsg.dependency, true)
          sender() ! Registry.MicroserviceCreated(createMsg.requestId)

      }

    case RequestMicroserviceList(requestId) =>
      sender() ! nameToMicroserviceDB.values.toList

    case HealthCheckAllMicroservices(requestId) =>
      log.info("checking health")
      context.actorOf(ServiceMonitor.props(actorToNameDB, requestId, requester = sender(), 3.seconds))

    case Terminated(microServiceActor) =>
      val serviceName = actorToNameDB(microServiceActor)
      log.info("Microservice actor {} has been terminated", serviceName)
      actorToNameDB -= microServiceActor
      nameToActorDB -= serviceName
      nameToMicroserviceDB -= serviceName
  }

  //pseudo unique id generator
  def generateRandomId(): Int = {
    100000 + new Random().nextInt(900000)
  }
}
