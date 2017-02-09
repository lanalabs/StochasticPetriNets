package org.processmining.plugins.stochasticpetrinet.simulator;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;

/**
 * Event based simulator.
 * Takes a net and checks what is allowed. The semantics are the same as in normal Petri net simulation.
 * However, when firing a transition, we don't put all tokens back immediately, but we let the resources walk to their next tasks (wherever these might be)
 *
 * @author Andreas Rogge-Solti
 */
public class AdvancedSimulator {

    /**
     * Simulates real entities performing a process.
     * almost normally, but resources need to go to their next activity after finishing some task.
     * Their "walk" is emitted as events which start and end right before and after a real service event.
     *
     * @param model
     * @param wc
     */
    public void simulateRealEntities(StochasticNet model, WorldConfiguration wc) {
        // TODO: development of this class stalled in favor of the LogLocationDelayInducer
    }
}
