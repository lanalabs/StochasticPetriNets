package org.processmining.plugins.stochasticpetrinet.simulator;

import org.deckfour.xes.model.XTrace;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

/**
 * XID-generation can take a looong of time.
 * This subclass does not care about traces -> it just returns the last timestamp that was simulated. 
 * 
 * Prediction by simulation uses the same distributions over and over -> cache makes sense to use reuse the burn-In periods for a Slice-sampler
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class PNSimulatorTraceLess extends PNSimulator {

	protected XTrace createTrace(int i) {
		return null;
	}

	protected void insertEvent(int i, XTrace trace, Pair<Transition, Long> transitionAndDuration, long firingTime) {
		// do nothing
	}

	protected Object getReturnObject(XTrace trace, long lastFiringTime) {
		return lastFiringTime;
	}
}
