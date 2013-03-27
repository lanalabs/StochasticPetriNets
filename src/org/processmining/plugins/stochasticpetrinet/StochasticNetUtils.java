package org.processmining.plugins.stochasticpetrinet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.petrinet.InhibitorNet;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.ResetInhibitorNet;
import org.processmining.models.graphbased.directed.petrinet.ResetNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.processmining.models.semantics.petrinet.impl.StochasticNetSemanticsImpl;
import org.processmining.plugins.astar.petrinet.PetrinetReplayerILPRestrictedMoveModel;
import org.processmining.plugins.astar.petrinet.manifestreplay.CostBasedCompleteManifestParam;
import org.processmining.plugins.astar.petrinet.manifestreplay.ManifestFactory;
import org.processmining.plugins.astar.petrinet.manifestreplay.PNManifestFlattener;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.petrinet.manifestreplayer.EvClassPattern;
import org.processmining.plugins.petrinet.manifestreplayer.PNManifestReplayerParameter;
import org.processmining.plugins.petrinet.manifestreplayer.TransClass2PatternMap;
import org.processmining.plugins.petrinet.manifestreplayer.transclassifier.DefTransClassifier;
import org.processmining.plugins.petrinet.manifestreplayer.transclassifier.TransClass;
import org.processmining.plugins.petrinet.manifestreplayer.transclassifier.TransClasses;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.stochasticpetrinet.distribution.RProvider;
import org.processmining.plugins.stochasticpetrinet.distribution.SimpleHistogramDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.TruncatedDistributionFactory;

public class StochasticNetUtils {
	
	public static final String ITERATION_KEY = "k-fold";
	
	private static Map<PetrinetGraph, Marking> initialMarkings = new HashMap<PetrinetGraph, Marking>();
	private static Map<PetrinetGraph, Marking> finalMarkings = new HashMap<PetrinetGraph, Marking>();
	
	public static final Double[] UNIT_CONVERSION_FACTORS = new Double[]{1.,1000.,1000.*60,1000.*3600,1000.*3600*24,1000.*3600*24*365};
	public static final String[] UNIT_NAMES = new String[]{"milliseconds","seconds", "minutes", "hours", "days", "years"};
	
	public static final String SEPARATOR_STRING = "_ITERATION_";
	
	private static boolean initialized = false;
	
