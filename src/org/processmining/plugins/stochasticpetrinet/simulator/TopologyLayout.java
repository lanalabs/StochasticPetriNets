package org.processmining.plugins.stochasticpetrinet.simulator;

import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.graph.Graph;

public class TopologyLayout<V, E> extends DAGLayout<V, E> {

    public TopologyLayout(Graph<V, E> g) {
        super(g);
    }
}
