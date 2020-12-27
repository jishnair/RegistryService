package com.jishnair

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.jishnair.actor.{MicroserviceActor, RegistryActor, ServiceMonitorActor}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.DurationInt

class ServiceMonitorActorSpec extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll
    with Matchers {

  "service monitor should check health of all microservices" in {
    val requester = TestProbe()

    val microservice1 = TestProbe()
    val microservice2 = TestProbe()

    val queryActor = system.actorOf(
      ServiceMonitorActor.props(
        actorToMicroserviceId = Map(microservice1.ref -> "service1", microservice2.ref -> "service2"),
        requestId = 1,
        requester = requester.ref,
        timeout = 5.seconds))

    microservice1.expectMsg(MicroserviceActor.RequestHealthCheck(1))
    microservice2.expectMsg(MicroserviceActor.RequestHealthCheck(1))

    queryActor.tell(MicroserviceActor.RespondHealthCheck(1), microservice1.ref)
    queryActor.tell(MicroserviceActor.RespondHealthCheck(1), microservice2.ref)

    requester.expectMsg(RegistryActor.HealthCheckResponse(1,Map("system" -> "OK")))
  }
}
