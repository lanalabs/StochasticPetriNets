package org.processmining.plugins.stochasticpetrinet.enricher.experiment;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.PetrinetSemantics;
import org.processmining.models.semantics.petrinet.impl.PetrinetSemanticsFactory;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricher;
import org.processmining.plugins.stochasticpetrinet.enricher.experiment.PerformanceEnricherExperimentPlugin.ExperimentType;

import java.util.*;

public class PerformanceEnricherExperimentResult {

    private Map<Integer, Map<ExecutionPolicy, ModelComparisonResult>> results;

    String SEPARATOR = ";";

    public PerformanceEnricherExperimentResult() {
        results = new TreeMap<Integer, Map<ExecutionPolicy, ModelComparisonResult>>();
    }

    public void add(int traceSize, ExecutionPolicy policy, ModelComparisonResult result) {
        if (!results.containsKey(traceSize)) {
            results.put(traceSize, new HashMap<ExecutionPolicy, ModelComparisonResult>());
        }
        results.get(traceSize).put(policy, result);
    }

    public ModelComparisonResult getComparisonResult(StochasticNet net, StochasticNet learnedNet, PerformanceEnricher enricher) {
        ModelComparisonResult result = new ModelComparisonResult();
        result.meanTraceFitness = enricher.getPerformanceCollector().getMeanTraceFitness();
        // go through all transitions to get first moments:
        for (Transition t : net.getTransitions()) {
            if (t instanceof TimedTransition) {
                TimedTransition tt = (TimedTransition) t;
                TimedTransition ttLearned = getTimedTransitionFromNet(learnedNet, tt);
                if (tt.getDistributionType().equals(DistributionType.IMMEDIATE) || ttLearned.getDistributionType().equals(DistributionType.IMMEDIATE)) {
                    System.out.println("transition " + tt.getLabel() + "(" + tt.getDistributionType() + "/" + ttLearned.getDistributionType() + ") weight: " + tt.getWeight() + ",\t learnedWeight: " + ttLearned.getWeight());
                } else {
                    System.out.println("transition " + tt.getLabel() + "(" + tt.getDistributionType() + "/" + ttLearned.getDistributionType() + ") weight: " + tt.getWeight() + ",\t learnedWeight: " + ttLearned.getWeight() + ",\t mean: " + tt.getDistribution().getNumericalMean() + ",\t learned mean: " + ttLearned.getDistribution().getNumericalMean());
                }
                if (tt.getLabel().equals("A")) {
                    // skip first activity
                } else if (tt.getLabel().equals("B") || tt.getLabel().equals("C") || tt.getLabel().equals("D")) {
                    if (tt.getDistribution() != null && ttLearned.getDistribution() != null) {
                        double trueMean = tt.getDistribution().getNumericalMean();
                        double learnedMean = ttLearned.getDistribution().getNumericalMean();
                        if (Double.isNaN(trueMean) || Double.isNaN(learnedMean)) {
                            System.out.println("Debug me!");
                        }
//						double absoluteError = Math.abs(learnedMean-trueMean);
//						result.firstMomentDifferences.addValue(absoluteError);
                        double relativeAbsoluteError = Math.abs(100 * (learnedMean - trueMean) / trueMean);
                        if (!Double.isInfinite(relativeAbsoluteError) && !Double.isNaN(relativeAbsoluteError)) {
                            result.firstMomentDifferences.addValue(relativeAbsoluteError);
                        } else {
                            result.firstMomentDifferences.addValue(100);
                        }
                        if (Double.isNaN(result.firstMomentDifferences.getMean())) {
                            System.out.println("Debug me!");
                        }
                    } else if (tt.getDistributionType().equals(DistributionType.IMMEDIATE) && ttLearned.getDistributionType().equals(DistributionType.IMMEDIATE)) {
                        // should not happen, because only timed transitions at this point!
                        result.firstMomentDifferences.addValue(0);
                    } else {
                        result.firstMomentDifferences.addValue(100);
                    }
                }

            } else {
                System.out.println("Debug me!");
            }
        }

        PetrinetSemantics semantics = PetrinetSemanticsFactory.elementaryPetrinetSemantics(StochasticNet.class);
        semantics.initialize(net.getTransitions(), StochasticNetUtils.getInitialMarking(null, net));
        // go through all relevant markings to get weight differences:
        for (String marking : enricher.getMarkingBasedSelections().keySet()) {
            Marking m = getMarking(net, marking);
            semantics.setCurrentState(m);
            List<Double> originalWeights = new LinkedList<Double>();
            List<Double> learnedWeights = new LinkedList<Double>();
            for (Transition t : semantics.getExecutableTransitions()) {
                originalWeights.add(((TimedTransition) t).getWeight());
                learnedWeights.add(getTransitionByLabel(learnedNet, t.getLabel()).getWeight());
            }
            if (originalWeights.size() > 1) {
                result.weightDifferences.addValue(getNormalizedDifference(originalWeights, learnedWeights));
            }
        }
        return result;
    }

