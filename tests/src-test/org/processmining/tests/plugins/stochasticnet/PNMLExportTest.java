package org.processmining.tests.plugins.stochasticnet;

import java.io.OutputStreamWriter;

import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.impl.StochasticNetImpl;
import org.processmining.plugins.pnml.exporting.PnmlExportStochasticNet;

public class PNMLExportTest{

	@Test 
	public void testExportEmptyNet() throws Exception{
		StochasticNet net = new StochasticNetImpl("testNet");
		net.setExecutionPolicy(ExecutionPolicy.RACE_RESAMPLING);
		net.setTimeUnit(TimeUnit.MINUTES);
		
		Place startPlace = net.addPlace("start");
		Place betweenPlace = net.addPlace("between");
		Place endPlace = net.addPlace("end");
		TimedTransition t1 = net.addTimedTransition("startEvent", DistributionType.IMMEDIATE);
		TimedTransition transition = net.addTimedTransition("A", DistributionType.NORMAL, 10,2);
		net.addArc(startPlace, t1);
		net.addArc(t1,betweenPlace);
		net.addArc(betweenPlace,transition);
		net.addArc(transition, endPlace);
		
		
		OutputStreamWriter writer = new OutputStreamWriter(System.out);
		PnmlExportStochasticNet exporter = new PnmlExportStochasticNet();
		exporter.exportPetriNetToPNMLFile(null, net, writer);
		
	}
}