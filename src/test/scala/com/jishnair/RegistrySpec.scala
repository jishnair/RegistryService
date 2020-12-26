package com.jishnair

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.jishnair.actor.{Microservice, Registry}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class RegistrySpec extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll
    with Matchers {

  "reply to CreateMicroservice requests" in {
    val probe = TestProbe()
    val registryActor = system.actorOf(Registry.props)

    registryActor.tell(Registry.CreateMicroservice(1, "A", true, 1, List.empty), probe.ref)
    probe.expectMsg(Registry.MicroserviceCreated(1))
    probe.lastSender should ===(registryActor)
  }

  "be able to list All router actors" in {
    val probe = TestProbe()
    val registryActor = system.actorOf(Registry.props)

    registryActor.tell(Registry.CreateMicroservice(1, "MicrosericeA", true, 1, List.empty), probe.ref)
    probe.expectMsg(Registry.MicroserviceCreated(1))

    registryActor.tell(Registry.CreateMicroservice(2, "MicrosericeB", true, 1, List.empty), probe.ref)
    probe.expectMsg(Registry.MicroserviceCreated(2))

    registryActor.tell(Registry.GetRouterActorList(requestId = 0), probe.ref)
    probe.expectMsg(Set("MicrosericeA", "MicrosericeB"))
  }

}
