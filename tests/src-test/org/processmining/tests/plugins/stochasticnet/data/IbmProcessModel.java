package org.processmining.tests.plugins.stochasticnet.data;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

@Element(name="processModel")
public class IbmProcessModel {

	@ElementList
	private List<IbmProcess> processes;

	@ElementList(required=false)
	private List<IbmTask> tasks;
	
	@ElementList(required=false)
	private List<IbmService> services;
	
	public List<IbmProcess> getProcesses() {
		return processes;
	}

	public void setProcesses(List<IbmProcess> processes) {
		this.processes = processes;
	}

	public List<IbmTask> getTasks() {
		return tasks;
	}

	public void setTasks(List<IbmTask> tasks) {
		this.tasks = tasks;
	}
	
	
}
