package org.processmining.plugins.stochasticpetrinet.distribution;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.random.Well1024a;

/**
 * A diagonal parallel to the line y = -x is defined by the sum of x and y.
 * <p>
 * We cut through two independent distributions along the diagonal line y = {@link #xAndY} - x.
 * This distribution is an approximation using {@link ApproximateDensityDistribution}
 * and is targeted at numerical analysis.
 *
 * @author Andreas Rogge-Solti
 */
public class DiagonalDistribution extends AbstractRealDistribution {
    private static final long serialVersionUID = -2815202484661628453L;

    /**
     * Marginal of the joint probability P(X,Y) on the X-axis
     * assuming independence of X and Y, i.e., P(X,Y) = P(X)P(Y)
     */
    RealDistribution distX;
    /**
     * Marginal of the joint probability P(X,Y) on the Y-axis
     * assuming independence of X and Y, i.e., P(X,Y) = P(X)P(Y)
     */
    RealDistribution distY;

    double xAndY;
    double scale = 1;

    ApproximateDensityDistribution delegate;
    private boolean initialized;

    /**
     * Creates an approximation of the distribution along the diagonal y = xAndY - x
     * of a joint probability distribution of P(X,Y) = P(X)P(Y)
     *
     * @param distX a {@link RealDistribution} specifying P(X) - a marginal distribution of P(X,Y)
     * @param distY a {@link RealDistribution} specifying P(Y) - a marginal distribution of P(X,Y)
     * @param xAndY the constraint that specifies the position of the diagonal line (x+y)
     */
    public DiagonalDistribution(RealDistribution distX, RealDistribution distY, double xAndY) {
        super(new Well1024a());
        this.distX = distX;
        this.distY = distY;
        this.xAndY = xAndY;
        this.delegate = new ApproximateDensityDistribution(this, false);
        this.initialized = true;
    }

    public double cumulativeProbability(double x) {
        if (initialized) {
            return delegate.cumulativeProbability(x);
        } else {
            throw new UnsupportedOperationException("not implemented...");
        }
    }

    /**
     * Density calculation assumes INDEPENDENCE of {@link #distX} and {@link #distY}!
     */
    public double density(double x) {
        if (initialized) {
            return delegate.density(x);
        } else {
            double y = xAndY - x;
            return distX.density(x) * distY.density(y);
        }
    }

    public double getNumericalMean() {
        return delegate.getNumericalMean();
    }

    public double getNumericalVariance() {
        return delegate.getNumericalVariance();
    }

    public double getSupportLowerBound() {
        return distX.getSupportLowerBound();
    }

    public double getSupportUpperBound() {
        return distX.getSupportUpperBound();
    }

    public boolean isSupportConnected() {
        return true;
    }

    public boolean isSupportLowerBoundInclusive() {
        return distX.isSupportLowerBoundInclusive();
    }

    @Deprecated
    public boolean isSupportUpperBoundInclusive() {
        return distX.isSupportUpperBoundInclusive();
    }

}
