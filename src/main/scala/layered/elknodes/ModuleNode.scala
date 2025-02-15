// SPDX-License-Identifier: Apache-2.0

package layered.elknodes

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

case class ModuleNode(
                       name:           String,
                       parentOpt:      Option[ElkNode],
                       var urlString: Option[String] = None,
                       subModuleDepth: Int = 0)
    extends ElkNode {

  val indentation: String = "    " * subModuleDepth
  var renderWithRank: Boolean = false

  val namedNodes:     mutable.HashMap[String, ElkNode] = new mutable.HashMap()
  val subModuleNames: mutable.HashSet[String] = new mutable.HashSet[String]()

  val connections: mutable.HashMap[String, String] = new mutable.HashMap()
  private val analogConnections = new mutable.HashMap[String, ArrayBuffer[String]]() {
    override def default(key: String): ArrayBuffer[String] = {
      this(key) = new ArrayBuffer[String]()
      this(key)
    }
  }
  val localConnections: mutable.HashMap[String, String] = new mutable.HashMap()

  /**
    * Renders this node
    * @return
    */
  def render: String = {
    def expandBiConnects(target: String, sources: ArrayBuffer[String]): String = {
      sources.map { vv => s"""${indentation}edge e${Edge.hash} : $vv -> $target""" }.mkString("\n    ")
    }
    val s = s"""
               |${indentation}node $name$graphUrl {
               |    ${indentation}portConstraints: FIXED_SIDE
               |    ${indentation}nodeLabels.placement: "H_CENTER V_TOP OUTSIDE"
               |    ${indentation}nodeSize.constraints: "PORTS PORT_LABELS NODE_LABELS"
               |    ${indentation}label "${name.stripPrefix("submodule_")}"
               |    $indentation${children.map(_.render).mkString("")}
               |    ${connections.map { case (k, v) => s"${indentation}edge e${Edge.hash} : $v -> $k" }.mkString("\n    ")}
               |    ${analogConnections.map { case (k, v) => expandBiConnects(k, v) }.mkString("\n    ")}
               |$indentation}
               |""".stripMargin
    s
  }

  override def absoluteName: String = {
    parentOpt match {
      case Some(parent) => s"${parent.absoluteName}.$name$graphUrl"
      case _            => s"$name"
    }
  }

  def graphUrl: String = {
    urlString match {
      case Some(url) => s"_$url"
      case None  => ""
    }
  }

  def connect(destination: ElkNode, source: ElkNode): Unit = {
    connect(destination.absoluteName, source.absoluteName)
  }

  def connect(destinationName: String, source: ElkNode): Unit = {
    connect(destinationName, source.absoluteName)
  }

  def connect(destination: ElkNode, sourceName: String): Unit = {
    connect(destination.absoluteName, sourceName)
  }

  def connect(destination: String, source: String, edgeLabel: String = ""): Unit = {
    connections(destination) = source
  }

  def analogConnect(destination: String, source: String, edgeLabel: String = ""): Unit = {
    analogConnections(destination) += source
  }

  def +=(childNode: ElkNode): Unit = {
    namedNodes(childNode.absoluteName) = childNode
    children += childNode
  }
}

object Edge {
  var pseudoHash: Long = 10

  def hash: Long = {
    pseudoHash += 1
    pseudoHash
  }
}