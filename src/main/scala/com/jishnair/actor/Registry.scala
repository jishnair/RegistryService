package com.jishnair.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import akka.routing.{BalancingPool, BroadcastPool, RoundRobinGroup, RoundRobinPool}
import com.jishnair.model.Model
import com.jishnair.model.Model.MicroserviceModel

import scala.concurrent.duration.DurationInt
import scala.util.Random

/**
 * Registry creates and track Microservices
 */
object Registry {
  def props: Props = Props(new Registry)

  //messages
  final case class CreateMicroservice(requestId: Long, serviceName: String, isEntryPoint: Boolean, replicas: Int, dependency: List[String])

  final case class MicroserviceCreated(requestId: Long)

  final case class MicroserviceAlreadyExists(requestId: Long)

  final case class RequestMicroserviceList(requestId: Long)

  final case class HealthCheckAllMicroservices(requestId: Long)

  final case class HealthCheckResponse(requestId: Long, message: Map[String, String])

  final case class GetHealthCheckReport(requestId: Long)

  final case class GetRouterActorList(requestId: Long)


}

class Registry extends Actor with ActorLogging {

  import Registry._

  //Keep track of the created Actors
  //TODO: This should be saved in a Database
  var routerToActorDB = Map.empty[String, ActorRef]
  var actorToRouterDB = Map.empty[ActorRef, String]
  var replicasToActorDB = Map.empty[String, ActorRef]
  var actorToReplicasDB = Map.empty[ActorRef, String]
  var nameToMicroserviceDB = Map.empty[String, MicroserviceModel]

  var healthCheckReport = Map.empty[String, String]

  override def preStart(): Unit = log.info("Registry started")

  override def postStop(): Unit = log.info("Registry stopped")

  override def receive: Receive = {
    case createMsg@CreateMicroservice(requestId, name, _, _, _) =>
      //check if a microservice with the given "name" already exist
      routerToActorDB.get(name) match {
        case Some(_) =>
          log.warning("The Microservice with name {} already exists", name)
          sender() ! MicroserviceAlreadyExists(requestId)

        case None =>
          //Create group router actor which manages all the replicas
          val groupActor: ActorRef = // group's master
            context.actorOf(RoundRobinGroup(createReplicas(createMsg)).props(), name)
          log.info("Creating router actor {} ", name)

          context.watch(groupActor)
          routerToActorDB += name -> groupActor
          actorToRouterDB += groupActor -> name
          nameToMicroserviceDB += name -> Model.MicroserviceModel(name, generateRandomId(), createMsg.isEntryPoint,createMsg.replicas, createMsg.dependency)
          sender() ! Registry.MicroserviceCreated(createMsg.requestId)

      }

    case RequestMicroserviceList(requestId) =>
      log.info(nameToMicroserviceDB.values.toList.toString())
      sender() ! nameToMicroserviceDB.values.toList

    case GetRouterActorList(requestId) =>
      sender() ! routerToActorDB.keySet

    case HealthCheckAllMicroservices(requestId) =>
      log.info("checking health")
      if (!actorToReplicasDB.isEmpty)
        context.actorOf(ServiceMonitor.props(actorToReplicasDB, requestId, requester = self, 3.seconds))

    case HealthCheckResponse(requestId, message) =>
      log.info("Health check result=" + message.toString())
      healthCheckReport = message

    case GetHealthCheckReport(requestId) =>
      sender() ! healthCheckReport

    case Terminated(actor) =>
      //If the parent actor is registry its a group actor, else a replica actor
      if (actor.path.parent == self.path) {
        val routerName = actorToRouterDB(actor)
        log.info("Microservice actor {} has been terminated", routerName)
        actorToRouterDB -= actor
        routerToActorDB -= routerName
        nameToMicroserviceDB -= routerName
      } else {
        val replicaName = actorToReplicasDB(actor)
        actorToReplicasDB -= actor
        replicasToActorDB -= replicaName
      }

  }

  //Create N instances of a Microservice
  def createReplicas(createMsg: CreateMicroservice): List[String] = {
    val range = (1 to createMsg.replicas).toList
    val replicaNames = range.map(createMsg.serviceName + "-" + "instance" + _)
    val actorPaths = replicaNames.map("/user/registry/" + _)

    replicaNames.foreach(name => {
      val replicaActor = context.actorOf(Microservice.props(name), name)
      replicasToActorDB += name -> replicaActor
      actorToReplicasDB += replicaActor -> name
      context.watch(replicaActor)
    })
    actorPaths
  }

  //pseudo unique id generator
  def generateRandomId(): Int = {
    100000 + new Random().nextInt(900000)
  }
}
