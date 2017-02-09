package org.processmining.models.graphbased.directed.petrinet.elements;

import com.google.common.collect.BoundType;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import org.apache.commons.math3.distribution.*;
import org.processmining.models.graphbased.directed.AbstractDirectedGraph;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.distribution.*;
import org.processmining.plugins.stochasticpetrinet.distribution.timeseries.ARMATimeSeries;
import org.processmining.plugins.stochasticpetrinet.distribution.timeseries.SinusoidalSeries;
import org.processmining.plugins.stochasticpetrinet.enricher.StochasticManifestCollector;
import org.utils.datastructures.ComparablePair;

import java.util.Arrays;
import java.util.List;

/**
 * A timed transition to be used in
 * Stochastic Petri Nets
 * with arbitrary distributions
 * <p>
 * Contains both logic of immediate and timed transitions.
 * <p>
 * That way different semantics can be realized.
 * weights:
 * - preselection can be achieved for timed transitions based on the weights
 * - race semantics can be implemented by sampling from the distributions and choosing the transition with less time
 * priority:
 * - priority groups could be used also for timed transitions (non-standard behavior)
 *
 * @author Andreas Rogge-Solti
 * @version 0.5 (added a transient cached view for easier access to the training data to not repeatedly call String.split() on it)
 */
public class TimedTransition extends Transition {

    /**
     * The priority group
     * <p>
     * Only enabled markings of the highest priority group can fire in a marking.
     * Usually only 0 (timed transitions) and 1 (immediate transitions) are used here,
     * but one could specify further higher priority groups for even more
     * urgent immediate transitions.
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
     * Contains the data that was used to train the model
     * (Basically, this is a table with a header to describe the data
     * and each line represents one entry)
     */
    protected String trainingData;

    /**
     * a sorted view on all the training data that was used to train this transition.
     * it is created on request and stays in memory, but is not serialized to disk - ({@link #trainingData} is).
     */
    protected transient SortedMultiset<ComparablePair<Long, List<Object>>> trainingDataCache;

    /**
     * By default generate a timed transition with exponential firing rate lambda=1
     *
     * @param label
     * @param net
     */
    public TimedTransition(String label,
                           AbstractDirectedGraph<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> net) {
        this(label, net, null, 1, 0, DistributionType.EXPONENTIAL, 1);
    }

    public TimedTransition(String label,
                           AbstractDirectedGraph<PetrinetNode, PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> net,
                           ExpandableSubNet parent,
                           double weight,
                           int priority,
                           DistributionType type,
                           double... parameters) {
        super(label, net, parent);
        this.weight = weight;
        this.priority = priority;
        this.distributionType = type;
        this.distributionParameters = parameters;
        this.distribution = initDistribution(Double.MAX_VALUE);
    }