    private double getNormalizedDifference(Collection<Double> originalWeights, Collection<Double> learnedWeights) {
        double originalSum = getSum(originalWeights);
        double learnedSum = getSum(learnedWeights);
        int size = originalWeights.size();
        assert (size == learnedWeights.size());

        DescriptiveStatistics stats = new DescriptiveStatistics();

        Iterator<Double> originalIter = originalWeights.iterator();
        Iterator<Double> learnedIter = learnedWeights.iterator();

        double difference = 0;
        while (originalIter.hasNext()) {
            double origVal = originalIter.next();
            double learnedVal = learnedIter.next();

            origVal = size * origVal / originalSum;
            learnedVal = size * learnedVal / learnedSum;
            stats.addValue(Math.abs(100 * (origVal - learnedVal) / origVal));
            difference += Math.abs(origVal - learnedVal);
        }
        return stats.getMean();
    }

    private double getSum(Collection<Double> collection) {
        double sum = 0;
        for (Double d : collection) {
            sum += d;
        }
        return sum;
    }

    private TimedTransition getTransitionByLabel(StochasticNet learnedNet, String label) {
        for (Transition t : learnedNet.getTransitions()) {
            if (label.equals(t.getLabel())) {
                return (TimedTransition) t;
            }
        }
        return null;
    }

    private Marking getMarking(StochasticNet net, String marking) {
        Place[] places = net.getPlaces().toArray(new Place[net.getPlaces().size()]);
        String cleanOfBrackets = marking.substring(1, marking.length() - 1);
        String[] placeCounts = cleanOfBrackets.split(",");
        Marking m = new Marking();
        for (int i = 0; i < places.length; i++) {
            int placeCount = Integer.valueOf(placeCounts[i].trim());
            if (placeCount > 0) {
                m.add(places[i], placeCount);
            }
        }
        return m;
    }

    public String getResultsCSV(ExperimentType type) {
        StringBuilder builder = new StringBuilder();

        String mode = (type.equals(ExperimentType.TRACE_SIZE_EXPERIMENT) ? "Trace Size" : "Noise Level");
        builder.append(mode + SEPARATOR);
        for (ExecutionPolicy policy : ExecutionPolicy.values()) {
            builder.append("MSE weights (" + policy.shortName() + ")" + SEPARATOR + "MSE means (" + policy.shortName() + ")" + SEPARATOR + "Trace Fitness" + SEPARATOR);
        }
        builder.append("\n");


        for (Integer traceSize : results.keySet()) {
            builder.append(traceSize + SEPARATOR);

            Map<ExecutionPolicy, ModelComparisonResult> result = results.get(traceSize);

            for (ExecutionPolicy policy : ExecutionPolicy.values()) {
                ModelComparisonResult r = result.get(policy);
                builder.append(r.weightDifferences.getMean() + SEPARATOR + r.firstMomentDifferences.getMean() + SEPARATOR + r.meanTraceFitness + SEPARATOR);
            }
            builder.append("\n");
        }
        return builder.toString();

    }

    private TimedTransition getTimedTransitionFromNet(StochasticNet learnedNet, TimedTransition tt) {
        for (Transition t : learnedNet.getTransitions()) {
            if (t.getLabel().equals(tt.getLabel())) {
                return (TimedTransition) t;
            }
        }
        return null;
    }
}
