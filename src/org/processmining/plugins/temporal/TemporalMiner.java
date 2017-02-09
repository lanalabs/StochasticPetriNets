package org.processmining.plugins.temporal;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.plugins.temporal.model.TemporalModel;

import javax.swing.*;

public class TemporalMiner {

    public TemporalModel mine(UIPluginContext context, XLog log) {
        //TODO: choose natural time unit for events in log

        TimeUnit unit = (TimeUnit) JOptionPane.showInputDialog(null, "Please select time unit:", "Time unit in log.", JOptionPane.PLAIN_MESSAGE, null, TimeUnit.values(), TimeUnit.MINUTES);
        if (unit == null) {
            unit = TimeUnit.MINUTES;
        }

        TemporalModel model = new TemporalModel(unit);
        XLogInfo info = XLogInfoFactory.createLogInfo(log);
        model.init(log, info);
        return model;
    }

}
