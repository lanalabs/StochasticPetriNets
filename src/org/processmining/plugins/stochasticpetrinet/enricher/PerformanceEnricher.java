package org.processmining.plugins.stochasticpetrinet.enricher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeContinuous;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XAttributeTimestamp;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.util.Pair;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.ToStochasticNet;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.petrinet.manifestreplayresult.ManifestEvClassPattern;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

public class PerformanceEnricher {

	public Object[] transform(UIPluginContext context, Manifest manifest){
		// ask for preferences:
		Pair<DistributionType,Double> mineConfig = getTypeOfDistributionForNet(context);
		try {
			if (mineConfig != null){
				return transform(context, manifest, mineConfig);
			}
		} catch (Exception e){
			context.log(e.getMessage());
			context.getFutureResult(0).cancel(true);
		}
		return null;
	}
	
	public Object[] transform(UIPluginContext context, Manifest manifest, Pair<DistributionType,Double> mineConfig) {
		XLog log = manifest.getLog();
		XLogInfo logInfo = XLogInfoImpl.create(log, manifest.getEvClassifier());
		XEventClasses ec = logInfo.getEventClasses();
		
		PetrinetGraph net = manifest.getNet();
		
		CollectorCounter performanceCounter = new CollectorCounter((ManifestEvClassPattern) manifest);
		
		String timeAttr = getTimeAttribute(manifest);
		boolean[] caseFilter = getCaseFilter(manifest);
		
		Object[] stochasticNetAndMarking = ToStochasticNet.fromPetriNetExternal(context, net, manifest.getInitMarking());
		StochasticNet sNet = (StochasticNet) stochasticNetAndMarking[0];
		
		performanceCounter.init((ManifestEvClassPattern) manifest, timeAttr, getClassFor(timeAttr, manifest), caseFilter);
		
		Iterator<Transition> originalTransitions = net.getTransitions().iterator();
		Iterator<Transition> newTimedTransitions = sNet.getTransitions().iterator();
		int progress = 0;
		
		String feedbackMessage = ""; // captures messages occurring during conversion
		
		context.getProgress().setMaximum(sNet.getTransitions().size());
		while(originalTransitions.hasNext()){
			context.getProgress().setValue(progress++);
			Transition originalTransition = originalTransitions.next();
			TimedTransition newTimedTransition = (TimedTransition) newTimedTransitions.next();
			int indexOfTransition = performanceCounter.getEncOfTrans(originalTransition);
			List<Double> transitionStats = performanceCounter.getPerformanceData(indexOfTransition);
			int weight = performanceCounter.getDataCount(indexOfTransition);
			newTimedTransition.setWeight(weight);
			if (transitionStats != null && transitionStats.size() > 0){
				if (containsOnlyZeros(transitionStats)){
					newTimedTransition.setImmediate(true);
				} else {
					feedbackMessage += addTimingInformationToTransition(newTimedTransition, transitionStats, mineConfig.getFirst(),mineConfig.getSecond());
				}
			} else {
				// silent or unobserved transition (or very first transition just containing 0-values)
				// for now treat as immediate transition
				newTimedTransition.setImmediate(true);
			}
			
			newTimedTransition.updateDistribution();
		}
		if (!feedbackMessage.isEmpty()){
			JOptionPane.showMessageDialog(null, feedbackMessage, "Distribution estimation information", JOptionPane.INFORMATION_MESSAGE);

		}
		return stochasticNetAndMarking;
		
//		
//		Map<Integer, List<Double>> timesForActivities = new HashMap<Integer, List<Double>>(); 
//		
//		int[] casePointer = manifest.getCasePointers();
//		for (int i = 0; i < casePointer.length; i++) {
//			// construct alignment from manifest
//			int[] caseEncoded = manifest.getManifestForCase(i);
//
//			List<StepTypes> stepTypesLst = new ArrayList<StepTypes>(caseEncoded.length / 2);
//			List<Object> eventClassLst = new ArrayList<Object>(caseEncoded.length / 2);
//			int pointer = 0;
//			Iterator<XEvent> it = log.get(i).iterator();
//			
////			TIntSet countedManifest = new TIntHashSet(caseEncoded.length / 2);
//		}
//		return new StochasticNetImpl("test");
	}

	/**
	 * 
	 * @param newTimedTransition
	 * @param transitionStats
	 * @param typeToMine
	 * @return String reporting special notes during estimation (e.g. for undefined log values) 
	 */
	private String addTimingInformationToTransition(TimedTransition newTimedTransition, List<Double> transitionStats,
			DistributionType typeToMine, Double unitConversionFactor) {
		String message = "";
		double[] values = new double[transitionStats.size()];
		int i = 0;
		for (Iterator<Double> iter = transitionStats.iterator(); iter.hasNext();){
			Double d = iter.next();
			values[i++] = d/unitConversionFactor;
		}
		Mean mean = new Mean();
		double meanValue = mean.evaluate(values);
		StandardDeviation sd = new StandardDeviation();
		double standardDeviation = sd.evaluate(values, meanValue);
		newTimedTransition.setDistributionType(typeToMine);
		switch(typeToMine){
			case NORMAL:
				newTimedTransition.setDistributionParameters(new double[]{meanValue,standardDeviation});
				break;
			case LOGNORMAL:
				double[] logValues = new double[transitionStats.size()];
				int nanValues = 0;
				i = 0;
				for (int j = 0; j < values.length; j++){
					logValues[j] = Math.log(values[j]);
					nanValues += (Double.isNaN(logValues[j]) || Double.isInfinite(logValues[j])?1:0);
				}
				// take log of values:
				if (nanValues > 0){
					// zeros or negative numbers break log-normal function.
					message = "Omitting "+nanValues+" values of "+logValues.length+" for estimation of " +
							"log-normal distribution of activity "+newTimedTransition.getLabel()+".\n";

				} 
				double logMeanValue = mean.evaluate(logValues);
				double logStandardDeviation = sd.evaluate(logValues, logMeanValue);
				newTimedTransition.setDistributionParameters(new double[]{logMeanValue,logStandardDeviation});
				break;
				case EXPONENTIAL:
				newTimedTransition.setDistributionParameters(new double[]{meanValue});
				break;
			case GAUSSIAN_KERNEL:
			case HISTOGRAM:
			case LOG_SPLINE:
				newTimedTransition.setDistributionParameters(values);
				break;
			default:
				// other distributions not supported yet...
		}
		newTimedTransition.setWeight(transitionStats.size());
		newTimedTransition.setPriority(0);
		
		return message;
	}

