package org.processmining.plugins.stochasticpetrinet.simulator;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;

/**
 * Configuration parameters for the simple simulation of (stochastic) Petri Nets.
 * Used by {@link PNSimulator}.
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class PNSimulatorConfig {
	
	/** The number of traces to simulate */
	int numberOfTraces;
	
	/** The arrival rate (new traces will be created with a Poisson distribution with this arrival rate) */
	double arrivalRate;
	
	/** The random seed to make results reproducible */
	long seed;
	
	/** In case of loopy models, this is useful to prevent infinite simulation. 
	 * We simply stop simulation for one trace, when this number of events is reached. */
	int maxEventsInOneTrace;
	
	/** 
	 * The name of the log to be generated.
	 */
	String logName;
	
	/**
	 * {@link TimeUnit} conversion factor to milliseconds in the log
	 * E.g., if the model times are specified in <b>minutes</b>, the sampled values (e.g., 5) 
	 * must be multiplied by this factor (e.g., 60*1000) 
	 */
	TimeUnit unitFactor;
	
	/**
	 * If the stochastic Petri net is not restricted to memoryless transitions, i.e., negative exponential ones, 
	 * the firing selection policy and the memory policy needs to be selected. This is called the execution policy.
	 */
	ExecutionPolicy executionPolicy;
	
	/**
	 * For a deterministic mode that tries to cover a large share of the allowed structural behavior in the model.
	 * In case of loopy models, the state space is infinite! 
	 * Therefore, only create traces that finish within the given number of {@link #maxEventsInOneTrace}.
	 */
	boolean deterministicBoundedStateSpaceExploration = false;
	
	/**
	 * If this is set to false, the simulation aborts with an {@link IllegalArgumentException}, 
	 * once there are two or more tokens on any place
	 */
	boolean allowUnbounded = true;
	
	/**
	 * Counter variable to provide a basic form of unique labeling of logs
	 * generated by the of consequent use of the simulator.
	 */
	private static int counter = 1;
	
	
	public PNSimulatorConfig(int numberOfTraces, StochasticNet net){
		this(numberOfTraces,net.getTimeUnit(),1,1,10000,net.getExecutionPolicy());
	}
	
	public PNSimulatorConfig(){
		this(1000);
	}
	public PNSimulatorConfig(int numberOfTraces){
		this(numberOfTraces, TimeUnit.MINUTES);
	}
	public PNSimulatorConfig(int numberOfTraces, TimeUnit unitFactor){
		this(numberOfTraces,unitFactor,1);
	}
	public PNSimulatorConfig(int numberOfTraces, TimeUnit unitFactor, long seed){
		this(numberOfTraces,unitFactor, seed, 1);
	}
	public PNSimulatorConfig(int numberOfTraces, TimeUnit unitFactor, long seed, double arrivalRate){
		this(numberOfTraces,unitFactor, seed, arrivalRate, 10000);
	}
	public PNSimulatorConfig(int numberOfTraces, TimeUnit unitFactor, long seed, double arrivalRate, int maxEventsInOneTrace){
		this(numberOfTraces,unitFactor, seed, arrivalRate, maxEventsInOneTrace, ExecutionPolicy.RACE_ENABLING_MEMORY);
	}
	public PNSimulatorConfig(int numberOfTraces, TimeUnit unitFactor, long seed, double arrivalRate, int maxEventsInOneTrace, ExecutionPolicy policy){
		this(numberOfTraces,unitFactor, seed, arrivalRate, maxEventsInOneTrace, policy, "Log"+counter++);
	}
	/**
	 * 
	 * @param numberOfTraces the number of traces to generate by simulation.
	 * @param unitFactor the time units factor represented in the stochastic distributions (1 = milliseconds, 1000 = seconds, 60.000=minutes...)
	 * @param seed the seed for the pseudo-random generator (helps to generate the same results, if required)
	 * @param arrivalRate the arrival rate lamda for the poisson distribution used to simulate the arriving process of new cases 
	 * @param maxEventsInOneTrace in order to avoid running into infinite loops, there is this variable that sets an upper limit.
	 * @param policy the {@link ExecutionPolicy} of the network, i.e., how to select the next transition (preselection/race), and how to deal with transitions that lose a race. 
	 * @param logName the name of the generated log.
	 */
	public PNSimulatorConfig(int numberOfTraces, TimeUnit unitFactor, long seed, double arrivalRate, int maxEventsInOneTrace, ExecutionPolicy policy, String logName){
		this.seed = seed;
		this.numberOfTraces = numberOfTraces;
		this.arrivalRate = arrivalRate;
		this.unitFactor = unitFactor; // default to seconds (= 1000 ms)
		this.maxEventsInOneTrace = maxEventsInOneTrace;
		this.logName = logName;
		this.executionPolicy = policy;
	}

	public boolean isDeterministicBoundedStateSpaceExploration() {
		return deterministicBoundedStateSpaceExploration;
	}

	public void setDeterministicBoundedStateSpaceExploration(boolean deterministicBoundedStateSpaceExploration) {
		this.deterministicBoundedStateSpaceExploration = deterministicBoundedStateSpaceExploration;
	}

	public boolean isAllowUnbounded() {
		return allowUnbounded;
	}

	public void setAllowUnbounded(boolean allowUnbounded) {
		this.allowUnbounded = allowUnbounded;
	}
	
}