    public RealDistribution initDistribution(double maxValue) {
        RealDistribution dist = null;
        if (distribution == null) {
            switch (distributionType) {
                case BETA:
                    checkParameterLengthForDistribution(2, "alpha", "beta");
                    dist = new BetaDistribution(distributionParameters[0], distributionParameters[1]);
                    break;
                case DETERMINISTIC:
                    dist = new DiracDeltaDistribution(distributionParameters[0]);
                    break;
                case NORMAL:
                    checkParameterLengthForDistribution(2, "mean", "standardDeviation");
                    dist = new NormalDistribution(distributionParameters[0], distributionParameters[1]);
                    break;
                case LOGNORMAL:
                    checkParameterLengthForDistribution(2, "scale", "shape");
                    dist = new LogNormalDistribution(distributionParameters[0], distributionParameters[1]);
                    break;
                case EXPONENTIAL:
                    checkParameterLengthForDistribution(1, "lamda");
                    dist = new ExponentialDistribution(distributionParameters[0]);
                    break;
                case GAMMA:
                    checkParameterLengthForDistribution(2, "shape", "scale");
                    dist = new GammaDistribution(distributionParameters[0], distributionParameters[1]);
                    break;
                case IMMEDIATE:
                    checkParameterLengthForDistribution(0);
                    break;
                case UNIFORM:
                    checkParameterLengthForDistribution(2, "lowerBound", "upperBound");
                    dist = new UniformRealDistribution(distributionParameters[0], distributionParameters[1]);
                    break;
                case GAUSSIAN_KERNEL:
                    dist = fitGaussianKernels();
                    break;
                case HISTOGRAM:
                    if (distributionParameters.length < 1) {
                        throw new IllegalArgumentException("Cannot create a nonparametric distribution without sample values!");
                    }
                    dist = new SimpleHistogramDistribution();
                    ((SimpleHistogramDistribution) dist).addValues(distributionParameters);
                    break;
                case LOGSPLINE:
                    if (distributionParameters.length < 10) {
                        throw new IllegalArgumentException("Cannot create a logspline distribution with less than 10 sample values!");
                    }
                    try {
                        dist = new RLogSplineDistribution(maxValue);
                        ((RLogSplineDistribution) dist).addValues(distributionParameters);
                        dist.getNumericalMean();
                    } catch (NonConvergenceException e) {
                        System.out.println("LogSpline fit not converged! Falling back to Gaussian Kernel density estimation");
                        distributionType = DistributionType.GAUSSIAN_KERNEL;
                        dist = fitGaussianKernels();
                    }
//					catch (TooManyEvaluationsException e){
//						System.out.println("Could not compute the mean of the logspline! Falling back to Gaussian Kernel density estimation");
//						distributionType = DistributionType.GAUSSIAN_KERNEL;
//						fitGaussianKernels();
//					}
                    break;
                case BERNSTEIN_EXPOLYNOMIAL:
                    if (distributionParameters.length < 4) {
                        throw new IllegalArgumentException("Bernstein Expolinomial approximations need at least 5 parameters:\n"
                                + "- a (lower bound)"
                                + "- b (upper bound)"
                                + "- c (scaling factor)"
                                + "- (n + 1) sample points");
                    }
                    Double a = distributionParameters[0];
                    Double b = distributionParameters[1];
                    Double c = distributionParameters[2];
                    double[] nPoints = new double[distributionParameters.length - 3];
                    for (int i = 3; i < distributionParameters.length; i++) {
                        nPoints[i - 3] = distributionParameters[i];
                    }
                    dist = new BernsteinExponentialApproximation(nPoints, a, b, c);
                    break;
                case SINUSOIDAL_SERIES:
                    if (distributionParameters.length != 4) {
                        throw new IllegalArgumentException("Sinusoidal series needs exactly 4 parameters:\n"
                                + "- amplitude (double)\n"
                                + "- period (double)\n"
                                + "- origin (double)\n"
                                + "- noise (double)");
                    }
                    dist = new SinusoidalSeries(distributionParameters[0], distributionParameters[1],
                            distributionParameters[2], distributionParameters[3]);
                    break;
                case ARMA_SERIES:
                    if (distributionParameters.length < 3) {
                        throw new IllegalArgumentException("ARMA processes need at least 3 arguments:\n"
                                + "- the order n of the AR part,\n"
                                + "- the order m of the MA part,\n"
                                + "- the noise standard deviation");
                    }
                    int n = (int) distributionParameters[0];
                    int m = (int) distributionParameters[1];
                    double noiseSd = distributionParameters[2];
                    long lastTime = (long) distributionParameters[3];
                    dist = new ARMATimeSeries(n, m, noiseSd);
                    ((ARMATimeSeries) dist).setLastTime(lastTime);
                    if (distributionParameters.length < 4 + n + m) {
                        throw new IllegalArgumentException("ARMA processes of order n:" + n + ", and m:" + m + " need at least " + (n + m) + " additional parameters beside the 4 arguments:\n"
                                + "- the order n of the AR part,\n"
                                + "- the order m of the MA part,\n"
                                + "- the noise standard deviation\n"
                                + "- the last time index in the series");
                    } else {
                        double[] arWeights = new double[n];
                        double[] maWeights = new double[m];
                        for (int i = 0; i < n; i++) {
                            arWeights[i] = distributionParameters[i + 4];
                        }
                        for (int i = 0; i < m; i++) {
                            maWeights[i] = distributionParameters[i + 4 + n];
                        }
                        ((ARMATimeSeries) dist).setArWeights(arWeights);
                        ((ARMATimeSeries) dist).setMaWeights(maWeights);
                        // rest of the parameters are values and errors
                        int startIndex = 4 + n + m;
                        int valuesRemaining = (distributionParameters.length - startIndex) / 2;
                        for (int i = 0; i < valuesRemaining; i++) {
                            ((ARMATimeSeries) dist).addValue(distributionParameters[startIndex + i], distributionParameters[startIndex + valuesRemaining + i]);
                        }
                    }
                case UNDEFINED:
                    // do nothing
                    break;
                default:
                    throw new IllegalArgumentException(distributionType.toString() + " distributions not supported yet!");
            }
        }
        return dist;
    }

