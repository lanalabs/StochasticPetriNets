package org.processmining.plugins.stochasticpetrinet.enricher;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class PerformanceEnricherPlugin {

    @Plugin(name = "Enrich Petri Net with performance data",
            parameterLabels = {"Manifest"},
            returnLabels = {StochasticNet.PARAMETER_LABEL, "Marking"},
            returnTypes = {StochasticNet.class, Marking.class},
            userAccessible = true,
            help = "Creates a new copy of the net enriched with performance data.")

    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public Object[] transform(UIPluginContext context, Manifest manifest) {
        PerformanceEnricher collector = new PerformanceEnricher();
        return collector.transform(context, manifest);
    }

    public static Object[] transform(PluginContext context, Manifest manifest, PerformanceEnricherConfig mineConfig) {
        PerformanceEnricher collector = new PerformanceEnricher();
        return collector.transform(context, manifest, mineConfig);
    }

    @Plugin(name = "Enrich Petri Net with performance data (default mapping)",
            parameterLabels = {"Petrinet", "Log"},
            returnLabels = {StochasticNet.PARAMETER_LABEL, "Marking"},
            returnTypes = {StochasticNet.class, Marking.class},
            userAccessible = true,
            help = "Creates a new copy of the net enriched with performance data.")
    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public static Object[] transform(UIPluginContext context, PetrinetGraph net, XLog log) {
        PerformanceEnricher collector = new PerformanceEnricher();
        Manifest manifest = (Manifest) StochasticNetUtils.replayLog(context, net, log, true, true);
        return collector.transform(context, manifest);
    }

    @Plugin(name = "Prepare Durations for Correlation Testing",
            parameterLabels = {"Log"},
            returnLabels = {"Result"},
            returnTypes = {String.class},
            userAccessible = true,
            help = "Writes the duration performance data into a .csv File.")
    @UITopiaVariant(affiliation = "Hasso Plattner Institute", author = "A. Rogge-Solti", email = "andreas.rogge-solti@hpi.uni-potsdam.de", uiLabel = UITopiaVariant.USEPLUGIN)
    public static String transform(UIPluginContext context, XLog log) throws IOException {
        XLogInfo logInfo = XLogInfoFactory.createLogInfo(log);
        XEventClasses eventClasses = logInfo.getEventClasses();

        JOptionPane.showMessageDialog(null, "Use this only for sequential processes - parallelism not implemented yet!");

        String SEPARATOR_STRING = ",";

        JFileChooser chooser = new JFileChooser(".");
        chooser.setFileFilter(new FileFilter() {
            public String getDescription() {
                return "Comma Separated Values .csv";
            }

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".csv");
            }
        });
        int choice = chooser.showSaveDialog(null);
        File fileToSave;
        if (choice == JFileChooser.APPROVE_OPTION) {
            fileToSave = chooser.getSelectedFile();
        } else {
            context.getFutureResult(0).cancel(true);
            return null;
        }
        String result = "";
        String header = "";
        for (XEventClass eClass : eventClasses.getClasses()) {
            if (!header.isEmpty()) {
                header += SEPARATOR_STRING;
            }
            header += eClass.getId() + " (case duration)" + SEPARATOR_STRING + eClass.getId() + " (duration)";
        }
        result += header + "\n";

        for (XTrace t : log) {
            String line = "";
            Date previousEventDate = XTimeExtension.instance().extractTimestamp(t.get(0));
            Date startTime = previousEventDate;

            for (XEventClass eClass : eventClasses.getClasses()) {
                if (!line.isEmpty()) {
                    line += SEPARATOR_STRING;
                }
                XEvent e = findByClass(t, eClass, eventClasses);
                int pos = t.indexOf(e);
                if (e != null) {
                    Date currentTime = XTimeExtension.instance().extractTimestamp(e);
                    long thisEventDuration = 0;
                    long caseDuration = 0;
                    if (pos > 0) {
                        thisEventDuration = currentTime.getTime() - XTimeExtension.instance().extractTimestamp(t.get(pos - 1)).getTime();
                        caseDuration = XTimeExtension.instance().extractTimestamp(t.get(pos - 1)).getTime() - startTime.getTime();
                    }
                    line += caseDuration + SEPARATOR_STRING + thisEventDuration;
                    previousEventDate = currentTime;
                } else {
                    line += "0" + SEPARATOR_STRING + "0";
                }
            }
            result += line + "\n";
        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave));
        writer.write(result);
        writer.flush();
        writer.close();
        context.getFutureResult(0).cancel(true);
        return null;
    }

    private static XEvent findByClass(XTrace t, XEventClass eClass, XEventClasses classes) {
        for (XEvent e : t) {
            if (classes.getClassOf(e).equals(eClass)) {
                return e;
            }
        }
        return null;
    }
}
