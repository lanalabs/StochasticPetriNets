package org.processmining.plugins.logmodeltrust.experiment;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.modelrepair.parameters.RepairConfiguration;
import org.processmining.modelrepair.plugins.Uma_RepairModel_Plugin;
import org.processmining.models.connections.petrinets.EvClassLogPetrinetConnection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMi;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.logmodeltrust.GeneralizedMinerPlugin;
import org.processmining.plugins.logmodeltrust.converter.RelaxedPT2PetrinetConverter;
import org.processmining.plugins.logmodeltrust.miner.GeneralizedMiner;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.miner.QualityCriterion;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;
import org.processmining.tests.plugins.stochasticnet.TestUtils;

import lpsolve.LpSolve;

/**
 * Experiment for the BPM2016 submission "In Log and Model We Trust?"
 * 
 * compares proposed generalized conformance checking approach with:
 * http://dx.doi.org/10.1016/j.is.2013.12.007 
 * (Dirk Fahland, Wil M.P. van der Aalst: Model repair - aligning process models to reality)
 * 
 * 
 * 
 *  
 * @author Andreas R-Solti
 *
 */
public class BPM2016 {
	private static final String SEP = ";";
	private PluginContext context;
	private XLog inputLog;

	private BPM2016(File logFile) throws Exception {
		init(logFile);
	}
	
	private void init(File logFile) throws Exception {
//		context = TestUtils.getDummyConsoleProgressContext(); // TODO: perhaps set up a mock plugin
		context = null;
		
		// the L in the paper
		inputLog = loadLog(logFile);
	}

	public static void main(String[] args) throws Exception {
		System.loadLibrary("lpsolve55j");
		System.out.println(LpSolve.lpSolveVersion());
		// the file to store the test log
		String testLogFile = "./tests/testfiles/bpm2016/BPI_Challenge_2013_incidents.xes";
		File logFile = new File(testLogFile);
		if (!logFile.exists()){
			throw new RuntimeException("Please download the BPI challenge 2013 log from \n"
					+ "http://data.3tu.nl/repository/uuid:500573e6-accc-4b0c-9576-aa5468b10cee\n"
					+ "and place it in the "+logFile.getParent()+" folder. Then you can re-run this experiment.");
		}
		BPM2016 experiment = new BPM2016(logFile);
		experiment.run();
	}
	
	
	private void run() throws NotYetImplementedException, InvalidProcessTreeException, IOException {
		
		MiningParameters parameters = new MiningParametersIMi();
		parameters.setNoiseThreshold(0.5f);
		
		// the M in the paper
		ProcessTree inputTree = IMProcessTree.mineProcessTree(inputLog, parameters);
		
		// Petri net version of M
		PetrinetWithMarkings inputPetrinet = RelaxedPT2PetrinetConverter.convert(inputTree);
		
		
		// repair with different trust levels:
		double[] trustLevels = new double[]{0.,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.};
//		double trustInModel = 0.5;
		double trustInLog = 1;
		
		GeneralizedMiner miner = GeneralizedMinerPlugin.mineGeneralLogModel(context, inputLog, inputTree);
		
		// repair M with "model repair approach"
		RepairConfiguration config = new RepairConfiguration(); // use default configuration
		
		Uma_RepairModel_Plugin modelRepairer = new Uma_RepairModel_Plugin();
		XLogInfo info = XLogInfoFactory.createLogInfo(inputLog, new XEventNameClassifier());
		TransEvClassMapping transEvClassMapping = StochasticNetUtils.getEvClassMapping(inputPetrinet.petrinet, inputLog);
		EvClassLogPetrinetConnection connection = new EvClassLogPetrinetConnection("connection", inputPetrinet.petrinet, inputLog,info.getEventClassifiers().iterator().next() , transEvClassMapping);
		context.getConnectionManager().addConnection(connection);
		Object[] netAndMarking = modelRepairer.repairModel_getT2Econnection(context, inputLog, inputPetrinet.petrinet, inputPetrinet.initialMarking, inputPetrinet.finalMarking, config);
		PetrinetWithMarkings repairedModel = new PetrinetWithMarkings();
		repairedModel.petrinet = (Petrinet) netAndMarking[0];
		repairedModel.initialMarking = (Marking) netAndMarking[1];
		repairedModel.finalMarking = StochasticNetUtils.getFinalMarking(context, repairedModel.petrinet);
		
		// evaluate quality criteria on models:
		StringBuffer buffer = new StringBuffer();
		buffer.append(getHeader()).append("\n");
		
		buffer.append(evaluateQualityCriteria(inputLog, "repaired Model",repairedModel, Double.NaN, Double.NaN));
		File resultFile = new File("./BPM16_results.csv");
		if (!resultFile.exists()) resultFile.createNewFile();
		FileUtils.write(resultFile, buffer.toString());
		// our approach:
		for (double trustInModel : trustLevels){
			Pair<XLog, ProcessTree> pair = miner.getFittingPair(trustInLog, trustInModel);
			// ProcessTree version of M*
			ProcessTree bestTree = pair.getSecond();
			// Petri net version of M*
			PetrinetWithMarkings bestPetrinet = RelaxedPT2PetrinetConverter.convert(bestTree);
			buffer.append(evaluateQualityCriteria(inputLog, "M*_"+trustInModel, bestPetrinet, trustInModel, trustInLog));
			FileUtils.write(resultFile, buffer.toString());
		}
		
		// Joos' approach?
		System.out.println(buffer.toString());
		FileUtils.write(resultFile, buffer.toString());
		
	}
 	public static String evaluateQualityCriteria(XLog inputLog, String modelName, PetrinetWithMarkings petrinet,
			 Double trustInModel, Double trustInLog) {
		// replay both models on the log.
		StringBuffer result = new StringBuffer();
		
		result.append(XConceptExtension.instance().extractName(inputLog)).append(SEP); // logname;
		result.append(modelName).append(SEP); // logname;
		result.append(trustInModel).append(SEP).append(trustInLog).append(SEP); // trustM;trustL;
		
		Map<QualityCriterion, Double> qualityRepaired = StochasticNetUtils.getDistance(petrinet, inputLog);
		addToResult(result, qualityRepaired);
		result.append("\n"); 
		return result.toString();
	}

	private static void addToResult(StringBuffer result, Map<QualityCriterion, Double> qualityRepaired) {
		result.append(qualityRepaired.get(QualityCriterion.FITNESS)).append(SEP);
		result.append(qualityRepaired.get(QualityCriterion.PRECISION1)).append(SEP);
		result.append(qualityRepaired.get(QualityCriterion.GENERALIZATION1)).append(SEP);
		result.append(qualityRepaired.get(QualityCriterion.PRECISION)).append(SEP);
		result.append(qualityRepaired.get(QualityCriterion.GENERALIZATION)).append(SEP);
		result.append(qualityRepaired.get(QualityCriterion.SIMPLICITY));
	}

	public static String getHeader() {
		return "logname"+SEP+"approach"+SEP+"trustM"+SEP+"trustL"+SEP+
				"fitness"+SEP+"precision"+SEP+"generalization"+SEP+"precision2"+SEP+"generalization2"+SEP+"simplicity";
	}

	/**
	 * Opens a Log from a given file.
	 * 
	 * @param file {@link File} containing the log.
	 * @return the loaded {@link XLog} 
	 * @throws Exception
	 */
	private XLog loadLog(File file) throws Exception {
		XUniversalParser parser = new XUniversalParser();
		Collection<XLog> logs = parser.parse(file);
		if (logs.size() > 0){
			return logs.iterator().next();
		}
		return null;
	}
}
