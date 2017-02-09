package org.processmining.plugins.filter.noise;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This noise filter adds noise to an event log by randomly removing events and adding events.
 * <p>
 * It is more accurate than the {@link NoiseLogFilter}, because it guarantees that
 * at least floor(N * noise) and maximum ceil(N * noise)
 * events will be affected in each trace. (N being the size of the trace)
 *
 * @author Andreas Rogge-Solti
 */
public class AccurateNoiseFilter {

    protected double noise;
    protected double deletionInsertionRatio;

    protected Random random = new Random();

    public AccurateNoiseFilter(double noise) {
        this(noise, 0.5, 1);
    }

    /**
     * @param noise                  the amount of noise between 1 (100% noise), 0 (0% noise)
     * @param deletionInsertionRatio value between 1 (only deletion) and 0 (only insertion)
     * @param seed                   the seed for the random number generator
     */
    public AccurateNoiseFilter(double noise, double deletionInsertionRatio, long seed) {
        assert (noise <= 1 && noise >= 0);
        assert (deletionInsertionRatio <= 1 && deletionInsertionRatio >= 0);
        this.noise = noise;
        this.deletionInsertionRatio = deletionInsertionRatio;
        this.random.setSeed(seed);
    }

    /**
     * Inserts noise into the log in each trace.
     *
     * @param log {@link XLog} to insert noise into.
     * @return a {@link XLog} that contains {@link #noise} with {@link #deletionInsertionRatio} ratio of deleted / inserted events
     */
    public XLog insertNoise(XLog log) {

        XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, new XEventNameClassifier());
        XEventClasses classes = logInfo.getEventClasses();
        ArrayList<XEventClass> classCollection = new ArrayList<>(classes.getClasses());

        double meanDuration = StochasticNetUtils.getMeanDuration(log);

        XLog resultLog = XFactoryRegistry.instance().currentDefault().createLog((XAttributeMap) log.getAttributes().clone());
        XConceptExtension.instance().assignName(resultLog, XConceptExtension.instance().extractName(log) + " with " + (int) (Math.floor(noise * 100)) + "% noise");
        for (XTrace trace : log) {
            XTrace resultTrace = XFactoryRegistry.instance().currentDefault().createTrace((XAttributeMap) trace.getAttributes().clone());
            for (XEvent e : trace) {
                XEvent resultEvent = XFactoryRegistry.instance().currentDefault().createEvent((XAttributeMap) e.getAttributes().clone());
                resultTrace.add(resultEvent);
            }

            long traceStart = XTimeExtension.instance().extractTimestamp(trace.get(0)).getTime();

            int affectedEvents = (int) Math.floor(trace.size() * noise);
            double introducedNoise = affectedEvents / (double) trace.size();
            double introducedNoiseWithOneMore = (affectedEvents + 1) / (double) trace.size();
            double chanceForOneMore = (noise - introducedNoise) / (introducedNoiseWithOneMore - introducedNoise);
            affectedEvents = random.nextDouble() < chanceForOneMore ? affectedEvents + 1 : affectedEvents;

            int deletedEvents = (int) Math.floor(affectedEvents * deletionInsertionRatio);
            double deletedEventsRatio = deletedEvents / (double) affectedEvents;
            double deletedEventsRatioWithOneMore = (deletedEvents + 1) / (double) affectedEvents;
            double chanceForOneMoreDeletion = (deletionInsertionRatio - deletedEventsRatio) / (deletedEventsRatioWithOneMore - deletedEventsRatio);
            deletedEvents = random.nextDouble() < chanceForOneMoreDeletion ? deletedEvents + 1 : deletedEvents;

            List<XEvent> affectedEventSet = new ArrayList<>(resultTrace); // make sure that each event is only passed once
            for (int i = 0; i < deletedEvents; i++) {
                XEvent e = affectedEventSet.remove(random.nextInt(affectedEventSet.size())); // randomly select next event
                resultTrace.remove(e);
                if (resultTrace.isEmpty()) {
                    // only occurs if we deleted all events -> we readd the last one
                    resultTrace.add(e);
                }
            }
            for (int i = deletedEvents; i < affectedEvents; i++) {
                XEvent e = affectedEventSet.remove(random.nextInt(affectedEventSet.size())); // randomly select next event
                XEventClass eClass = classCollection.get(random.nextInt(classCollection.size()));
                XEvent newEvent = XFactoryRegistry.instance().currentDefault().createEvent((XAttributeMap) e.getAttributes().clone());
                XConceptExtension.instance().assignName(newEvent, eClass.getId());
                XTimeExtension.instance().assignTimestamp(newEvent, (long) (traceStart + random.nextDouble() * meanDuration));
                resultTrace.insertOrdered(newEvent);
            }
            resultLog.add(resultTrace);
        }
        return resultLog;
    }

    public double getNoise() {
        return noise;
    }

    public void setNoise(double noise) {
        this.noise = noise;
    }

    public double getDeletionInsertionRatio() {
        return deletionInsertionRatio;
    }

    public void setDeletionInsertionRatio(double deletionInsertionRatio) {
        this.deletionInsertionRatio = deletionInsertionRatio;
    }
}
