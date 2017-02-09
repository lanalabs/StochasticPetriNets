/** HOWTO enable experiment: 
 * 1) Register an academic license and download Gurobi solver from http://www.gurobi.com/. 
 * 2) add gurobi.jar to classpath
 * 3) add native libraries to java library path, e.g. add   -Djava.library.path=./lib/gurobi   to the VM arguments.
 * 4) uncomment below code
 * 5) run testAllocationExperiment() as a unit test.
 *    Then ...wait a bit....
 * 6) Check the results in the base-directory starting with ROAD...csv  
 */


package org.processmining.tests.plugins.stochasticnet.external;

import gurobi.GRB;
import gurobi.GRBEnv;
import gurobi.GRBException;
import gurobi.GRBLinExpr;
import gurobi.GRBModel;
import gurobi.GRBVar;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.junit.Assert;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.petrinets.analysis.gedsim.utils.StringEditDistance;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.converter.ConvertDistributionsPlugin;
import org.processmining.plugins.stochasticpetrinet.external.Allocatable;
import org.processmining.plugins.stochasticpetrinet.external.Allocation;
import org.processmining.plugins.stochasticpetrinet.external.Allocation.AllocType;
import org.processmining.plugins.stochasticpetrinet.external.AllocationBasedNetGenerator;
import org.processmining.plugins.stochasticpetrinet.external.AllocationConfig;
import org.processmining.plugins.stochasticpetrinet.external.AllocationDistribution;
import org.processmining.plugins.stochasticpetrinet.external.Building;
import org.processmining.plugins.stochasticpetrinet.external.Compartment;
import org.processmining.plugins.stochasticpetrinet.external.Person;
import org.processmining.plugins.stochasticpetrinet.external.PetrinetModelAllocations;
import org.processmining.plugins.stochasticpetrinet.external.PoissonAllocation;
import org.processmining.plugins.stochasticpetrinet.external.Role;
import org.processmining.plugins.stochasticpetrinet.external.Room;
import org.processmining.plugins.stochasticpetrinet.external.interaction.Interaction;
import org.processmining.plugins.stochasticpetrinet.external.sensor.LogToSensorIntervalConverter;
import org.processmining.plugins.stochasticpetrinet.external.sensor.SensorInterval;
import org.processmining.plugins.stochasticpetrinet.external.sensor.SortedSensorIntervals;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.processmining.plugins.stochasticpetrinet.simulator.PNUnfoldedSimulator;
import org.processmining.tests.plugins.stochasticnet.TestUtils;
import org.utils.datastructures.measure.JaccardSimilarity;

import com.google.common.base.Joiner;
import com.zaxxer.sparsebits.SparseBitSet;

public class AllocationExperimentTest {

	private static final String RESOURCE_SIMILARITY = "ResourceSimilarity";
	private static final String LOCATION_ACCURACY = "LocationAccuracy";
	private static final String ENTROPY = "Entropy";
	
	private static final String HITRATE = "hitrate";

	private static final String DURATION_SIM = "durationSim";

	static Logger logger = Logger.getLogger(AllocationExperimentTest.class);
	
	public static final String SEP = ";";
	
	public static final int NUM_RUNS = 30;
//	public static final int[] NUM_CASES = new int[]{10,50,100,150,200,250,300,350,400};
	public static final int[] ROOM_SIZES = new int[]{1,2,3,4,5,6,7};
	//public static final int[] NUM_CASES = new int[]{20};
	public static final double[] NOISE_LEVELS = new double[]{0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9};
	//public static final double[] NOISE_LEVELS = new double[]{0.9};
	
	public static final double[] DROPOFF_LEVELS = new double[]{0.0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9, 1.0};
	public static final int REPETITIONS = 30;

	public enum ExperimentType{
		COMPLEXITY, AMBIGUITY, NOISE;
	}
	
	@Test
	public void testAllocationExperiment() throws Exception {
		logger.setLevel(Level.INFO);
		
		Object[] modelAndMarking = TestUtils.loadModel("CAISE_16_main_process", true);
		Petrinet net = (Petrinet) modelAndMarking[0];
		for (int i = 0; i < REPETITIONS; i++){
			String suffix=""+i;
			
			Object[] stochNetAndMarking = ConvertDistributionsPlugin.enrichStochasticInformation(null, net, DistributionType.GAMMA);
			StochasticNet stochNet = (StochasticNet) stochNetAndMarking[0];
			stochNet.setTimeUnit(TimeUnit.MINUTES);
			stochNet.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
			
			
			int maxCapacity = 1; // to be tweaked by experiment
			
			// dropOffRate = probability for adding more resources to an allocation set
			double dropOffRate = 0.5; // to be tweaked by experiment
			double arrivalRate = 1. / 5; // 1 per 5 minutes (to be tweaked by experiment)
			double meanTimeBetweenArrivals = 1 / arrivalRate;
			
			long startTime = new GregorianCalendar(2016,1,24,8,0).getTimeInMillis();
			
			int maxResources = 4;
			double noise = 0;
			
			// complexity experiment
			runComplexityExperiment(stochNet, "ROADComplexityExperimentResults."+suffix+".csv", maxCapacity, dropOffRate, noise, startTime);
			
			// noise Experiment
			runNoiseExperiment(stochNet, "ROADNoiseExperimentResults."+suffix+".csv", dropOffRate, meanTimeBetweenArrivals, startTime);
			
			// ambiguity Experiment
			runAmbiguityExperiment(stochNet, "ROADAmbiguityExperimentResults."+suffix+".csv", meanTimeBetweenArrivals, maxResources, startTime);
		}
	}

	protected void runAmbiguityExperiment(StochasticNet stochNet, String fileName,
			double meanTimeBetweenArrivals, int maxResources, long startTime) {
		List<String> resultFileLines;
		String finalString;
		int rooms;
		int maxCapacity;
		int nResources;
		double dropOffRate;
		double noise;
		noise = 0.;
		resultFileLines = new ArrayList<String>();
		resultFileLines.add(getHeader());
		for (int i = 1; i < NUM_RUNS; i++){
			maxCapacity = 1;
			rooms = 1;
			nResources = maxResources;
			int numCases = 100;
			for (int n = 0; n < DROPOFF_LEVELS.length; n++){
				dropOffRate = DROPOFF_LEVELS[n];
				resultFileLines.addAll(runExperiment(i, stochNet, rooms, maxCapacity, nResources, dropOffRate, meanTimeBetweenArrivals,
						numCases, noise, ExperimentType.AMBIGUITY, startTime));
			}
		}
		finalString = Joiner.on("\n").join(resultFileLines);
		StochasticNetUtils.writeStringToFile(finalString, fileName);
	}

	protected void runNoiseExperiment(StochasticNet stochNet, String fileName, double dropOffRate,
			double meanTimeBetweenArrivals, long startTime) {
		List<String> resultFileLines;
		String finalString;
		int rooms;
		int maxCapacity;
		int nResources;
		resultFileLines = new ArrayList<String>();
		resultFileLines.add(getHeader());
		for (int i = 0; i < NUM_RUNS; i++){
			maxCapacity = 1;
			rooms = 1;
			nResources = 1;
			int numCases = 100;
			for (int n = 0; n < NOISE_LEVELS.length; n++){
				double noiseLevel = NOISE_LEVELS[n];
				resultFileLines.addAll(runExperiment(i, stochNet, rooms, maxCapacity, nResources, dropOffRate, meanTimeBetweenArrivals,
						numCases, noiseLevel, ExperimentType.NOISE, startTime));
			}
		}
		finalString = Joiner.on("\n").join(resultFileLines);
		StochasticNetUtils.writeStringToFile(finalString, fileName);
	}

	protected void runComplexityExperiment(StochasticNet stochNet, String fileName,
			int maxCapacity, double dropOffRate, double noise, long startTime) {
		List<String> resultFileLines = new ArrayList<String>();
		resultFileLines.add(getHeader());
		double meanTimeBetweenArrivals = 1;
		for (int i = 0; i < NUM_RUNS; i++){
			int numCases = 100;
			for (int n = 0; n < ROOM_SIZES.length; n++){
				int maxCapacityOfRooms = ROOM_SIZES[n];
				int rooms = 1;
				int nResources = maxCapacityOfRooms;
				
				resultFileLines.addAll(runExperiment(i, stochNet, rooms, maxCapacityOfRooms, nResources, dropOffRate, meanTimeBetweenArrivals,
						numCases, noise, ExperimentType.COMPLEXITY, startTime));
			}
		}
		String finalString = Joiner.on("\n").join(resultFileLines);
		StochasticNetUtils.writeStringToFile(finalString, fileName);
	}

