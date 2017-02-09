package org.processmining.plugins.stochasticpetrinet.enricher.experiment;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.filter.noise.NoiseLogFilter;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricher;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherConfig;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

/**
 * Performs a round-trip: model -> log -> model
 * 1. generates a log for each execution semantics ({@link ExecutionPolicy}).
 * 2. strips the performance information of the stochastic net to get a plain model.
 * 3. uses logs and model to enrich Petri nets
 * 4. compare model parameters between each learned model and the original model.
 *
 * @author Andreas Rogge-Solti
 */
public class PerformanceEnricherExperimentPlugin {

    public static final int[] TRACE_SIZES = new int[]{10, 30, 100, 300, 1000, 3000, 10000};

    public static final int[] NOISE_LEVELS = new int[]{0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 70};

    public static final DistributionType DISTRIBUTION_TYPE = DistributionType.LOGSPLINE;

    public enum ExperimentType {
        TRACE_SIZE_EXPERIMENT, NOISE_LEVEL_EXPERIMENT;
    }

    /**
     * Use minutes as unit in the model
     */
    public static final TimeUnit UNIT_FACTOR = TimeUnit.MINUTES;

    /**
     * Trace size for noise experiment:
     */
    private static final int NOISE_LEVEL_EXPERIMENT_TRACE_SIZE = 1000;


    @Plugin(name = "Enrich Petri Net (Simulated Experiment)",
            parameterLabels = {"StochasticNet"},
            returnLabels = {"Enrichment quality"},
            returnTypes = {PerformanceEnricherExperimentResult.class},
            userAccessible = true,
            help = "Simulates a log given a stochastic model and then uses the underlying model to enrich it with information from the log.")
    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public PerformanceEnricherExperimentResult plugin(UIPluginContext context, StochasticNet net) {
        Marking initialMarking = StochasticNetUtils.getInitialMarking(context, net);

        return performExperiment(context, net, initialMarking, ExperimentType.NOISE_LEVEL_EXPERIMENT);
    }

    /**
     * We first simulate the net a number of times with a combination of given trace-sizes and policies, and then
     * enrich the base Petri net to be stochastic again. Then we compare the resulting model parameters to each other.
     *
     * @param context
     * @param net
     * @param initialMarking
     * @return
     */
    public PerformanceEnricherExperimentResult performExperiment(UIPluginContext context, StochasticNet net,
                                                                 Marking initialMarking, ExperimentType type) {

        for (Transition t : net.getTransitions()) {
            TimedTransition tt = (TimedTransition) t;
            if (tt.getDistributionType().equals(DistributionType.IMMEDIATE)) {
                t.setInvisible(true);
            }
        }

        PerformanceEnricherExperimentResult result = new PerformanceEnricherExperimentResult();

        PNSimulator simulator = new PNSimulator();
        // construct a plain copy of the net:
        Petrinet plainNet = StochasticNetUtils.getPlainNet(context, net);

        int[] series = null;
        switch (type) {
            case NOISE_LEVEL_EXPERIMENT:
                series = NOISE_LEVELS;
                break;
            case TRACE_SIZE_EXPERIMENT:
                series = TRACE_SIZES;
                break;
            default:
        }

        int traceSize = 0;

        // Repetitions to average out errors introduced just by sampling from small sets.
        int repetitions = 5;

        for (int step : series) {
            switch (type) {
                case NOISE_LEVEL_EXPERIMENT:
                    traceSize = NOISE_LEVEL_EXPERIMENT_TRACE_SIZE;
                    break;
                case TRACE_SIZE_EXPERIMENT:
                    traceSize = step;
                    break;
            }
            for (ExecutionPolicy policy : ExecutionPolicy.values()) {
                // make sure we use at least a 1000 traces!
//				if (traceSize < NOISE_LEVEL_EXPERIMENT_TRACE_SIZE){
//					repetitions = NOISE_LEVEL_EXPERIMENT_TRACE_SIZE / traceSize;
//				}
//				if (repetitions < 5) {
//					repetitions = 5;
//				}
                ModelComparisonResult[] results = new ModelComparisonResult[repetitions];

                for (int repetition = 0; repetition < repetitions; repetition++) {
                    long seed = repetition;
                    PNSimulatorConfig config = new PNSimulatorConfig(traceSize, UNIT_FACTOR, seed + repetitions, 1, 1000, policy);
                    XLog log = simulator.simulate(null, net, StochasticNetUtils.getSemantics(net), config, initialMarking);

                    NoiseLogFilter noiseFilter = new NoiseLogFilter(2 * seed);

                    if (type.equals(ExperimentType.NOISE_LEVEL_EXPERIMENT)) {
                        noiseFilter.setPercentageAdd(step);
                        noiseFilter.setPercentageRemove(step);
                        log = noiseFilter.introduceNoise(context, log);
                    }

                    PerformanceEnricher enricher = new PerformanceEnricher();
                    Manifest manifest = (Manifest) StochasticNetUtils.replayLog(context, plainNet, log, true, true);
                    PerformanceEnricherConfig mineConfig = new PerformanceEnricherConfig(DISTRIBUTION_TYPE, UNIT_FACTOR, policy, null);
                    Object[] enrichedNet = enricher.transform(context, manifest, mineConfig);
                    StochasticNet learnedNet = (StochasticNet) enrichedNet[0];
                    // Debugging code to view the logspline distribution:
//					if (repetition == 1 && policy.equals(ExecutionPolicy.GLOBAL_PRESELECTION) && step == 50){
//						Iterator<Transition> iter = learnedNet.getTransitions().iterator();
//						while (iter.hasNext()){
//							Transition t = iter.next();
//							PlotPanelFreeChart plot = new PlotPanelFreeChart();
//							TimedTransition tt = (TimedTransition) t;
//							Plot p = new Plot(tt.getLabel());
//							if (tt.getDistributionType().equals(DistributionType.LOGSPLINE)){
//								System.out.println("Distribution for "+tt.getLabel()+" based on these numbers:");
//								System.out.println(Arrays.toString(tt.getDistributionParameters()));
//								p.add(tt.getDistribution());
//								plot.addPlot(p);
//								JFrame frame = new JFrame();
//								frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//								frame.add(plot);
//								frame.setPreferredSize(new Dimension(500,500));
//								frame.setVisible(true);
//							}
//						}
//					}
                    results[repetition] = result.getComparisonResult(net, learnedNet, enricher);
                }

                result.add(step, policy, ModelComparisonResult.getAverage(results));
            }
        }
        System.out.println("-----------------------------");
        System.out.println(result.getResultsCSV(type));
        System.out.println("-----------------------------");
        return result;
    }
}
