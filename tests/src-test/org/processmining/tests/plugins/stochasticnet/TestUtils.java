package org.processmining.tests.plugins.stochasticnet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.importing.StochasticNetDeserializer;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.experiment.PerformanceEnricherExperimentPlugin;
import org.processmining.plugins.stochasticpetrinet.enricher.experiment.PerformanceEnricherExperimentPlugin.ExperimentType;
import org.processmining.plugins.stochasticpetrinet.enricher.experiment.PerformanceEnricherExperimentResult;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * Contains shared functionality of the test cases.
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class TestUtils {

	
	public static void runExperimentAndSaveOutput(ExperimentType experimentType, String fileName) throws Exception,
			IOException {
		Object[] netAndMarking = TestUtils.loadModel(fileName);
		StochasticNet net = (StochasticNet) netAndMarking[0];
		Marking initialMarking = StochasticNetUtils.getInitialMarking(null, net);

		PerformanceEnricherExperimentPlugin enrichmentPlugin = new PerformanceEnricherExperimentPlugin();

		PerformanceEnricherExperimentResult result = enrichmentPlugin.performExperiment(null, net, initialMarking,
				experimentType);
		TestUtils.saveCSV(result.getResultsCSV(experimentType), fileName + "_" + experimentType + ".csv");
	}
	/**
	 * 
	 * @param name String filename without the suffix ".pnml" tries to load a {@link StochasticNet} from
	 * the tests/testfiles folder of the installation.
	 *  
	 * @return size 2 Object[] containing the {@link StochasticNet} and the initial {@link Marking} of the net.  
	 * @throws Exception
	 */
	public static Object[] loadModel(String name) throws Exception {
		Serializer serializer = new Persister();
		File source = new File("tests/testfiles/"+name+".pnml");

		PNMLRoot pnml = serializer.read(PNMLRoot.class, source);

		StochasticNetDeserializer converter = new StochasticNetDeserializer();
		Object[] netAndMarking = converter.convertToNet(null, pnml, name, false);
		return netAndMarking;
	}
	
	public static void saveCSV(String csvContent, String fileName) throws IOException{
		File resultsFolder = new File("./experimentResults");
		if (!resultsFolder.exists()){
			resultsFolder.mkdir();
		}
		File resultsFile = new File(resultsFolder, fileName);
		if (!resultsFile.exists()){
			resultsFile.createNewFile();
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFile));
		writer.write(csvContent);
		writer.flush();
		writer.close();
	}
}