	protected List<String> runExperiment(int runId, StochasticNet stochNet, int rooms,
			int maxCapacity, int nResources, double dropOffRate, double meanTimeBetweenArrivals, int numCases,
			double noise, ExperimentType experimentType, long startTime) {
		List<String> resultLines = new ArrayList<String>();
		Set<Allocatable> allResources = new HashSet<Allocatable>();

		// build Structure:
		Building hospital = new Building("hospital");
		Set<Allocatable> examRooms = addCompartment(rooms, maxCapacity, hospital, "exam_room", allResources);
		Set<Allocatable> nurseRooms = addCompartment(rooms, maxCapacity, hospital, "nurse_room", allResources);
		Set<Allocatable> infusionRooms = addCompartment(rooms, maxCapacity, hospital, "infusion_room", allResources);
		Set<Allocatable> consultingRooms = addCompartment(rooms, maxCapacity, hospital, "consulting_room", allResources);

		// build resources:
		Role hospitalStaff = new Role("staff");
		Role phlebotomistRole = new Role("phlebotomist");
		phlebotomistRole.addCanPerform(hospitalStaff);
		Set<Allocatable> phlebotomists = new HashSet<Allocatable>();

		Role nurseRole = new Role("nurse");
		nurseRole.addCanPerform(phlebotomistRole);
		nurseRole.addCanPerform(hospitalStaff);
		Set<Allocatable> nurses = new HashSet<Allocatable>();

		Role doctorRole = new Role("doctor");
		doctorRole.addCanPerform(hospitalStaff);
		doctorRole.addCanPerform(nurseRole);
		doctorRole.addCanPerform(phlebotomistRole);
		Set<Allocatable> doctors = new HashSet<Allocatable>();

		for (int i = 1; i <= nResources; i++) {
			Person nurse = new Person("nurse" + i);
			nurse.addRole(nurseRole);
			nurses.add(nurse);
			allResources.add(nurse);

			Person phlebotomist = new Person("phlebotomist" + i);
			phlebotomist.addRole(phlebotomistRole);
			phlebotomists.add(phlebotomist);
			allResources.add(phlebotomist);

			Person doctor = new Person("doctor" + i);
			doctor.addRole(doctorRole);
			doctors.add(doctor);
			allResources.add(doctor);
		}

		// add rooms

		Transition consulting = null;
		Transition bloodDraw = null;
		Transition examination = null;
		Transition infusion = null;

		for (Transition t : stochNet.getTransitions()) {
			if (t.getLabel().equals("consulting")) {
				consulting = t;
			}
			if (t.getLabel().equals("blood draw")) {
				bloodDraw = t;
			}
			if (t.getLabel().equals("examination")) {
				examination = t;
			}
			if (t.getLabel().equals("infusion")) {
				infusion = t;
			}
		}
		Map<Transition, Integer> relevantActivityMapping = getRelevantActivities(consulting, bloodDraw, examination,
				infusion);
		Map<Integer, Transition> activityIds = handleRelevantActivities(stochNet, relevantActivityMapping);

		
		PetrinetModelAllocations modelAllocations = null;
		
		switch(experimentType){
			case NOISE:
			case COMPLEXITY:
				// hard code allocation patterns (complexity is determined by room capacity)
				modelAllocations = generateNoiseAllocations(stochNet, dropOffRate,
						examRooms, nurseRooms, infusionRooms, consultingRooms, phlebotomists, nurses, doctors, consulting,
						bloodDraw, examination, infusion);
				break;
				// increase entropy by allocating resources in an exponential round-robin fashion:
				// a(1,0,0,0)       a(0.9,0.1,0.0,0.0)
				// b(0,1,0,0) --- \ b(0.0,0.9,0.1,0.0)
				// c(0,0,1,0) --- / c(0.0,0.0,0.9,0.1)
				// d(0,0,0,1)       d(0.1,0.0,0.0,0.9)
			case AMBIGUITY:
				modelAllocations = generateAllocations(stochNet, nResources, dropOffRate, allResources, activityIds);
				break;
		}

		System.out.println("Reverse location Entropy: "+modelAllocations.getReverseEntropy(AllocType.LOCATION));
		System.out.println("Reverse resource Entropy: "+modelAllocations.getReverseEntropy(AllocType.RESOURCE));
		
		logger.debug("\n\n");
		List<Pair<Transition, Transition>> orderedTransitions = modelAllocations.getOrderRelation(stochNet);
		for (Pair<Transition, Transition> orderedPair : orderedTransitions) {
			logger.debug(orderedPair.getFirst().getLabel() + " -> " + orderedPair.getSecond().getLabel());
		}
		logger.debug("\n\n");

		Object[] generatedNetAndMarking = AllocationBasedNetGenerator.generateNet(stochNet, modelAllocations,
				allResources, numCases, meanTimeBetweenArrivals, noise, startTime);

		PNUnfoldedSimulator simulator = new PNUnfoldedSimulator();

		PNSimulatorConfig config = new PNSimulatorConfig(1);
		StochasticNet generatedNet = (StochasticNet) generatedNetAndMarking[0];

		// visualize generated net!
//		TestUtils.showModel(generatedNet);

		XLog log = simulator.simulate(null, generatedNet, StochasticNetUtils.getSemantics(generatedNet), config,
				(Marking) generatedNetAndMarking[1]);
		StochasticNetUtils.writeLogToFile(log, new File("tests/testfiles/out/richLog" + numCases + ".xes"));
		debugLogToConsole(log);

		logger.debug("\n\n-----------------\n\n");

		SortedSensorIntervals intervals = LogToSensorIntervalConverter.convertLog(log, generatedNet.getTimeUnit(), false, 1);
		logger.debug(intervals.toString());

//		JComponent orig = SensorIntervalVisualization.visualize(null, intervals);
//		
//		SortedSensorIntervals intervalsAdjusted = LogToSensorIntervalConverter.convertLog(log, generatedNet.getTimeUnit(), true, 1);
//		
//		JComponent adjusted = SensorIntervalVisualization.visualize(null, intervalsAdjusted);
//		
//		JComponent comparison = new JPanel();
//		comparison.setLayout(new BorderLayout());
//		comparison.add(orig, BorderLayout.NORTH);
//		comparison.add(adjusted, BorderLayout.SOUTH);
//		
//		TestUtils.showComponent(comparison);

		StochasticNetUtils.writeStringToFile(intervals.toString(), "tests/testfiles/out/intervalLog" + numCases
				+ ".csv");

		// ###########################################################
		// we have the process model, the allocations, the intervals.
		// Let's create the ILP problem!
		// ###########################################################

		List<Interaction> possibleInteractions = collectPossibleInteractions(intervals);

		// helper that stores all interactions of a case:
		Map<String, Set<Integer>> caseInteractions = new HashMap<String, Set<Integer>>();
		Map<Integer, Interaction> interactionMapping = new HashMap<Integer, Interaction>();
		Map<String, List<Interaction>> locationInteractions = new HashMap<String, List<Interaction>>();
		int counter = 0;
		for (Interaction interaction : possibleInteractions) {
			interactionMapping.put(interaction.getId(), interaction);
			if (!locationInteractions.containsKey(interaction.getLocationKey())) {
				locationInteractions.put(interaction.getLocationKey(), new LinkedList<Interaction>());
			}
			locationInteractions.get(interaction.getLocationKey()).add(interaction);
			if (!caseInteractions.containsKey(interaction.getInstanceKey())) {
				caseInteractions.put(interaction.getInstanceKey(), new HashSet<Integer>());
			}
			caseInteractions.get(interaction.getInstanceKey()).add(counter);
			counter++;
		}

		Map<String, Map<Integer, Set<Integer>>> interactionPrecedenceRelation = getInteractionPrecendenceRelations(
				caseInteractions, interactionMapping);

		Set<SparseBitSet> excludingInteractions = getExcludingInteractions(locationInteractions, interactionMapping);

		// we use Gurobi for that (http://www.gurobi.com/)
		try {
			GRBEnv env = new GRBEnv("allocation_experiment.log");
			GRBModel model = new GRBModel(env);
			
			long startTimeOfExperiment = System.currentTimeMillis();

			// Create variables:

			// x_i,j: for each interaction i, and each activity a, we can have a potential mapping
			Map<Integer, Map<Integer, GRBVar>> mappingVariables = new HashMap<Integer, Map<Integer, GRBVar>>();
			Map<Integer, Map<Integer, GRBVar>> mappingHelpers = new HashMap<Integer, Map<Integer, GRBVar>>();
			// z_i,a,b for each interaction there are some implications
			Map<String, GRBVar> mappingImplications = new HashMap<String, GRBVar>();

			initializeVariables(relevantActivityMapping, activityIds, orderedTransitions, possibleInteractions, model,
					mappingVariables, mappingHelpers, mappingImplications);

			// Integrate new variables
			model.update();

			addHelperConstraintsForPrecedenceImplications(activityIds, caseInteractions, interactionPrecedenceRelation,
					model, mappingVariables, mappingHelpers);

			addMaxOneActivityPerCaseConstraints(caseInteractions, activityIds, mappingVariables, model);

			addImplicationFunctionsForAllPrecedenceRelations(relevantActivityMapping, orderedTransitions,
					possibleInteractions, model, mappingVariables, mappingHelpers, mappingImplications, activityIds);

			//		      addMaxOneActivityConstraints()

			// Integrate new variables
			model.update();

			// add exclusiveness constraints on interactions that overlap in time and resources:
			addExcludingInteractionsConstraints(activityIds, excludingInteractions, model, mappingVariables);

			/**
			 * Cost optimization function: fix costs per x_i,a assignment:
			 */
			Map<String, Double> locationAllocationSums = calculateAllocationSums(activityIds, modelAllocations,
					AllocType.LOCATION);
			Map<String, Double> resourceAllocationSums = calculateAllocationSums(activityIds, modelAllocations,
					AllocType.RESOURCE);

			GRBLinExpr optExpr = new GRBLinExpr();
			
			List<Double> wIAstats = new ArrayList<Double>();

			Map<Integer, Map<Integer, Double>> interactionActivityProbabilities = new HashMap<Integer, Map<Integer, Double>>();
			for (Interaction interaction : possibleInteractions) {
				int i = interaction.getId();
				String location = interaction.getLocationKey();
				String resources = PNUnfoldedSimulator.getResourceString(interaction.getResourceKeys());
				long duration = interaction.getEndTime() - interaction.getStartTime();
				double durationProbabilitySum = getCompetingDurations(duration, relevantActivityMapping);

				interactionActivityProbabilities.put(i, new HashMap<Integer, Double>());
				for (Integer activityId : activityIds.keySet()) {
					interactionActivityProbabilities.get(i).put(activityId, 0.0);

					Transition activityTransition = activityIds.get(activityId);
					Set<Allocation> allocs = modelAllocations.getAllocations(activityTransition);

					double durationRelativeLikelihood = getTransitionDensity(duration, activityIds.get(activityId))
							/ durationProbabilitySum;

					double probability = 1;
					int changed = 0;
					for (Allocation alloc : allocs) {
						boolean isLocation = alloc.getType().equals(AllocType.LOCATION);
						String key = isLocation ? location : resources;

						Map<String, Double> probs = alloc.getProbabilitiesOfAllocations();
						if (probs.containsKey(key)) {
							changed++;
							probability *= isLocation ? probs.get(key) / locationAllocationSums.get(key) : probs
									.get(key) / resourceAllocationSums.get(key);

						}
					}
					//require both location and resources to be possible allocations!
					if (changed > 1) {
						interactionActivityProbabilities.get(i).put(activityId, probability);
					}
					double oldProb = interactionActivityProbabilities.get(i).get(activityId);
					double newProb = oldProb * durationRelativeLikelihood;
					interactionActivityProbabilities.get(i).put(activityId, newProb);
					if (Double.isNaN(newProb)) {
						logger.error("Debug me!");
					}
					if (newProb > 0){
						wIAstats.add(newProb);
					}
					logger.debug("interaction " + i + " and activity " + activityId + " -> " + newProb
							+ " probability");
					optExpr.addTerm(newProb, mappingVariables.get(i).get(activityId));
				}
			}
			// gets the median of the weights
			Collections.sort(wIAstats);
			double precedenceViolationCost = wIAstats.get(wIAstats.size()/2);
			
			for (String varName : mappingImplications.keySet()) {
				GRBVar implVar = mappingImplications.get(varName);
				optExpr.addTerm(precedenceViolationCost, implVar);
			}

			model.setObjective(optExpr, GRB.MAXIMIZE);

			model.update();
			// Optimize model
			model.optimize();

			logger.debug("Obj: " + model.get(GRB.DoubleAttr.ObjVal));

			Map<Interaction, Transition> interactionToTransition = new HashMap<Interaction, Transition>();
			for (Interaction interaction : possibleInteractions) {
				int intId = interaction.getId();
				for (Integer activityId : activityIds.keySet()) {
					GRBVar var = mappingVariables.get(intId).get(activityId);
					int value = (int) var.get(GRB.DoubleAttr.X);
					if (value > 0) {
						logger.debug("Mapped " + interaction.getInstanceKey() + ": x_" + intId + "," + activityId
								+ " in optimal solution");
						interactionToTransition.put(interaction, activityIds.get(activityId));
					}
				}
			}
			for (String varName : mappingImplications.keySet()) {
				GRBVar implVar = mappingImplications.get(varName);
				int value = (int) implVar.get(GRB.DoubleAttr.X);
				if (value == 0) {
					logger.warn("Violated " + varName + ":");
					String[] parts = varName.split("_");
					int intId = Integer.valueOf(parts[0].substring(1));
					int aId = Integer.valueOf(parts[1]);
					int bId = Integer.valueOf(parts[2]);
					Interaction i = interactionMapping.get(intId);

					logger.info("model says that constraint " + activityIds.get(aId) + " precedes "
							+ activityIds.get(bId) + " for " + i.getInstanceKey() + " is violated.");
					logger.info("helper " + ("z" + intId + "_" + aId) + " is "
							+ mappingHelpers.get(intId).get(aId).get(GRB.DoubleAttr.X));
				}
			}

			// Dispose of model and environment

			model.dispose();
			env.dispose();

			XLog reconstructedLog = reconstructLog(interactionToTransition, generatedNet.getTimeUnit());
			logger.debug("------------");
			debugLogToConsole(log);
			logger.debug("------------");
			debugLogToConsole(reconstructedLog);
			logger.debug("------------");

			long durationOfExperiment = System.currentTimeMillis()-startTimeOfExperiment;
			
			double minEntropy = Math.min(modelAllocations.getReverseEntropy(AllocType.LOCATION), modelAllocations.getReverseEntropy(AllocType.RESOURCE));
			
			resultLines = measureDistance(runId, log, reconstructedLog, noise, durationOfExperiment, maxCapacity, nResources, rooms, minEntropy);
		} catch (GRBException e) {
			logger.fatal("Error code: " + e.getErrorCode() + ". " + e.getMessage());
			e.printStackTrace();
		}
		return resultLines;
	}