	public static Pair<DistributionType, Double> getTypeOfDistributionForNet(UIPluginContext context) {
		ProMPropertiesPanel panel = new ProMPropertiesPanel("Stochastic Net properties:");
		DistributionType[] supportedTypes = new DistributionType[]{DistributionType.NORMAL, DistributionType.LOGNORMAL, DistributionType.EXPONENTIAL, DistributionType.GAUSSIAN_KERNEL,DistributionType.HISTOGRAM};
		if (StochasticNetUtils.splinesSupported()){
			supportedTypes = Arrays.copyOf(supportedTypes, supportedTypes.length+1);
			supportedTypes[supportedTypes.length-1] = DistributionType.LOG_SPLINE;
		} else {
			panel.add(new JLabel("<html><body><p>To enable spline smoothers, make sure you have a running R installation \n<br>" +
					"and the native jri-binary is accessible in your java.library.path!</p></body></html>"));
		}
		JComboBox distTypeSelection = panel.addComboBox("Type of distributions", supportedTypes);
		JComboBox timeUnitSelection = panel.addComboBox("Time unit in model", StochasticNetUtils.UNIT_NAMES);
		InteractionResult result = context.showConfiguration("Select type of distribution:", panel);
		if (result.equals(InteractionResult.CANCEL)){
			context.getFutureResult(0).cancel(true);
			return null;
		} else {
			DistributionType distType = (DistributionType) distTypeSelection.getSelectedItem();
			Double timeUnit = StochasticNetUtils.UNIT_CONVERSION_FACTORS[timeUnitSelection.getSelectedIndex()];
			return new Pair<StochasticNet.DistributionType, Double>(distType, timeUnit);
		}
	}

	private boolean containsOnlyZeros(List<Double> transitionStats) {
		for (Double d : transitionStats){
			if (!Double.isNaN(d) && d != 0){
				return false;
			}
		}
		return true;
	}

	private boolean[] getCaseFilter(Manifest manifest) {
		// create caseFilter
		boolean[] caseFilter = new boolean[manifest.getLog().size()];
		Arrays.fill(caseFilter, true);

//		if (!showUnreliableCases) {
//			for (int i = 0; i < caseFilter.length; i++) {
//				caseFilter[i] = manifest.isCaseReliable(i);
//			}
//		}
		return caseFilter;
	}

	private String getTimeAttribute(Manifest manifest) {
		// only propose attribute that exists in all events
		Set<String> attributes = new HashSet<String>();

		for (Entry<String, XAttribute> entry : manifest.getLog().iterator().next().iterator().next()
				.getAttributes().entrySet()) {
			if ((entry.getValue() instanceof XAttributeTimestamp)
					|| (entry.getValue() instanceof XAttributeDiscrete)
					|| (entry.getValue() instanceof XAttributeContinuous)) {
				attributes.add(entry.getKey());
			}
		}
		;
		Iterator<String> it = null;
		traceIterator: for (XTrace t : manifest.getLog()) {
			for (XEvent e : t) {
				if (attributes.size() > 0) {
					it = attributes.iterator();
					nextAttribute: while (it.hasNext()) {
						String checkAttr = it.next();
						// check per attribute
						for (Entry<String, XAttribute> eSet : e.getAttributes().entrySet()) {
							if (checkAttr.equals(eSet.getKey())) {
								continue nextAttribute;
							}
						}
						it.remove();
					}
				} else {
					break traceIterator;
				}
			}
		}

		if (attributes.size() == 0) {
			throw new IllegalArgumentException(
					"There is no timestamp/discrete/continuous attributes found in the original log.");
		}

		Object[] arrAttributes = attributes.toArray();
		int selectedIndex = 0;
		for (int i = 0; i < arrAttributes.length; i++) {
			if (arrAttributes[i].toString().toLowerCase().startsWith("time")) {
				selectedIndex = i;
				break;
			}
		}
		if(arrAttributes.length == 1){
			return String.valueOf(arrAttributes[0]);
		}
		String timeAttr = (String) JOptionPane.showInputDialog(new JPanel(),
				"Choose which data type will be accounted as \"timestamp\"", "Timestamp attribute selection",
				JOptionPane.PLAIN_MESSAGE, null, arrAttributes, arrAttributes[selectedIndex]);
		return timeAttr;
	}
	
	private Class<?> getClassFor(String timeAtt, Manifest m) {
		XAttribute xattr = m.getLog().iterator().next().iterator().next().getAttributes().get(timeAtt);
		if (xattr instanceof XAttributeTimestamp) {
			return java.util.Date.class;
		} else if (xattr instanceof XAttributeContinuous) {
			return Double.class;
		} else if (xattr instanceof XAttributeDiscrete) {
			return Integer.class;
		}
		throw new IllegalArgumentException("Not supported data type");
	}
}
