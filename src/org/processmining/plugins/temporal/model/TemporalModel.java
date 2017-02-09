package org.processmining.plugins.temporal.model;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemporalModel {

    public static final String PARAMETER_LABEL = "Temporal Model";

    private TimeUnit timeUnit;

    private Map<XEventClass, TemporalNode> nodes;

    private TemporalNode startNode;
    private TemporalNode latestNode;

    private Map<TemporalNode, List<TemporalConnection>> connections;

    public static final XEventClass START_CLASS = new XEventClass("_Start", -1);

    private double smoothingFactor = 0.5;

    private double maxDistanceFromStartNode;

    public TemporalModel(TimeUnit unit) {
        this.timeUnit = unit;
        this.nodes = new HashMap<>();
        this.connections = new HashMap<>();
    }

    public void init(XLog log, XLogInfo info) {
        XEventClasses eventClasses = info.getEventClasses();
        // add start node:
        startNode = new TemporalNode("Start", START_CLASS);
        nodes.put(START_CLASS, startNode);

        for (XTrace trace : log) {
            XEvent startEventInTrace = trace.get(0);
            long startEventTime = XTimeExtension.instance().extractTimestamp(startEventInTrace).getTime();

            for (int i = 0; i < trace.size(); i++) {
                XEvent event = trace.get(i);
                TemporalNode node = getNode(event, eventClasses);

                long eventTime = XTimeExtension.instance().extractTimestamp(event).getTime();

                // add temporal relations from start node to node:
                addRelation(startNode, node, (eventTime - startEventTime) / timeUnit.getUnitFactorToMillis(), 1);

                // add temporal relations to all previous nodes:
                for (int j = 0; j < i; j++) {
                    XEvent eventBefore = trace.get(j);
                    TemporalNode nodeBefore = getNode(eventBefore, eventClasses);
                    long eventBeforeTime = XTimeExtension.instance().extractTimestamp(eventBefore).getTime();

                    // add temporal relations to all previous nodes:
                    addRelation(nodeBefore, node, (eventTime - eventBeforeTime) / timeUnit.getUnitFactorToMillis(), i - j);
                }
            }
        }
        maxDistanceFromStartNode = 0;
        for (TemporalConnection conn : connections.get(startNode)) {
            double dist = conn.getStats().getMean();
            if (dist > maxDistanceFromStartNode) {
                maxDistanceFromStartNode = dist;
                latestNode = conn.getAfter();
            }
        }
    }

    /**
     * Adds a sample to an existing connection (or creates a fresh connection) between two nodes
     *
     * @param fromNode
     * @param toNode
     * @param timePassed
     * @param traceDistance
     */
    private void addRelation(TemporalNode fromNode, TemporalNode toNode, double timePassed, int traceDistance) {
        TemporalConnection connection = getConnection(fromNode, toNode);
        connection.addSample(timePassed, traceDistance);
    }

    /**
     * Gets or creates a new connection between two nodes
     *
     * @param fromNode
     * @param toNode
     * @return
     */
    private TemporalConnection getConnection(TemporalNode fromNode, TemporalNode toNode) {
        if (!connections.containsKey(fromNode)) {
            connections.put(fromNode, new ArrayList<TemporalConnection>());
        }
        for (TemporalConnection connection : connections.get(fromNode)) {
            if (connection.getAfter().equals(toNode)) {
                return connection;
            }
        }
        TemporalConnection newConnection = new TemporalConnection(fromNode, toNode);
        connections.get(fromNode).add(newConnection);
        return newConnection;
    }

    /**
     * Obtains or creates a node for an event (one node exists per event class)
     *
     * @param event
     * @param eventClasses
     * @return
     */
    private TemporalNode getNode(XEvent event, XEventClasses eventClasses) {
        XEventClass eventClass = eventClasses.getClassOf(event);
        if (!nodes.containsKey(eventClass)) {
            TemporalNode node = new TemporalNode(XConceptExtension.instance().extractName(event), eventClass);
            nodes.put(eventClass, node);
        }
        return nodes.get(eventClass);
    }

    /**
     * Average distance of the event from the start node.
     *
     * @param node
     * @return value between 0 and 1
     */
    public double getRelativePositionInTrace(TemporalNode node) {
        if (node.equals(startNode)) {
            return 0;
        }
        for (TemporalConnection conn : connections.get(startNode)) {
            if (conn.getAfter().equals(node)) {
                return conn.getStats().getMean() / this.maxDistanceFromStartNode;
            }
        }
        return -1;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public Map<XEventClass, TemporalNode> getNodes() {
        return nodes;
    }

    public Map<TemporalNode, List<TemporalConnection>> getConnections() {
        return connections;
    }

    public double getSmoothingFactor() {
        return smoothingFactor;
    }

    public TemporalNode getStartNode() {
        return startNode;
    }

    public TemporalNode getLatestNode() {
        return latestNode;
    }

}
