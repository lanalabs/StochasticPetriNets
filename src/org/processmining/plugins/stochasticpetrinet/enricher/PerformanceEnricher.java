package org.processmining.plugins.stochasticpetrinet.enricher;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.*;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.widgets.ProMPropertiesPanel;
import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.ToStochasticNet;
import org.processmining.plugins.filter.context.LoadAnnotationPlugin;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.petrinet.manifestreplayresult.ManifestEvClassPattern;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.distribution.BernsteinExponentialApproximation;
import org.processmining.plugins.stochasticpetrinet.distribution.GaussianKernelDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.NonConvergenceException;
import org.processmining.plugins.stochasticpetrinet.distribution.RCensoredLogSplineDistribution;
import org.processmining.plugins.stochasticpetrinet.enricher.optimizer.WeightsOptimizer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class PerformanceEnricher {

    public static final double EPSILON = 0.00001;

    /**
     * Allow up to 5 percent of noise, before judging a transition as non-deterministic time
     */
    public static final double THRESHOLD_FOR_DETERMINISTIC_VALUES = 0.05;

    private static int failCount = 0;

    private StochasticManifestCollector performanceCollector;

    private Map<String, int[]> markingBasedSelections;

    private Random random;

    public PerformanceEnricher() {
        this.random = new Random();
    }

    public Object[] transform(PluginContext context, Manifest manifest) {
        // ask for preferences:
        PerformanceEnricherConfig mineConfig = getTypeOfDistributionForNet(context, manifest.getNet());
        try {
            if (mineConfig != null) {
                return transform(context, manifest, mineConfig);
            }
        } catch (Exception e) {
            context.log(e);
            context.getFutureResult(0).cancel(true);
        }
        return null;
    }

    public Object[] transform(PluginContext context, Manifest manifest, PerformanceEnricherConfig mineConfig) {
        long startTime = System.currentTimeMillis();

        PetrinetGraph net = manifest.getNet();

        performanceCollector = new StochasticManifestCollector((ManifestEvClassPattern) manifest, mineConfig);

        Object[] stochasticNetAndMarking = ToStochasticNet.fromPetriNetExternal(context, net, manifest.getInitMarking());
        StochasticNet sNet = (StochasticNet) stochasticNetAndMarking[0];
        sNet.setExecutionPolicy(mineConfig.getPolicy());
        sNet.setTimeUnit(mineConfig.getTimeUnit());

        XLog enrichedLog = null;
        if (mineConfig.isCollectingLoadData() && !manifest.getLog().get(0).get(0).getAttributes().containsKey(LoadAnnotationPlugin.CONTEXT_LOAD)) {
            LoadAnnotationPlugin annotator = new LoadAnnotationPlugin();
            enrichedLog = annotator.addNumberOfInstancesInSystemToLogHeadless(context, manifest.getLog());
        }

        performanceCollector.collectDataFromManifest(enrichedLog);
        if (mineConfig.getCorrelationMatrixFile() != null) {
            performanceCollector.outputCorrelationMatrix();
        }

        Iterator<Transition> originalTransitions = net.getTransitions().iterator();
        Iterator<Transition> newTimedTransitions = sNet.getTransitions().iterator();
        int progress = 0;

        String feedbackMessage = ""; // captures messages occurring during conversion

        if (context != null) {
            context.getProgress().setMaximum(sNet.getTransitions().size());
        }
        while (originalTransitions.hasNext()) {
            if (context != null) {
                context.getProgress().setValue(progress++);
            }
            Transition originalTransition = originalTransitions.next();
            TimedTransition newTimedTransition = (TimedTransition) newTimedTransitions.next();
            int indexOfTransition = performanceCollector.getEncOfTrans(originalTransition);
            List<Double> transitionStats = performanceCollector.getFiringTimes(indexOfTransition);
            List<Double> censoredStats = performanceCollector.getCensoredFiringTimes(indexOfTransition);
            String trainingData = performanceCollector.getTrainingData(indexOfTransition);

            if (!censoredStats.isEmpty()) {
                System.out.println("Transition " + originalTransition.getLabel() + " has " + censoredStats.size() + " censored and " + transitionStats.size() + " observed firings...");
            }

            feedbackMessage += addTimingInformationToTransition(newTimedTransition, transitionStats, censoredStats, mineConfig, performanceCollector.getMeanTraceFitness(), performanceCollector.getMaxTraceDuration());

            newTimedTransition.setDistribution(newTimedTransition.initDistribution(performanceCollector.getMaxTraceDuration()));
            newTimedTransition.setTrainingData(trainingData);
        }
        // weights of the transitions are calculated based on firing ratios on each marking.
        double[] weights = new double[net.getTransitions().size()];
        Arrays.fill(weights, 1);
        markingBasedSelections = performanceCollector.getMarkingBasedSelections();
        if (!mineConfig.getPolicy().equals(ExecutionPolicy.GLOBAL_PRESELECTION)) {
            // Race policy: using weights ONLY for immediate transitions! remove all Marking based selections, where no immediate transitions are fired!
            Transition[] timedTransitions = sNet.getTransitions().toArray(new Transition[sNet.getTransitions().size()]);
            List<String> markingsWhereRaceConditionApplies = new LinkedList<String>();
            for (String marking : markingBasedSelections.keySet()) {
                int[] firingCountsInMarking = markingBasedSelections.get(marking);
                boolean raceConditionAppliesInMarking = true;
                for (int tId = 0; tId < firingCountsInMarking.length; tId++) {
                    if (firingCountsInMarking[tId] > 0) {
                        // race condition only applies, if there are no immediate transitions firing in marking...
                        raceConditionAppliesInMarking &= !((TimedTransition) timedTransitions[tId]).getDistributionType().equals(DistributionType.IMMEDIATE);
                    }
                }
                if (raceConditionAppliesInMarking) {
                    markingsWhereRaceConditionApplies.add(marking);
                }
            }
            for (String raceConditionMarking : markingsWhereRaceConditionApplies) {
                markingBasedSelections.remove(raceConditionMarking);
            }
        }
        boolean freechoice = true; //TODO: check if net is really free choice
        if (freechoice && !mineConfig.getPolicy().equals(ExecutionPolicy.GLOBAL_PRESELECTION)) {
            //weights are simply the relative counts of firings
            for (String marking : markingBasedSelections.keySet()) {
                int[] firings = markingBasedSelections.get(marking);
                for (int i = 0; i < firings.length; i++) {
                    weights[i] += firings[i];
                }
            }
        } else {
            WeightsOptimizer optimizer = new WeightsOptimizer(weights, markingBasedSelections);
            weights = optimizer.optimizeWeights();
        }


        int i = 0;
        for (Transition timedTransition : sNet.getTransitions()) {
            ((TimedTransition) timedTransition).setWeight(weights[i++]);
        }

        if (!feedbackMessage.isEmpty()) {
            if (context != null) {
                // provide user feedback only in GUI mode
                JOptionPane.showMessageDialog(null, feedbackMessage, "Distribution estimation information", JOptionPane.INFORMATION_MESSAGE);
            } else {
                System.out.println(feedbackMessage);
            }
        }
        if (context != null) {
            context.log("finished discovery of GDT_SPN model for " + mineConfig.getPolicy() + " policy.");
            context.log("Took " + (System.currentTimeMillis() - startTime) / 1000. + " sec.");
        } else {
            System.out.println("finished discovery of GDT_SPN model for " + mineConfig.getPolicy() + " policy.");
            System.out.println("Took " + (System.currentTimeMillis() - startTime) / 1000. + " sec.");
        }
        return stochasticNetAndMarking;
    }

    private boolean getIsDeterministic(List<Double> transitionStats, List<Double> censoredStats, double traceFitness) {
        assert (!transitionStats.isEmpty());

        Collections.sort(transitionStats);
        double[] sampleValues = StochasticNetUtils.getAsDoubleArray(transitionStats);
        double quantile25to75 = Double.MAX_VALUE;
        if (sampleValues.length > 4) {
            quantile25to75 = sampleValues[3 * sampleValues.length / 4] - sampleValues[sampleValues.length / 4];
        }
        if (quantile25to75 > EPSILON) {
            return false;
        }
        // looks deterministic, now check if censored entries all contain the value:
        double value = getMedian(transitionStats);
        int count = 0;
        for (double cens : censoredStats) {
            if (cens > value + EPSILON) {
                count++;
            }
        }
        if (count / (double) censoredStats.size() > THRESHOLD_FOR_DETERMINISTIC_VALUES) {
            return false;
        }
        return true;
    }

    private double getMedian(List<Double> transitionStats) {
        return transitionStats.get(transitionStats.size() / 2);
    }

    /**
     * @param newTimedTransition
     * @param transitionStats    observed transition durations
     * @param censoredStats      unobserved transition durations (right censored, i.e., some other transition was faster...)
     * @param traceFitness
     * @param maxTraceLength     the duration of the trace in model-units (ms divided by unit factor)
     * @return String reporting special notes during estimation (e.g. for undefined log values)
     */
    private String addTimingInformationToTransition(TimedTransition newTimedTransition, List<Double> transitionStats,
                                                    List<Double> censoredStats, PerformanceEnricherConfig config, double traceFitness, double maxTraceLength) {
        DistributionType typeToMine = config.getType();
        String message = "";
        newTimedTransition.setPriority(0);

//		if (newTimedTransition.getLabel().equalsIgnoreCase("D")){
////			typeToMine = DistributionType.GAUSSIAN_KERNEL;
//			censoredStats = new LinkedList<Double>();
//		}

        // special cases:
        if (transitionStats.isEmpty()) {
            // we have no sample values! assume uniform over all positive values:
            double lowerLimit = 0;
            double upperLimit = 200;
            if (!censoredStats.isEmpty()) {
                // we know some lower limit for the transition:
                lowerLimit = Collections.min(censoredStats);
            } else {
                // silent or unobserved transition (or very first transition just containing 0-values)
                // for now treat as immediate transition
                newTimedTransition.setInvisible(true);
            }
            newTimedTransition.setDistributionType(DistributionType.UNIFORM);
            newTimedTransition.setDistributionParameters(new double[]{lowerLimit, upperLimit});
        } else if (containsOnlyZeros(transitionStats) || newTimedTransition.isInvisible()) {
            // immediate transition
            newTimedTransition.setImmediate(true);
        } else if (getIsDeterministic(transitionStats, censoredStats, traceFitness)) {
            // deterministic transition
            newTimedTransition.setDistributionType(DistributionType.DETERMINISTIC);
            newTimedTransition.setDistributionParameters(new double[]{getMedian(transitionStats)});
        } else if (transitionStats.size() == 1) {
            // only one sample!
            // assume a gaussian heap with standard deviation 1:
            newTimedTransition.setDistributionType(DistributionType.NORMAL);
            newTimedTransition.setDistributionParameters(new double[]{transitionStats.get(0), 1});
        } else {
            // regular fitting according to selected distribution model specified in the configuration.
            double[] values = StochasticNetUtils.getAsDoubleArray(transitionStats);
            Mean mean = new Mean();
            double meanValue = mean.evaluate(values);
            StandardDeviation sd = new StandardDeviation();
            double standardDeviation = sd.evaluate(values, meanValue);
            newTimedTransition.setDistributionType(typeToMine);
            // degenerate case: all same values...
            if (values.length > 1 && standardDeviation == 0 && !typeToMine.equals(DistributionType.EXPONENTIAL)) {
                newTimedTransition.setDistributionType(DistributionType.DETERMINISTIC);
            }
            switch (typeToMine) {
                case NORMAL:
                    newTimedTransition.setDistributionParameters(new double[]{meanValue, standardDeviation});
                    break;
                case LOGNORMAL:
                    double[] logValues = new double[transitionStats.size()];
                    int nanValues = 0;
                    for (int j = 0; j < values.length; j++) {
                        logValues[j] = Math.log(values[j]);
                        nanValues += (Double.isNaN(logValues[j]) || Double.isInfinite(logValues[j]) ? 1 : 0);
                    }
                    // take log of values:
                    if (nanValues > 0) {
                        // zeros or negative numbers break log-normal function.
                        message = "Omitting " + nanValues + " values of " + logValues.length + " for estimation of " +
                                "log-normal distribution of activity " + newTimedTransition.getLabel() + ".\n";
                    }
                    double logMeanValue = mean.evaluate(logValues);
                    double logStandardDeviation = sd.evaluate(logValues, logMeanValue);
                    newTimedTransition.setDistributionParameters(new double[]{logMeanValue, logStandardDeviation});
                    break;
                case EXPONENTIAL:
                    newTimedTransition.setDistributionParameters(new double[]{meanValue});
                    break;
                case GAUSSIAN_KERNEL:
                case HISTOGRAM:
                    newTimedTransition.setDistributionParameters(values);
                    break;
                case BERNSTEIN_EXPOLYNOMIAL:
                    // approximate the kernel density with the bernstein exponential
                    GaussianKernelDistribution dist = new GaussianKernelDistribution();
                    dist.addValues(values);
                    BernsteinExponentialApproximation bea = new BernsteinExponentialApproximation((RealDistribution) dist, 0.0, dist.getSupportUpperBound());
                    newTimedTransition.setDistributionParameters(bea.getParameters());
                    newTimedTransition.setDistribution(bea);
                    break;
                case LOGSPLINE:
//					if (transitionStats.size() < 10){
//						// log-spline fitting needs more data -> fall back to Gaussian kernel
//						newTimedTransition.setDistributionType(DistributionType.GAUSSIAN_KERNEL);
//					}
                    //preprocess transitionStats
                    List<Double> perturbedStats = new LinkedList<Double>();
                    for (Double d : transitionStats) {
                        double delta = -10;
                        while (delta < -3) {
                            delta = random.nextGaussian();
                        }
                        perturbedStats.add(d + (delta + 3) / 100);
                    }
                    if (censoredStats.size() > 0 && transitionStats.size() > 10) {
                        if (censoredStats.size() > transitionStats.size()) {
                            censoredStats = censoredStats.subList(0, transitionStats.size() - 1);
                        }
                        try {
                            // try using better approximation capable to deal with right censored data
                            RCensoredLogSplineDistribution censoredDistribution = new RCensoredLogSplineDistribution(maxTraceLength);
                            censoredDistribution.initWithValues(StochasticNetUtils.getAsDoubleArray(perturbedStats), StochasticNetUtils.getAsDoubleArray(censoredStats));
                            newTimedTransition.setDistribution(censoredDistribution);
                            double newMean = censoredDistribution.getNumericalMean();
                            if (Double.isInfinite(newMean) || newMean > 100) {
                                System.out.println("Debug me!");
                                newMean = censoredDistribution.getNumericalMean();
                            }
                        } catch (NonConvergenceException e) {
                            System.err.println("----> bias correction failed the " + (failCount++) + ". time!");
                            message = "Fitting of logspline with " + transitionStats.size() + " observed values and " + censoredStats.size() + " censored values failed to converge.\n" +
                                    "Falling back to log-spline estimation for transition " + newTimedTransition.getLabel() + " based on the observed values. (creating bias)\n";
                        }
                    }
                    newTimedTransition.setDistributionParameters(StochasticNetUtils.getAsDoubleArray(perturbedStats));
                    break;
                case DETERMINISTIC:
                    newTimedTransition.setDistributionParameters(new double[]{getMedian(transitionStats)});
                    break;
                default:
                    // other distributions not supported yet...
            }
        }
        return message;
    }

    public static PerformanceEnricherConfig getTypeOfDistributionForNet(final PluginContext context, PetrinetGraph net) {
        ProMPropertiesPanel panel = new ProMPropertiesPanel("Stochastic Net properties:");

        final TimeConstraintsPanel constraintPanel = getConstraintPanel(net);
        final TimeConstraints constraints = new TimeConstraints();

        DistributionType[] supportedTypes = new DistributionType[]{DistributionType.NORMAL, DistributionType.LOGNORMAL, DistributionType.EXPONENTIAL, DistributionType.GAUSSIAN_KERNEL, DistributionType.BERNSTEIN_EXPOLYNOMIAL, DistributionType.HISTOGRAM};
        if (StochasticNetUtils.splinesSupported()) {
            supportedTypes = Arrays.copyOf(supportedTypes, supportedTypes.length + 1);
            supportedTypes[supportedTypes.length - 1] = DistributionType.LOGSPLINE;
        } else {
            panel.add(new JLabel("<html><body><p>To enable spline smoothers, make sure you have a running R installation<br>" +
                    "and the native jri-binary is accessible in your java.library.path!</p></body></html"));
            panel.add(StochasticNetUtils.linkify("See also the documentation PDF", "https://svn.win.tue.nl/repos/prom/Documentation/Package_StochasticPetriNets.pdf", "PDF-Documentation of the StochasticPetriNets plugin"));
        }
        JComboBox distTypeSelection = panel.addComboBox("Type of distributions", supportedTypes);
        JComboBox timeUnitSelection = panel.addComboBox("Time unit in model", TimeUnit.values());
        JButton advancedTimeConstraintButton = new JButton("advanced settings");
        advancedTimeConstraintButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                InteractionResult result = ((UIPluginContext) context).showConfiguration("Advanced time settings", constraintPanel);
                if (result.equals(InteractionResult.CANCEL)) {
                    // do nothing
                } else {
                    constraintPanel.updateConstraints(constraints);
                }

            }
        });
        panel.add("", advancedTimeConstraintButton);
        JComboBox executionPolicySelection = panel.addComboBox("Execution policy", StochasticNet.ExecutionPolicy.values());
        ProMTextField correlationFileField = panel.addTextField("Correlation Matrix Output", "");
        if (context instanceof UIPluginContext) {
            InteractionResult result = ((UIPluginContext) context).showConfiguration("Select type of distribution:", panel);
            if (result.equals(InteractionResult.CANCEL)) {
                context.getFutureResult(0).cancel(true);
                return null;
            } else {
                DistributionType distType = (DistributionType) distTypeSelection.getSelectedItem();
                TimeUnit timeUnit = (TimeUnit) timeUnitSelection.getSelectedItem();
                File correlationFile = null;
                if (!correlationFileField.getText().trim().isEmpty()) {
                    correlationFile = new File(correlationFileField.getText().trim());
                    if (!correlationFile.exists()) {
                        boolean succeeded = false;
                        try {
                            succeeded = correlationFile.createNewFile();
                        } catch (IOException e) {
                        }
                        if (!succeeded) {
                            JOptionPane.showMessageDialog(panel, "Could not create file <" + correlationFile.getAbsolutePath() + ">!\n" +
                                    "Enricher will not store transition durations by case.");
                        }
                    }
                }
                return new PerformanceEnricherConfig(distType, timeUnit, (ExecutionPolicy) executionPolicySelection.getSelectedItem(), correlationFile);
            }
        } else {
            return null;
        }
    }

    private static TimeConstraintsPanel getConstraintPanel(PetrinetGraph net) {
        return new TimeConstraintsPanel("Temporal Constraints for the Enrichment", net);
    }

    private boolean containsOnlyZeros(List<Double> transitionStats) {
        int count = 0;
        for (Double d : transitionStats) {
            if (!Double.isNaN(d) && Math.abs(d) > EPSILON) {
                count++;
            }
        }
        if (count / (double) transitionStats.size() > THRESHOLD_FOR_DETERMINISTIC_VALUES) {
            return false;
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
        traceIterator:
        for (XTrace t : manifest.getLog()) {
            for (XEvent e : t) {
                if (attributes.size() > 0) {
                    it = attributes.iterator();
                    nextAttribute:
                    while (it.hasNext()) {
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
        if (arrAttributes.length == 1) {
            return String.valueOf(arrAttributes[0]);
        }
        String timeAttr = (String) JOptionPane.showInputDialog(new JPanel(),
                "Choose which data type will be accounted as \"timestamp\"", "Timestamp attribute selection",
                JOptionPane.PLAIN_MESSAGE, null, arrAttributes, arrAttributes[selectedIndex]);
        return timeAttr;
    }

    public StochasticManifestCollector getPerformanceCollector() {
        return performanceCollector;
    }

    /**
     * Gets all markings, where selection is based on weights (markings, where only immediate transitions are enabled, or all markings, where )
     *
     * @return
     */
    public Map<String, int[]> getMarkingBasedSelections() {
        return markingBasedSelections;
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
