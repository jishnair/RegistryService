package com.jishnair

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.jishnair.actor.Microservice
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class MicroserviceSpec extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll
    with Matchers {

  "should reply for greeting message" in {
    val probe = TestProbe()
    val microserviceActor = system.actorOf(Microservice.props("microservice-A"))

    microserviceActor.tell( Microservice.RequestGreeting(requestId = 100), probe.ref)
    val response = probe.expectMsgType[Microservice.RespondGreeting]
    response.requestId should ===(100L)
    response.message should ===("Greetings from microservice-A")
  }

}
