package org.processmining.plugins.filter.noise;

import com.fluxicon.slickerbox.components.NiceIntegerSlider;
import com.fluxicon.slickerbox.components.NiceSlider.Orientation;
import com.fluxicon.slickerbox.factory.SlickerFactory;
import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Introduces noise into an event log by randomly removing items, or adding duplicate events.
 * It maintains the condition, that always at least one event remains in the trace.
 *
 * @author Ronny Mans, Andreas Rogge-Solti
 */
public class NoiseLogFilter {

    protected float selectedNoisePercentageAdd = 0.0f;
    protected float selectedNoisePercentageRemove = 0.0f;

    //protected long selectedSeed = 123456789;

    protected Random random;

    public NoiseLogFilter() {
        this(0l);
    }

    public NoiseLogFilter(long seed) {
        random = new Random(seed);
    }

    public enum NoiseTypes {
        ADD_EVENTS, REMOVE_EVENTS;

        public String toString() {
            String output = name().toString();
            output = output.charAt(0) + output.substring(1).toLowerCase();
            output = output.replace("_", " ");
            return output;
        }
    }

    @Plugin(name = "Add Noise to Log Filter", parameterLabels = {"log"}, returnLabels = {"log"}, returnTypes = {XLog.class}, userAccessible = true)
    @UITopiaVariant(affiliation = UITopiaVariant.EHV, author = "R.S. Mans", email = "r.s.mans@tue.nl")
    public XLog timeStampInjection(UIPluginContext context, XLog oldLog) throws Exception {
        NoiseFilterPanel parameters = new NoiseFilterPanel();

        InteractionResult interActionResult = context.showConfiguration("Add noise to Log Filter", parameters);
        if (interActionResult.equals(InteractionResult.CANCEL)) {
            context.getFutureResult(0).cancel(true);
            return null;
        }
        //context.getFutureResult(0).setLabel(XConceptExtension.instance().extractName(oldLog) + "_changed (Trace Name Filter)");
        context.getProgress().setMinimum(0);
        context.getProgress().setMaximum(oldLog.size());
        context.getProgress().setIndeterminate(false);
        context.getProgress().setValue(0);

        // get parameters
        setParameters(parameters);

        XLog result = introduceNoise(context, oldLog);
        return result;
    }

    /**
     * Sets the parameters for this noisy log filter.
     *
     * @param parameters {@link NoiseLogFilterParameters}
     */
    public void setParameters(NoiseLogFilterParameters parameters) {
        setPercentageAdd(parameters.getAddPercentage());
        setPercentageRemove(parameters.getRemovePercentage());
    }

    /**
     * @param addPercentage noise level to add in percentage (duplication of events)
     */
    public void setPercentageAdd(Integer addPercentage) {
        this.selectedNoisePercentageAdd = addPercentage / 100.0f;
    }

    /**
     * @param removePercentage chance to remove events from log in percentage
     */
    public void setPercentageRemove(Integer removePercentage) {
        this.selectedNoisePercentageRemove = removePercentage / 100.0f;
    }

    public XLog introduceNoise(PluginContext context, XLog log) {


        XLog result = XFactoryRegistry.instance().currentDefault().createLog(log.getAttributes());
        XTimeExtension xTime = XTimeExtension.instance();

        for (XTrace t : log) {
            List<XEvent> duplicatedEvents = new ArrayList<XEvent>();
            XTrace copy = XFactoryRegistry.instance().currentDefault().createTrace(t.getAttributes());
            Pair<Long, Long> traceBounds = StochasticNetUtils.getBufferedTraceBounds(t);
            if (context != null) {
                context.getProgress().inc();
            }
            try {
                for (XEvent e : t) {
                    //					if (selectedNoiseTypes.contains(NoiseTypes.REMOVE_EVENTS) && r.nextDouble() > selectedNoisePercentageRemove) {
                    if (random.nextDouble() >= selectedNoisePercentageRemove) {
                        // keep this one
                        XEvent copyEvent = XFactoryRegistry.instance().currentDefault().createEvent(e.getAttributes());
                        copy.insertOrdered(copyEvent);
                    } else {
                        // do not add it to the new trace
                    }
                    if (random.nextDouble() < selectedNoisePercentageAdd) {
                        // duplicate event, such that it can be randomly added to the trace later on
                        XEvent copyEventAgain = XFactoryRegistry.instance().currentDefault().createEvent(
                                e.getAttributes());
                        duplicatedEvents.add(copyEventAgain);
                    }
                }
                // in case of removing events, make sure there is always one event in the trace
                if (copy.size() == 0) {
                    System.out.println("trace empty -> adding random event for trace:" + XConceptExtension.instance().extractName(t));
                    XEvent e = t.get(random.nextInt(t.size()));
                    XEvent copyEvent = XFactoryRegistry.instance().currentDefault().createEvent(e.getAttributes());
                    copy.insertOrdered(copyEvent);
                }

                // in case of ADD_EVENTS, add events randomly to the trace
                for (XEvent e : duplicatedEvents) {
                    adjustTimeStampOfNewEvent(random, xTime, traceBounds, e);
//					XConceptExtension.instance().assignName(e, "random_exception_event_"+random.nextInt());
                    copy.insertOrdered(e);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (copy != null && !copy.isEmpty()) {
                result.add(copy);
            }
        }

        return result;
    }

    public void adjustTimeStampOfNewEvent(Random r, XTimeExtension xTime, Pair<Long, Long> traceBounds, XEvent duplicatedEvent) throws Exception {
        // the new event has timestamp in between
        xTime.assignTimestamp(duplicatedEvent, traceBounds.getFirst() + r.nextInt((int) (traceBounds.getSecond() - traceBounds.getFirst())));
    }

    public interface NoiseLogFilterParameters {
        public Integer getRemovePercentage();

        public Integer getAddPercentage();
    }

    class NoiseFilterPanel extends JPanel implements NoiseLogFilterParameters {
        private static final long serialVersionUID = 7423846369741879276L;

        private NiceIntegerSlider removePercentage;
        private NiceIntegerSlider addPercentage;

        public NoiseFilterPanel() {
            super();

            // create a parameter object
            SlickerFactory factory = SlickerFactory.instance();
            // texts
            String stringForRemove = "Noise level in percentage for removing events";
            String stringForAdd = "Noise level in percentage for adding events  ";
            // set remove percentage
            removePercentage = factory.createNiceIntegerSlider(stringForRemove, 0, 100, 0, Orientation.HORIZONTAL);

            // set add percentage
            addPercentage = factory.createNiceIntegerSlider(stringForAdd, 0, 100, 0, Orientation.HORIZONTAL);

            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            add(removePercentage);
            add(addPercentage);
        }

        public Integer getRemovePercentage() {
            return removePercentage.getValue();
        }

        public Integer getAddPercentage() {
            return addPercentage.getValue();
        }

    }

}
