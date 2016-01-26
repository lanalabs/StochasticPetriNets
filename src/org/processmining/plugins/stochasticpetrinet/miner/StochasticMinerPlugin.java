package org.processmining.plugins.stochasticpetrinet.miner;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.plugins.stochasticpetrinet.miner.distance.DistanceFunction;
import org.processmining.plugins.stochasticpetrinet.miner.distance.SimpleDistanceFunction;

public class StochasticMinerPlugin {

	@Plugin(name = "Mine optimal model and log", 
			parameterLabels = { "Log", "Petri Net"}, 
			returnLabels = { "Log", "Petri Net" }, 
			returnTypes = { XLog.class, PetrinetGraph.class }, 
			userAccessible = true,
			help = "Tries to seek for altered combination of a log and model pair that best fits each other and are close to the given model and log.")

	@UITopiaVariant(affiliation = "Vienna University of Economics and Business", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at", uiLabel = UITopiaVariant.USEPLUGIN)
	public static Object[] mineLogAndModel(PluginContext context, XLog log, PetrinetGraph model){
		Object[] obj = new Object[2];
		
		DistanceFunction distFun = new SimpleDistanceFunction(); 
		
		OptimalMiner miner = new LocalSearchMiner(distFun, context, log, model);
		miner.searchForBetterLogAndModel();
		obj[0] = miner.getBestLog();
		obj[1] = miner.getBestModel();
		return obj;
	}
}
