package org.processmining.models.graphbased.directed.petrinet;

import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;

public interface StochasticNet extends ResetNet, Petrinet{

	/**  
	 * Supported parametric distributions
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
	
	// immediate transitions
	TimedTransition addImmediateTransition(String label);
	TimedTransition addImmediateTransition(String label, double weight);
	TimedTransition addImmediateTransition(String label, double weight, int priority);
	
	// timed transitions
	TimedTransition addTimedTransition(String label, DistributionType type, double... distributionParameters);
	TimedTransition addTimedTransition(String label, double weight, DistributionType type, double... distributionParameters);
}
