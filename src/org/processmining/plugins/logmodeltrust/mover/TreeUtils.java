package org.processmining.plugins.logmodeltrust.mover;

import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.ProcessTreeImpl;

public class TreeUtils {

	public static ProcessTree getClone(ProcessTree orig){
		ProcessTree clone = new ProcessTreeImpl(orig);
		return clone;
	}
	
	
}
