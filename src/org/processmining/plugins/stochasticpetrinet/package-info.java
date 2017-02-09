/**
 * Stochastic Petri net package for importing / exporting stochastic Petri nets.
 * Can be used to enrich existing Petri net models with stochastical information obtained from a log.
 * Transitions are separated in timed transitions (firing after a probabilistic delay) and immediate transitions.
 * Allows different parametric models to specify time:
 * <ul>
 * <li>exponential distribution</li>
 * <li>normal distribution</li>
 * <li>log-normal distribution</li>
 * <li>beta distribution</li>
 * <li>gamma distribution</li>
 * <li>uniform distribution</li>
 * </ul>
 * and non-parametric models:
 * <ul>
 * <li>plain histograms</li>
 * <li>Gaussian kernel density estimation</li>
 * <li>log-spline estimation (With dependencies to R-project)</li>
 * </ul>
 *
 * @see org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition
 * {@link org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType}
 */
package org.processmining.plugins.stochasticpetrinet;