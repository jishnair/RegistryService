package com.jishnair.util

import com.jishnair.domain.Domain.Deployment


object DeploymentUtil {

  type Node = Map[String, List[String]]

  /**
   * A Sanity checker for the input deplyment file.
   * Contains a Depth First Search algorithm for finding cycles in the dependency tree
   *
   * @param deploymentList
   * @return true => sanity check passed (No cyclic dependency, Only one entry point)
   *         false => no cyclic dependency
   */
  def checkDeploymentSanity(deploymentList: List[Deployment]): (Boolean, String) = {

    val maybeEntryPoint = deploymentList.filter(_.entryPoint == true)
    if (maybeEntryPoint.isEmpty) {
      return (false, "Error: No entry point defined.")
    }
    else if (maybeEntryPoint.length > 1) {
      return (false, "Error: More than one entry point defined.")
    }
    val entryPoint = maybeEntryPoint.head
    val startNode: (String, List[String]) = entryPoint.name -> entryPoint.dependencies
    val dependecyTree: Node = deploymentList.map(l => l.name -> l.dependencies).toMap

    //Depth first search
    def dfs(node: (String, List[String]), tree: Node, visited: List[String]): (Boolean, String) = {
      if (visited.contains(node._1)) {
        if (node == startNode) {
          println("found cycle ", visited ++ List(node._1))
          return (false, s"Error: Cyclcic dependency: ${(visited ++ List(node._1)).toString()}")
        }
      } else {
        val newVisited = visited ++ List(node._1)
        node._2.foreach(n => return dfs(n -> tree.getOrElse(n, List.empty), tree, newVisited))
      }
      return (true, "OK")
    }

    dfs(startNode, dependecyTree, List.empty)
  }

  /**
   * Create a service list in the order it need to be invoked.
   *
   * @param deploymentList
   * @return List of ordered dependencies
   */
  def getOrderedDependencyList(deploymentList: List[Deployment]): Set[String] = {

    val entryPoint = deploymentList.filter(_.entryPoint == true).head
    val startNode: (String, List[String]) = entryPoint.name -> entryPoint.dependencies
    val dependecyTree: Node = deploymentList.map(l => l.name -> l.dependencies).toMap

    //Depth first search
    def dfs(node: (String, List[String]), tree: Node, visited: Set[String]): Set[String] = {
      if (visited.contains(node._1) || node._2.isEmpty) {
        return Set(node._1) ++ visited

      } else {
        val newVisited = Set(node._1) ++ visited
        node._2.foreach { n =>
          return dfs(n -> tree.getOrElse(n, List.empty), tree, newVisited) ++ newVisited
        }
      }
      return visited
    }

    dfs(startNode, dependecyTree, Set.empty)
  }
}
