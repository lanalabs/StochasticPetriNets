package org.processmining.plugins.stochasticpetrinet.generator;

import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.StochasticNetImpl;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.distribution.timeseries.ARMATimeSeries;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Generates block-structured Petri nets with an iterative mechanism.
 * Usable for scalability analysis of algorithms.
 *
 * @author Andreas Rogge-Solti
 */
public class Generator {

    private int pId = 0; // place counter
    private int tId = 0; // transition counter
    private int tRoutingId = 0; // routing transition counter

    private enum Structure {
        SEQUENCE, XOR, AND, LOOP;
    }

    private Random random;

    public Generator(long seed) {
        resetCounters();
        random = new Random(seed);
    }

    private void resetCounters() {
        pId = 0;
        tId = 0;
        tRoutingId = 0;
    }

    /**
     * @param config {@link GeneratorConfig}
     * @return Object[] consisting of the stochastic net [index 0], the initial marking [index 1] and the final marking [index 2]
     */
    public Object[] generateStochasticNet(GeneratorConfig config) {
        resetCounters();
        StochasticNet net = new StochasticNetImpl(config.getName());
        // add basic structure: place -> transition -> place

        net.setExecutionPolicy(config.getExecutionPolicy());
        net.setTimeUnit(config.getTimeUnit());

        Marking initialMarking = new Marking();
        Marking finalMarking = new Marking();


        Place p1 = net.addPlace("pStart");
        if (!config.isCreateDedicatedImmediateStartTransition()) {
            initialMarking.add(p1);
        }

        Place pEnd = net.addPlace("pEnd");
        finalMarking.add(pEnd);

        Transition t = net.addTimedTransition(nextRealTransitionName(), config.getDistributionType(), getRandomParameters(config.getDistributionType()));

        net.addArc(p1, t);
        net.addArc(t, pEnd);

        int counter = 1;
        while (counter < config.getTransitionSize()) {
            addRandomStructure(net, config, counter++);
        }

        if (config.isCreateDedicatedImmediateStartTransition()) {
            Place pStart = net.addPlace("pInit");
            initialMarking.add(pStart);
            Transition tStart = net.addTimedTransition("tStart", DistributionType.IMMEDIATE);
            tStart.setInvisible(false);

            net.addArc(pStart, tStart);
            net.addArc(tStart, p1);
        }

        return new Object[]{net, initialMarking, finalMarking};
    }


