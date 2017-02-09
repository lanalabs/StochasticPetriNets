package org.processmining.plugins.stochasticpetrinet.measures;

/**
 * @author Andreas Rogge-Solti
 */
public abstract class AbstractMeasure<T extends Number> {

    /**
     * the stored and (computed) value
     */
    private T value;

    /**
     * We assume that all measures can be expressed in terms of a {@link Number}
     *
     * @return
     */
    public final T getValue() {
        return this.value;
    }

    public final void setValue(T value) {
        this.value = value;
    }

    public abstract String getName();

    public String toString() {
        return getName();
    }
}
