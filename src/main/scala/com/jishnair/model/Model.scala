package com.jishnair.model

object Model {

  final case class Deployment(name: String, entryPoint: Boolean = false, replicas: Int, dependencies: List[String])

  final case class MicroserviceModel(name: String, id: Int, isEntryPoint: Boolean, dependency: List[String], isHealthy: Boolean)

}
