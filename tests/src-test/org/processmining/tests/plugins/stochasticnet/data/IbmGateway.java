package org.processmining.tests.plugins.stochasticnet.data;

import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Transient;

public class IbmGateway extends IbmNode{
	@ElementList(entry="inputBranch", inline=true)
    private List<IbmInputBranch> inputBranches;
	
	@ElementList(entry="outputBranch", inline=true)
    private List<IbmOutputBranch> outputBranches;

	public List<IbmInputBranch> getInputBranches() {
		return inputBranches;
	}

	public void setInputBranches(List<IbmInputBranch> inputBranches) {
		this.inputBranches = inputBranches;
	}

	public List<IbmOutputBranch> getOutputBranches() {
		return outputBranches;
	}

	public void setOutputBranches(List<IbmOutputBranch> outputBranches) {
		this.outputBranches = outputBranches;
	}
	@Transient
	public String getNodeName(){
		return "gateway";
	}
}
