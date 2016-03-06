package org.processmining.plugins.logmodeltrust.miner;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.Pair;
import org.processmining.processtree.ProcessTree;

public interface GeneralizedMiner {

	String PARAMETER_LABEL = "Generalized Miner";

	public void init(ProcessTree inputTree, XLog eventLog);
	
	/**
	 * "Mines" a process tree from a process tree base and an event log 
	 * 
	 * @param trust double trust level in the model (between 0="no trust at all" and 1="full trust")
	 * @return {@link ProcessTree}
	 */
	public ProcessTree getProcessTreeBasedOnTrust(double trust);
	
	/**
	 * "Mines" a fitting process tree and a corresponding log from given 
	 * inputs supplied by {@link #init(ProcessTree, XLog)} method. 
	 * @param trustLog double trust in the log.
	 * @param trustModel double trust in the log.
	 * @return a pair of best fitting log and model.
	 */
	public Pair<XLog, ProcessTree> getFittingPair(double trustLog, double trustModel);
}
