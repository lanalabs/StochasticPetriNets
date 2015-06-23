package org.processmining.tests.plugins.stochasticnet;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

public class LolaTest {

	@Test
	public void testLoLaExport() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("parallel2", true);
		System.out.println(StochasticNetUtils.toLoLaFromPetrinet((Petrinet)netAndMarking[0], (Marking)netAndMarking[1]));
	}
	
	@Test
	public void testLoLaRun() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("parallel2", true);
		System.out.println(StochasticNetUtils.getReachableStateSpaceSize((Petrinet)netAndMarking[0], (Marking)netAndMarking[1]));
	}
	
	@Test
	public void testLoLaRun50NoLoops() throws Exception {
		Object[] netAndMarking = TestUtils.loadModel("50_no_loops", true);
		System.out.println(StochasticNetUtils.getReachableStateSpaceSize((Petrinet)netAndMarking[0], (Marking)netAndMarking[1]));
	}
	
	@Test
	public void testIBM_Models() throws Exception {
		File ibmFolder = new File("tests/testfiles/ibm/converted_expanded2");
		
		File[] pnmlFiles = ibmFolder.listFiles(new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				return name.endsWith(".pnml");
			}
		});
		Map<String, Long> processesAndStates = new HashMap<>();
		for (File pnml : pnmlFiles){
			Object[] netAndMarking = TestUtils.loadModel("ibm/converted_expanded2/"+pnml.getName(), true);
			Long states = StochasticNetUtils.getReachableStateSpaceSize((Petrinet)netAndMarking[0], (Marking)netAndMarking[1]);
			System.out.println(pnml.getName()+" -> "+states +" states");
			processesAndStates.put(pnml.getName(), states);
		}
		
	}
}
