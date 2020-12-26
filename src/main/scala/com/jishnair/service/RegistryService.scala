package com.jishnair.service

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.pattern.ask
import akka.util.Timeout
import com.jishnair.actor.Registry.{CreateMicroservice, GetHealthCheckReport, HealthCheckAllMicroservices, RequestMicroserviceList}
import com.jishnair.model.Model.{Deployment, MicroserviceModel}
import com.jishnair.util.DeploymentUtil._
import spray.json.DefaultJsonProtocol._
import spray.json.enrichAny

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object RegistryService {

  implicit val timeout = Timeout(2 seconds)
  implicit val deploymentFormat = jsonFormat5(MicroserviceModel)

  def deploy(deploymentListInput: List[Deployment], registryActor: ActorRef): HttpResponse = {
    //remove empty strings from dependencies
    val deploymentList = deploymentListInput.map(l => Deployment(l.name, l.entryPoint, l.replicas, l.dependencies.filterNot(_.isBlank)))
    val dependecyTree = deploymentList.map(l => l.name -> l).toMap
    val dependencyKeySet = dependecyTree.keySet
    //Check sanity of input deployment json
    val sanityCheckResult = checkDeploymentSanity(deploymentList)
    if (sanityCheckResult._1) {
      val orderedDependencyList = getOrderedDependencyList(deploymentList).filterNot(_.isBlank)

      //Check if there is a dependency which is not specified in the dependency list
      //: TODO Check if any of the dependecies are already running
      val unspecifiedDependency = orderedDependencyList.filterNot(dependencyKeySet.contains(_))
      if (unspecifiedDependency.nonEmpty)
        return HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(ContentTypes.`application/json`,
          s"""{"message": "There are unspecified dependencies: ${unspecifiedDependency.mkString(",")}"}"""))

      //Invoke the dependencies in the correct order
      orderedDependencyList.foreach(name =>
        registryActor ! CreateMicroservice(1, name,
          dependecyTree.get(name).map(_.entryPoint).getOrElse(false),
          dependecyTree.get(name).map(_.replicas).getOrElse(1),
          dependecyTree.get(name).map(_.dependencies).getOrElse(List.empty)
        ))
      HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`application/json`, """{"message":"Microservices created"}"""))
    } else {
      HttpResponse(StatusCodes.BadRequest, entity = HttpEntity(ContentTypes.`application/json`, s"""{"message": "${sanityCheckResult._2}"}"""))
    }
  }

  def getListOfRunningMicroservices(registryActorRef: ActorRef): Future[HttpEntity.Strict] = {
    val microserviceListFuture = (registryActorRef ? RequestMicroserviceList(1)).mapTo[List[MicroserviceModel]]

    microserviceListFuture.map { microserviceList =>
      HttpEntity(ContentTypes.`application/json`, microserviceList.toJson.prettyPrint)
    }
  }

  def healthCheck(registryActorRef: ActorRef): Unit = {
    registryActorRef ! HealthCheckAllMicroservices(1)
  }

  def getHealthReport(registryActorRef: ActorRef): Future[HttpEntity.Strict] = {
    val healthReportFuture = (registryActorRef ? GetHealthCheckReport(1)).mapTo[Map[String, String]]
    healthReportFuture.map { report =>
      HttpEntity(ContentTypes.`application/json`, report.toJson.prettyPrint)
    }
  }
}
