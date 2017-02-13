package org.processmining.plugins.stochasticpetrinet.prediction.timeseries;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientStochasticNetSemanticsImpl;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.prediction.AbstractTimePredictor;
import org.processmining.plugins.stochasticpetrinet.prediction.TimePredictor;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulator;
import org.processmining.plugins.stochasticpetrinet.simulator.PNSimulatorConfig;
import org.processmining.plugins.stochasticpetrinet.simulator.PNTimeSeriesSimulator;

import java.util.Date;

public class TimeseriesPredictor extends AbstractTimePredictor {


    private final TimeSeriesConfiguration config;
    private PNSimulator simulator;

    public TimeseriesPredictor() {
        this(new TimeSeriesConfiguration());
    }

    public TimeseriesPredictor(TimeSeriesConfiguration config) {
        // perform a headless simulation
        this.simulator = new PNTimeSeriesSimulator(config);
        this.config = config;
    }


    protected DescriptiveStatistics getPredictionStats(StochasticNet model, XTrace observedEvents, Date currentTime,
                                                       boolean useOnlyPastTrainingData, Semantics<Marking, Transition> semantics) {
        Marking currentMarking = semantics.getCurrentState();

        PNSimulatorConfig config = new PNSimulatorConfig(1, model.getTimeUnit());
        config.setSimulateTraceless(true);
        Long lastEventTime;
        if (observedEvents.isEmpty()) {
            lastEventTime = currentTime.getTime();
        } else {
            lastEventTime = XTimeExtension.instance().extractTimestamp(observedEvents.get(observedEvents.size() - 1))
                    .getTime();
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        //long now = System.currentTimeMillis();

        StochasticNetUtils.useCache(true);
        double errorPercent = 100; // percentage in error of 99% confidence band
        int i = 0;
        long now = System.currentTimeMillis();
        while (errorPercent > ERROR_BOUND_PERCENT && i < MAX_RUNS) {
            i++;
            //			long now = System.currentTimeMillis();
            stats.addValue((Long) simulator.simulateOneTrace(model, semantics, config, currentMarking, lastEventTime,
                    currentTime.getTime(), i, false, null));
            //			System.out.println("Took "+(System.currentTimeMillis()-now)+" ms to sample one trace!");
            semantics.setCurrentState(currentMarking);
            if (i % 100 == 0) {
                // update error:
                errorPercent = getErrorPercent(stats);
            }
        }
        long timeTaken = System.currentTimeMillis() - now;
        if (timeTaken > 1000) {
            System.out.println("Took " + timeTaken + "ms to sample one case.");
        }
        return stats;
    }

    public String getCode(){
        return config.getTimeseriesType().toString();
    }

//	private List<Set<Transition>> getConflictingTransitions(Semantics<Marking, Transition> semantics){
//		// assume free choice!
//		List<Set<Transition>> conflictingTransitions = new LinkedList<>();
//		Marking marking = semantics.getCurrentState();
//		Iterator<Place> markingIterator = marking.iterator();
//		while (markingIterator.hasNext()){
//			Place p = markingIterator.next();
//			Collection<PetrinetEdge<? extends PetrinetNode,? extends PetrinetNode>> outEdges = p.getGraph().getOutEdges(p);
//			if (outEdges.size() > 1){
//				Set<Transition> conflictingTransitionsForThisPlace = new HashSet<>();
//				for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> edge : outEdges){
//					conflictingTransitionsForThisPlace.add((Transition) edge.getTarget());
//				}
//				conflictingTransitions.add(conflictingTransitionsForThisPlace);
//			}
//		}
//		return conflictingTransitions;
//	}
}