	protected PetrinetModelAllocations generateAllocations(Petrinet net, int nResources, double dropOffRate,
			Set<Allocatable> allResources, Map<Integer, Transition> activityIds) {
		PetrinetModelAllocations allocations = new PetrinetModelAllocations(net);
		// allocate locations and resources in the same manner:
		Set<Allocatable> locations = getResources(allResources, AllocType.LOCATION);
		Set<Allocatable> resources = getResources(allResources, AllocType.RESOURCE);
		if (locations.size()< nResources || resources.size() < nResources){
			throw new IllegalArgumentException("not enough resources to distribute.");
		}
		for (Integer actId : activityIds.keySet()){
			Transition activity = activityIds.get(actId);
			List<Allocatable> locationList = new ArrayList<Allocatable>(locations);
			locationList = locationList.subList(0, nResources);
			Collections.rotate(locationList, actId);// important! we would like to have the same number of resources 
			Allocation locationAllocation = new PoissonAllocation(locationList, dropOffRate, AllocType.LOCATION);
			allocations.addAllocation(activity, locationAllocation);
			
			List<Allocatable> resourceList = new ArrayList<Allocatable>(resources);
			resourceList = resourceList.subList(0, nResources);
			Collections.rotate(resourceList, actId);// important! we would like to have the same number of resources
			Allocation resourceAllocation = new PoissonAllocation(resourceList, dropOffRate, AllocType.RESOURCE);
			
			allocations.addAllocation(activity, resourceAllocation);
		}
		return allocations;
	}

	private Set<Allocatable> getResources(Set<Allocatable> allResources, AllocType location) {
		Set<Allocatable> resourcesOfType = new HashSet<Allocatable>();
		for (Allocatable alloc : allResources){
			switch (location){
				case LOCATION:
					if (alloc instanceof Room){
						resourcesOfType.add(alloc);
					}
					break;
				case RESOURCE:
					if (alloc instanceof Person){
						resourcesOfType.add(alloc);
					}
					break;
			}
		}
		return resourcesOfType;
	}

	protected Map<Integer, Transition> handleRelevantActivities(StochasticNet stochNet,
			Map<Transition, Integer> relevantActivityMapping) {
		Map<Integer, Transition> activityIds = new HashMap<Integer, Transition>();

		for (Transition t : relevantActivityMapping.keySet()) {
			activityIds.put(relevantActivityMapping.get(t), t);
			Assert.assertNotNull(t);
		}
		for (Transition t : stochNet.getTransitions()) {
			if (!relevantActivityMapping.containsKey(t)) {
				t.setInvisible(true);
			}
		}
		return activityIds;
	}

	protected Map<Transition, Integer> getRelevantActivities(Transition...transitions) {
		Map<Transition, Integer> relevantActivityMapping = new HashMap<Transition, Integer>();
		for (int i = 0; i < transitions.length; i++){
			relevantActivityMapping.put(transitions[i], i);
		}
		return relevantActivityMapping;
	}

