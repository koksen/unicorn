/*******************************************************************************
 * (C) Copyright 2015 ADP, LLC.
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.adp.unicorn.graph

/**
 * Graph operations on graphs of vertex type V and edge type E.
 * 
 * @author Haifeng Li
 */
class GraphOps[V, E] {
  
    /**
     * Depth-first search of graph.
     * @param vertex the current vertex to visit
     * @param edge optional incoming edge
     * @param visitor a visitor object to process the current vertex and also to return
     * an iterator of edges of interest associated with the vertex. Note that the visitor
     * may not return all edges in the graph. For example, we may be only interested in
     * "work with" relationships and would like to filter out "reports to" relationships.
     * @param mark a set of visited vertices.
     * @param hops the number of hops to reach this vertex from the starting vertex.
     */
    private def dfs(vertex: V, edge: Edge[V, E], visitor: Visitor[V, E], mark: collection.mutable.Set[V], hops: Int) {
      visitor.visit(vertex, edge, hops)
      mark add vertex
      visitor.edges(vertex, hops).foreach { e =>
        if (!mark.contains(e.target))
          dfs(e.target, e, visitor, mark, hops + 1)
      }
    }

    /**
     * Depth-first search of graph.
     * @param vertex the starting vertex
     * @param visitor a visitor object to process the current vertex and also to return
     * an iterator of edges of interest associated with the vertex. Note that the visitor
     * may not return all edges in the graph. For example, we may be only interested in
     * "work with" relationships and would like to filter out "reports to" relationships.
     */
    def dfs(vertex: V, visitor: Visitor[V, E]) {
      val mark = collection.mutable.Set[V]()
      dfs(vertex, null, visitor, mark, 0)
    }

    /**
     * Breadth-first search of graph.
     * @param vertex the current vertex to visit
     * @param visitor a visitor object to process the current vertex and also to return
     * an iterator of edges of interest associated with the vertex. Note that the visitor
     * may not return all edges in the graph. For example, we may be only interested in
     * "work with" relationships and would like to filter out "reports to" relationships.
     */
    def bfs(vertex: V, visitor: Visitor[V, E]) {
      val mark = collection.mutable.Set[V]()
      val queue = collection.mutable.Queue[(Edge[V, E], Int)]()
      
      visitor.visit(vertex, null, 0)
      mark add vertex
      visitor.edges(vertex, 0).foreach { edge =>
        if (!mark.contains(edge.target))
          queue += ((edge, 1))
      }
      
      while (!queue.isEmpty) {
        val (edge, hops) = queue.dequeue
        visitor.visit(edge.target, edge, hops)
        mark add vertex
        visitor.edges(vertex, hops).foreach { edge =>
          if (!mark.contains(edge.target))
            queue += ((edge, hops + 1))
        }
      }
    }
    
    /**
     * Helper ordering object in A* for priority queue
     */
    private object NodeOrdering extends scala.math.Ordering[(V, Double)] {
      def compare(x: (V, Double), y: (V, Double)): Int = {
        x._2.compare(y._2)
      }
    }

    /**
     * Default edge weight.
     */
    def weight(source: V, target: V, edge: E) = 1.0
    
    /**
     * Dijkstra shortest path search algorithm.
     * 
     * @param start  the start vertex
     * @param goal   the goal vertex
     * @param neighbors a function to returns vertex's neighbors of interest
     * @param g      the past path-cost function, which is the known distance between two vertices.
     * @return       the path from source to goal
     */
    def dijkstra(start: V, goal: V, neighbors: V => Iterator[(V, E)], g: (V, V, E) => Double = weight): List[(V, Option[E])] = {

      val queue = new scala.collection.mutable.PriorityQueue[(V, Double)]()(NodeOrdering)
      queue.enqueue((start, 0.0))
      
      val dist = scala.collection.mutable.Map[V, Double]().withDefaultValue(Double.PositiveInfinity)
      dist(start) = 0.0

      // The map of navigated vertices
      val cameFrom = scala.collection.mutable.Map[V, (V, E)]()

      while (!queue.isEmpty) {
        val (current, distance) = queue.dequeue
        if (current == goal) return reconstructPath(cameFrom, goal).reverse

        neighbors(current).foreach { case (neighbor, edge) =>
          val alt = distance+ g(current, neighbor, edge)
          if (alt < dist(neighbor)) {
            dist(neighbor) = alt
            cameFrom(neighbor) = (current, edge)
            queue.enqueue((neighbor, alt))
          }
        }
      }

      // Fail. No path exists between the start vertex and the goal.
      return List[(V, Option[E])]()
    }
    
    /**
     * A* search algorithm for path finding and graph traversal.
     * It is an extension of Dijkstra algorithm and achieves better performance by using heuristics.
     * 
     * @param start  the start vertex
     * @param goal   the goal vertex
     * @param neighbors a function to returns vertex's neighbors of interest
     * @param h      the future path-cost function, which is an admissible
     *               "heuristic estimate" of the distance from the current vertex to the goal.
     *               Note that the heuristic function must be monotonic.
     * @param g      the past path-cost function, which is the known distance between two vertices.
     * @return       the path from source to goal
     */
    def astar(start: V, goal: V, neighbors: V => Iterator[(V, E)], h: (V, V) => Double, g: (V, V, E) => Double = weight): List[(V, Option[E])] = {
      // The queue to find vertex with lowest f score
      // Note that Scala priority queue maintains largest value on the top.
      // So we will use negative f score in the queue.
      val openQueue = new scala.collection.mutable.PriorityQueue[(V, Double)]()(NodeOrdering)
      openQueue.enqueue((start, -h(start, goal)))
      
      // The set of tentative vertices to be evaluated.
      val openSet = scala.collection.mutable.Set[V](start)
      
      // The set of vertices already evaluated.
      val closedSet = scala.collection.mutable.Set[V]()

      // The map of navigated vertices
      val cameFrom = scala.collection.mutable.Map[V, (V, E)]()

      // Cost from start along best known path.
      val gScore = scala.collection.mutable.Map[V, Double]()
      gScore(start) = 0.0
        
      // Estimated total cost from start to goal through y.
      val fScore = scala.collection.mutable.Map[V, Double]()
      fScore(start) = h(start, goal)

      while (!openQueue.isEmpty) {
        val (current, _) = openQueue.dequeue
        
        if (current == goal) return reconstructPath(cameFrom, goal).reverse

        openSet.remove(current)
        closedSet.add(current)

        neighbors(current).foreach {
          case (neighbor, _) if (closedSet.contains(neighbor)) => ()
          case (neighbor, edge) =>
            val alt = gScore(current) + g(current, neighbor, edge)
 
            if (!openSet.contains(neighbor) || alt < gScore(neighbor)) { 
              cameFrom(neighbor) = (current, edge)
              gScore(neighbor) = alt
              val f = -gScore(neighbor) - h(neighbor, goal)
              fScore(neighbor) = f
              if (!openSet.contains(neighbor)) {
                openSet.add(neighbor)
                openQueue.enqueue((neighbor, f))
              }
            }
        }
      }

      // Fail. No path exists between the start vertex and the goal.
      return List[(V, Option[E])]()
    }
    
    /**
     * Reconstructs the A* search path.
     */
    private def reconstructPath(cameFrom: scala.collection.mutable.Map[V, (V, E)], current: V): List[(V, Option[E])] = {
      if (cameFrom.contains(current)) {
        val (from, edge) = cameFrom(current)
        val path = reconstructPath(cameFrom, from)
        return (current, Some(edge)) :: path
      } else {
        return List[(V, Option[E])]((current, None))
      }
    }
 }