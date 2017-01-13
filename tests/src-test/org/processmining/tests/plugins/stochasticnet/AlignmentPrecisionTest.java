package org.processmining.tests.plugins.stochasticnet;

import org.deckfour.xes.model.XLog;
import org.junit.Test;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.StochasticNetSemanticsImpl;
import org.processmining.plugins.astar.petrinet.manifestreplay.PNManifestFlattener;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

public class AlignmentPrecisionTest {

	@Test
	public void testPrecision() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("peter/M1", false);
		Petrinet net = (Petrinet) netAndMarking[0];
		Marking marking = (Marking) netAndMarking[1];

		int numberOfTracesToSimulate = 10;
		PNSimulator simulator = new PNSimulator();
		// do a simulation with global preselection:
		PNSimulatorConfig config = new PNSimulatorConfig(numberOfTracesToSimulate);
		XLog simulatedLog = simulator.simulate(null, net, new StochasticNetSemanticsImpl(), config, marking);

		PNRepResult result = ((Pair<PNRepResult,PNManifestFlattener>) StochasticNetUtils.replayLog(null, net, simulatedLog, false, true)).getFirst();
		System.out.println(result.getInfo().toString());
	}
}