	/**
	 * Generates a baseline allocation knowledge for noise experiments.
	 * 
	 * @param stochNet
	 * @param mixedResources
	 * @param dropOffRate
	 * @param examRooms
	 * @param nurseRooms
	 * @param infusionRooms
	 * @param consultingRooms
	 * @param phlebotomists
	 * @param nurses
	 * @param doctors
	 * @param consulting
	 * @param bloodDraw
	 * @param examination
	 * @param infusion
	 * @return
	 */
	protected PetrinetModelAllocations generateNoiseAllocations(StochasticNet stochNet,
			double dropOffRate, Set<Allocatable> examRooms, Set<Allocatable> nurseRooms,
			Set<Allocatable> infusionRooms, Set<Allocatable> consultingRooms, Set<Allocatable> phlebotomists,
			Set<Allocatable> nurses, Set<Allocatable> doctors, Transition consulting, Transition bloodDraw,
			Transition examination, Transition infusion) {
		PetrinetModelAllocations modelAllocations = new PetrinetModelAllocations(stochNet);
		
		AllocationDistribution bloodDrawResourceAllocation = new AllocationDistribution(AllocType.RESOURCE);
		AllocationDistribution bloodDrawLocationAllocation = new AllocationDistribution(AllocType.LOCATION);
		// *** RESOURCES ***
		// phlebotomist = 0.6
		addAlloc(bloodDrawResourceAllocation, new AllocationConfig(phlebotomists,1,0.6, AllocType.RESOURCE));
		// nurse = 0.2
		addAlloc(bloodDrawResourceAllocation, new AllocationConfig(nurses, 1, 0.2, AllocType.RESOURCE));
		// nurse, nurse = 0.2
		addAlloc(bloodDrawResourceAllocation, new AllocationConfig(nurses, 2, 0.2, AllocType.RESOURCE));
		// *** LOCATIONS ***
		// nurse room = 0.6
		addAlloc(bloodDrawLocationAllocation, new AllocationConfig(nurseRooms, 1, 0.6, AllocType.LOCATION));
		// infusion room = 0.4
		addAlloc(bloodDrawLocationAllocation, new AllocationConfig(infusionRooms, 1, 0.4, AllocType.LOCATION));
		
		AllocationDistribution consultingResourceAllocation = new AllocationDistribution(AllocType.RESOURCE);
		AllocationDistribution consultingLocationAllocation = new AllocationDistribution(AllocType.LOCATION);
		// *** RESOURCES ***
		// doctor = 0.95
		addAlloc(consultingResourceAllocation, new AllocationConfig(doctors, 1, 0.95, AllocType.RESOURCE));
		// doctor, doctor = 0.05
		addAlloc(consultingResourceAllocation, new AllocationConfig(doctors, 2, 0.05, AllocType.RESOURCE));
		// *** LOCATIONS ***
		// consulting room = 0.6
		addAlloc(consultingLocationAllocation, new AllocationConfig(consultingRooms, 1, 0.8, AllocType.LOCATION));
		// exam room = 0.4
		addAlloc(consultingLocationAllocation, new AllocationConfig(examRooms, 1, 0.2, AllocType.LOCATION));
				
		AllocationDistribution examinationResourceAllocation = new AllocationDistribution(AllocType.RESOURCE);
		AllocationDistribution examinationLocationAllocation = new AllocationDistribution(AllocType.LOCATION);
		// *** RESOURCES ***
		// doctor = 0.4
		addAlloc(examinationResourceAllocation, new AllocationConfig(doctors, 1, 0.4, AllocType.RESOURCE));
		// nurse = 0.2
		addAlloc(examinationResourceAllocation, new AllocationConfig(nurses, 1, 0.2, AllocType.RESOURCE));
		// doctor, nurse = 0.2
		addAlloc(examinationResourceAllocation, new AllocationConfig(doctors, nurses, 1, 1, 0.2, AllocType.RESOURCE));
		// doctor, doctor = 0.2
		addAlloc(examinationResourceAllocation, new AllocationConfig(doctors, 2, 0.2, AllocType.RESOURCE));
		// *** LOCATIONS ***
		// exam room = 0.9
		addAlloc(examinationLocationAllocation, new AllocationConfig(examRooms, 1, 0.9, AllocType.LOCATION));
		// consulting room = 0.1
		addAlloc(examinationLocationAllocation, new AllocationConfig(consultingRooms, 1, 0.1, AllocType.LOCATION));
				
		
		AllocationDistribution infusionResourceAllocation = new AllocationDistribution(AllocType.RESOURCE);
		AllocationDistribution infusionLocationAllocation = new AllocationDistribution(AllocType.LOCATION);
		// *** RESOURCES ***
		// nurse = 0.8
		addAlloc(infusionResourceAllocation, new AllocationConfig(nurses, 1, 0.8, AllocType.RESOURCE));
		// nurse, nurse = 0.2
		addAlloc(infusionResourceAllocation, new AllocationConfig(nurses, 2, 0.2, AllocType.RESOURCE));
		// *** LOCATIONS ***
		// infusion room = 0.95
		addAlloc(infusionLocationAllocation, new AllocationConfig(infusionRooms, 1, 0.95, AllocType.LOCATION));
		// nurse room = 0.05
		addAlloc(infusionLocationAllocation, new AllocationConfig(nurseRooms, 1, 0.05, AllocType.LOCATION));

		
//		Set<Set<Allocatable>> doctorSubsets = StochasticNetUtils.generateAllSubsets(doctors);
//		Set<Allocatable> doctorsAndNurses = new HashSet<Allocatable>(doctors);
//		doctorsAndNurses.addAll(nurses);
//		Set<Set<Allocatable>> doctorNurseSubsets = StochasticNetUtils.generateAllSubsets(doctorsAndNurses);
//		Set<Set<Allocatable>> nurseSubsets = StochasticNetUtils.generateAllSubsets(nurses);
//		Set<Set<Allocatable>> phlebotomistSubsets = StochasticNetUtils.generateAllSubsets(phlebotomists);
//
//		Iterator<Set<Allocatable>> subSetIter = doctorSubsets.iterator();
//		while (subSetIter.hasNext()) {
//			Set<Allocatable> subset = subSetIter.next();
//			if (subset.size() > 0 && subset.size() <= 3) {
//				double probability = Math.pow(dropOffRate, subset.size() - 1);
//				consultingAllocation.addAllocationOption(subset, probability);
//				examinationAllocation.addAllocationOption(subset, probability);
//			}
//		}
//
//		Iterator<Set<Allocatable>> nurseSubSetIter = nurseSubsets.iterator();
//		while (nurseSubSetIter.hasNext()) {
//			Set<Allocatable> subset = nurseSubSetIter.next();
//			if (subset.size() > 0 && subset.size() <= 3) {
//				double probability = Math.pow(dropOffRate, subset.size() - 1);
//				if (mixedResources) {
//					bloodDrawAllocation.addAllocationOption(subset, probability);
//				}
//				infusionAllocation.addAllocationOption(subset, probability);
//			}
//		}
//		Iterator<Set<Allocatable>> phleboSubSetIter = phlebotomistSubsets.iterator();
//		while (phleboSubSetIter.hasNext()) {
//			Set<Allocatable> subset = phleboSubSetIter.next();
//			if (subset.size() > 0 && subset.size() <= 3) {
//				double probability = Math.pow(dropOffRate, subset.size() - 1);
//				bloodDrawAllocation.addAllocationOption(subset, probability);
//				if (mixedResources) {
//					infusionAllocation.addAllocationOption(subset, probability);
//				}
//			}
//		}
//
//		
		modelAllocations.addAllocation(infusion, infusionResourceAllocation);
		modelAllocations.addAllocation(consulting, consultingResourceAllocation);
		modelAllocations.addAllocation(bloodDraw, bloodDrawResourceAllocation);
		modelAllocations.addAllocation(examination, examinationResourceAllocation);
		
		modelAllocations.addAllocation(infusion, infusionLocationAllocation);
		modelAllocations.addAllocation(consulting, consultingLocationAllocation);
		modelAllocations.addAllocation(bloodDraw, bloodDrawLocationAllocation);
		modelAllocations.addAllocation(examination, examinationLocationAllocation);
//
//		modelAllocations.addAllocation(infusion, new UniformAllocation(infusionRooms, AllocType.LOCATION));
//		modelAllocations.addAllocation(consulting, new UniformAllocation(consultingRooms, AllocType.LOCATION));
//		modelAllocations.addAllocation(bloodDraw, new UniformAllocation(nurseRooms, AllocType.LOCATION));
//		modelAllocations.addAllocation(examination, new UniformAllocation(examRooms, AllocType.LOCATION));
		return modelAllocations;
	}