	/**
	 * Gets the initial marking, that is associated with the Petri Net
	 * Uses a cache to not bother with connections on multiple calls
	 * @param context
	 * @param petriNet
	 * @return
	 */
	public static Marking getInitialMarking(PluginContext context, PetrinetGraph petriNet) {
		return getInitialMarking(context, petriNet, true);
	}
	/**
	 * Gets a very simple mapping based on the naming of activities
	 * @param sNet
	 * @param log
	 * @return
	 */
	public static TransEvClassMapping getEvClassMapping(PetrinetGraph sNet, XLog log) {
		XEventClass evClassDummy = new XEventClass("DUMMY", -1);
		XEventClasses ecLog = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.STANDARD_CLASSIFIER).getEventClasses();
		Iterator<Transition> transIt;
		TransEvClassMapping mapping = new TransEvClassMapping(XLogInfoImpl.STANDARD_CLASSIFIER, evClassDummy);
		transIt =  sNet.getTransitions().iterator();
		while (transIt.hasNext()) {
			Transition trans = transIt.next();
			// search for event which starts with transition name
			for (XEventClass ec : ecLog.getClasses()) {
				String[] ecBaseParts = ec.getId().split("\\+|"+SEPARATOR_STRING);
				String transitionLabel = trans.getLabel().split(SEPARATOR_STRING)[0]; 
				String ecBaseName = ecBaseParts[0];
				if (ecBaseName.equals(transitionLabel)) {
					// found the one
					mapping.put(trans, ec);
				}
			}
			if (mapping.get(trans) == null){
				mapping.put(trans, evClassDummy);
			}
		}
		return mapping;
	}

	/**
	 * Gets the initial marking, that is associated with the Petri Net
	 * @param context
	 * @param petriNet
	 * @param useCache indicates, whether markings should be cached, or not.
	 * @return
	 */
	public static Marking getInitialMarking(PluginContext context, PetrinetGraph petriNet, boolean useCache) {
		Marking initialMarking = null;
		if (useCache && initialMarkings.containsKey(petriNet)){
			return initialMarkings.get(petriNet);
		}
		try { 
			InitialMarkingConnection imc = context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, petriNet);
			initialMarking = imc.getObjectWithRole(InitialMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained e) {
			e.printStackTrace();
			
			// creating initial marking with a token on each input place. 
			initialMarking = new Marking();
			for (Place p : petriNet.getPlaces()){
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = petriNet.getInEdges(p);
				if (inEdges == null || inEdges.size() == 0){
					initialMarking.add(p);
				}
			}
		}
		if (useCache && initialMarking != null){
			initialMarkings.put(petriNet, initialMarking);
		}
		return initialMarking;
	}
	
	public static Marking getFinalMarking(PluginContext context, PetrinetGraph petriNet){
		return getFinalMarking(context, petriNet, true);
	}
	
	/**
	 * Gets the final marking that is associated with the Petri Net
	 * @param context
	 * @param petriNet
	 * @return
	 */
	public static Marking getFinalMarking(PluginContext context, PetrinetGraph petriNet, boolean useCache) {
		Marking finalMarking = null;
		if (useCache && finalMarkings.containsKey(petriNet)){
			return finalMarkings.get(petriNet);
		}
		try { 
			FinalMarkingConnection imc = context.getConnectionManager().getFirstConnection(FinalMarkingConnection.class, context, petriNet);
			finalMarking = imc.getObjectWithRole(FinalMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained e) {
			e.printStackTrace();
			
			// creating final marking with a token on each output place. 
			finalMarking = new Marking();
			for (Place p : petriNet.getPlaces()){
				Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = petriNet.getOutEdges(p);
				if (outEdges == null || outEdges.size() == 0){
					finalMarking.add(p);
				}
			}
		}
		if (useCache && finalMarking != null){
			finalMarkings.put(petriNet, finalMarking);
		}
		return finalMarking;
	}
	/**
	 * Samples a value from the distribution
	 * @param distribution
	 * @param positiveConstraint sample should be bigger than this value (results in truncated distribution)
	 * @return
	 */
	public static double sampleWithConstraint(RealDistribution distribution, Random rand, double positiveConstraint) {
		double sample = positiveConstraint;
		if (Double.isInfinite(positiveConstraint) || positiveConstraint == Double.NEGATIVE_INFINITY){
			sample = distribution.sample();
		} else if (distribution instanceof SimpleHistogramDistribution){
			sample = ((SimpleHistogramDistribution)distribution).sample(positiveConstraint);
		} else {
			RealDistribution wrapper = TruncatedDistributionFactory.getConstrainedWrapper(distribution,positiveConstraint);
			sample = wrapper.sample();
		} 
		return sample;
//		
//		double sample = -1;
//		int tries = 0;
//		while (sample < positiveConstraint){
//			if (tries++ > MAX_RETRIES){
////				System.err.println("Maximum sample retries reached! Falling back to manual sampling");
//				// fall back to sampling via inverse distribution
//				double threshold = distribution.cumulativeProbability(positiveConstraint);
//				if (threshold == 1){
//					// TODO: some other sampling method!
//				}
//				double span = 1-threshold;
//				double nextVal = threshold+rand.nextDouble()*span;
////				if (nextVal == 1){
////					// do NOT return Infinity
////					nextVal = 0.99999999999;
////				}
//				return distribution.inverseCumulativeProbability(nextVal);
//			}
//			sample = distribution.sample();
//		}
//		
//		return sample;
	}
	
	/**
	 * Takes a double[] of weights and selects an item randomly according to a random number generator
	 * such that each item in the array has a probability of (weight of item / sum of weights);
	 * 
	 * @param weights double[] containing weights
	 * @return index randomly chosen among the items 
	 */
	public static int getRandomIndex(double[] weights, Random random){
		if (weights.length == 1){
			return 0; // only one option
		}
		double[] weightsCumulative = new double[weights.length];
		weightsCumulative[0] = 0;
		int i = 0;
		for (double d : weights){
			int lastindex = i == 0?0:i-1; 
			weightsCumulative[i] = weightsCumulative[lastindex]+d;
			i++;
		}
		double choice = random.nextDouble()*weightsCumulative[weights.length-1];
		int index = 0;
		
		double lower = 0;
		double upper = weightsCumulative[index];
		boolean found = false;
		while(!found){
			if (choice >= lower && choice < upper){
				found = true;
			} else {
				index++;
				lower = upper;
				upper = weightsCumulative[index];
			}
		}
		return index;
	}
	
	/**
	 * Gets a regular semantic for a Petri Net
	 * @param petriNet
	 * @return
	 */
	public static Semantics<Marking, Transition> getSemantics(PetrinetGraph petriNet) {
		Semantics<Marking, Transition> semantics = null;
		if (petriNet instanceof StochasticNet){
			semantics = new StochasticNetSemanticsImpl();
		} else if (petriNet instanceof ResetInhibitorNet) {
			semantics = PetrinetSemanticsFactory.regularResetInhibitorNetSemantics(ResetInhibitorNet.class);
		} else if (petriNet instanceof InhibitorNet) {
			semantics = PetrinetSemanticsFactory.regularInhibitorNetSemantics(InhibitorNet.class);
		} else if (petriNet instanceof ResetNet) {
			semantics = PetrinetSemanticsFactory.regularResetNetSemantics(ResetNet.class);
		} else if (petriNet instanceof Petrinet){
			semantics = PetrinetSemanticsFactory.regularPetrinetSemantics(Petrinet.class);
		}
		return semantics;
	}
	
	/**
	 * Filter traces based on modulo
	 * Idea is to split log in two disjoint sets: the training set and the test set.
	 * 
	 * @param log the log to be partitioned
	 * @param kFoldCount the fold count
	 * @param k the iteration number
	 * @param returnModulo if true -> returns only the fraction, where the modulo by kFoldCount is k (for the test set)
	 * 					   if false -> returns the rest, where the modulo by kFoldCount is not k (for the training set)
	 * @return
	 */
	public static XLog filterTracesBasedOnModulo(XLog log, int kFoldCount, int k, boolean returnModulo) {
		XLog result = XFactoryRegistry.instance().currentDefault().createLog(log.getAttributes());
		for (int i = 0; i < log.size(); i++){
			boolean isModulo = i % kFoldCount == k; 
			if (isModulo && returnModulo || !isModulo && !returnModulo){
				XTrace t = log.get(i);
				XTrace copy = XFactoryRegistry.instance().currentDefault().createTrace(t.getAttributes());
				for (XEvent e : t) {
					// keep this one
					XEvent copyEvent = XFactoryRegistry.instance().currentDefault().createEvent(e.getAttributes());
					copy.add(copyEvent);
				}
				result.add(copy);
			}
		}
		return result;
	}
	
	/**
	 * Gets the mean duration of all the traces in the log in milliseconds
	 *  @param log
	 * @param timeUnitFactor
	 * @return
	 */
	public static double getMeanDuration(XLog log) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (XTrace t : log){
			XEvent firstEvent = t.get(0);
			XEvent lastEvent = t.get(t.size()-1);
			double duration = XTimeExtension.instance().extractTimestamp(lastEvent).getTime()-XTimeExtension.instance().extractTimestamp(firstEvent).getTime();
			stats.addValue(duration);
		}
		return stats.getMean();
	}
	
	/**
	 * Replays a log on a model based on selected parameters.
	 * Returns either a {@link PNRepResult} replay result, or converts that into a {@link Manifest} (parameter getManifest)
	 * @param net the Petri net that captures the execution possibilities
	 * @param log the Log that contains time information (time stamps) for performance analysis
	 * @param parameters different replay parameters.
	 * @param getManifest
	 * @return
	 */
	public static Object replayLog(UIPluginContext context, PetrinetGraph net, XLog log, boolean getManifest) {
		PNManifestReplayerParameter parameters = getParameters(log, getEvClassMapping(net, log), net, StochasticNetUtils.getInitialMarking(context, net), StochasticNetUtils.getFinalMarking(context, net), XLogInfoImpl.STANDARD_CLASSIFIER);
		return replayLog(context, net, log, parameters, getManifest);
	}
	
	public static Object replayLog(UIPluginContext context, PetrinetGraph net, XLog log, PNManifestReplayerParameter parameters, boolean getManifest) {

		/**
		 * Local variables
		 */
		PNManifestFlattener flattener = new PNManifestFlattener(net, parameters); // stores everything about petri net

		/**
		 * To Debug: print the flattened petri net
		 */
		//ProvidedObjectHelper.publish(context, "Flattened " + net.getLabel(), (ResetInhibitorNet) flattener.getNet(), ResetInhibitorNet.class, true);

		// create parameter
		CostBasedCompleteManifestParam parameter = new CostBasedCompleteManifestParam(flattener.getMapEvClass2Cost(),
				flattener.getMapTrans2Cost(), flattener.getMapSync2Cost(), flattener.getInitMarking(),
				flattener.getFinalMarkings(), parameters.getMaxNumOfStates(), 
				flattener.getFragmentTrans());
		parameter.setGUIMode(false);
		parameter.setCreateConn(false);

		// select algorithm with ILP
		PetrinetReplayerILPRestrictedMoveModel replayWithILP = new PetrinetReplayerILPRestrictedMoveModel();
		if (!initialized){
			replayWithILP.isAllReqSatisfied(null, net, log, flattener.getMap(), parameter);
			initialized = true;
		}
		
		PNRepResult pnRepResult  = replayWithILP.replayLog(context, flattener.getNet(), log, flattener.getMap(), parameter);
		if (getManifest){
			// translate result back to desired output
			try {
				Manifest manifestation = ManifestFactory.construct(net, parameters.getInitMarking(),
							parameters.getFinalMarkings(), log, flattener, pnRepResult, parameters.getMapping());
				return manifestation;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		} else {
			return pnRepResult;
		}
	}
	public static PNManifestReplayerParameter getParameters(XLog originalTrace, TransEvClassMapping mapping, PetrinetGraph net,
			Marking initialMarking, Marking finalMarking, XEventClassifier classifier) {
		// event classes, costs
		Map<XEventClass, Integer> mapEvClass2Cost = new HashMap<XEventClass, Integer>();
		for ( XEventClass c : mapping.values()) {
			mapEvClass2Cost.put(c, 1);
		}
		// transition classes
		Map<TransClass, Integer> trans2Cost = new HashMap<TransClass, Integer>();
		Map<TransClass, Integer> transSync2Cost = new HashMap<TransClass, Integer>();
		TransClasses transClasses = new TransClasses(net, new DefTransClassifier());
		for (TransClass tc : transClasses.getTransClasses()) {
			// check if tc corresponds with invisible transition
			boolean check =  getTransitionClassIsInvisible(tc, net);
			if (!check) {
				trans2Cost.put(tc, 1);
			}
			else {
				trans2Cost.put(tc, 0);
			}
			transSync2Cost.put(tc, 0);
		}
		
		Map<TransClass, Set<EvClassPattern>> mapTCtiECP = new HashMap<TransClass, Set<EvClassPattern>>();
		// fill map, for each transition there is exactly one event class (pattern)
		
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(originalTrace, classifier);
		List<XEventClass> evClassCol = new ArrayList<XEventClass>(logInfo.getEventClasses().getClasses());
		
		for (XEventClass c : evClassCol){
			if(mapping.containsValue(c)){
				for (Entry<Transition, XEventClass> entry : mapping.entrySet()){
					if (entry.getValue().equals(c)){
						TransClass transClassT = transClasses.getClassOf(entry.getKey());
						EvClassPattern patt = new EvClassPattern();
						patt.add(c);
						Set<EvClassPattern> setPatt = new HashSet<EvClassPattern>();
						setPatt.add(patt);
						mapTCtiECP.put(transClassT, setPatt);
						break;
					}
				}
			}
		}
				
		TransClass2PatternMap patternMap = new TransClass2PatternMap(originalTrace, net, classifier, transClasses, mapTCtiECP);

		PNManifestReplayerParameter parameters = new PNManifestReplayerParameter();
		parameters.setInitMarking(initialMarking);
		parameters.setFinalMarkings(new Marking[]{finalMarking});
		parameters.setGUIMode(false);
		parameters.setMapEvClass2Cost(mapEvClass2Cost);
		parameters.setMaxNumOfStates(50000);
		parameters.setTrans2Cost(trans2Cost);
		parameters.setTransSync2Cost(transSync2Cost);
		parameters.setMapping(patternMap);
		return parameters;
	}
	
	/**
	 * Checks whether at least one of the transitions of the transition class are invisible.
	 *  
	 * @param tc {@link TransClass}
	 * @param net Petri net graph {@link PetrinetGraph}
	 * @return
	 */
	public static boolean getTransitionClassIsInvisible(TransClass tc, PetrinetGraph net) {
		String id = tc.getId();
		for (Transition t : net.getTransitions()) {
			if (id.startsWith(t.getLabel()) && t.isInvisible()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks, whether the R-engine is accessible over JRI
	 * @see {@link http://stats.math.uni-augsburg.de/JRI/}
	 * @return
	 */
	public static boolean splinesSupported(){
		boolean jriAvaialble = false;
		try{
			jriAvaialble = RProvider.getEngineAvailable();
		} catch (UnsatisfiedLinkError error){
			System.out.println(error.getMessage());
		}
		return jriAvaialble;
	}
	
	/**
	 * Convenience utility function to write a string to a file specified by a file name.
	 * @param string
	 * @param fileName
	 */
	public static void writeStringToFile(String string, String fileName){
		try {
			File file = new File(fileName);
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			writer.write(string);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
