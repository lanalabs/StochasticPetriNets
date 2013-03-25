package org.processmining.plugins.stochasticpetrinet.distribution;

import java.util.Random;

import org.apache.commons.math3.analysis.UnivariateFunction;

public class SliceSampler {

	private int burnIn;
	
	private double x,y;
	
	private double width = 2;
	
	private static Random rand;
	
	public SliceSampler(){
		this(100);
	}
	public SliceSampler(int burnIn){
		this.burnIn = burnIn;
		rand = new Random();
	}
	
	public void setSeed(long seed){
		rand.setSeed(seed);
	}
	
	public double sample(UnivariateFunction function, double xStart, double yStart){
		x = xStart;
		y = yStart;
		burnIn(function);
		return slice(function);
	}
	
	public double[] sample(UnivariateFunction function, double xStart, double yStart, int size){
		x = xStart;
		y = yStart;
		burnIn(function);
		double[] values = new double[size];
		for (int i = 0; i < size; i++){
			values[i] = slice(function);
		}
		return values;
	}
	
	/**
	 * Samples a number of times to reach the ergodic state of the markov chain.
	 * @param function
	 */
	private void burnIn(UnivariateFunction function) {
		for (int i = 0; i < burnIn; i++){
			slice(function);
		}
	}
	
	/**
	 * Performs a sample operation:
	 * First samples vertically (uniformly given the current x)
	 * Then samples horizontally (uniformly given the current y)
	 * 
	 * @param function the function to evaluate
	 * @return double sample x
	 */
	private double slice(UnivariateFunction function) {
		assert(function.value(x)>y);
		assert(y>0);
		//sample vertically:
		double yMax = function.value(x); 
		y = rand.nextDouble()*yMax;
		while (y == 0){
			y = rand.nextDouble()*yMax;
		}
		
		// sample horizontally:
		double lowerBound = x-width;
		double higherBound = x+width;
		double stepSize = width;
		while (function.value(lowerBound)>y){
			stepSize *= 2;
			lowerBound -= stepSize; 
		}
		stepSize = width;
		while (function.value(higherBound) > y){
			stepSize *= 2;
			higherBound += stepSize;
		}
		double nextX = lowerBound+rand.nextDouble()*(higherBound-lowerBound);
		while (function.value(nextX)<y){
			if (nextX > x){
				higherBound = nextX;
			} else {
				lowerBound = nextX;
			}
			nextX = lowerBound+rand.nextDouble()*(higherBound-lowerBound);
		}
		x = nextX;
		
		return x;
	}
}
