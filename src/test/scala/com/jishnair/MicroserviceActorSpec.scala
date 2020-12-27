package com.jishnair

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.jishnair.actor.MicroserviceActor
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class MicroserviceActorSpec extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll
    with Matchers {

  "should reply for greeting message" in {
    val probe = TestProbe()
    val microserviceActor = system.actorOf(MicroserviceActor.props("microservice-A"))

    microserviceActor.tell( MicroserviceActor.RequestGreeting(requestId = 100), probe.ref)
    val response = probe.expectMsgType[MicroserviceActor.RespondGreeting]
    response.requestId should ===(100L)
    response.message should ===("Greetings from microservice-A")
  }


}
