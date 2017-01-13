package org.processmining.tests.plugins.overfit;

import org.deckfour.xes.model.XLog;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.plugins.overfit.OverfitMiner;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

public class OverfitTest {

	
	public static void main(String[] args) throws Exception {
		//XLog log = TestUtils.loadLog("FiveActivities.xes");
		//XLog log = TestUtils.loadLog("ThreeActivities.xes");
		XLog log = TestUtils.loadLog("loan_process_filtered_only_A.xes");
		
		
		OverfitMiner miner = new OverfitMiner();
		Object[] netAndMarking = miner.mine(null, log);
	
		TestUtils.showModel((Petrinet) netAndMarking[0]);
	}

	@Test
	public void testSomething() throws Exception {
		System.out.println(System.getenv("LD_LIBRARY_PATH"));

		for (String key : System.getenv().keySet()){
			System.out.println(key+" -> "+System.getenv(key));
		}

	}
}
