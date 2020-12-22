package com.jishnair.domain

object Domain {

  final case class Deployment(name: String, entryPoint: Boolean = false, replicas: Int, dependencies: List[String])

  final case class MicroserviceDto(name: String, id: Int, isEntryPoint: Boolean, dependency: List[String], isHealthy: Boolean)

}
