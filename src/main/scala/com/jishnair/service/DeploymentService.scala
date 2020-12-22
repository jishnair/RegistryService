package com.jishnair.service

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import com.jishnair.actor.Registry.CreateMicroservice
import com.jishnair.domain.Domain.Deployment
import com.jishnair.util.DeploymentUtil._

object DeploymentService {

  def deploy(deploymentList: List[Deployment], registryActor: ActorRef): HttpResponse = {

    val dependecyTree = deploymentList.map(l => l.name -> l).toMap

    if (checkDeploymentSanity(deploymentList)) {
      val orderedDependencyList = getOrderedDependencyList(deploymentList)
      orderedDependencyList.map(name => registryActor ! CreateMicroservice(name, dependecyTree.get(name).map(_.replicas)))

      HttpResponse(StatusCodes.OK, entity = "Created microservices ")
    } else {
      HttpResponse(StatusCodes.BadRequest, entity = "Error in deployment specification")
    }
  }


}
