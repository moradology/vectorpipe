package vectorpipe.osm

import scala.collection.parallel.ParSeq

import cats.data.State
import cats.implicits._
import com.vividsolutions.jts.geom.LineString
import com.vividsolutions.jts.operation.linemerge.LineMerger
import geotrellis.util._
import geotrellis.vector._
import org.apache.spark.rdd._
import spire.std.any._
import vectorpipe.osm._
import vectorpipe.util._

package object internal {
  def wayIdToComponents(
    nodes: RDD[(Long, Node)],
    ways: RDD[(Long, Way)]
  ): RDD[(Long, (Iterable[Way], Iterable[Node]))] = {

    val nodesToWayIds: RDD[(Node, Iterable[Long])] =
      nodes
        .cogroup(
          ways
            .flatMap { case (wayId, way) =>
              way.nodes.map { nodeId => (nodeId, wayId) }
            }
        )
        .flatMap { case (_, (nodes, wayIds)) =>
          if(nodes.isEmpty) { None }
          else {
            /* ASSUMPTION: `nodes` contains distinct elements */
            val node = nodes.head
            Some((node, wayIds))
          }
        }


    val wayIdToNodes: RDD[(Long, Node)] =
      nodesToWayIds
        .flatMap { case (node, wayIds) =>
          wayIds.map(wayId => (wayId, node))
        }

    ways.cogroup(wayIdToNodes)
  }
}