    /**
     * Adds a random block-structured element to the net.
     * Ensures that all timed transitions have only one incoming and outgoing place.
     *
     * @param net    {@link StochasticNet}
     * @param config {@link GeneratorConfig}
     * @return int number of nodes added to the net
     */
    private void addRandomStructure(StochasticNet net, GeneratorConfig config, int counter) {
        Structure nextStructure = getNextStructure(config, counter);
        Transition t = pickNextTimedTransition(net.getTransitions());
        // get places around the transition:
        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = t.getGraph().getInEdges(t);
        assert (inEdges.size() == 1);
        Place inPlace = (Place) inEdges.iterator().next().getSource();

        Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = t.getGraph().getOutEdges(t);
        assert (outEdges.size() == 1);
        Place outPlace = (Place) outEdges.iterator().next().getTarget();

        // add new timed Transition
        Transition newTransition = net.addTimedTransition(nextRealTransitionName(), config.getDistributionType(), getRandomParameters(config.getDistributionType()));

        switch (nextStructure) {
            case SEQUENCE:
                net.removeArc(t, outPlace);
                // add new Place:
                Place inBetweenPlace = net.addPlace(nextPlaceName());
                // connect new elements:
                net.addArc(t, inBetweenPlace);
                net.addArc(inBetweenPlace, newTransition);
                net.addArc(newTransition, outPlace);
                break;
            case XOR:
                // remove arc to transition:
                net.removeArc(inPlace, t);
                // add immediate choice transitions:
                double weightUp = random.nextDouble() * 0.6 + 0.2; // (uniform between 0.2-0.8)
                TimedTransition up = net.addImmediateTransition(nextTransitionName(), weightUp);
                up.setInvisible(config.isImmedateTransitionsInvisible());
                Place upPlace = net.addPlace(nextPlaceName());
                TimedTransition down = net.addImmediateTransition(nextTransitionName(), 1 - weightUp);
                down.setInvisible(config.isImmedateTransitionsInvisible());
                Place downPlace = net.addPlace(nextPlaceName());
                // connect everything:
                net.addArc(inPlace, up);
                net.addArc(inPlace, down);
                net.addArc(up, upPlace);
                net.addArc(upPlace, t);
                net.addArc(down, downPlace);
                net.addArc(downPlace, newTransition);
                net.addArc(newTransition, outPlace);
                break;
            case AND:
                // remove arcs to transition:
                net.removeArc(inPlace, t);
                net.removeArc(t, outPlace);
                // add split and join:
                TimedTransition split = net.addImmediateTransition(nextTransitionName() + "_split");
                split.setInvisible(config.isImmedateTransitionsInvisible());
                TimedTransition join = net.addImmediateTransition(nextTransitionName() + "_join");
                join.setInvisible(config.isImmedateTransitionsInvisible());
                // add corresponding places
                Place pUpper1 = net.addPlace(nextPlaceName());
                Place pUpper2 = net.addPlace(nextPlaceName());
                Place pLower1 = net.addPlace(nextPlaceName());
                Place pLower2 = net.addPlace(nextPlaceName());
                // connect everything
                net.addArc(inPlace, split);
                // upper branch:
                net.addArc(split, pUpper1);
                net.addArc(pUpper1, t);
                net.addArc(t, pUpper2);
                net.addArc(pUpper2, join);
                // lower branch:
                net.addArc(split, pLower1);
                net.addArc(pLower1, newTransition);
                net.addArc(newTransition, pLower2);
                net.addArc(pLower2, join);
                net.addArc(join, outPlace);
                break;
            case LOOP:
                // remove outgoing arc:
                net.removeArc(t, outPlace);
                // add leaving branch and returning branch:
                double weightLeave = random.nextDouble() * 0.5 + 0.4; // (uniform between 0.5-0.9)
                Transition tLeave = net.addImmediateTransition(nextTransitionName() + "_leaveLoop", weightLeave);
                tLeave.setInvisible(config.isImmedateTransitionsInvisible());
                Transition tStay = net.addImmediateTransition(nextTransitionName() + "_stayInLoop", 1 - weightLeave);
                tStay.setInvisible(config.isImmedateTransitionsInvisible());

                Place pDecide = net.addPlace(nextPlaceName());
                Place pLoopBack = net.addPlace(nextPlaceName());
                // connect everything:
                net.addArc(t, pDecide);
                net.addArc(pDecide, tStay);
                net.addArc(tStay, pLoopBack);
                net.addArc(pLoopBack, newTransition);
                net.addArc(newTransition, inPlace);
                net.addArc(pDecide, tLeave);
                net.addArc(tLeave, outPlace);
                break;
        }
    }


    /**
     * Selects a transition randomly
     *
     * @param transitions
     * @return
     */
    private Transition pickNextTimedTransition(Collection<Transition> transitions) {
        List<Transition> timedTransitions = new LinkedList<Transition>();
        for (Transition t : transitions) {
            if (t instanceof TimedTransition) {
                TimedTransition timedTransition = (TimedTransition) t;
                if (!timedTransition.getDistributionType().equals(DistributionType.IMMEDIATE)) {
                    timedTransitions.add(timedTransition);
                }
            }
        }
        return timedTransitions.get(random.nextInt(timedTransitions.size()));
    }

    /**
     * Picks a structure randomly according to the configuration.
     *
     * @param config
     * @param counter
     * @return
     */
    private Structure getNextStructure(GeneratorConfig config, int counter) {
        int xors = config.getDegreeOfExclusiveChoices();
        int seqs = config.getDegreeOfSequences();
        int pars = config.getDegreeOfParallelism();
        if (config.isParallelismOnlyInParts() && counter < config.getTransitionSize() / 2.) {
            pars = 0;
        }
        int loops = config.isContainsLoops() ? config.getDegreeOfLoops() : 0;
        int allTogether = xors + seqs + pars + loops;
        int pick = random.nextInt(allTogether);
        pick -= xors;
        if (pick < 0) {
            return Structure.XOR;
        }
        pick -= seqs;
        if (pick < 0) {
            return Structure.SEQUENCE;
        }
        pick -= pars;
        if (pick < 0) {
            return Structure.AND;
        } else {
            return Structure.LOOP;
        }
    }