	/**
	 * Adds allocation options according to the give AllocationConfig. 
	 * @param distr the distribution to add the options to.
	 * @param allocationConfig the configuration to use 
	 */
	protected void addAlloc(AllocationDistribution distr, AllocationConfig allocationConfig) {
		Allocation alloc = allocationConfig.getResultingAllocationDistribution();
		Map<String, Double> probs = alloc.getProbabilitiesOfAllocations();
		for (String allocString : probs.keySet()){
			Set<Allocatable> allocatables = alloc.getAllocation(allocString);
			distr.addAllocationOption(allocatables, probs.get(allocString)*allocationConfig.getProbability());
		}
	}

	private List<String> measureDistance(int runId, XLog log, XLog reconstructedLog, double noise, long durationOfExperiment, int maxCapacity, int nResources, int rooms, double minEntropy) {
		List<String> resultPerCase = new ArrayList<String>();
		// measure each line:
		Map<String, String> activityToLetterMapping = new HashMap<String, String>();
//		String abc = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOKQRSTUVWXYZ123456789";
//		int pos = 0;
		for (XTrace origTrace : log){
			for (XEvent e : origTrace){
				String eventName = XConceptExtension.instance().extractName(e);
				if (!eventName.startsWith("arrival_case_") && !activityToLetterMapping.containsKey(eventName)){
//					// this is one idea:
//					activityToLetterMapping.put(eventName, abc.substring(pos, ++pos));
					// for now, simply use first letter:
					activityToLetterMapping.put(eventName, eventName.substring(0, 1));
				}
			}
		}
		
		for (XTrace origTrace : log){
			String traceString = getTraceString(origTrace,activityToLetterMapping);
			String instance = XConceptExtension.instance().extractName(origTrace);
			XTrace reconstructedTrace = getReconstructedTrace(reconstructedLog, instance);
			String reconstructedTraceString = reconstructedTrace!=null?getTraceString(reconstructedTrace, activityToLetterMapping):"";
			logger.debug("compare ("+instance+")\t"+traceString);
			logger.debug("to\t\t"+reconstructedTraceString);
			
			double similarity = StringEditDistance.similarity(traceString, reconstructedTraceString);
			int distance = StringEditDistance.editDistance(traceString, reconstructedTraceString);
			Map<String,Double> distanceMeasures = getDistanceMeasures(origTrace, reconstructedTrace);
			String resultLine = runId	+SEP+log.size()	+SEP+noise		+SEP+maxCapacity	+SEP+nResources		+SEP+durationOfExperiment	+SEP+distance		+SEP+similarity		+SEP+distanceMeasures.get(DURATION_SIM)	+SEP+distanceMeasures.get(HITRATE)	+SEP+distanceMeasures.get(LOCATION_ACCURACY)	+SEP+distanceMeasures.get(RESOURCE_SIMILARITY)	+SEP+minEntropy	+SEP+traceString	+SEP+reconstructedTraceString;
			// 					"RunId"	+SEP+"Patients"	+SEP+"Noise"	+SEP+"MaxCapacity"	+SEP+"numResources"	+SEP+"Duration"				+SEP+"Stringedits"	+SEP+"Similarity"	+SEP+"PercentageError"					+SEP+"Matches"						+SEP+LOCATION_ACCURACY							+SEP+RESOURCE_SIMILARITY						+SEP+ENTROPY	+SEP+"OrigTrace"	+SEP+"ReconstructedTrace"
			//RUN_ID"+SEP+"NUMPATIENTS"+SEP+"NOISE"+SEP+"RECALL"+SEP+"PRECISION"+SEP+"STRINGEDITS"+SEP+"SIMILARITY"+SEP+"RMSE";
			resultPerCase.add(resultLine);
		}
		return resultPerCase;
	}

	private Map<String, Double> getDistanceMeasures(XTrace origTrace, XTrace restoredTrace) {
		int errors = 0;
		int total = 0;
		DescriptiveStatistics stats = new DescriptiveStatistics();
		DescriptiveStatistics locationAccuracy = new DescriptiveStatistics();
		DescriptiveStatistics resourceAccuracy = new DescriptiveStatistics();
		// simply go through both traces and and try to extract times:
		Map<String, Long> origTimes = getDurationsFromTrace(origTrace);
		Map<String, String> origLocations = getAttributesFromTrace(origTrace, AllocType.LOCATION);
		Map<String, String> origResources = getAttributesFromTrace(origTrace, AllocType.RESOURCE);
		Map<String, Long> restoredTimes = getDurationsFromTrace(restoredTrace);
		Map<String, String> restoredLocations = getAttributesFromTrace(restoredTrace, AllocType.LOCATION);
		Map<String, String> restoredResources = getAttributesFromTrace(restoredTrace, AllocType.RESOURCE);
		
		for (String name : origTimes.keySet()){
			total++;
			if (restoredLocations.containsKey(name)){
				if (restoredLocations.get(name).equals(origLocations.get(name))){
					locationAccuracy.addValue(1); // hit
				} else {
					locationAccuracy.addValue(0); // miss
				}
			}
			if (restoredResources.containsKey(name)){
				String[] restored = restoredResources.get(name).split(",");
				String[] orig = origResources.get(name).split(",");
				resourceAccuracy.addValue(JaccardSimilarity.getSimilarity(restored, orig));
			} else {
				resourceAccuracy.addValue(0.0);
			}
			if (restoredTimes.containsKey(name)){
				// compute (multiplicative) Percentage error
				long origTime = origTimes.get(name);
				long restoredTime = restoredTimes.get(name);
				stats.addValue(getAbsolutePercentageError(origTime, restoredTime));
			} else {
				errors++;
			}
		}
		Map<String, Double> map = new HashMap<String, Double>();
		map.put(DURATION_SIM, getMean(stats));
		map.put(HITRATE, 1-(errors/(double)total));
		map.put(LOCATION_ACCURACY, getMean(locationAccuracy));
		map.put(RESOURCE_SIMILARITY, getMean(resourceAccuracy));
		return map;
	}

	/**
	 * Computes the symmetric absolute percentage error:
	 * | f - a |
	 * ---------
	 * |f| + |a|
	 * 
	 * This has the range between 0  (f=a) and 1 (either of them is 0)
	 * @param origTime
	 * @param restoredTime
	 * @return
	 */
	private double getAbsolutePercentageError(long origTime, long restoredTime) {
		if (origTime == restoredTime){
			return 0;
		} else {
			return Math.abs(origTime-restoredTime) / (Math.abs(origTime)+Math.abs(restoredTime));
		}
//		else if (origTime > restoredTime){
//			stats.addValue((double)restoredTime / origTime);
//		} else {
//			stats.addValue((double) origTime / restoredTime);
//		}
	}

	protected double getMean(DescriptiveStatistics stats) {
		return stats.getN()==0l?0:stats.getMean();
	}

	private Map<String, String> getAttributesFromTrace(XTrace trace, AllocType type) {
		Map<String, String> locations = new HashMap<String, String>();
		if (trace != null){
			for (XEvent e : trace){
				String name = XConceptExtension.instance().extractName(e);
				String value = null;
				switch(type){
					case LOCATION:
						if (e.getAttributes().containsKey(PNSimulator.LOCATION_ROOM))
							value = e.getAttributes().get(PNSimulator.LOCATION_ROOM).toString();
						break;
					case RESOURCE:
						value = XOrganizationalExtension.instance().extractResource(e);
						break;
				}
				if (value != null)
					locations.put(name, value);
			}
		}
		return locations;
	}

	private Map<String, Long> getDurationsFromTrace(XTrace trace) {
		Map<String, Long> durations = new HashMap<String, Long>();
		Map<String, XEvent> startEvents = new HashMap<String, XEvent>();
		if (trace != null){
			for (XEvent e: trace){
				String name = XConceptExtension.instance().extractName(e);
				String lifecycle = XLifecycleExtension.instance().extractTransition(e);
				if (lifecycle.equals("start")){
					startEvents.put(name, e);
				} else {
					if (startEvents.containsKey(name)){
						long startTime = XTimeExtension.instance().extractTimestamp(startEvents.get(name)).getTime();
						long endTime = XTimeExtension.instance().extractTimestamp(e).getTime();
						durations.put(name, endTime-startTime);
					}
				}
			}
		}
		return durations;
	}

	private XTrace getReconstructedTrace(XLog reconstructedLog, String instance) {
		for (XTrace trace : reconstructedLog){
			String instanceName = XConceptExtension.instance().extractName(trace);
			if (instanceName.equals(instance)){
				return trace;
			}
		}
		return null;
	}

	private String getTraceString(XTrace origTrace, Map<String, String> activityToLetterMapping) {
		
		StringBuilder builder = new StringBuilder(); 
		for (XEvent e : origTrace){
			String eventName = XConceptExtension.instance().extractName(e);
			if (activityToLetterMapping.containsKey(eventName)){
				builder.append(activityToLetterMapping.get(eventName));
			} else {
//				System.out.println("key "+eventName+" not added to trace string.");
			}
		}	
		return builder.toString();
	}

