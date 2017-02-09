package org.processmining.plugins.stochasticpetrinet.analyzer;

public class LoopsNotSupportedException extends Exception {
    private static final long serialVersionUID = -5826842730230583501L;

    public LoopsNotSupportedException() {
        super();
    }

    public LoopsNotSupportedException(String string) {
        super(string);
    }

}
