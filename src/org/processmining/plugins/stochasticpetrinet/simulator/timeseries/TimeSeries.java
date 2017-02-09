package org.processmining.plugins.stochasticpetrinet.simulator.timeseries;

import org.utils.datastructures.LimitedQueue;

import java.util.List;

/**
 * A time series of values (might be durations, might be rates, might be anything)
 *
 * @param <H> the predicted variable which is supposedly correlated to its previous values.
 * @author Andreas Rogge-Solti
 */
public abstract class TimeSeries<H> {

    /**
     * the current {@link Observation}s that are available to fit different time series models
     */
    private LimitedQueue<Observation<H>> currentObservations;

    private boolean upToDate;

    /**
     * the window to use for predictions
     */
    protected int lag = 24 * 28; // 28 days

    /**
     * the seasonality of the data (is there seasonality after 7 days?)
     */
    protected int season = 24;

    /**
     * A unique key for this time series
     */
    protected String key;

    public TimeSeries() {
        this.currentObservations = new LimitedQueue<>(lag);
        this.upToDate = false;
    }

    /**
     * resets the current observations to the passed observations
     * (useful for initialization)
     *
     * @param observations List of {@link Observation}s
     */
    public void resetTo(List<Observation<H>> observations) {
        this.currentObservations.clear();
        this.currentObservations.addAll(observations);
        this.upToDate = false;
    }

    /**
     * Performs a forecast based on the current observations h steps ahead.
     *
     * @param h the forecast horizon
     * @return {@link Prediction} a prediction
     */
    public Prediction<H> predict(int h, Object... payload) {
        if (!upToDate) {
            fit(currentObservations);
            upToDate = true;
        }
        return getPrediction(h, payload);
    }

    public void addObservation(Observation<H> observation) {
        this.currentObservations.add(observation);
    }

    public Observation<H> getLastObservation() {
        return currentObservations.getLast();
    }

    protected Observation<H> findLastAvailableObservation() {
        for (int i = 0; i < currentObservations.size(); i++) {
            Observation<H> obs = currentObservations.get(currentObservations.size() - 1 - i);
            if (isAvailable(obs.observation)) {
                return obs;
            }
        }
        // only NaNs in the observations!!
        return null;
    }


    public Observation<H> getObservationOfLastSeason(long index) {
        // traverse observations from newest to oldest to get the one which is in the same season
        for (int i = currentObservations.size() - 1; i > 0; i--) {
            Observation<H> obs = currentObservations.get(i);
            long seasonId = index % season;
            if (obs.timestamp % season == seasonId && isAvailable(obs.observation)) {
                return obs;
            }
        }
        // fall back, if we do not find an observation
        System.out.println("Retrieval of last season's observation failed!\n"
                + "Make sure your lag is larger than one season!\n"
                + "Falling back to the latest observation.");
        return currentObservations.getLast();
    }

    protected abstract boolean isAvailable(H observation);

    public void setKey(String key) {
        this.key = getUniqueKey(key);
    }

    private static int counter = 0;

    private static String getUniqueKey(String key) {
        return key.replaceAll("\\s", "") + counter++;
    }

    /**
     * Subclasses need to fit their corresponding time series model to the currently available data.
     */
    protected abstract void fit(final LimitedQueue<Observation<H>> currentObservations);

    /**
     * Subclasses need to provide an implementation for the prediction of h-steps into the future given the current position
     *
     * @param h       the prediction/forecast horizon
     * @param payload additional information which is available at time of prediction (if there are other additional explanatory variables)
     */
    protected abstract Prediction<H> getPrediction(int h, Object... payload);
}
