package org.processmining.tests.plugins.stochasticnet.data;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;

@Root(strict=false)
@Element(name="process")
public class IbmProcess extends IbmNode{
	@Element(required=false)
	protected String description;
	
	@ElementList(required=false, name="inputs")
	protected List<IbmInput> inputs;
	
	@ElementList(required=false)
	protected List<IbmOutput> outputs;
	
	@Element
	protected IbmFlowContent flowContent;
	
	public IbmProcess(){
		this.inputs = new ArrayList<IbmInput>();
		this.outputs = new ArrayList<IbmOutput>();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<IbmInput> getInputs() {
		return inputs;
	}

	public void setInputs(List<IbmInput> inputs) {
		this.inputs = inputs;
	}

	public List<IbmOutput> getOutputs() {
		return outputs;
	}

	public void setOutputs(List<IbmOutput> outputs) {
		this.outputs = outputs;
	}

	public IbmFlowContent getFlowContent() {
		return flowContent;
	}

	public void setFlowContent(IbmFlowContent flowContent) {
		this.flowContent = flowContent;
	}
	
	@Transient
	public String getNodeName(){
		return "process";
	}
}
