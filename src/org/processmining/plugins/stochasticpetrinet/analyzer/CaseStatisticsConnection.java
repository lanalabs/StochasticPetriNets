package org.processmining.plugins.stochasticpetrinet.analyzer;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.connections.impl.AbstractConnection;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;

public class CaseStatisticsConnection extends AbstractConnection {

    public final static String STOCHASTIC_NET = "StochasticNet";
    public final static String LOG = "Event Log";
    public final static String CASE_STATISTICS = "Case Statistics";

    public CaseStatisticsConnection(StochasticNet net, XLog log, CaseStatisticsList caseStatistics) {
        super("Connection to case statistics of model: " + net.getLabel() + " and log: " + log.toString());
        put(STOCHASTIC_NET, net);
        put(LOG, log);
        put(CASE_STATISTICS, caseStatistics);
    }
}
