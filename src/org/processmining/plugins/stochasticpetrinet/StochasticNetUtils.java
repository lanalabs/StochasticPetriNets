package org.processmining.plugins.stochasticpetrinet;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.FastMath;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XOrganizationalExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.deckfour.xes.out.XesXmlSerializer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.packages.PackageManager;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.DirectedGraphElement;
import org.processmining.models.graphbased.directed.petrinet.InhibitorNet;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.ResetInhibitorNet;
import org.processmining.models.graphbased.directed.petrinet.ResetNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetImpl;
import org.processmining.models.graphbased.directed.petrinet.impl.ToStochasticNet;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.processmining.plugins.alignment.override.PrecisionAligner;
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
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.pnalignanalysis.conformance.AlignmentPrecGenRes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.stochasticpetrinet.distribution.DiracDeltaDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.RProvider;
import org.processmining.plugins.stochasticpetrinet.distribution.SimpleHistogramDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.TruncatedDistributionFactory;
import org.processmining.plugins.stochasticpetrinet.prediction.TimePredictor;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;
import org.rosuda.JRI.Rengine;
import org.utils.datastructures.ComparablePair;
import org.utils.datastructures.SortedSetComparator;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;

public class StochasticNetUtils {
	
	public static final String ITERATION_KEY = "k-fold";
	public static final String TIME_ATTRIBUTE_KEY = "time:timestamp";
	
	public static final String PRECISION_MEASURE = "Precision";
	public static final String GENERALIZATION_MEASURE = "Generalization";
	
	private static int cacheSize = 100;
	private static boolean cacheEnabled = false;
	
	private static Random random = new Random(1);
	public static double getRandomDouble(){
		return random.nextDouble();
	}
	public static int getRandomInt(int n){
		return random.nextInt(n);
	}
	public static void setRandomSeed(long seed){
		random.setSeed(seed);
	}
	
	private static Map<PetrinetGraph, Marking> initialMarkings = new LinkedHashMap<PetrinetGraph, Marking>() {
		private static final long serialVersionUID = 6745901965568659463L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<PetrinetGraph, Marking> eldest) {
			return size() > StochasticNetUtils.cacheSize;
		}
	}; 
	private static Map<PetrinetGraph, Marking> finalMarkings = new LinkedHashMap<PetrinetGraph, Marking>() {
		private static final long serialVersionUID = -6216937984123036411L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<PetrinetGraph, Marking> eldest) {
			return size() > StochasticNetUtils.cacheSize;
		}
	}; 
	
	public static void setCacheEnabled(boolean enabled){
		cacheEnabled = enabled;
	}
	
	public static <E extends Comparable<E>> int compareSortedSets(SortedSet<E> first, SortedSet<E> second){
		SortedSetComparator<E> comparator = new SortedSetComparator<E>();
		return comparator.compare(first, second);
	}
	
	/**
	 * Computes entropy of a discrete probability distribution. 
	 * @param probs a map assigning probabilities to objects. Note that probs.values() should sum to 1.
	 * @return the entropy of the map.
	 */
	public static <E extends Object> double getEntropy(Map<E, Double> probs){
		double h = 0;
		for (E key :  probs.keySet()){
			double p = probs.get(key);
			h -= p*FastMath.log(p);
		}
		return h;
	}
	