    public RealDistribution fitGaussianKernels() {
        if (distributionParameters.length < 1) {
            throw new IllegalArgumentException("Cannot create a nonparametric distribution without sample values!");
        }
        RealDistribution dist = new GaussianKernelDistribution();
        ((GaussianKernelDistribution) dist).addValues(distributionParameters);
        return dist;
    }

    private void checkParameterLengthForDistribution(int parameters, String... names) {
        if (parameters == 0 && distributionParameters == null) {
            return;
        }
        if (distributionParameters.length != parameters) {
            String errorString = distributionType.toString() + " distributions must have " + parameters + " parameters:\n";
            for (int i = 0; i < parameters; i++) {
                errorString += (i + 1) + ". parameter: " + names[i] + "\n";
            }
            errorString += "But the transition " + getLabel() + " has " + distributionParameters.length + " parameters!";
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

    public void setDistributionParameters(double... parameters) {
        distributionParameters = parameters;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * @param immediate
     */
    public void setImmediate(boolean immediate) {
        if (immediate) {
            distributionType = DistributionType.IMMEDIATE;
            distributionParameters = new double[0];
            priority = 1;
        } else {
            distributionType = DistributionType.EXPONENTIAL;
            priority = 0;
            distributionParameters = new double[]{1};
        }
        distribution = null;
        distribution = initDistribution(0);
    }

    public void setDistribution(RealDistribution dist) {
        this.distribution = dist;
    }

    public void setDistributionType(DistributionType distType) {
        this.distributionType = distType;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Training Data in the form of RELATIVE_DURATION  {@value StochasticManifestCollector#DELIMITER}  SYSTEM_LOAD  {@value StochasticManifestCollector#DELIMITER}  TIMESTAMP
     *
     * @return String
     */
    public String getTrainingData() {
        return trainingData;
    }

    public void setTrainingData(String trainingData) {
        this.trainingData = trainingData;
    }

    public SortedMultiset<ComparablePair<Long, List<Object>>> getTrainingDataUpTo(long currentTime) {
        if (this.trainingDataCache == null) {
            // init training data cache:
            SortedMultiset<ComparablePair<Long, List<Object>>> sortedTrainingData = TreeMultiset.<ComparablePair<Long, List<Object>>>create();
            String[] entries = trainingData.split("\n");
            // ignore header!
            for (int i = 1; i < entries.length; i++) {
                String[] entryParts = entries[i].split(StochasticManifestCollector.DELIMITER);
                long time = Long.valueOf(entryParts[2]);
                sortedTrainingData.add(new ComparablePair<>(time, Arrays.<Object>asList(Double.valueOf(entryParts[0]), Double.valueOf(entryParts[1]))));
            }
            this.trainingDataCache = sortedTrainingData;
        }
        // retrieve subset of training data that fulfills condition
        return this.trainingDataCache.headMultiset(new ComparablePair<Long, List<Object>>(currentTime, null), BoundType.OPEN);
    }

}