	private String getHeader() {
		return "RunId"+SEP+"Patients"+SEP+"Noise"+SEP+"MaxCapacity"+SEP+"numResources"+SEP+"Duration"+SEP+"Stringedits"+SEP+"Similarity"+SEP+"PercentageError"+SEP+"Matches"+SEP+LOCATION_ACCURACY+SEP+RESOURCE_SIMILARITY+SEP+ENTROPY+SEP+"OrigTrace"+SEP+"ReconstructedTrace";
	}

	private void addMaxOneActivityPerCaseConstraints(Map<String, Set<Integer>> caseInteractions, Map<Integer, Transition> activityIds, Map<Integer, Map<Integer, GRBVar>> mappingVariables, GRBModel model) throws GRBException {
		for (String caseId : caseInteractions.keySet()) {
			Set<Integer> interactions = caseInteractions.get(caseId);
			for (Integer activityId : activityIds.keySet()) {
				GRBLinExpr maxOneActExpr = new GRBLinExpr();
				for (Integer interact : interactions){
					maxOneActExpr.addTerm(1.0, mappingVariables.get(interact).get(activityId));
				}
				model.addConstr(maxOneActExpr, GRB.LESS_EQUAL, 1.0, "maxOneAct"+caseId+"_"+activityId);
			}
		}
	}

	protected void addExcludingInteractionsConstraints(Map<Integer, Transition> activityIds,
			Set<SparseBitSet> excludingInteractions, GRBModel model, Map<Integer, Map<Integer, GRBVar>> mappingVariables)
			throws GRBException {
		int excludingInt = 0;
		  for (SparseBitSet excludingInteraction : excludingInteractions){
			  GRBLinExpr expr = new GRBLinExpr();  
			  for( int i = excludingInteraction.nextSetBit(0); i >= 0; i = excludingInteraction.nextSetBit(i+1) ){
				  for(Integer activityId : activityIds.keySet()){
					  expr.addTerm(1, mappingVariables.get(i).get(activityId));
				  }
			  }
			  model.addConstr(expr, GRB.LESS_EQUAL, 1, "excluding_i_"+(excludingInt++));
		  }
	}

	protected void debugLogToConsole(XLog log) {
		List<XTrace> traceList = new ArrayList<XTrace>(log);
		Collections.sort(traceList, new Comparator<XTrace>() {
			public int compare(XTrace o1, XTrace o2) {
				String instance = XConceptExtension.instance().extractName(o1);
				String instance2 = XConceptExtension.instance().extractName(o2);
				return instance.compareTo(instance2);
			}
		});
		for (XTrace trace : traceList){
			String instance = XConceptExtension.instance().extractName(trace);
			logger.debug("trace "+instance+":"+StochasticNetUtils.debugTrace(trace));
		}
	}

	private XLog reconstructLog(Map<Interaction, Transition> interactionToTransition, TimeUnit timeUnit) {
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();
		XConceptExtension.instance().assignName(log, "reconstructed log by the ROAD ILP solution");
		Map<String, XTrace> traces = new HashMap<String, XTrace>();
		for (Interaction interaction : interactionToTransition.keySet()){
			Transition t = interactionToTransition.get(interaction);
			String instance = parseCaseId(interaction.getInstanceKey());
			if (!traces.containsKey(instance)){
				XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
				XConceptExtension.instance().assignName(trace, instance);
				traces.put(instance, trace);
			}
			
			XEvent eventStart = getEventForInteraction(interaction, t, instance, XLifecycleExtension.StandardModel.START, (long) (interaction.getStartTime()*timeUnit.getUnitFactorToMillis()));
			XEvent eventComplete = getEventForInteraction(interaction, t, instance, XLifecycleExtension.StandardModel.COMPLETE, (long) (interaction.getEndTime()*timeUnit.getUnitFactorToMillis()));
			// find 
			insertOrdered(traces.get(instance), eventComplete, true);
			insertOrdered(traces.get(instance), eventStart, false);
			
			
		}
		for (String instance : traces.keySet()){
			log.add(traces.get(instance));
		}
		
		return log;
	}

	private String parseCaseId(String instanceKey) {
		return instanceKey.substring(Person.CASE_PREFIX.length());
	}

	/**
	 * Inserts an event into a trace, and ensures that if there are two events at the same time, the start is after the complete!
	 * @param xTrace
	 * @param event
	 * @param firstAtTime
	 */
	private void insertOrdered(XTrace xTrace, XEvent event, boolean firstAtTime) {
		Iterator<XEvent> eventIter = xTrace.iterator();
		int lastPos = 0;
		long timeStamp = XTimeExtension.instance().extractTimestamp(event).getTime();
		while(eventIter.hasNext()){
			XEvent nextEvent = eventIter.next();
			long nextStampInLog = XTimeExtension.instance().extractTimestamp(nextEvent).getTime();
			if (nextStampInLog < timeStamp){
				// continue to search
			} else if (nextStampInLog == timeStamp){
				if (firstAtTime){
					// insert here:
					break;
				} // else continue to search
			} else {
				// insert here:
				break;
			}
			lastPos++;
		}
		xTrace.add(lastPos, event);
	}

	private XEvent getEventForInteraction(Interaction interaction, Transition t, String instance, XLifecycleExtension.StandardModel lifecycle, long time) {
		XEvent event = XFactoryRegistry.instance().currentDefault().createEvent();
		XConceptExtension.instance().assignInstance(event, instance);
		XConceptExtension.instance().assignName(event, t.getLabel());
		XLifecycleExtension.instance().assignStandardTransition(event, lifecycle);
		event.getAttributes().put(PNSimulator.LOCATION_ROOM, new XAttributeLiteralImpl(PNSimulator.LOCATION_ROOM, interaction.getLocationKey()));
		XOrganizationalExtension.instance().assignResource(event, PNUnfoldedSimulator.getResourceString(interaction.getResourceKeys()));	
		XTimeExtension.instance().assignTimestamp(event, time);
		
		return event;
	}

	/**
	 *  add location knowledge: probability to map an activity to a certain location
	 *  P(x_i,a)_l is equal to P(l(a) = l(i))  (i.e., probability of mapping i to a depends on the location of i.)
	 *  that is, in which activities is the location used.
	 *  compute the probabilities based on the allocations:
	 *  
	 * @param activityIds
	 * @param modelAllocations
	 * @param type 
	 * @return
	 */
	protected Map<String, Double> calculateAllocationSums(Map<Integer, Transition> activityIds,
			PetrinetModelAllocations modelAllocations, AllocType type) {
		
		  Map<String, Double> allocationSums = new HashMap<String, Double>();
		  for (Integer activityId : activityIds.keySet()){
			  Transition activityTransition = activityIds.get(activityId);
			  Set<Allocation> allocs = modelAllocations.getAllocations(activityTransition);
			  for (Allocation alloc : allocs){
				  if(alloc.getType().equals(type)){
					  Map<String, Double> probs = alloc.getProbabilitiesOfAllocations();
					  for (String set : probs.keySet()){
						  // we can assume that locations are singletons:
						  if (!allocationSums.containsKey(set)){
							  allocationSums.put(set, 0.0);
						  }
						  allocationSums.put(set, allocationSums.get(set)+probs.get(set));
					  }
				  }
			  }
		  }
		return allocationSums;
	}

