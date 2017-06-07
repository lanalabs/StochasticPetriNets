package org.processmining.plugins.stochasticpetrinet.measures.entropy;

/**
 *
 * Created by andreas on 6/7/17.
 */
public class EntropyCalculatorQuantile extends EntropyCalculatorExact {

    private final double quantile;

    public EntropyCalculatorQuantile() {
        this(0.9);
    }

    /**
     * Constructs a calculator that measures the entropy of a model's possible runs up to a given quantile o
     * @param quantile
     */
    public EntropyCalculatorQuantile(double quantile) {
        super();
        if (quantile < 0 || quantile > 1){
            throw new IllegalArgumentException("Confidence must be between 0 and 1!");
        }
        this.quantile = quantile;
        this.config.setQuantile(quantile);
    }

    public String getMeasureName() {
        return "Model entropy measure (using "+Math.round(this.quantile*100)+"% quantile of states)";
    }

    protected String getNameInfo() {
        return "quantile";
    }
}
