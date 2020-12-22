package com.jishnair.service

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.pattern.ask
import akka.routing.{GetRoutees, Routees}
import akka.util.Timeout
import com.jishnair.actor.Registry.{CreateMicroservice, HealthCheckAllMicroservices, RequestMicroserviceList}
import com.jishnair.controller.RegistryController.{registryRef, rnd}
import com.jishnair.domain.Domain
import com.jishnair.domain.Domain.{Deployment, MicroserviceDto}
import com.jishnair.util.DeploymentUtil._
import spray.json.DefaultJsonProtocol._
import spray.json.enrichAny

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, DurationInt}
import scala.language.postfixOps

object RegistryService {

  implicit val timeout = Timeout(2 seconds)
  implicit val deploymentFormat = jsonFormat5(MicroserviceDto)

  def deploy(deploymentList: List[Deployment], registryActor: ActorRef): HttpResponse = {
    val dependecyTree = deploymentList.map(l => l.name -> l).toMap

    val sanityCheckResult = checkDeploymentSanity(deploymentList)
    if (sanityCheckResult._1) {
      val orderedDependencyList = getOrderedDependencyList(deploymentList)
      orderedDependencyList.foreach(name =>
        registryActor ! CreateMicroservice(1, name,
          dependecyTree.get(name).map(_.entryPoint).getOrElse(false),
          dependecyTree.get(name).map(_.replicas).getOrElse(1),
          dependecyTree.get(name).map(_.dependencies).getOrElse(List.empty)
        ))

      HttpResponse(StatusCodes.OK, entity = "Created microservices ")
    } else {
      HttpResponse(StatusCodes.BadRequest, entity = sanityCheckResult._2)
    }
  }

  def getListOfRunningMicroservices(registryActorRef: ActorRef): Future[HttpEntity.Strict] = {
    val microserviceListFuture = (registryRef ? RequestMicroserviceList).mapTo[List[MicroserviceDto]]

    microserviceListFuture.map { microserviceList =>
      HttpEntity(ContentTypes.`application/json`, microserviceList.toJson.prettyPrint)
    }
  }


  def healthCheck(registryActorRef: ActorRef): Unit = {
    registryActorRef ? HealthCheckAllMicroservices(1)
  }

}
