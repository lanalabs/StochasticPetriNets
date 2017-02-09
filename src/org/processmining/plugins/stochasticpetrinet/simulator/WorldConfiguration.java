package org.processmining.plugins.stochasticpetrinet.simulator;

import edu.uci.ics.jung.graph.Graph;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.stochasticpetrinet.external.Allocation;
import org.processmining.plugins.stochasticpetrinet.external.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorldConfiguration {
    private Graph<Location, Double> locationGraph;

    private Map<Transition, Set<Allocation>> transitionLocationMapping;

    private Map<String, Double> resourceSpeedFactors;

    private double[][] distances;
    private String[] locations;
    private Map<String, Integer> locationIds;
    private double[][] adjacencyMatrix;

    private boolean locationEnabledSimulation;

    private boolean resourceTimeAsynchronous = true;

    private double missingRatio = 0.0;

    private String filePrefix = "";

    public WorldConfiguration() {
        this(null, null, false);
    }

    public WorldConfiguration(Graph<Location, Double> locationGraph, Map<Transition, Set<Allocation>> transitionLocationMapping, boolean locationEnabled) {
        this.locationGraph = locationGraph;
        this.setTransitionLocationMapping(transitionLocationMapping);
        this.locationEnabledSimulation = locationEnabled;
        this.locationIds = new HashMap<String, Integer>();
        this.resourceSpeedFactors = new HashMap<String, Double>();
    }

    public void setLocationGraph(String[] locationNames, double[][] adjacencyMatrix) {
        this.locations = locationNames;
        this.adjacencyMatrix = adjacencyMatrix;
        this.distances = floydWarshall(adjacencyMatrix);
        this.locationIds = new HashMap<String, Integer>();
        for (int i = 0; i < locations.length; i++) {
            locationIds.put(locations[i], i);
        }
    }

    public Graph<Location, Double> getLocationGraph() {
        return locationGraph;
    }

    public void setLocationGraph(Graph<Location, Double> locationGraph) {
        this.locationGraph = locationGraph;
    }


    public boolean isLocationEnabledSimulation() {
        return locationEnabledSimulation;
    }

    public void setLocationEnabledSimulation(boolean locationEnabledSimulation) {
        this.locationEnabledSimulation = locationEnabledSimulation;
    }

    public Map<Transition, Set<Allocation>> getTransitionLocationMapping() {
        return transitionLocationMapping;
    }

    public void setTransitionLocationMapping(Map<Transition, Set<Allocation>> transitionLocationMapping) {
        this.transitionLocationMapping = transitionLocationMapping;
    }

    public double[][] getDistances() {
        return distances;
    }

    public String[] getLocations() {
        return locations;
    }

    public int getLocationId(String location) {
        return locationIds.get(location);
    }

    public double[][] getAdjacencyMatrix() {
        return adjacencyMatrix;
    }

    /**
     * Assumes that graph is an adjacency matrix of distances
     * (0 for connected with no cost or identity, val for directly connected with cost val, Double.POSITIVE_INFINITY for unconnected)
     */
    public static double[][] floydWarshall(double graph[][]) {
        int n = graph.length;
        double dist[][] = new double[n][n];
        int i, j, k;
 
        /* Initialize the solution matrix same as input graph matrix.
           Or we can say the initial values of shortest distances
           are based on shortest paths considering no intermediate
           vertex. */
        for (i = 0; i < n; i++)
            for (j = 0; j < n; j++)
                dist[i][j] = graph[i][j];
 
        /* Add all vertices one by one to the set of intermediate
           vertices.
          ---> Before start of a iteration, we have shortest
               distances between all pairs of vertices such that
               the shortest distances consider only the vertices in
               set {0, 1, 2, .. k-1} as intermediate vertices.
          ----> After the end of a iteration, vertex no. k is added
                to the set of intermediate vertices and the set
                becomes {0, 1, 2, .. k} */
        for (k = 0; k < n; k++) {
            // Pick all vertices as source one by one
            for (i = 0; i < n; i++) {
                // Pick all vertices as destination for the
                // above picked source
                for (j = 0; j < n; j++) {
                    // If vertex k is on the shortest path from
                    // i to j, then update the value of dist[i][j]
                    if (dist[i][k] + dist[k][j] < dist[i][j])
                        dist[i][j] = dist[i][k] + dist[k][j];
                }
            }
        }

        return dist;
    }

    public void setResourceSpeedFactors(Map<String, Double> resourceSpeedFactors) {
        this.resourceSpeedFactors = resourceSpeedFactors;
    }


    public Double getResourceSpeedFactor(String resource) {
        if (this.resourceSpeedFactors.containsKey(resource)) {
            return this.resourceSpeedFactors.get(resource);
        }
        return 1.0;
    }

    public boolean isResourceTimeAsynchronous() {
        return resourceTimeAsynchronous;
    }

    public void setResourceTimeAsynchronous(boolean asynchronous) {
        this.resourceTimeAsynchronous = asynchronous;
    }

    public void setMissingRatio(double missingRatio) {
        this.missingRatio = missingRatio;
    }

    public double getMissingRatio() {
        return this.missingRatio;
    }

    public void setFilePrefix(String prefix) {
        this.filePrefix = prefix;
    }

    public String getFilePrefix() {
        return this.filePrefix;
    }
}
