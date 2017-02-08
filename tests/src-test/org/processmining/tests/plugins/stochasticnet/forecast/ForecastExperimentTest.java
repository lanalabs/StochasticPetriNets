package org.processmining.tests.plugins.stochasticnet.forecast;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;
import org.junit.Test;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.miner.StochasticMinerPlugin;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

/**
 * Created by andreas on 2/8/17.
 */
public class ForecastExperimentTest {

    public static final String TRAFFIC_LOG = "bpm2016/Road_Traffic_Fine_Management_Process.xes.gz";

    @Test
    public void testMethod() throws Exception {
        XLog log = TestUtils.loadLog(TRAFFIC_LOG);
        System.out.println("loaded log:    \t"+ XConceptExtension.instance().extractName(log));
        System.out.println("traces loaded: \t"+log.size());

        // split in training and test log:
        Pair<XLog, XLog> trainingLogTestLog = StochasticNetUtils.splitTracesBasedOnRatio(log, 0.5);

        // discover a "good" Petri net
        Object[] discoveredModel = StochasticMinerPlugin.discoverStochNetMode(StochasticNetUtils.getDummyUIContext(), trainingLogTestLog.getFirst());
        StochasticNet net = (StochasticNet) discoveredModel[0];
        StochasticNetUtils.exportAsDOTFile(net, "out", "out.dot");

        // use training log to enrich three models:
        // a stochastic petri net, a time series petri net, and a transition system with probailities (Markov Chain)




    }


}
