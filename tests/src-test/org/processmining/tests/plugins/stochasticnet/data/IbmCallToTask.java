package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;

@Root(strict=false)
@Element(name="callToTask")
public class IbmCallToTask extends IbmCall {
	
	@Attribute(name="task", required=false)
	private String task;

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
	}
	 @Transient
	public String getNodeName(){
		return "callToTask";
	}
}
