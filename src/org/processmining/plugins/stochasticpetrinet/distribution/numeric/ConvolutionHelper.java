package org.processmining.plugins.stochasticpetrinet.distribution.numeric;

import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.ArithmeticUtils;
import org.processmining.plugins.stochasticpetrinet.distribution.ApproximateDensityDistribution;
import org.processmining.plugins.stochasticpetrinet.distribution.DistributionUtils;

public class ConvolutionHelper {

	
	public static RealDistribution getConvolvedDistribution(RealDistribution f, RealDistribution g){
		return getConvolvedDistribution(f, g, 1000);
	}
	
	public static RealDistribution getConvolvedDistribution(RealDistribution f, RealDistribution g, int steps){
		if (!ArithmeticUtils.isPowerOfTwo(steps)){
			steps = 1 << (32 - Integer.numberOfLeadingZeros(steps - 1));
		}
		double precision = 0.0001;
		double min = Math.min(f.getSupportLowerBound(), g.getSupportLowerBound());
		if (min == Double.NEGATIVE_INFINITY){
			min = Math.min(f.inverseCumulativeProbability(precision),g.inverseCumulativeProbability(precision));
		}
		min = Math.min(min, 0);
		double max = Math.max(f.getSupportUpperBound(), g.getSupportUpperBound());
		if (max == Double.POSITIVE_INFINITY){
			max = f.inverseCumulativeProbability(1-precision) + g.inverseCumulativeProbability(1-precision);
		}
		assert(max > min);
		assert(max != Double.POSITIVE_INFINITY && min != Double.NEGATIVE_INFINITY);
		double length = max - min;
		double[] discretizedF = new double[steps];
		double[] discretizedG = new double[steps];
		double lastFDensity = f.density(min);
		double lastGDensity = g.density(min);
		for (int i = 0; i < steps; i++){
			double pos = min+ (i / (double)steps) * length;
			double thisFDensity = f.density(pos);
			double thisGDensity = g.density(pos);
			discretizedF[i] = (thisFDensity+lastFDensity)/2;
			discretizedG[i] = (thisGDensity+lastGDensity)/2;
			lastFDensity = thisFDensity;
			lastGDensity = thisGDensity;
		}
//		double[] check = convolve(discretizedF, discretizedG);
		
		// Now we have discrete versions of the densities of two distributions.
		// We can perform the Fourier transformation on them.
		FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.UNITARY);
		org.apache.commons.math3.complex.Complex[] fTransform = transformer.transform(discretizedF, TransformType.FORWARD);
		org.apache.commons.math3.complex.Complex[] gTransform = transformer.transform(discretizedG, TransformType.FORWARD);
		// lets multiply them in fourier space: (update fTransform)
		for (int i = 0; i < fTransform.length; i++){
			fTransform[i] = fTransform[i].multiply(gTransform[i]);
		}
		// transform them back:
		org.apache.commons.math3.complex.Complex[] result = transformer.transform(fTransform, TransformType.INVERSE);
		// use real part of result as new density function (interpolate between points linearly
		double[] resultDensities = DistributionUtils.getRealPart(result);
//		DEBUG:
//		for (int i = 0; i < steps; i++){
//			double pos = min+ (i / (double)steps) * length;
//			System.out.println(pos+";"+resultDensities[i]);
//		}
		
//		System.out.println("\n\n");
//		for (int i = 0; i < check.length; i++){
//			double pos = min+ (i / (double)check.length) * length;
//			System.out.println(pos+";"+check[i]);
//		}
//		return new ApproximateDensityDistribution(check, 0, 2*(max-min), 2*min);
		return new ApproximateDensityDistribution(resultDensities, 0, (max-min), 2*min);
	}
	
//	
//	/** Compute convolution of two sequences. The length of the two sequences need not be
//	  * the same, and the lengths need not be powers of two. The length of the output array
//	  * will be the length of <tt>x</tt> plus the length of <tt>y</tt>, less one.
//	  */
//	private static double[] convolve( double[] x, double[] y )
//	{
//		int Npadded = x.length+y.length, N = x.length+y.length-1;
//
//		if ( (1 << FFT.ilog2(Npadded)) != Npadded )
//			// Npadded is not a power of two; round up to next power of 2.
//			Npadded = 1 << (FFT.ilog2(Npadded)+1);
//
//		Complex[] xy = new Complex[ Npadded ];
//
//		for ( int i = 0; i < Npadded; i++ ) xy[i] = new Complex();
//		for ( int i = 0; i < x.length; i++ ) xy[i].real = x[i];
//		for ( int i = 0; i < y.length; i++ ) xy[i].imag = y[i];
//
//		FFT.fft(xy);		// compute FFT on both columns at once.
//
//		// Compute product of FFT's of each column of data.
//
//		Complex[] xx = new Complex[ Npadded ];
//
//		for ( int i = 0; i < Npadded; i++ )
//		{
//			// The FFT coefficient of the first column is (R1,I1), and 
//			// that of the second is (R2,-I2). This bit is taken from
//			// Brigham (1974), The Fast Fourier Transform, Figure 10-9.
//			
//			int i_reflect = (i == 0? 0: Npadded-i);
//
//			double R1 = (xy[i].real + xy[i_reflect].real)/2;
//			double I1 = (xy[i].imag - xy[i_reflect].imag)/2;
//			double R2 = (xy[i].imag + xy[i_reflect].imag)/2;
//			double I2 = (xy[i].real - xy[i_reflect].real)/2;
//
//			// Multiply the coefficients to obtain the convolution.
//
//			xx[i] = new Complex();
//			xx[i].real = R1*R2 + I1*I2;
//			xx[i].imag = -R1*I2 + I1*R2;
//		}
//
//		FFT.invfft(xx);	// inverse transform to obtain convolution.
//
//		double[] cxy = new double[N];
//
//		for ( int i = 0; i < cxy.length; i++ )
//			cxy[i] = xx[i].real;	// imaginary part is always zero.
//
//		return cxy;
//	}
}
