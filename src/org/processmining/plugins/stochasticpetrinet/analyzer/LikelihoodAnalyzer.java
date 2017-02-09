package org.processmining.plugins.stochasticpetrinet.analyzer;

import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.connections.Connection;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.plugins.petrinet.manifestreplayresult.Manifest;
import org.processmining.plugins.petrinet.manifestreplayresult.ManifestEvClassPattern;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.PerformanceEnricherConfig;
import org.processmining.plugins.stochasticpetrinet.enricher.StochasticManifestCollector;

public class LikelihoodAnalyzer {

    /**
     * Computes the log likelihood of a trace given a net.
     * Replays all traces in the log.
     *
     * @param context {@link PluginContext} can be null
     * @param log     {@link XLog} the log to be replayed in the model
     * @param net     {@link StochasticNet} the stochastic net tells us which traces are more likely than others.
     * @return list of loglikelihoods (same indices as in the log)
     */
    public static CaseStatisticsList getLogLikelihoods(PluginContext context, XLog log, StochasticNet net) {
        // replay trace in model.
        // first get Alignment
        Manifest manifest = (Manifest) StochasticNetUtils.replayLog(context, net, log, true, true);
        PerformanceEnricherConfig mineConfig = new PerformanceEnricherConfig(DistributionType.EXPONENTIAL, net.getTimeUnit(), net.getExecutionPolicy(), null);

        StochasticManifestCollector performanceCollector = new StochasticManifestCollector((ManifestEvClassPattern) manifest, mineConfig);
        performanceCollector.collectDataFromManifest(null);
        CaseStatisticsList logLikelihoods = new CaseStatisticsList();
        for (int traceId = 0; traceId < log.size(); traceId++) {
            logLikelihoods.add(performanceCollector.getCaseStatistics(traceId));
        }
        if (context != null) {
            Connection connection = new CaseStatisticsConnection(net, log, logLikelihoods);
            context.addConnection(connection);
        }
        return logLikelihoods;
    }

    /**
     * Computes the log likelihood of a trace given a net.
     * Only replays ONE trace!
     */
    public static double getLogLikelihood(PluginContext context, XLog log, StochasticNet net, int traceIndex) {

        XLog logWithOneTrace = XFactoryRegistry.instance().currentDefault().createLog(log.getAttributes());
        logWithOneTrace.add(log.get(traceIndex));

        Manifest manifest = (Manifest) StochasticNetUtils.replayLog(context, net, logWithOneTrace, true, true);
        PerformanceEnricherConfig mineConfig = new PerformanceEnricherConfig(DistributionType.EXPONENTIAL, net.getTimeUnit(), net.getExecutionPolicy(), null);

        StochasticManifestCollector performanceCollector = new StochasticManifestCollector((ManifestEvClassPattern) manifest, mineConfig);
        performanceCollector.collectDataFromManifest(null);

        return performanceCollector.getCaseStatistics(0).getLogLikelihood();
//		
//		XEventClass evClassDummy = new XEventClass("DUMMY", -1);
//		// create mapping for each transition to the event class of the repaired log
//		TransEvClassMapping mappingTransEvClass = new TransEvClassMapping(XLogInfoImpl.STANDARD_CLASSIFIER,
//				evClassDummy);
//		XEventClassifier eventClassifier = mappingTransEvClass.getEventClassifier();
//		PNUnroller unroller = new PNUnroller(eventClassifier);
//		
//		
//
//		try {
//			PetrinetGraph pn = unroller.unrolPNbasedOnTrace(logWithOneTrace, mappingTransEvClass, net, StochasticNetUtils.getInitialMarking(context,
//					net), StochasticNetUtils.getFinalMarking(context, net), false);
//			if (pn instanceof StochasticNet){
//				StochasticNet unrolledNet = (StochasticNet) pn;
//				XTrace trace = logWithOneTrace.get(0);
//				// replay in model:
//				for(XEvent event : trace){
//					long timeStamp = XTimeExtension.instance().extractTimestamp(event).getTime();
//					// find previous events
//				}
//				
//			}
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//		return loglikelihood;
    }

}
