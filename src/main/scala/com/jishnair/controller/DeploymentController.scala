package com.jishnair.controller

import akka.actor.ActorSystem
import akka.actor.Status.Success
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.jishnair.actor.Registry
import com.jishnair.actor.Registry.RequestMicroserviceList
import com.jishnair.domain.Domain.Deployment
import spray.json.DefaultJsonProtocol._


import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.jishnair.service.DeploymentService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.Failure

object DeploymentController extends App {

  implicit val system = ActorSystem("registry-system")
  implicit val materializer = ActorMaterializer()
  implicit val timeout = Timeout(2 seconds)
  implicit val deploymentFormat = jsonFormat4(Deployment)


  val registryRef = system.actorOf(Registry.props, "microservice-registry")
  val rnd = new scala.util.Random

  val route =
    pathPrefix("api") {

      path("deployment") {
        post {
          entity(as[List[Deployment]]) { deployment =>
            val response = DeploymentService.deploy(deployment, registryRef)
            complete(response)
          }
        }

      } ~ path("microservices") {
        get {
          val response=registryRef ? RequestMicroserviceList( rnd.nextInt())
          response.map{
            case Success(msg) => println(msg)
            case Failure(msg) => println(msg)

          }
          println(response)

          complete("")
        }
      }
    }


  val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
