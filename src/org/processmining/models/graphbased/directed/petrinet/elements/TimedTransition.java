package org.processmining.models.graphbased.directed.petrinet.elements;

import java.util.List;

import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.processmining.models.graphbased.directed.AbstractDirectedGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.distribution.DiracDeltaDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.GaussianKernelDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.RLogSplineDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.SimpleHistogramDistribution;

/**
 * A timed transition to be used in 
 * Stochastic Petri Nets 
 * with arbitrary distributions
 * 
 * Contains both logic of immediate and timed transitions.
 * 
 * That way different semantics can be realized.
 * weights: 
 * - preselection can be achieved for timed transitions based on the weights
 * - race semantics can be implemented by sampling from the distributions and choosing the transition with less time 
 * priority:
 * - priority groups could be used also for timed transitions (non-standard behavior)
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class TimedTransition extends Transition{
	
	/**
	 * The priority group
	 * 
	 * Only enabled markings of the highest priority group can fire in a marking.
	 * Usually only 0 (timed transitions) and 1 (immediate transitions) are used here,
	 * but one could specify further higher priority groups for even more 
	 * urgent immediate transitions.
	 * 
	 */
	protected int priority;
	
	/**
	 * The weight of the transition
	 * If multiple immediate transitions are enabled concurrently, 
	 * the decision which one to fire is done on a probabilistic basis.
	 * Each transition has the chance of firing:
	 * weight / (sum of weights of currently enabled transitions)  
	 */
	protected double weight;
	
	
	/**
	 * The distribution to sample from when a timer is requested.
	 */
	protected RealDistribution distribution;
	
	/**
	 * The {@link DistributionType} which determines the 
	 * parametric or non-parametric shape of the distribution
	 */
	protected DistributionType distributionType;
	
	/**
	 * The parameters for the parameterized distribution,
	 * or the observations to generate a non-parametric distribution of. 
	 */
	protected double[] distributionParameters;
	
	/**
	 * By default generate a timed transition with exponential firing rate lamda=1
	 * 
	 * @param label
	 * @param net
	 */
	public TimedTransition(String label,
			AbstractDirectedGraph<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> net) {
		this(label, net, null,1,0,DistributionType.EXPONENTIAL,1);
	}

	public TimedTransition(String label,
			AbstractDirectedGraph<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> net,
			ExpandableSubNet parent,
			double weight,
			int priority,
			DistributionType type,
			double...parameters) {
		super(label, net, parent);
		this.weight = weight;
		this.priority = priority;
		this.distributionType = type;
		this.distributionParameters = parameters;
		initDistribution();
	}

	public void initDistribution() {
		if (distribution == null){
			switch(distributionType){
				case BETA:
					checkParameterLengthForDistribution(2, "alpha", "beta");
					distribution = new BetaDistribution(distributionParameters[0], distributionParameters[1]);
					break;
				case DETERMINISTIC:
					distribution = new DiracDeltaDistribution(distributionParameters[0]);
					break;
				case NORMAL:
					checkParameterLengthForDistribution(2, "mean", "standardDeviation");
					distribution = new NormalDistribution(distributionParameters[0], distributionParameters[1]);
					break;
				case LOGNORMAL:
					checkParameterLengthForDistribution(2, "scale", "shape");
					distribution = new LogNormalDistribution(distributionParameters[0], distributionParameters[1]);
					break;
				case EXPONENTIAL:
					checkParameterLengthForDistribution(1, "lamda");
					distribution = new ExponentialDistribution(distributionParameters[0]);
					break;
				case GAMMA:
					checkParameterLengthForDistribution(2, "shape","scale");
					distribution = new GammaDistribution(distributionParameters[0], distributionParameters[1]);
					break;
				case IMMEDIATE:
					checkParameterLengthForDistribution(0);
					break;
				case UNIFORM:
					checkParameterLengthForDistribution(2, "lowerBound", "upperBound");
					distribution = new UniformRealDistribution(distributionParameters[0], distributionParameters[1]);
					break;
				case GAUSSIAN_KERNEL:
					if (distributionParameters.length < 1){
						throw new IllegalArgumentException("Cannot create a nonparametric distribution without sample values!");
					}
					distribution = new GaussianKernelDistribution();
					((GaussianKernelDistribution)distribution).addValues(distributionParameters);
					break;
				case HISTOGRAM:
					if (distributionParameters.length < 1){
						throw new IllegalArgumentException("Cannot create a nonparametric distribution without sample values!");
					}
					distribution = new SimpleHistogramDistribution();
					((SimpleHistogramDistribution)distribution).addValues(distributionParameters);
					break;
				case LOG_SPLINE:
					if (distributionParameters.length < 10){
						throw new IllegalArgumentException("Cannot create a logspline distribution with less than 10 sample values!");
					}
					distribution = new RLogSplineDistribution();
					((RLogSplineDistribution)distribution).addValues(distributionParameters);
					break;
				case UNDEFINED:
					// do nothing
					break;
				default:
					throw new IllegalArgumentException(distributionType.toString()+" distributions not supported yet!");
			}
		}
	}

	private void checkParameterLengthForDistribution(int parameters, String... names) {
		if (parameters == 0 && distributionParameters == null){
			return;
		}
		if (distributionParameters.length != parameters){
			String errorString = distributionType.toString()+" distributions must have "+parameters+" parameters:\n";
			for (int i = 0; i < parameters; i++){
				errorString+=(i+1)+". parameter: "+names[i]+"\n";
			}
			errorString+= "But the transition "+getLabel()+" has "+distributionParameters.length+" parameters!";
			throw new IllegalArgumentException(errorString);
		}
	}

	public int getPriority() {
		return priority;
	}

	public double getWeight() {
		return weight;
	}

	public RealDistribution getDistribution() {
		return distribution;
	}

	public DistributionType getDistributionType() {
		return distributionType;
	}

	public double[] getDistributionParameters() {
		return distributionParameters;
	}

	public void setDistributionParameters(List<Double> transitionStats) {
		distributionParameters = StochasticNetUtils.getAsDoubleArray(transitionStats);
	}
	public void setDistributionParameters(double[] parameters){
		distributionParameters = parameters;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	/**
	 * 
	 * @param immediate
	 */
	public void setImmediate(boolean immediate) {
		if (immediate){
			distributionType = DistributionType.IMMEDIATE;
			distributionParameters = new double[0];
			priority = 1;
		} else {
			distributionType = DistributionType.EXPONENTIAL;
			priority = 0;
			distributionParameters = new double[]{1};
		}
		distribution = null;
		initDistribution();
	}

	public void setDistribution(RealDistribution dist){
		this.distribution = dist;
	}
	
	public void setDistributionType(DistributionType distType) {
		this.distributionType = distType;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
}
