package org.processmining.plugins.logmodeltrust.miner;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.Pair;
import org.processmining.processtree.ProcessTree;

public abstract class AbstractGeneralizedMiner implements GeneralizedMiner {

	private boolean initialized = false;
	
	protected ProcessTree tree;
	protected XLog log;
	
	public void init(ProcessTree inputTree, XLog eventLog) {
		this.tree = inputTree;
		this.log = eventLog;
		doInit();
		this.initialized = true;
	}
	
	/**
	 * Template method for initialization in sub classes. 
	 */
	protected abstract void doInit();

	/**
	 * 
	 */
	public abstract ProcessTree getProcessTreeBasedOnTrust(double trust);

	/**
	 * Decompose the problem:
	 * First "mine" a fitting model based on trust.
	 * Then, align the input log to the new model and remove infrequent log moves, until the trust level is reached.
	 */
	public Pair<XLog, ProcessTree> getFittingPair(double trustLog, double trustModel) {
		if (!initialized){
			throw new IllegalArgumentException("You first need to call init() with inputs!");
		}
		ProcessTree bestTree = getProcessTreeBasedOnTrust(trustModel);
		
		// TODO: align log and tree!
		XLog bestLog = log;
		return new Pair<XLog, ProcessTree>(bestLog, bestTree);
	}

}
