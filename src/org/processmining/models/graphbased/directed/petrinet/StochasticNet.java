package org.processmining.models.graphbased.directed.petrinet;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;

/**
 * "stochastic Petri net" = Petri net with annotated stochastic information.
 * Orientation after GSPN formalism with immediate transitions and timed transitions.
 * <p>
 * Transitions have <b>priority</b> (only transitions with highest priority can fire when enabled).
 * <p>
 * Transitions have a <b>weight</b>, if multiple immediate transitions are enabled concurrently, a
 * probabilistic choice is made based on their weights, to decide which one can fire first.
 * <p>
 * Timed transitions can have arbitrary distributions. See supported {@link DistributionType}s.
 *
 * @author Andreas Rogge-Solti
 */
public interface StochasticNet extends ResetNet, Petrinet {

    public static final String PARAMETER_LABEL = "Stochastic Petri Net";

    /**
     * Supported parametric and non-parametric distributions
     */
    public enum DistributionType {
        // parametric continuous distributions
        BETA, EXPONENTIAL, NORMAL, LOGNORMAL, GAMMA, STUDENT_T, UNIFORM, WEIBULL,
        // nonparametric continuous distributions
        GAUSSIAN_KERNEL, HISTOGRAM, LOGSPLINE, BERNSTEIN_EXPOLYNOMIAL,
        // immediate transitions
        IMMEDIATE,
        // a deterministic transition (e.g. takes always exactly 5 time units)
        DETERMINISTIC,
        // time series distribution
        SINUSOIDAL_SERIES, ARMA_SERIES,
        // undefined
        UNDEFINED;

        public static DistributionType fromString(String text) {
            if (text == null) {
                return UNDEFINED;
            }
            for (DistributionType dType : DistributionType.values()) {
                if (text.equalsIgnoreCase(dType.toString())) {
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
     */
    public enum ExecutionPolicy {
        GLOBAL_PRESELECTION("global preselection"), RACE_RESAMPLING("race (resampling)"),
        RACE_ENABLING_MEMORY("race (enabling memory)"), RACE_AGE_MEMORY("race (age memory)");

        private String name;

        public String shortName() {
            switch (this) {
                case GLOBAL_PRESELECTION:
                    return "preSel";
                case RACE_AGE_MEMORY:
                    return "raceAge";
                case RACE_ENABLING_MEMORY:
                    return "raceEnabl";
                case RACE_RESAMPLING:
                    return "raceResampl";
            }
            return null;
        }

        ExecutionPolicy(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

        public static ExecutionPolicy fromString(String value) {
            for (ExecutionPolicy ep : ExecutionPolicy.values()) {
                if (ep.toString().equalsIgnoreCase(value)) {
                    return ep;
                }
            }
            return RACE_ENABLING_MEMORY;
        }
    }

    /**
     * Enumeration specifying in which time unit the parameters of the net are given.
     * <p>
     * For example, if a timed transition in a {@link StochasticNet} has a duration distribution of
     * {@link NormalDistribution}(10,2), this tells us, if it takes usually 10 seconds, or 10 hours to complete.
     */
    public enum TimeUnit {
        NANOSECONDS("nanoseconds"), MICROSECONDS("microseconds"), MILLISECONDS("milliseconds"),
        SECONDS("seconds"), MINUTES("minutes"), HOURS("hours"),
        DAYS("days"), WEEKS("weeks"), YEARS("years"), UNSPECIFIED("unspecified");

        private String stringValue;

        TimeUnit(String string) {
            this.stringValue = string;
        }

        public String toString() {
            return stringValue;
        }

        public static TimeUnit fromString(String s) {
            for (TimeUnit tu : TimeUnit.values()) {
                if (tu.toString().equalsIgnoreCase(s)) {
                    return tu;
                }
            }
            return UNSPECIFIED;
        }

        /**
         * Returns the conversion factor
         *
         * @return
         */
        public double getUnitFactorToMillis() {
            switch (this) {
                case NANOSECONDS:
                    return 1. / 1000000; // 1 / (1000 * 1000) ms is a nanosecond
                case MICROSECONDS:
                    return 1. / 1000; // 1/1000 ms is a microsecond
                case MILLISECONDS:
                    return 1; // nothing to convert
                case SECONDS:
                    return 1000; // 1000 ms is a second
                case MINUTES:
                    return 60000; // 1000 * 60 ms is a minute
                case HOURS:
                    return 3600000; // 1000 * 60 * 60 ms is an hour
                case DAYS:
                    return 86400000; // 1000 * 60 * 60 * 24 ms is a day
                case WEEKS:
                    return 604800000; // 1000 * 60 * 60 * 24 * 7 ms is a week
                case YEARS:
                    return 31536000000.; // 1000 * 60 * 60 * 24 * 7 * 365 ms is a day
                default:
                    return 1; // unspecified case = milliseconds
            }
        }
    }

    /**
     * Returns the execution policy (see {@link ExecutionPolicy}) of the net.
     *
     * @return {@link ExecutionPolicy}
     */
    public ExecutionPolicy getExecutionPolicy();

    /**
     * Sets the execution policy of the net.
     *
     * @param policy {@link ExecutionPolicy}
     */
    public void setExecutionPolicy(ExecutionPolicy policy);

    /**
     * The time unit used in the stochastic net
     *
     * @return {@link TimeUnit}
     */
    public TimeUnit getTimeUnit();

    /**
     * Sets the time unit of the net
     *
     * @param timeUnit {@link TimeUnit}
     */
    public void setTimeUnit(TimeUnit timeUnit);

    // immediate transitions
    public TimedTransition addImmediateTransition(String label);

    public TimedTransition addImmediateTransition(String label, double weight);

    public TimedTransition addImmediateTransition(String label, double weight, int priority, String trainingData);

    // timed transitions
    public TimedTransition addTimedTransition(String label, DistributionType type, double... distributionParameters);

    public TimedTransition addTimedTransition(String label, double weight, DistributionType type, double... distributionParameters);

    public TimedTransition addTimedTransition(String label, double weight, DistributionType type, String trainingData, double... distributionParameters);
}
