package com.jishnair

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.jishnair.actor.MicroserviceActor
import com.jishnair.model.Model.Deployment
import com.jishnair.util.DeploymentUtil
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class DeploymentSanitySpec extends TestKit(ActorSystem("MySpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll
  with Matchers {

  "should return false if no Entrypoint defined" in {
    val input = List(Deployment("A", false, 1, List("B")), Deployment("B", false, 1, List.empty))
    val checkSanity = DeploymentUtil.checkDeploymentSanity(input)
    checkSanity._1 should ===(false)
  }

  "should return false if more than one Entrypoint defined" in {
    val input = List(Deployment("A", true, 1, List("B")), Deployment("B", true, 1, List.empty))
    val checkSanity = DeploymentUtil.checkDeploymentSanity(input)
    checkSanity._1 should ===(false)
  }

  "should return false if there is cyclic dependency" in {
    val input = List(Deployment("A", true, 1, List("B")), Deployment("B", false, 1, List("A")))
    val checkSanity = DeploymentUtil.checkDeploymentSanity(input)
    checkSanity._1 should ===(false)
  }

  "should return ordered dependency List" in {
    val input = List(
      Deployment("A", true, 1, List("B", "C")),
      Deployment("B", false, 1, List("C")),
      Deployment("C", false, 1, List.empty)
    )
    val dependencyList = DeploymentUtil.getOrderedDependencyList(input)
    dependencyList should ===(Set("C", "B", "A"))
  }


}