//	/**
//	 * indexed array of conversion factors
//	 * [0] = ms
//	 * [1] = s
//	 * [2] = min
//	 * [3] = hours
//	 * [4] = days
//	 * [5] = years
//	 */
//	public static final Double[] UNIT_CONVERSION_FACTORS = new Double[]{1.,1000.,1000.*60,1000.*3600,1000.*3600*24,1000.*3600*24*365};
//	public static final String[] UNIT_NAMES = new String[]{"milliseconds","seconds", "minutes", "hours", "days", "years"};
	
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
		return getInitialMarking(context, petriNet, cacheEnabled);
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
				String label = trans.getLabel()==null?"":trans.getLabel();
				String transitionLabel = label.split(SEPARATOR_STRING)[0]; 
				String ecBaseName = ecBaseParts[0];
				if (ecBaseName.equals(transitionLabel) || (ecBaseName+"+complete").equals(transitionLabel)) {
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
			if (context == null || context.getConnectionManager() == null){
				throw new ConnectionCannotBeObtained("No plugin context available!", InitialMarkingConnection.class);
			}
			InitialMarkingConnection imc = context.getConnectionManager().getFirstConnection(InitialMarkingConnection.class, context, petriNet);
			initialMarking = imc.getObjectWithRole(InitialMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained e) {
//			e.printStackTrace();
//			System.err.println("Unable to get initial marking connection -> setting a default one (each place without input gets a token).");
			
			initialMarking = getDefaultInitialMarking(petriNet);
		}
		if (useCache && initialMarking != null){
			initialMarkings.put(petriNet, initialMarking);
		}
		return initialMarking;
	}
	
	public static Marking getDefaultInitialMarking(PetrinetGraph petriNet) {
		Marking initialMarking;
		// creating initial marking with a token on each input place. 
		initialMarking = new Marking();
		for (Place p : petriNet.getPlaces()){
			Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = petriNet.getInEdges(p);
			if (inEdges == null || inEdges.size() == 0){
				initialMarking.add(p);
			}
		}
		return initialMarking;
	}
	
	public static Marking getFinalMarking(PluginContext context, PetrinetGraph petriNet){
		return getFinalMarking(context, petriNet, cacheEnabled);
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
			if (context == null || context.getConnectionManager() == null){
				throw new ConnectionCannotBeObtained("No plugin context available!", FinalMarkingConnection.class);
			}
			FinalMarkingConnection imc = context.getConnectionManager().getFirstConnection(FinalMarkingConnection.class, context, petriNet);
			finalMarking = imc.getObjectWithRole(FinalMarkingConnection.MARKING);
		} catch (ConnectionCannotBeObtained e) {
//			System.err.println("Unable to get final marking connection -> setting a default one.");
			//e.printStackTrace();
			
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
	
	public static void cacheFinalMarking(PetrinetGraph net, Marking finalMarking){
		if (finalMarking != null)
			finalMarkings.put(net, finalMarking);
	}
	
	public static void cacheInitialMarking(PetrinetGraph net, Marking initialMarking){
		if (initialMarking != null)
			initialMarkings.put(net, initialMarking);
	}
	
	/**
	 * Samples a value from the distribution of a timed transition
	 * @param transition {@link TimedTransition}
	 * @param positiveConstraint sample should be bigger than this value (results in truncated distribution)
	 * @return
	 */
	public static double sampleWithConstraint(TimedTransition transition, double positiveConstraint) {
//		long now = System.currentTimeMillis();
		double sample = positiveConstraint;
		RealDistribution distribution = transition.getDistribution();
		String key = transition.getLabel()+"_"+transition.getDistributionType()+"_"+positiveConstraint;
		// try to get distribution from cache:
		if (transition.getDistributionType().equals(DistributionType.GAUSSIAN_KERNEL) && useCache && distributionCache.containsKey(key)){
			sample = distributionCache.get(key).sample();
		} else if (transition.getDistributionType().equals(DistributionType.IMMEDIATE)){
			return 0.0;
		} else {
			sample = sampleWithConstraint(distribution, key, positiveConstraint);
		}
//		long after = System.currentTimeMillis();
//		System.out.println("sampling with constraint took "+(after-now)+" ms");
		return sample;
	}
	
	/**
	 * Samples a value from the distribution
	 * @param distribution {@link RealDistribution} sampling distribution
	 * @param cacheLabel String denoting the key for this distribution to be cached
	 * @param positiveConstraint sample should be bigger than this value (results in truncated distribution)
	 * @return
	 */
	public static double sampleWithConstraint(RealDistribution distribution, String cacheLabel, double positiveConstraint) {
		double sample;
		if (Double.isInfinite(positiveConstraint) || positiveConstraint == Double.NEGATIVE_INFINITY){
			sample = distribution.sample();
		} else if (distribution instanceof SimpleHistogramDistribution){
			long nanos = System.nanoTime();
			sample = ((SimpleHistogramDistribution)distribution).sample(positiveConstraint);
			nanos = System.nanoTime()-nanos;
			if (nanos > 100000){
				System.out.println("Took "+(nanos/1000)+"ms to sample from histogram");
			}
		} else if (distribution instanceof DiracDeltaDistribution){
			sample = distribution.sample();
			if (positiveConstraint > sample){
				sample = positiveConstraint;
			}
		} else if (distribution instanceof UniformRealDistribution) {
			if (distribution.getSupportLowerBound() < positiveConstraint){
				if (distribution.getSupportUpperBound() < positiveConstraint){
					sample = positiveConstraint;
				} else {
					RealDistribution constrainedDist = new UniformRealDistribution(positiveConstraint, distribution.getSupportUpperBound());
					sample = constrainedDist.sample();
				}
			} else {
				sample = distribution.sample();
			}
		} else if (distribution instanceof ExponentialDistribution){
			double constraint = Math.max(0, positiveConstraint);
			sample = distribution.sample() + constraint;
		} else {
			// be optimistic and try to sample 100 times:
			for (int i = 0; i < 100; i++){
				sample = distribution.sample();
				if (sample > positiveConstraint){
					return sample;
				}
			}
			// fall back to old method
			RealDistribution wrapper = TruncatedDistributionFactory.getConstrainedWrapper(distribution,positiveConstraint);
			if (useCache){
				// store distribution in cache
				distributionCache.put(cacheLabel, wrapper);
//				System.out.println("caching distribution for "+key+". caching "+distributionCache.size()+" distributions");
			}
			sample = wrapper.sample();
		} 
		return sample;
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
			semantics = new EfficientStochasticNetSemanticsImpl();
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
	 * Generates all subsets of a set.
	 * @param original
	 * @return
	 */
	public static <T> Set<Set<T>> generateAllSubsets(Set<T> original) {
	    Set<Set<T>> allSubsets = new HashSet<Set<T>>();

	    allSubsets.add(new HashSet<T>()); //Add empty set.

	    for (T element : original) {
	        // Copy subsets so we can iterate over them without ConcurrentModificationException
	        Set<Set<T>> tempClone = new HashSet<Set<T>>(allSubsets);

	        // All element to all subsets of the current power set.
	        for (Set<T> subset : tempClone) {
	            Set<T> extended = new HashSet<T>(subset);
	            extended.add(element);
	            allSubsets.add(extended);
	        }
	    }
	    return allSubsets;
	}
	
	public static <T> Set<Set<T>> generateAllSubsetsOfSize(Set<T> original, int minSize, int maxSize) {
		Set<Set<T>> allSubsets = new HashSet<Set<T>>();
		if (maxSize == 0){
			return allSubsets;
		}

	    allSubsets.add(new HashSet<T>()); //Add empty set.

	    for (T element : original) {
	        // Copy subsets so we can iterate over them without ConcurrentModificationException
	        Set<Set<T>> tempClone = new HashSet<Set<T>>(allSubsets);

	        // All element to all subsets of the current power set.
	        for (Set<T> subset : tempClone) {
	            Set<T> extended = new HashSet<T>(subset);
	            extended.add(element);
	            if (extended.size() <= maxSize){
	            	allSubsets.add(extended);
	            }
	        }
	    }
	    Set<Set<T>> restrictedSubsets = new HashSet<Set<T>>();
	    Iterator<Set<T>> iter = allSubsets.iterator();
	    while(iter.hasNext()){
	    	Set<T> subset = iter.next();
	    	if (subset.size() >= minSize && subset.size() <= maxSize){
	    		restrictedSubsets.add(subset);
	    	}
	    }
	    return restrictedSubsets;
	}
	
	public static <T> Set<Set<T>> generateCrossProduct(Set<Set<T>> setA,
			Set<Set<T>> setB) {
		if (setA.isEmpty()) {
			return setB;
		}
		if (setB.isEmpty()){
			return setA;
		}
		Set<Set<T>> allOptions = new HashSet<Set<T>>();
		for (Set<T> a : setA){
			Set<T> product = new HashSet<T>(a);
			for (Set<T> b : setB){
				product = new HashSet<T>(a);
				product.addAll(b);
				allOptions.add(product);
			}
		}
		return allOptions;
	}
	
	/**
	 * This method extracts the portion of the trace that has happened before the timeUntil parameter.
	 * 
	 * Assumption: The events in the trace are ordered incrementally by their time!
	 * 
	 * @param trace the original trace containing a number of events
	 * @param timeUntil only events up to this point in time (inclusive) are added to the resulting sub-trace
	 * @return the sub-trace with events filtered to be less or equal to timeUntil 
	 */
	public static XTrace getSubTrace(XTrace trace, long timeUntil) {
		XTrace subTraceUntilTime = new XTraceImpl(trace.getAttributes());
		int traceIndex = 0;
		
		XEvent event = trace.get(traceIndex++);
		long lastEventTime = XTimeExtension.instance().extractTimestamp(event).getTime();
		if (lastEventTime>timeUntil){
			throw new IllegalArgumentException("The trace starts later than allowed by the timeUntil parameter!");
		}
		
		while (lastEventTime <= timeUntil){
			subTraceUntilTime.add(event);
			
			if (traceIndex < trace.size()){
				event = trace.get(traceIndex++);
				lastEventTime = XTimeExtension.instance().extractTimestamp(event).getTime();
			} else {
				lastEventTime = Long.MAX_VALUE;
			}
		}
		return subTraceUntilTime;
	}
	
	/**
	 * Gets the mean duration of all the traces in the log in milliseconds
	 *  @param log
	 * @param timeUnitFactor
	 * @return
	 */
	public static double getMeanDuration(XLog log) {
		DescriptiveStatistics stats = getDurationsStats(log);
		return stats.getMean();
	}
	
	public static DescriptiveStatistics getDurationsStats(XLog log) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (XTrace t : log){
			XEvent firstEvent = t.get(0);
			XEvent lastEvent = t.get(t.size()-1);
			double duration = XTimeExtension.instance().extractTimestamp(lastEvent).getTime()-XTimeExtension.instance().extractTimestamp(firstEvent).getTime();
			stats.addValue(duration);
		}
		return stats;
	}
	/**
	 * Gets the mean duration of the model by a simple simulation.
	 * TODO: Replace with analytical calculations, as for example done in the tool ORIS (www.oris-tool.org)
	 * @param net
	 * @param initialMarking
	 * @return
	 */
	public static double getMeanDuration(StochasticNet net, Marking initialMarking){
		TimePredictor predictor = new TimePredictor(false);
		XTrace emptyTrace = XFactoryRegistry.instance().currentDefault().createTrace();
		Pair<Double, Double> prediction = predictor.predict(net, emptyTrace, new Date(0), initialMarking);
		return prediction.getFirst();
	}
	
	public static double getUpperBoundDuration(StochasticNet net, Marking initialMarking) {
		TimePredictor predictor = new TimePredictor(false);
		XTrace emptyTrace = XFactoryRegistry.instance().currentDefault().createTrace();
		Pair<Double, Double> prediction = predictor.predict(net, emptyTrace, new Date(0), initialMarking);
		return prediction.getFirst()+prediction.getSecond()/2; // add half of the confidence interval to the mean to get a reasonable upper bound
	}
	
	/**
	 * Replays a log on a model based on selected parameters.
	 * Returns either a {@link PNRepResult} replay result, or converts that into a {@link Manifest} (parameter getManifest)
	 * @param net the Petri net that captures the execution possibilities
	 * @param log the Log that contains time information (time stamps) for performance analysis
	 * @param parameters different replay parameters.
	 * @param getManifest
	 * @param addSmallDeltaCosts specifies, if a small cost should be added to increase the cost of uncommon transitions 
	 * (favors alignment containing more frequent activities)
	 * @return {@link PNRepResult} or {@link Manifest}, depending on the flag getManifest. 
	 */
	public static Object replayLog(PluginContext context, PetrinetGraph net, XLog log, boolean getManifest, boolean addSmallDeltaCosts) {
		TransEvClassMapping transitionEventClassMap = getEvClassMapping(net, log);
		TransClasses transClasses = new TransClasses(net, new DefTransClassifier());
		PNManifestReplayerParameter parameters = getParameters(log, transitionEventClassMap, net, StochasticNetUtils.getInitialMarking(context, net), StochasticNetUtils.getFinalMarking(context, net), XLogInfoImpl.STANDARD_CLASSIFIER, transClasses);
		if (addSmallDeltaCosts){
			// update costs accordingly (use a minimum cost and add a little for frequent transitions, and a bit more to infrequent  
			Map<Integer, Set<XEventClass>> sortedCount = getEventClassCountsInLog(log);
			int i = sortedCount.keySet().size();
			int highestCost = 2*i+100;
			// add highest costs to infrequent activities and less to frequent activities, but maintain that 2* cost > highestCost, i.e., 
			// to use two frequent transitions should be always more expensive than to select one less frequent transition! (base this on absolute occurrences in the log)
			for (Integer count : sortedCount.keySet()){
				int cost = highestCost + --i;
				assert(cost > 0);
				assert(2 * cost > highestCost);
				for (XEventClass eClass : sortedCount.get(count)){
					parameters.getMapEvClass2Cost().put(eClass, cost);
					if(transitionEventClassMap.containsValue(eClass)){
						for (Entry<Transition, XEventClass> entry : transitionEventClassMap.entrySet()){
							if (entry.getValue().equals(eClass)){
								TransClass transClassT = transClasses.getClassOf(entry.getKey());
								parameters.getTransClass2Cost().put(transClassT,cost);
							}
						}
					}
				}
			}
//			System.out.println("------ Parameters --------");
//			System.out.println(parameters.getMapEvClass2Cost());
//			System.out.println(parameters.getTransClass2Cost());
		}
		return replayLog(context, net, log, parameters, getManifest);
	}
	
	public static SyncReplayResult replayTrace(XLog originalTrace, TransEvClassMapping mapping, Petrinet net, Marking initialMarking, Marking finalMarking, XEventClassifier classifier) throws Exception {
		PNRepResult repResult = replayLogWithMapping(originalTrace, mapping, net, initialMarking, finalMarking, classifier);
		if (repResult.size() > 0){
			SyncReplayResult result = repResult.first();
			return result;
		}
		throw new IllegalArgumentException("Could not replay trace on Model:\n"+debugTrace(originalTrace.get(0)));
	}
	protected static PNRepResult replayLogWithMapping(XLog originalTrace, TransEvClassMapping mapping, Petrinet net,
			Marking initialMarking, Marking finalMarking, XEventClassifier classifier) {
		TransClasses transClasses = new TransClasses(net, new DefTransClassifier());
		PNManifestReplayerParameter parameters = getParameters(originalTrace, mapping, net, initialMarking,	finalMarking, classifier, transClasses);
		PNRepResult repResult =  (PNRepResult) replayLog(null, net, originalTrace, parameters, false);
		return repResult;
	}
	
	public static String debugTrace(XTrace trace){
		DateFormat format = new SimpleDateFormat();
		String s = "";
		for (XEvent e : trace){
			if (!s.isEmpty()){
				s += ", ";
			}
			Date eventTime = getTraceDate(e);
			s += XConceptExtension.instance().extractName(e);
			if (XLifecycleExtension.instance().extractTransition(e)!=null){
				s+= "+"+XLifecycleExtension.instance().extractTransition(e);
			}
			if (XOrganizationalExtension.instance().extractResource(e)!=null){
				s+= " by "+XOrganizationalExtension.instance().extractResource(e);
			}
			XAttribute location = e.getAttributes().get(PNSimulator.LOCATION_ROOM);
			if (location != null){
				s+= " in "+location.toString();
			}
			if (eventTime != null){
				s += "("+format.format(eventTime)+")"; 
			}
		}
		return s;
	}
	
	private static Map<Integer, Set<XEventClass>> getEventClassCountsInLog(XLog log) {
		XLogInfo logInfo = XLogInfoImpl.create(log, XLogInfoImpl.STANDARD_CLASSIFIER);
		XEventClasses eventClasses = logInfo.getEventClasses();
		Map<XEventClass, Integer> eventCounts = new HashMap<XEventClass, Integer>();
		
		for (XTrace trace : log){
			for (XEvent e : trace){
				XEventClass eventClass = eventClasses.getClassOf(e);
				if (!eventCounts.containsKey(eventClass)){
					eventCounts.put(eventClass, 1);
				} else {
					eventCounts.put(eventClass, eventCounts.get(eventClass)+1);
				}
			}
		}
		Map<Integer, Set<XEventClass>> sortedEventClasses = new TreeMap<Integer, Set<XEventClass>>();
		for (XEventClass eClass : eventCounts.keySet()){
			Integer eventCount = eventCounts.get(eClass); 
			if (!sortedEventClasses.containsKey(eventCount)){
				sortedEventClasses.put(eventCount, new HashSet<XEventClass>());
			}
			sortedEventClasses.get(eventCount).add(eClass);
		}
		return sortedEventClasses;
	}
	public static Object replayLog(PluginContext context, PetrinetGraph net, XLog log, PNManifestReplayerParameter parameters, boolean getManifest) {

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
			PackageManager.getInstance().setAutoUpdate(false);
			replayWithILP.isAllReqSatisfied(null, net, log, flattener.getMap(), parameter);
			initialized = true;
		}
		ResetInhibitorNet riNet = flattener.getNet();
		for (Transition tr : riNet.getTransitions()){
			if (flattener.getOrigTransFor(tr).isInvisible()){
				tr.setInvisible(true);
			}
		}
		
		
		PNRepResult pnRepResult  = replayWithILP.replayLog(context, flattener.getNet(), log, flattener.getMap(), parameter);
		
		try{
			PrecisionAligner aligner = new PrecisionAligner();
			AlignmentPrecGenRes result = aligner.measureConformanceAssumingCorrectAlignment(context, flattener.getMap(), pnRepResult, flattener.getNet(), flattener.getInitMarking(), false);
			
			pnRepResult.getInfo().put(PRECISION_MEASURE, result.getPrecision());
			pnRepResult.getInfo().put(GENERALIZATION_MEASURE, result.getGeneralization());
		} catch (Exception e){
			e.printStackTrace();
		}
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
			Marking initialMarking, Marking finalMarking, XEventClassifier classifier, TransClasses transClasses) {
		// event classes, costs
		Map<XEventClass, Integer> mapEvClass2Cost = new HashMap<XEventClass, Integer>();
		XEventClasses ecLog = XLogInfoFactory.createLogInfo(originalTrace, XLogInfoImpl.STANDARD_CLASSIFIER).getEventClasses();
		for ( XEventClass c : ecLog.getClasses()) {
			mapEvClass2Cost.put(c, 100);
		}
		// transition classes
		Map<TransClass, Integer> trans2Cost = new HashMap<TransClass, Integer>();
		Map<TransClass, Integer> transSync2Cost = new HashMap<TransClass, Integer>();
		for (TransClass tc : transClasses.getTransClasses()) {
			// check if tc corresponds with invisible transition
			boolean check =  getTransitionClassIsInvisible(tc, net);
			if (!check) {
				trans2Cost.put(tc, 100);
			}
			else {
				trans2Cost.put(tc, 2);
			}
			transSync2Cost.put(tc, 1);
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
		boolean jriAvailable = false;
		try{
			// check if native binaries of jri are available
			jriAvailable = RProvider.getJRIAvailable();
			
			// if so, check, if we can start up an R engine:
			if (jriAvailable){
				Rengine engine = RProvider.getEngine();
				jriAvailable = jriAvailable && engine != null;
			}
		} catch (UnsatisfiedLinkError error){
			System.out.println(error.getMessage());
		} catch (UnsupportedOperationException e){
			jriAvailable = false;
		}
		return jriAvailable;
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
	
	/**
	 * Creates a {@link JLabel} acting as a clickable link
	 * code from http://stackoverflow.com/questions/527719/how-to-add-hyperlink-in-jlabel
	 * 
	 * @param text the title / content of the link
	 * @param url the URL that will be opened in the system browser
	 * @param toolTip the tooltip of the 
	 * @return a JLabel
	 */
	public static JLabel linkify(final String text, String url, String toolTip) {
		URI temp = null;
		try {
			temp = new URI(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		final URI uri = temp;
		final JLabel link = new JLabel();
		link.setText("<HTML><FONT color=\"#000099\">" + text + "</FONT></HTML>");
		if (!toolTip.equals(""))
			link.setToolTipText(toolTip);
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.addMouseListener(new MouseListener() {
			public void mouseExited(MouseEvent arg0) {
				link.setText("<HTML><FONT color=\"#000099\">" + text + "</FONT></HTML>");
			}

			public void mouseEntered(MouseEvent arg0) {
				link.setText("<HTML><FONT color=\"#000099\"><U>" + text + "</U></FONT></HTML>");
			}

			public void mouseClicked(MouseEvent arg0) {
				if (Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().browse(uri);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					JOptionPane.showMessageDialog(null, "Could not open link.");
				}
			}
			public void mousePressed(MouseEvent e) {
			}
			public void mouseReleased(MouseEvent e) {
			}
		});
		return link;
	}
	
	/**
	 * Converts any advanced Petri net into a plain net -> (useful for stochastic nets, especially)
	 * 
	 * @param context plugin context
	 * @param net the net
	 * @return
	 */
	public static Petrinet getPlainNet(UIPluginContext context, Petrinet net) {
		Petrinet plainNet;
		Marking initialMarking = getInitialMarking(context, net);
		
		Map<DirectedGraphElement, DirectedGraphElement> mapping = new HashMap<DirectedGraphElement, DirectedGraphElement>();
		plainNet = new PetrinetImpl(net.getLabel());
		for(Place p : net.getPlaces()){
			Place pNew = plainNet.addPlace(p.getLabel());
			mapping.put(p, pNew);
		}
		for (Transition t : net.getTransitions()){
			Transition tNew = plainNet.addTransition(t.getLabel());
			tNew.setInvisible(t.isInvisible());
			mapping.put(t, tNew);
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : t.getGraph().getInEdges(t)){
				plainNet.addArc((Place) mapping.get(edge.getSource()), tNew);
			}
			for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : t.getGraph().getOutEdges(t)){
				plainNet.addArc(tNew, (Place) mapping.get(edge.getTarget()));
			}
		}
		Marking newMarking = new Marking();
		for (Place p : initialMarking){
			newMarking.add((Place) mapping.get(p));
		}
		if (context != null){
			context.addConnection(new InitialMarkingConnection(plainNet, newMarking));
		}
		return plainNet;
	}
	public static double[] getAsDoubleArray(Collection<Double> values) {
		double[] arr = new double[values.size()];
		Iterator<Double> iter = values.iterator();
		int i = 0;
		while(iter.hasNext()){
			arr[i++] = iter.next();
		}
		return arr;
	}
	
	public static int getIndexBinarySearch(double[] sortedValues, double valueToSearchFor) {
		int l = 0;
		int u = sortedValues.length-1;
		int i = 0;
		
		// extreme cases:
		if (sortedValues[u] <= valueToSearchFor){
			return u;
		} else if (sortedValues[l] >= valueToSearchFor){
			return l;
		}
		
		while(u > l){
			i = l+(u-l)/2;
			if (sortedValues[i] > valueToSearchFor){
				// search in left half:
				u = i;
			} else if (sortedValues[i] < valueToSearchFor){
				// search in right half:
				if (l == i) {
					return i;
				}
				l = i;
			} else {
				u = l = i;
			}
		}
		return i;
	}
	public static int getIndexBinarySearch(long[] sortedValues, long valueToSearchFor) {
		int l = 0;
		int u = sortedValues.length-1;
		int i = 0;
		
		// extreme cases:
		if (sortedValues[u] <= valueToSearchFor){
			return u;
		} else if (sortedValues[l] >= valueToSearchFor){
			return l;
		}
		
		while(u > l){
			i = l+(u-l)/2;
			if (sortedValues[i] > valueToSearchFor){
				// search in left half:
				u = i;
			} else if (sortedValues[i] < valueToSearchFor){
				// search in right half:
				if (l == i) {
					return i;
				}
				l = i;
			} else {
				u = l = i;
			}
		}
		return i;
	}
	
	public static Pair<Long, Long> getBufferedTraceBounds(XTrace trace) {
		long lowerBound = Long.MAX_VALUE;
		long upperBound = Long.MIN_VALUE;
		
		for (XEvent event : trace){
			long eventTime = XTimeExtension.instance().extractTimestamp(event).getTime();
			lowerBound = Math.min(lowerBound, eventTime);
			upperBound = Math.max(upperBound, eventTime);
		}
		return new Pair<Long,Long>(lowerBound-10, upperBound+10);
	}
	
	
	private static Map<String, RealDistribution> distributionCache = new HashMap<String, RealDistribution>(); 
	private static boolean useCache = false;
	/**
	 * Enables or disables cache of distributions.
	 * @param useCache
	 */
	public synchronized static void useCache(boolean useCache) {
		StochasticNetUtils.useCache = useCache;
		distributionCache.clear();
	}
	
	/**
	 * 
	 * @param spn a stochastic Petri net containing all kinds of timed distributions
	 * @return a stochastic Petri net containing only immediate and normal distributions. 
	 *         Timed transitions of other distribution shape are replaced by normal approximations.
	 */
	public static StochasticNet convertToNormal(StochasticNet spn) {
		// approximate all timed transitions with normal ones (mean and variance):
		Iterator<Transition> transitionIter = spn.getTransitions().iterator();
		while(transitionIter.hasNext()){
			Transition transition = transitionIter.next();
			if (transition instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) transition;
				if (!tt.getDistributionType().equals(DistributionType.IMMEDIATE) && !tt.getDistributionType().equals(DistributionType.NORMAL)){
					tt.setDistributionType(DistributionType.NORMAL);
					double mean = tt.getDistribution().getNumericalMean();
					double variance = tt.getDistribution().getNumericalVariance();
					variance = variance <= 0?0.0000001:variance; // ensure numerical stability for deterministic distributions.
					tt.setDistributionParameters(new double[]{mean,Math.sqrt(variance)});
					tt.setDistribution(null);
					tt.setDistribution(tt.initDistribution(0));
				}
			}
		}
		return spn;
	}
	
	/**
	 * 
	 * @param spn a stochastic Petri net containing all kinds of timed distributions
	 * @return a stochastic Petri net containing only immediate and exponential distributions. 
	 *         Timed transitions of other distribution shape are replaced by exponential approximations.
	 */
	public static StochasticNet convertToGSPN(StochasticNet spn) {
		// approximate all timed transitions with normal ones (mean and variance):
		Iterator<Transition> transitionIter = spn.getTransitions().iterator();
		while(transitionIter.hasNext()){
			Transition transition = transitionIter.next();
			if (transition instanceof TimedTransition){
				TimedTransition tt = (TimedTransition) transition;
				if (!tt.getDistributionType().equals(DistributionType.IMMEDIATE) && !tt.getDistributionType().equals(DistributionType.EXPONENTIAL)){
					tt.setDistributionType(DistributionType.EXPONENTIAL);
					double mean = tt.getDistribution().getNumericalMean();
					tt.setDistributionParameters(new double[]{mean});
					tt.setDistribution(null);
					tt.setDistribution(tt.initDistribution(0));
				}
			}
		}
		return spn;
	}
	
	/**
	 * Exports a stochastic net as a 
	 * @param net {@link Petrinet} to export
	 * @param relativeFolderName String a relative folder name (e.g. "tests/testfiles/output") 
	 * @param fileName String a name for the output file (e.g. "myProcess") an extension to mark it as PostScript (".ps") will be appended.
	 *  
	 */
	public static void exportAsDOTFile(Petrinet net, String relativeFolderName, String fileName){
		String fName = relativeFolderName+fileName+".dot";
		try {
			if (relativeFolderName == null || relativeFolderName.isEmpty()){
				relativeFolderName = ".";
			}
			if (!relativeFolderName.endsWith(File.separator)){
				relativeFolderName += File.separator;
			}
			String fNamePostScript = relativeFolderName+fileName+".ps";
			String dotString = ToStochasticNet.convertPetrinetToDOT(net);
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fName)));
			writer.write(dotString);
			writer.flush();
			writer.close();
			
			Process p = Runtime.getRuntime().exec("dot -Tps "+fName+" -o "+fNamePostScript);
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("...continuing");
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.err.println("...continuing");
		} catch (Exception e){
			e.printStackTrace();
			System.err.println("...continuing");
		} finally {
			try {
				Process p = Runtime.getRuntime().exec("rm "+fName);
				p.waitFor();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Extracts the date of an event
	 * @param event
	 * @return
	 */
	public static Date getTraceDate(XEvent event) {
		return XTimeExtension.instance().extractTimestamp(event);
	}
	
	/**
	 * 
	 * @param trace
	 * @return
	 */
	public static Date getMinimalDate(XTrace trace) {
		Date minDate = new GregorianCalendar(10000,1,1).getTime();
		for (XEvent event : trace){
			if (event.getAttributes().get(TIME_ATTRIBUTE_KEY) != null){
				Date date = getTraceDate(event);
				if (date.before(minDate)){
					minDate = date;
				}
			}
		}
		return minDate;
	}
	public static String printDistribution(RealDistribution distribution) {
		if (distribution instanceof NormalDistribution){
			NormalDistribution dist = (NormalDistribution) distribution;
			return "norm("+((int)(distribution.getNumericalMean()*10)/10.)+","+((int)(dist.getStandardDeviation()*10)/10.)+")"; 
		}
		return "";
	}
	
	public static double getMean(Collection<? extends Number> values) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Number n : values){
			Double d = n.doubleValue();
			if (!Double.isNaN(d)){
				stats.addValue(d);
			}
		}
		return stats.getMean();
	}

	/**
	 * The log of probability values is always less than or equal to zero.
	 * This method updates the probability of a trace by adding the log probabilities.
	 * That is, the probabilities get multiplied with each new event that occurred for the trace.
	 * @param trace {@link XTrace} trace that stores the log-probability value in an attribute.
	 * @param logProbability
	 */
	public static void updateLogProbability(XTrace trace, double logProbability){
		if (!trace.getAttributes().containsKey(PNSimulator.SIMULATED_LOG_PROBABILITY)){
			trace.getAttributes().put(PNSimulator.SIMULATED_LOG_PROBABILITY, new XAttributeContinuousImpl(PNSimulator.SIMULATED_LOG_PROBABILITY, logProbability));
		} else {
			double val =  ((XAttributeContinuousImpl)trace.getAttributes().get(PNSimulator.SIMULATED_LOG_PROBABILITY)).getValue();
			((XAttributeContinuousImpl)trace.getAttributes().get(PNSimulator.SIMULATED_LOG_PROBABILITY)).setValue(val+logProbability);
		}
	}
	/**
	 * Retrieves the log-probability of a trace that is stored in its attributes.
	 * @param trace
	 * @return
	 */
	public static double getLogProbability(XTrace trace){
		if (!trace.getAttributes().containsKey(PNSimulator.SIMULATED_LOG_PROBABILITY)){
			return 0; // the zero corresponds to a probability of 1 (the default value.
		} else {
			return ((XAttributeContinuousImpl)trace.getAttributes().get(PNSimulator.SIMULATED_LOG_PROBABILITY)).getValue();
		}
	}
	
	/**
	 * Returns the weight of a transition. 
	 * By default a transition has a weight of one.
	 * @param transition
	 * @return
	 */
	public static double getWeight(Transition transition){
		if (transition instanceof TimedTransition){
			return ((TimedTransition) transition).getWeight();
		} 
		return 1.0;
	}
	
	public static double getFiringRate(Transition transition){
		if (transition instanceof TimedTransition){
			TimedTransition tt = (TimedTransition) transition;
			if (tt.getDistributionType().equals(DistributionType.IMMEDIATE)){
				System.out.println("Debug me: Should not ask for the rate of an immediate transition!");
			} 
			if (tt.getDistribution() != null){
				return 1./tt.getDistribution().getNumericalMean();
			}
		}
		// by default return 1:
		return 1;
	}
	
	public static XLog cloneLog(XLog log){
		XLog logClone = XFactoryRegistry.instance().currentDefault().createLog((XAttributeMap) log.getAttributes().clone());
		for (XTrace trace : log){
			XTrace traceClone = XFactoryRegistry.instance().currentDefault().createTrace((XAttributeMap) trace.getAttributes().clone());
			for (XEvent event : trace){
				XEvent eventClone = XFactoryRegistry.instance().currentDefault().createEvent((XAttributeMap) event.getAttributes().clone());
				traceClone.add(eventClone);
			}
			logClone.add(traceClone);
		}
		return logClone;
	}
	
	public static XLog getSortedLog(XLog unsortedLog) {
		SortedMultiset<ComparablePair<Long, XTrace>> sortedTracesByStartTime = TreeMultiset.<ComparablePair<Long,XTrace>>create();
		for (XTrace trace : unsortedLog){
			Date startTime = XTimeExtension.instance().extractTimestamp(trace.get(0));
			sortedTracesByStartTime.add(new ComparablePair<Long, XTrace>(startTime.getTime(), trace));
		}
		XLog sortedLog = XFactoryRegistry.instance().currentDefault().createLog((XAttributeMap) unsortedLog.getAttributes().clone());
		for (ComparablePair<Long, XTrace> tracePair : sortedTracesByStartTime){
			XTrace clone = XFactoryRegistry.instance().currentDefault().createTrace((XAttributeMap) tracePair.getSecond().getAttributes().clone());
			clone.addAll(tracePair.getSecond());
			sortedLog.add(clone);
		}
		return sortedLog;
	}

	public static String formatMillisToHumanReadableTime(long millis){
		return String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis),
			    TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
			    TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1));
	}
	
	public static String toLoLaFromPetrinnet(Petrinet net){
		return toLoLaFromPetrinet(net, getInitialMarking(null, net));
	}
	
	public static String toLoLaFromPetrinet(Petrinet net, Marking marking){
		StringBuilder builder = new StringBuilder();
		
		Map<Place, String> placeNames = new HashMap<>();

		// the places
		builder.append("PLACE\n ");
		int i = 0;
		boolean first = true;
		for (Place p : net.getPlaces()){
			String name = p.getLabel();
			while (placeNames.containsValue(name)){
				name = p.getLabel()+i++;
			}
			placeNames.put(p, name);
			if (!first){
				builder.append(",");
			} else {
				builder.append(" ");
				first = false;
			}
			builder.append(name);
			
		}
		builder.append(";\n");
		
		// the marking:
		builder.append("MARKING ");
		boolean firstElement = true;
		for (Place p : marking.baseSet()){
			if (firstElement){
				firstElement = false;

			} else {
				builder.append(",");
			}
			builder.append(placeNames.get(p)).append(":").append(marking.occurrences(p));
		}
		builder.append(";\n");
		
		// the transitions
		Map<Transition, String> transitionNames = new HashMap<>();
		for (Transition t :net.getTransitions()){
			String name = t.getLabel();
			i = 0;
			while (transitionNames.containsValue(name)){
				name = t.getLabel()+i++;
			}
			transitionNames.put(t, name);
			builder.append("TRANSITION ").append(name).append("\n");
			builder.append("  CONSUME ");
			if (t.getGraph().getInEdges(t).size() > 0){
				int inEdges = 0;
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : t.getGraph().getInEdges(t)){
					if (inEdges > 0){
						builder.append(",");
					}
					builder.append(placeNames.get(edge.getSource())).append(":1");
					inEdges ++;
				}
			}
			builder.append(";\n");
			
			builder.append("  PRODUCE ");
			if (t.getGraph().getOutEdges(t).size() > 0){
				int outEdges = 0;
				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : t.getGraph().getOutEdges(t)){
					if (outEdges > 0){
						builder.append(",");
					}
					builder.append(placeNames.get(edge.getTarget())).append(":1");
					outEdges ++;
				}
			}
			builder.append(";\n");
		}
		return builder.toString();
	}
	
	public static Long getReachableStateSpaceSize(Petrinet net) throws InterruptedException, IOException{
		return getReachableStateSpaceSize(net, getInitialMarking(null, net));
	}
	
	public static Long getReachableStateSpaceSize(Petrinet net, Marking marking) throws InterruptedException, IOException{
		String loLaString = toLoLaFromPetrinet(net, marking);
		// call LoLA and wait until it is finished
		
		File processFile = File.createTempFile("lolaIn", ".lola");
		processFile.deleteOnExit();
		
		FileUtils.writeStringToFile(processFile, loLaString);

		File outputFile = File.createTempFile("lolaOut", ".json");
		outputFile.deleteOnExit();
		
		ProcessBuilder b = new ProcessBuilder("lola", processFile.getAbsolutePath(), "--quiet" ,"--check=full", "--json="+ outputFile.getAbsolutePath());

		Process processLoLA = b.start();
		IOUtils.copy(processLoLA.getInputStream(), System.out);
		IOUtils.copy(processLoLA.getErrorStream(), System.err);
		processLoLA.waitFor();
//		processLoLA.getOutputStream().close();
//		processLoLA.getErrorStream().close();
		
		Object result = JSONValue.parse(FileUtils.readFileToString(outputFile));
		JSONObject resultObject = (JSONObject) result;
		JSONObject analysisObject = (JSONObject) resultObject.get("analysis");
		JSONObject statsObject = (JSONObject) analysisObject.get("stats");
		return (Long) statsObject.get("states");
	}
	
	public static boolean writeLogToFile(XLog log, File file){
		boolean success = false;
		try {
			if (!file.getParentFile().exists()){
				file.getParentFile().mkdirs();
			}
			if (!file.exists()){
				file.createNewFile();
			}
			
			FileOutputStream fos = new FileOutputStream(file);
			XesXmlSerializer serializer = new XesXmlSerializer();
			serializer.serialize(log, fos);
			fos.flush();
			fos.close();
			success = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return success;
	}
	
	public static String getActivityName(XEventClass eClass){
		String eClassName = eClass.getId();
		if (eClassName.contains("+")){
			eClassName = eClassName.substring(0, eClassName.indexOf("+"));
		}
		return eClassName;
	}
	/**
	 * Aligns the petri net to a log and return the distance.
	 * TODO: incorporate further quality criteria!
	 * 
	 * @param petriNet
	 * @param first
	 * @return
	 */
	public static double getDistance(PetrinetWithMarkings petriNet, XLog log) {
		double distance = 0;
		
		PNRepResult result = (PNRepResult) replayLog(null, petriNet.petrinet, log, false, false);
		distance = Double.valueOf(result.getInfo().get(PNRepResult.TRACEFITNESS).toString());
		
		System.out.println("Unaligned traces:");
		// debug example misaligned trace:
		for (SyncReplayResult repResult : result){
			for(StepTypes stepType : repResult.getStepTypes()){
				if (stepType.equals(StepTypes.L) || stepType.equals(StepTypes.MREAL)){
					System.out.println(debugTrace(log.get(repResult.getTraceIndex().first())));
					break;
				}
			}
		}
		
		return distance;
	}
}
