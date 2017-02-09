package org.utils.datastructures.graph;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;
import org.apache.commons.collections15.Transformer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Simple implementation of the Bellman Ford distance computation.
 * It solves the single source all shortest paths problem and allows to handle negative edges.
 * Here, I did not take into account the cyclic case, but assume to be given a DAG.
 *
 * @param <V>
 * @param <E>
 * @author Andreas Rogge-Solti
 */
public class BellmanFordDistance<V, E> {

    private Graph<V, E> graph;
    private Double[] distance;
    private int[] predecessors;
    private Map<V, Integer> vertexIds;

    protected Transformer<E, ? extends Number> nev;

    public BellmanFordDistance(Graph<V, E> g, Transformer<E, ? extends Number> nev) {
        this.graph = g;
        this.nev = nev;
        distance = new Double[graph.getVertexCount()];
        this.predecessors = new int[graph.getVertexCount()];
        this.vertexIds = new HashMap<V, Integer>();
        Iterator<V> vertexIter = g.getVertices().iterator();
        for (int i = 0; i < g.getVertexCount(); i++) {
            this.vertexIds.put(vertexIter.next(), i);
        }
    }

    public Map<V, Number> getDistanceMap(V startPoint) {
        int startId = vertexIds.get(startPoint);
        for (int i = 0; i < distance.length; i++) {
            predecessors[i] = -1;
            distance[i] = Double.POSITIVE_INFINITY;
            if (i == startId) {
                distance[i] = 0.0;
            }
        }

        for (int i = 0; i < distance.length - 1; i++) {
            for (E edge : graph.getEdges()) {
                Number weight = nev.transform(edge);
                Pair<V> endPoints = graph.getEndpoints(edge);
                int u = vertexIds.get(endPoints.getFirst());
                int v = vertexIds.get(endPoints.getSecond());
                if (distance[u] + weight.doubleValue() < distance[v]) {
                    distance[v] = distance[u] + weight.doubleValue();
                    predecessors[v] = u;
                }
            }
        }
        Map<V, Number> distanceMap = new HashMap<V, Number>();
        for (V vertex : graph.getVertices()) {
            int v = vertexIds.get(vertex);
            distanceMap.put(vertex, distance[v]);
        }
        return distanceMap;
    }


}
