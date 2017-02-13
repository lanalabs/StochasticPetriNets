package org.processmining.plugins.stochasticpetrinet.prediction;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;

import java.util.Date;

public class TimePredictor extends AbstractTimePredictor {

    private boolean useTimeContraint;

    /**
     * Creates a stochastic Net predictor based on simulation that uses either the time as a constraint, or not.
     *
     * @param useTimeConstraint indicator, whether to use the elapsed time as a constraint for the simulation
     */
    public TimePredictor(boolean useTimeConstraint) {
        this.useTimeContraint = useTimeConstraint;
    }

    public DescriptiveStatistics getPredictionStats(StochasticNet model, XTrace observedEvents, Date currentTime, boolean useOnlyPastTrainingData, Semantics<Marking, Transition> semantics) {
        if (semantics.getCurrentState() == null) {
            System.out.println("Debug me!");
        }
        Marking currentMarking = semantics.getCurrentState();
        Long lastEventTime;
        if (observedEvents.isEmpty()) {
            lastEventTime = currentTime.getTime();
        } else {
            lastEventTime = XTimeExtension.instance().extractTimestamp(observedEvents.get(observedEvents.size() - 1)).getTime();
        }
//		System.out.println("Time between last event and current time: "+(currentTime.getTime()-lastEventTime)+"ms");
        PNSimulator simulator = new PNSimulator();
        simulator.setUseOnlyPastTrainingData(useOnlyPastTrainingData);
        PNSimulatorConfig config = new PNSimulatorConfig(1, model.getTimeUnit());
        config.setSimulateTraceless(true); // do not create traces, as the generation of unique ids is just too slow.

        DescriptiveStatistics stats = new DescriptiveStatistics();
        //long now = System.currentTimeMillis();

//		StochasticNetUtils.useCache(true);
        double errorPercent = 100; // percentage in error of 99% confidence band
        int i = 0;
//		double error = 1000000;
//		while (error > ABS_ERROR_THRESHOLD && i < MAX_RUNS){
        while (errorPercent > ERROR_BOUND_PERCENT && i < MAX_RUNS) {
            i++;
            stats.addValue((Long) simulator.simulateOneTrace(model, semantics, config, currentMarking, lastEventTime, currentTime.getTime(), i, useTimeContraint, null));
            semantics.setCurrentState(currentMarking);
            if (i % 300 == 0) {
                // update error:
                errorPercent = getErrorPercent(stats);
//				error = getError(stats);
            }
        }
        if (i > 300) System.out.println("i " + i);
        return stats;
    }
}
