package org.processmining.plugins.stochasticpetrinet.simulator.timeseries;

import org.processmining.plugins.stochasticpetrinet.distribution.RProvider;
import org.processmining.plugins.stochasticpetrinet.prediction.timeseries.TimeSeriesConfiguration.AvailableScripts;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public abstract class RTimeSeries<H> extends TimeSeries<H> {

    protected static Set<AvailableScripts> loadedScriptsJRI = new HashSet<AvailableScripts>();

    protected static REngine rEngine = RProvider.getREngine();

    /**
     * The maximum number of historical values to use to train a time series predictor...
     */
    public static final int MAX_SIZE = 1000;

    public RTimeSeries() {
    }

    /**
     * TODO: make this work from a jar:
     *
     * @param metricScript
     */
    protected void loadScriptJRI(AvailableScripts metricScript) {
        if (!loadedScriptsJRI.contains(metricScript)) {
            try {
                String sourceString = "source('" + new File(metricScript.getPath()).getAbsolutePath() + "')";
                System.out.println(sourceString);
                org.rosuda.REngine.REXP exp = rEngine.parseAndEval(sourceString);
                if (exp != null && !exp.isNull()) {
                    loadedScriptsJRI.add(metricScript);
                }
            } catch (REngineException e) {
                e.printStackTrace();
            } catch (REXPMismatchException e) {
                e.printStackTrace();
            }
        }
    }
}