	protected void initializeVariables(Map<Transition, Integer> relevantActivityMapping,
			Map<Integer, Transition> activityIds, List<Pair<Transition, Transition>> orderedTransitions,
			List<Interaction> possibleInteractions, GRBModel model,
			Map<Integer, Map<Integer, GRBVar>> mappingVariables, Map<Integer, Map<Integer, GRBVar>> mappingHelpers,
			Map<String, GRBVar> mappingImplications) throws GRBException {
		for (Interaction interaction : possibleInteractions){
			  int intId = interaction.getId();
			  for (Integer activityId : activityIds.keySet()){
				  if (!mappingVariables.containsKey(intId)){
					  mappingVariables.put(intId, new HashMap<Integer, GRBVar>());
					  mappingHelpers.put(intId, new HashMap<Integer, GRBVar>());
				  }
				  mappingVariables.get(intId).put(activityId, model.addVar(0,1,0,GRB.BINARY, "x"+intId+"_"+activityId));
				  mappingHelpers.get(intId).put(activityId, model.addVar(0.0, 1.0, 0.0, GRB.CONTINUOUS, "z"+intId+"_"+activityId));
			  }
			  for (Pair<Transition, Transition> orderedTransitionPair : orderedTransitions){
		    	  Integer aId = relevantActivityMapping.get(orderedTransitionPair.getFirst());
		    	  Integer bId = relevantActivityMapping.get(orderedTransitionPair.getSecond());
		    	  
		    	  String helperName = getImplicationHelperName(intId, aId, bId);
		    	  GRBVar zIab = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, helperName);
		    	  mappingImplications.put(helperName, zIab);
			  }
		  }
	}

	protected void addImplicationFunctionsForAllPrecedenceRelations(Map<Transition, Integer> relevantActivityMapping,
			List<Pair<Transition, Transition>> orderedTransitions, List<Interaction> possibleInteractions,
			GRBModel model, Map<Integer, Map<Integer, GRBVar>> mappingVariables,
			Map<Integer, Map<Integer, GRBVar>> mappingHelpers, Map<String, GRBVar> mappingImplications, Map<Integer, Transition> activityIds)
			throws GRBException {
		// add a lot of implication functions:
		  // we look at all pairs of activities that are in a precedence relation and demand 
		  // that if we map the second one, we must have mapped the previous one as well.
		  // Constraints have the form x_i,b -->(implies) z_i,a 
		  // if activity a is causal precondition (preceding) b
		  for (Pair<Transition, Transition> orderedTransitionPair : orderedTransitions){
			  Integer aId = relevantActivityMapping.get(orderedTransitionPair.getFirst());
			  Integer bId = relevantActivityMapping.get(orderedTransitionPair.getSecond());
			  if (aId != null && bId != null){
				  // add implies constraints for all of them
				  // we need all x_i,b here
				  for (Interaction interaction : possibleInteractions){
					  int i = interaction.getId();
					  logger.debug("Adding precedence constraint for case "+interaction.getInstanceKey()+" saying "+activityIds.get(aId).getLabel()+" precedes "+activityIds.get(bId).getLabel());
					  addImplication(aId,bId,i, mappingVariables, mappingHelpers, mappingImplications, model);
				  }
			  }
		  }
	}

	/**
	 * Sets the z_i,a variables to be equal to the sum of all x_j,a that are in precedence to x_i,a.
	 * 
	 * @param activityIds
	 * @param caseInteractions
	 * @param interactionPrecedenceRelation
	 * @param model
	 * @param mappingVariables
	 * @param mappingHelpers
	 * @throws GRBException
	 */
	protected void addHelperConstraintsForPrecedenceImplications(Map<Integer, Transition> activityIds,
			Map<String, Set<Integer>> caseInteractions,
			Map<String, Map<Integer, Set<Integer>>> interactionPrecedenceRelation, GRBModel model,
			Map<Integer, Map<Integer, GRBVar>> mappingVariables, Map<Integer, Map<Integer, GRBVar>> mappingHelpers)
			throws GRBException {
		int c = 0;

		// add helpers z_i,a for precedence relations: 
		for (String caseId : caseInteractions.keySet()) {
			for (Integer interaction : interactionPrecedenceRelation.get(caseId).keySet()) {
				for (Integer activityId : activityIds.keySet()) {					
					if (!interactionPrecedenceRelation.get(caseId).get(interaction).isEmpty()) {
						GRBLinExpr expr = new GRBLinExpr();
						String debugString = mappingHelpers.get(interaction).get(activityId).get(GRB.StringAttr.VarName)+" = ";
						for (Integer precedingId : interactionPrecedenceRelation.get(caseId).get(interaction)) {
							expr.addTerm(1.0, mappingVariables.get(precedingId).get(activityId));
							debugString+= precedingId+" + ";
						}
						logger.debug(debugString);
						model.addConstr(expr, GRB.EQUAL, mappingHelpers.get(interaction).get(activityId), "z_help"
								+ (c++));
					}
				}
			}
		}
	}

	/**
	 * Collects for all cases c (first index) 
	 * and for all interactions i (second index)
	 * all other interactions that "really" precede interaction i.
	 * We need to only traverse the interactions of particular cases, as the interactions are partitioned by the cases in our setting.
	 * @param caseInteractions
	 * @param interactionMapping
	 * @return
	 */
	protected Map<String, Map<Integer, Set<Integer>>> getInteractionPrecendenceRelations(
			Map<String, Set<Integer>> caseInteractions, Map<Integer, Interaction> interactionMapping) {
		Map<String, Map<Integer, Set<Integer>>> interactionPrecedenceRelation = new HashMap<String, Map<Integer,Set<Integer>>>();
		// fill interval precedence constraints
		for (String caseId : caseInteractions.keySet()){
			if (!interactionPrecedenceRelation.containsKey(caseId)){
				interactionPrecedenceRelation.put(caseId, new HashMap<Integer, Set<Integer>>());
			}
			for (Integer intId : caseInteractions.get(caseId)){
				Interaction interaction = interactionMapping.get(intId);
				for (Integer otherIntId : caseInteractions.get(caseId)){
					Interaction otherInteraction = interactionMapping.get(otherIntId);
					if (otherIntId != intId && precedes(otherInteraction, interaction)){
						if(!interactionPrecedenceRelation.get(caseId).containsKey(intId)){
							interactionPrecedenceRelation.get(caseId).put(intId, new HashSet<Integer>());
						}
						interactionPrecedenceRelation.get(caseId).get(intId).add(otherIntId);
					}
				}	
			}
		}
		return interactionPrecedenceRelation;
	}

	private double getCompetingDurations(long l, Map<Transition, Integer> relevantActivityMapping) {
		// sum over all the densities and return it
		double sum = 0;
		
		for (Transition t : relevantActivityMapping.keySet()){
			sum += getTransitionDensity(l, t);
		}
		return sum;
	}

	private double getTransitionDensity(long l, Transition t) {
		if(t instanceof TimedTransition){
			TimedTransition tt = (TimedTransition) t;
			return tt.getDistribution().density(l);
		}
		return 0;
	}

	private GRBVar addImplication(Integer aId, Integer bId, int i, Map<Integer, Map<Integer, GRBVar>> mappingVariables,
			Map<Integer, Map<Integer, GRBVar>> mappingHelpers, Map<String, GRBVar> mappingImplications, GRBModel model) throws GRBException {
		String helperName = getImplicationHelperName(i,aId,bId);
		GRBVar zIab = mappingImplications.get(helperName);
		GRBLinExpr expr = new GRBLinExpr();
		expr.addTerm(1.0, mappingHelpers.get(i).get(aId));
	    model.addConstr(expr, GRB.LESS_EQUAL, zIab, "c"+helperName+"_1");
	    
	    expr = new GRBLinExpr();
	    expr.addConstant(1);
	    expr.addTerm(-1, mappingVariables.get(i).get(bId));
	    model.addConstr(expr, GRB.LESS_EQUAL, zIab, "c"+helperName+"_2");
	    
	    expr = new GRBLinExpr();
	    expr.addConstant(1);
	    expr.addTerm(-1, mappingVariables.get(i).get(bId));
	    expr.addTerm(1, mappingHelpers.get(i).get(aId));
	    model.addConstr(expr, GRB.GREATER_EQUAL, zIab, "c"+helperName+"_3");
		
	    return zIab;
	}

	private String getImplicationHelperName(int i, Integer aId, Integer bId) {
		return "z"+i+"_"+aId+"_"+bId;
	}

	/**
	 * Returns true 
	 * @param interaction
	 * @param otherInteraction
	 * @return
	 */
	private boolean precedes(Interaction interaction, Interaction otherInteraction) {
		return interaction.getEndTime() <= otherInteraction.getStartTime();
	}

	private List<Interaction> collectPossibleInteractions(SortedSensorIntervals intervals) {
		List<Interaction> interactions = new ArrayList<Interaction>();
		
		Map<String, SortedSensorIntervals> intervalsPerLocation = new HashMap<String, SortedSensorIntervals>();
		for (SensorInterval interval : intervals){
			String loc = interval.getLocationKey();
			if (!intervalsPerLocation.containsKey(loc)){
				intervalsPerLocation.put(loc, new SortedSensorIntervals());
			}
			intervalsPerLocation.get(loc).add(interval);
		}
		
		for (String location : intervalsPerLocation.keySet()){
			SortedSet<Long> times = new TreeSet<Long>();
			Map<Long, List<SensorInterval>> enteringAtTime = new HashMap<Long, List<SensorInterval>>();
			Map<Long, List<SensorInterval>> leavingAtTime = new HashMap<Long, List<SensorInterval>>();
			
//			SortedBag<SortedPair<Long,SensorInterval>> timedInteractions = new TreeBag<SortedPair<Long,SensorInterval>> ();
			for(SensorInterval interval : intervalsPerLocation.get(location)){
				long start = interval.getStartTime();
				long end = interval.getEndTime();
				times.add(start);
				times.add(end);
				if (!enteringAtTime.containsKey(start)){
					enteringAtTime.put(start, new ArrayList<SensorInterval>());
				}
				if (!leavingAtTime.containsKey(end)){
					leavingAtTime.put(end, new ArrayList<SensorInterval>());
				}
				enteringAtTime.get(start).add(interval);
				leavingAtTime.get(end).add(interval);
//				timedInteractions.add(new SortedPair<Long, SensorInterval>(interval.getStartTime(), interval));
//				timedInteractions.add(new SortedPair<Long, SensorInterval>(interval.getEndTime(), interval));
			}
			Set<SensorInterval> activeRecords = new HashSet<SensorInterval>();
			
			for (Long time : times){
				List<SensorInterval> entering = enteringAtTime.get(time);
				List<SensorInterval> leaving = leavingAtTime.get(time);
				
				if (leaving != null){
					activeRecords.removeAll(leaving);
				}
				
				addFinishingInteractions(activeRecords, time, interactions);
				
				if (entering != null){
					activeRecords.addAll(entering);
				}
				addStartingInteractions(activeRecords, time, interactions);
			}
		}
		return interactions;
	}
	
	private Set<SparseBitSet> getExcludingInteractions(Map<String, List<Interaction>> locationInteractions, Map<Integer, Interaction> interactionMapping){
		Set<SparseBitSet> excludingInteractions = new HashSet<SparseBitSet>();
		
		for (String location : locationInteractions.keySet()){
			SortedMap<Long, Set<Interaction>> times = collectTimes(locationInteractions.get(location));
			// when multiple interactions are overlapping in time, we need to check their intersection of resources
			// in particular the maximal subsets of active interactions that still share resources are interesting!
			
			Set<Integer> activeInteractions = new HashSet<Integer>();
			for (Entry<Long, Set<Interaction>> entry: times.entrySet()){
				long time = entry.getKey();
				// remove finishing interactions
				for(Interaction interaction : entry.getValue()){
					if (interaction.getEndTime()==time){
						activeInteractions.remove(interaction.getId());
					}
				}
				// add new interactions:
				for(Interaction interaction : entry.getValue()){
					if (interaction.getStartTime()==time){
						activeInteractions.add(interaction.getId());
					}
				}
				// add excluding interactions to the set:
				excludingInteractions.addAll(getConflictingInteractions(activeInteractions, interactionMapping));
			}
		}
		
		
		return excludingInteractions;
	}
	
	
	private Set<SparseBitSet> getConflictingInteractions(Set<Integer> activeInteractions,
			Map<Integer, Interaction> interactionMapping) {
		Set<SparseBitSet> conflictingInteractionIds = new HashSet<SparseBitSet>();
		Map<String, SortedSet<Integer>> appearancesOfResources = new HashMap<String, SortedSet<Integer>>();
		for (int interaction : activeInteractions){
			for (String resource : interactionMapping.get(interaction).getResourceKeys()){
				addOccurrence(appearancesOfResources, resource, interaction);
			}
			addOccurrence(appearancesOfResources, interactionMapping.get(interaction).getInstanceKey(), interaction);
		}
		List<Entry<String,SortedSet<Integer>>> sortedEntries = new ArrayList<Entry<String,SortedSet<Integer>>>(appearancesOfResources.entrySet());
		// sort backwards:
		Collections.sort(sortedEntries, new Comparator<Entry<String,SortedSet<Integer>>>() {
			public int compare(Entry<String, SortedSet<Integer>> o1, Entry<String, SortedSet<Integer>> o2) {
				return Integer.valueOf(o2.getValue().size()).compareTo(o1.getValue().size());
			}
		});
		for (Entry<String, SortedSet<Integer>> entry : sortedEntries){
			SparseBitSet thisSet = new SparseBitSet();
			for (Integer i : entry.getValue()){
				thisSet.set(i);
			}
			boolean fullyContainedInOneOfTheExisting = false;
			for (SparseBitSet set : conflictingInteractionIds){
				SparseBitSet intersect = SparseBitSet.and(thisSet, set);
				if (SparseBitSet.andNot(set, intersect).isEmpty()){
					fullyContainedInOneOfTheExisting = true;
					break;
				}
			}
			if (!fullyContainedInOneOfTheExisting){
				conflictingInteractionIds.add(thisSet);
			}
		}
		return conflictingInteractionIds;
	}

	protected <E extends Object> void addOccurrence(Map<E, SortedSet<Integer>> appearances, E object, int id) {
		if (!appearances.containsKey(object)){
			appearances.put(object, new TreeSet<Integer>());
		}
		appearances.get(object).add(id);
	}

	private SortedMap<Long, Set<Interaction>> collectTimes(List<Interaction> list) {
		SortedMap<Long, Set<Interaction>> interactionsWithBoundariesAtTime = new TreeMap<Long, Set<Interaction>>();
		
		for (Interaction i : list){
			if (!interactionsWithBoundariesAtTime.containsKey(i.getStartTime())){
				interactionsWithBoundariesAtTime.put(i.getStartTime(), new HashSet<Interaction>());
			}
			interactionsWithBoundariesAtTime.get(i.getStartTime()).add(i);
			if (!interactionsWithBoundariesAtTime.containsKey(i.getEndTime())){
				interactionsWithBoundariesAtTime.put(i.getEndTime(), new HashSet<Interaction>());
			}
			interactionsWithBoundariesAtTime.get(i.getEndTime()).add(i);
		}
		return interactionsWithBoundariesAtTime;
	}

	private void addFinishingInteractions(Set<SensorInterval> activeRecords, Long time, List<Interaction> interactions) {
		Set<Set<SensorInterval>> subSets = StochasticNetUtils.generateAllSubsets(activeRecords);
		for (Set<SensorInterval> subset : subSets){
			if (subset.size()>=2){
				SortedSet<String> cases = getCaseResources(subset, false);
				SortedSet<String> resources = getCaseResources(subset, true);
				if (cases.size() == 1){
					String instance = cases.first();
					Interaction interaction = new Interaction(instance, resources, subset.iterator().next().getLocationKey(), getMaxStartTime(subset), time);
					if(interaction.getEndTime()-interaction.getStartTime() < 0){
						logger.error("Debug me!");
					}
					interaction.setId(interactions.size());
					interactions.add(interaction);
				}
			}
		}
	}
	private void addStartingInteractions(Set<SensorInterval> activeRecords, Long time, List<Interaction> interactions) {
		Set<Set<SensorInterval>> subSets = StochasticNetUtils.generateAllSubsets(activeRecords);
		for (Set<SensorInterval> subset : subSets){
			if (subset.size()>=2){
				SortedSet<String> cases = getCaseResources(subset, false);
				SortedSet<String> resources = getCaseResources(subset, true);
				if (cases.size() == 1){
					String instance = cases.first();
					Interaction interaction = new Interaction(instance, resources, subset.iterator().next().getLocationKey(), time, getMinEndTime(subset));
					if(interaction.getEndTime()-interaction.getStartTime() < 0){
						logger.error("Debug me!");
					}
					interaction.setId(interactions.size());
					interactions.add(interaction);
				}
			}
		}
	}

	private long getMaxStartTime(Set<SensorInterval> subset) {
		long maxStartTime = -1;
		for (SensorInterval interval : subset){
			maxStartTime = Math.max(maxStartTime, interval.getStartTime());
		}
		return maxStartTime;
	}
	private long getMinEndTime(Set<SensorInterval> subset) {
		long minEndTime = Long.MAX_VALUE;
		for (SensorInterval interval : subset){
			minEndTime = Math.min(minEndTime, interval.getEndTime());
		}
		return minEndTime;
	}

	/**
	 * Collects all resources that are cases (if !negation)
	 * Otherwise, collect all resources that are not cases. 
	 * We encode case resources by having a certain prefix.
	 * 
	 * @param subset
	 * @return
	 */
	private SortedSet<String> getCaseResources(Set<SensorInterval> subset, boolean negation) {
		SortedSet<String> cases = new TreeSet<String>();
		for (SensorInterval interval : subset){
			boolean isCaseResource = Person.isCaseResourceName(interval.getResourceKey()); 
			if (!negation && isCaseResource || negation && !isCaseResource){
				cases.add(interval.getResourceKey());
			}
		}
		return cases;
	}
	

