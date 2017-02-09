package org.processmining.plugins.stochasticpetrinet.distribution.numeric;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.apache.commons.math3.util.FastMath;
import org.processmining.plugins.stochasticpetrinet.distribution.ApproximateDensityDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.DistributionUtils;

public class ConvolutionHelper {


    public static RealDistribution getConvolvedDistribution(RealDistribution f, RealDistribution g) {
        return getConvolvedDistribution(f, g, 2000);
    }

    public static RealDistribution getConvolvedDistribution(RealDistribution f, RealDistribution g, int steps) {
        if (!ArithmeticUtils.isPowerOfTwo(steps)) {
            steps = 1 << (32 - Integer.numberOfLeadingZeros(steps - 1));
        }
        double precision = 0.0001;
        double minF = f.getSupportLowerBound();
        if (minF == Double.NEGATIVE_INFINITY) {
            minF = f.inverseCumulativeProbability(precision);
        }
        double maxF = f.getSupportUpperBound();
        if (maxF == Double.POSITIVE_INFINITY) {
            maxF = f.inverseCumulativeProbability(1 - precision);
        }
        double minG = g.getSupportLowerBound();
        if (minG == Double.NEGATIVE_INFINITY) {
            minG = g.inverseCumulativeProbability(precision);
        }
        double maxG = g.getSupportUpperBound();
        if (maxG == Double.POSITIVE_INFINITY) {
            maxG = g.inverseCumulativeProbability(1 - precision);
        }
        double length = FastMath.max(maxF - minF, maxG - minG) * 2;

        double[] discretizedF = new double[steps];
        double[] discretizedG = new double[steps];
        double lastFDensity = f.density(minF);
        double lastGDensity = g.density(minG);
        for (int i = 0; i < steps; i++) {
            double posF = minF + (i / (double) steps) * length;
            double posG = minG + (i / (double) steps) * length;
            double thisFDensity = f.density(posF);
            double thisGDensity = g.density(posG);
            discretizedF[i] = (thisFDensity + lastFDensity) / 2;
            discretizedG[i] = (thisGDensity + lastGDensity) / 2;
            lastFDensity = thisFDensity;
            lastGDensity = thisGDensity;
        }

        // Now we have discrete versions of the densities of two distributions.
        // We can perform the Fourier transformation on them.
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.UNITARY);
        org.apache.commons.math3.complex.Complex[] fTransform = transformer.transform(discretizedF, TransformType.FORWARD);
        org.apache.commons.math3.complex.Complex[] gTransform = transformer.transform(discretizedG, TransformType.FORWARD);
        // lets multiply them in fourier space: (update fTransform)
        for (int i = 0; i < fTransform.length; i++) {
            fTransform[i] = fTransform[i].multiply(gTransform[i]);
        }
        // transform them back:
        org.apache.commons.math3.complex.Complex[] result = transformer.transform(fTransform, TransformType.INVERSE);
        // use real part of result as new density function (interpolate between points linearly
        double[] resultDensities = DistributionUtils.getVectorLength(result);

        return new ApproximateDensityDistribution(resultDensities, 0, length, (minF + minG));
    }
}
