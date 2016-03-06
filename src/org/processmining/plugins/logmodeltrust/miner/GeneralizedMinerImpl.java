package org.processmining.plugins.logmodeltrust.miner;

import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMi;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.logmodeltrust.mover.TreeMover;
import org.processmining.processtree.ProcessTree;

public class GeneralizedMinerImpl extends AbstractGeneralizedMiner {

	private TreeMover mover;
	
	private ProcessTree targetTree; 
	
	public GeneralizedMinerImpl(){
		super();
	}
	
	protected void doInit() {
		MiningParameters parameters = new MiningParametersIMi();
		parameters.setNoiseThreshold(0.0f);
		
		targetTree = IMProcessTree.mineProcessTree(log, parameters);
		
		mover = new TreeMover(tree, targetTree);
	}

	public ProcessTree getProcessTreeBasedOnTrust(double trust) {
		return mover.getProcessTreeBasedOnTrust(trust);
	}

}
