package org.processmining.plugins.stochasticpetrinet.external;

import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.Map;
import java.util.Set;


public abstract class AbstractAllocation implements Allocation {
    private AllocType type;

    public AllocType getType() {
        return type;
    }

    public AbstractAllocation(AllocType type) {
        this.type = type;
    }

    protected static String getString(Set<Allocatable> allocs) {
        StringBuffer buf = new StringBuffer();
        for (Allocatable alloc : allocs) {
            if (buf.length() > 0) {
                buf.append(",");
            }
            buf.append(alloc.getName());
        }
        return buf.toString();
    }

    /**
     * Returns the entropy of the allocation distribution.
     *
     * @return
     */
    public double getEntropy() {
        Map<String, Double> probs = getProbabilitiesOfAllocations();
        return StochasticNetUtils.getEntropy(probs);
    }
}
