package org.processmining.plugins.logmodeltrust;

import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.plugins.logmodeltrust.miner.GeneralizedMiner;
import org.processmining.plugins.logmodeltrust.miner.GeneralizedMinerImpl;
import org.processmining.processtree.ProcessTree;

public class GeneralizedMinerPlugin {

	@Plugin(name = "Mine Best Log Model Pair Based On Trust", parameterLabels = { "Event Log",
			"Process Tree" }, returnLabels = { GeneralizedMiner.PARAMETER_LABEL }, returnTypes = { GeneralizedMiner.class}, userAccessible = true, 
			help = "Based on trust levels in input event log and input model, generates the best log-model pair that fit the inputs and each other best.")

	@UITopiaVariant(affiliation = "Vienna University of Economics and Business", author = "A. Rogge-Solti", email = "andreas.rogge-solti@wu.ac.at", uiLabel = UITopiaVariant.USEPLUGIN)
	public static GeneralizedMiner mineGeneralLogModel(PluginContext context, XLog log, ProcessTree tree) {
		
		GeneralizedMiner miner = new GeneralizedMinerImpl();
		miner.init(tree, log);
		
		return miner;
	}
}