//	private Set<SensorInterval> getInOrOutSet(Long time, SortedBag<SortedPair<Long, SensorInterval>> timedInteractions,
//			boolean entering) {
//		Iterator<SortedPair<Long, SensorInterval>> iter = timedInteractions.iterator();
//		long currentTime = -1;
//		Set<SensorInterval> intervals = new HashSet<SensorInterval>();
//		while (iter.hasNext()){
//			SortedPair<Long, SensorInterval> item = iter.next();
//			currentTime = item.getFirst();
//			if (currentTime == time){
//				boolean in = currentTime == item.getSecond().getStartTime();
//				if (entering && in || !entering && !in) {
//					intervals.add(item.getSecond());
//				}
//			}
//		}
//		return intervals;
//	}

	protected Set<Allocatable> addCompartment(int nRooms, int maxCapacity, Building hospital, String name, Set<Allocatable> allResources) {
		Compartment rooms = new Compartment(name+"s");
		Set<Allocatable> allocationRooms = new HashSet<Allocatable>();
		rooms.setParentLocation(hospital);
		// 
		for (int i = 0; i < nRooms; i++){
			Room room = new Room(name+"_"+i, maxCapacity);
			room.setParentLocation(rooms);
			allResources.add(room);
			allocationRooms.add(room);
		}
		return allocationRooms;
	}
}
