package org.processmining.models.graphbased.directed.petrinet;

import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;

/**
 * "stochastic Petri net" = Petri net with annotated stochastic information.
 * Orientation after GSPN formalism with immediate transitions and timed transitions.
 * 
 * Transitions have <b>priority</b> (only transitions with highest priority can fire when enabled).
 * 
 * Transitions have a <b>weight</b>, if multiple immediate transitions are enabled concurrently, a
 * probabilistic choice is made based on their weights, to decide which one can fire first.
 * 
 * Timed transitions can have arbitrary distributions. See supported {@link DistributionType}s.
 *  
 * @author Andreas Rogge-Solti
 *
 */
public interface StochasticNet extends ResetNet, Petrinet{

	/**  
	 * Supported parametric and non-parametric distributions
	 */
	public enum DistributionType{
		// parametric continuous distributions
		BETA, EXPONENTIAL, NORMAL, LOGNORMAL, GAMMA, STUDENT_T, UNIFORM, WEIBULL,
		// nonparametric continuous distributions
		GAUSSIAN_KERNEL,HISTOGRAM,LOG_SPLINE,
		// immediate transitions
		IMMEDIATE, 
		// a deterministic transition (e.g. takes always exactly 5 time units)
		DETERMINISTIC,
		// undefined
		UNDEFINED;

		public static DistributionType fromString(String text) {
			if (text == null){
				return UNDEFINED;
			}
			for (DistributionType dType : DistributionType.values()){
				if (text.equalsIgnoreCase(dType.toString())){
					return dType;
				}
			}
			return UNDEFINED;
		}
	}

	/**
	 * Execution policy of the network.
	 * 
	 * @see paper:
	 * Ajmone Marsan, M., et al. "The effect of execution policies on the semantics and analysis of stochastic Petri nets." Software Engineering, IEEE Transactions on 15.7 (1989): 832-846.
	 *
	 */
	public enum ExecutionPolicy{
		GLOBAL_PRESELECTION, RACE_RESAMPLING, RACE_ENABLING_MEMORY, RACE_AGE_MEMORY;
	}
	
	// immediate transitions
	TimedTransition addImmediateTransition(String label);
	TimedTransition addImmediateTransition(String label, double weight);
	TimedTransition addImmediateTransition(String label, double weight, int priority);
	
	// timed transitions
	TimedTransition addTimedTransition(String label, DistributionType type, double... distributionParameters);
	TimedTransition addTimedTransition(String label, double weight, DistributionType type, double... distributionParameters);
}
