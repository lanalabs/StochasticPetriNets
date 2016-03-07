package org.processmining.plugins.logmodeltrust.miner;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

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
		
		try {
			// convert tree to Petri net
			PetrinetWithMarkings pnWithMarkings = ProcessTree2Petrinet.convert(bestTree);
			
			// align petri net with log:
			PNRepResult result = (PNRepResult) StochasticNetUtils.replayLog(null, pnWithMarkings.petrinet, log, false, true);
			for (SyncReplayResult repResult : result){
				for (StepTypes sTypes : repResult.getStepTypes()){
					switch (sTypes){
					case LMGOOD: // synch, replaced, or swapped
					case MINVI: // invisible model move <- that's fine	
						// fine
						break;
					case L: // log move
						// bad! <- excess entry in the log?
						break;
					case MREAL: // model move
						// bad! <- missing entry in the log?
						break;
					}
				}
			}
			
			
		} catch (NotYetImplementedException e) {
			e.printStackTrace();
		} catch (InvalidProcessTreeException e) {
			e.printStackTrace();
		}
		
		
		// TODO: align log and tree!
		XLog bestLog = log;
		return new Pair<XLog, ProcessTree>(bestLog, bestTree);
	}

}