    /**
     * returns random parameters of distributions, such that
     * the mean is between 1 and 10 and
     * the variance is around 2
     *
     * @param type
     * @return
     */
    public double[] getRandomParameters(DistributionType type) {
//		// parametric continuous distributions
//				BETA, EXPONENTIAL, NORMAL, LOGNORMAL, GAMMA, STUDENT_T, UNIFORM, WEIBULL,
//				// nonparametric continuous distributions
//				GAUSSIAN_KERNEL,HISTOGRAM,LOGSPLINE,
//				// immediate transitions
//				IMMEDIATE, 
//				// a deterministic transition (e.g. takes always exactly 5 time units)
//				DETERMINISTIC,
//				// undefined
//				UNDEFINED;
        switch (type) {
            case IMMEDIATE:
                return null;
            case BETA:
                return new double[]{random.nextDouble() * 10 + 0.5, random.nextDouble() * 10 + 0.5};
            case UNIFORM:
                double lowerBound = random.nextDouble();
                double upperBound = (Math.sqrt(12) * (0.9 + random.nextDouble())) + lowerBound;
                return new double[]{lowerBound, upperBound};
            case EXPONENTIAL:
                double var = random.nextDouble() + 1.5;
                return new double[]{Math.sqrt(var)};
            case DETERMINISTIC:
                return new double[]{0.01 + (random.nextDouble() * 10)};
            case NORMAL:
                double mean = random.nextDouble() * 3 + 1; // uniform 1-10
                double sd = 0.30 + (2 * random.nextDouble());
                return new double[]{mean, sd};
            case LOGNORMAL:
                // meanlog 			  sdlog
                // 1.33927791068597, 0.28160530554971
                // meanlog            sdlog
                // 1.34072542813359  0.27664373206155
                double logMean = 1.0 + (random.nextDouble() * 0.004 - 0.002);
                double logSd = 0.45 + (random.nextDouble() * 0.04 - 0.02);
                return new double[]{logMean, logSd};
            case GAUSSIAN_KERNEL:
                mean = random.nextDouble() * 5 + 1; // uniform 1-10
                sd = Math.abs(1.2 + (0.24 * random.nextDouble()));
                // use a sample from the normal dist
                int samples = random.nextInt(50) + 50;
                double[] params = new double[samples];
                for (int i = 0; i < samples; i++) {
                    params[i] = random.nextGaussian() * sd + mean;
                }
                return params;
            case HISTOGRAM:
            case LOGSPLINE:
                List<Double> values = new ArrayList<Double>();
                // 1-5 randomly chosen heaps in the range of 1-20
                int heaps = random.nextInt(5) + 1;
                for (int i = 0; i < heaps; i++) {
                    int valuesInThisHeap = random.nextInt(50) + 10;
                    double heapMean = random.nextInt(20) + 1;
                    double heapSd = heapMean * (0.1 + random.nextDouble() * 0.3);
                    for (int v = 0; v < valuesInThisHeap; v++) {
                        values.add(random.nextGaussian() * heapSd + heapMean);
                    }
                }
                return StochasticNetUtils.getAsDoubleArray(values);
            case SINUSOIDAL_SERIES:
                // amplitude, period, origin, noise
                double randomAmplitude = 1 + random.nextDouble() * 9;
                double randomPeriod = 0.1 + random.nextDouble() * 7;
                double randomOrigin = 10 + random.nextDouble() * 5;
                double randomNoiseStandardDeviation = 0.1 + random.nextDouble() * 2.9;
                return new double[]{randomAmplitude, randomPeriod, randomOrigin, randomNoiseStandardDeviation};
            case ARMA_SERIES:
                // n=2, m=3, random noise,
                //ARMATimeSeries series = new ARMATimeSeries(random.nextInt(4), random.nextInt(4), random.nextDouble()+0.5);
                ARMATimeSeries series = new ARMATimeSeries(random.nextInt(4), random.nextInt(4), random.nextDouble() + 0.5);
                //create a random walk
//				ARMATimeSeries series = new ARMATimeSeries(1, 0, random.nextDouble()+0.0001);
//				series.setArWeights(new double[]{1.});
                series.setCurrentTime(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(10));
                series.sample();
                return series.getParameters();
            default:
                throw new IllegalArgumentException("type " + type + " not supported yet!");
        }
    }

    private String nextTransitionName() {
        String name = "t" + tRoutingId;
        tRoutingId++;
        return name;
    }

    private String nextRealTransitionName() {
        String name = "t" + getNameForNumber(tId);
        tId++;
        return name;
    }

    public static String getNameForNumber(int i) {
        final CharSequence cs = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String label = "";
        while (i / cs.length() > 0) {
            label = cs.charAt(i % cs.length()) + label;
            i = i / cs.length() - 1;
        }
        return cs.charAt(i) + label;
    }

    private String nextPlaceName() {
        return "p" + pId++;
    }
}
