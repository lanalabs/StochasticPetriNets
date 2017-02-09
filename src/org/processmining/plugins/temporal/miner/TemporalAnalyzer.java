package org.processmining.plugins.temporal.miner;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;

public class TemporalAnalyzer {

    public TemporalProfile getTemporalProfile(PluginContext context, XLog log) {
        TemporalProfile profile = new TemporalProfile(log);
        return profile;
    }
}
