package com.jishnair

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.jishnair.actor.{Microservice, Registry, ServiceMonitor}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.DurationInt

class ServiceMonitorSpec extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll
    with Matchers {

  "service monitor should check health" in {
    val requester = TestProbe()

    val microservice1 = TestProbe()
    val microservice2 = TestProbe()

    val queryActor = system.actorOf(
      ServiceMonitor.props(
        actorToMicroserviceId = Map(microservice1.ref -> "service1", microservice2.ref -> "service2"),
        requestId = 1,
        requester = requester.ref,
        timeout = 5.seconds))

    microservice1.expectMsg(Microservice.RequestHealthCheck(1))
    microservice2.expectMsg(Microservice.RequestHealthCheck(1))

    queryActor.tell(Microservice.RespondHealthCheck(1), microservice1.ref)
    queryActor.tell(Microservice.RespondHealthCheck(1), microservice2.ref)

    requester.expectMsg(Registry.RespondAllHealthCheck(1,Map("service1"-> "OK", "service2"-> "OK")))

  }

}
