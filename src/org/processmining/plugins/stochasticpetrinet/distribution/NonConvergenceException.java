package org.processmining.plugins.stochasticpetrinet.distribution;

/**
 * It might be, that the EM-fitting algorithm used to fit densities to data values might not converge.
 * Then this exception happens. Implementations using fitted distributions should be able to gracefully fall back to some simpler method (e.g., use the {@link GaussianKernelDistribution})
 *
 * @author Andreas Rogge-Solti
 */
public class NonConvergenceException extends Exception {
    private static final long serialVersionUID = 7476669736953699563L;

    public NonConvergenceException() {
        super();
    }

    public NonConvergenceException(String message) {
        super(message);
    }

    public NonConvergenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
