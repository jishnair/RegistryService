package com.jishnair

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.jishnair.actor.{MicroserviceActor, RegistryActor}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class RegistryActorSpec extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll
    with Matchers {

  "reply to CreateMicroservice requests" in {
    val probe = TestProbe()
    val registryActor = system.actorOf(RegistryActor.props)

    registryActor.tell(RegistryActor.CreateMicroservice(1, "A", true, 1, List.empty), probe.ref)
    probe.expectMsg(RegistryActor.MicroserviceCreated(1))
    probe.lastSender should ===(registryActor)
  }

  "be able to list All router actors" in {
    val probe = TestProbe()
    val registryActor = system.actorOf(RegistryActor.props)

    registryActor.tell(RegistryActor.CreateMicroservice(1, "MicrosericeA", true, 1, List.empty), probe.ref)
    probe.expectMsg(RegistryActor.MicroserviceCreated(1))

    registryActor.tell(RegistryActor.CreateMicroservice(2, "MicrosericeB", true, 1, List.empty), probe.ref)
    probe.expectMsg(RegistryActor.MicroserviceCreated(2))

    registryActor.tell(RegistryActor.GetRouterActorList(requestId = 0), probe.ref)
    probe.expectMsg(Set("MicrosericeA", "MicrosericeB"))
  }

}
