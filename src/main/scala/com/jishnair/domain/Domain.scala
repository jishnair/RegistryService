package com.jishnair.domain

object Domain {

  final case class Deployment(name: String, entryPoint: Boolean = false, replicas: Int, dependencies: List[String])

}
