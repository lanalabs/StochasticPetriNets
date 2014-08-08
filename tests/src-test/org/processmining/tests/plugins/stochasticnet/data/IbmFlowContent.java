package org.processmining.tests.plugins.stochasticnet.data;

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(strict=false)
public class IbmFlowContent {

	@ElementList(entry="startNode", inline=true, required=false)
	private List<IbmStartNode> startNodes;

	@ElementList(entry="endNode", inline=true, required=false)
    private List<IbmEndNode> endNodes;
	
	@ElementList(entry="stopNode", inline=true, required=false)
	private List<IbmEndNode> stopNodes;
	
	@ElementList(entry="task", inline=true, required=false)
	private List<IbmTask> tasks;
	
	@ElementList(entry="humanTask", inline=true, required=false)
	private List<IbmHumanTask> humanTasks;
	
	@ElementList(entry="process", inline=true, required=false)
	private List<IbmProcess> processes;
	
	@ElementList(entry="decision", inline=true, required=false)
	private List<IbmDecision> decisions;
	
	@ElementList(entry="merge", inline=true, required=false)
	private List<IbmMerge> merges;
	
	@ElementList(entry="fork", inline=true, required=false)
	private List<IbmFork> forks;
	
	@ElementList(entry="join", inline=true, required=false)
	private List<IbmJoin> joins;
	
	@ElementList(entry="callToProcess", inline=true, required=false)
	private List<IbmCallToProcess> callsToProcess;
	
	@ElementList(entry="callToTask", inline=true, required=false)
	private List<IbmCallToTask> callsToTask;
	
	@ElementList(entry="callToService", inline=true, required=false)
	private List<IbmCallToService> callsToService;
	
	
	@ElementList(entry="connection", inline=true, required=false)
	private List<IbmConnection> connections;
	
	public IbmFlowContent(){
		this.startNodes = new ArrayList<IbmStartNode>();
		this.endNodes = new ArrayList<IbmEndNode>();
		this.stopNodes = new ArrayList<IbmEndNode>();
		this.tasks = new ArrayList<IbmTask>();
		this.humanTasks = new ArrayList<IbmHumanTask>();
		this.processes = new ArrayList<IbmProcess>();
		this.decisions = new ArrayList<IbmDecision>();
		this.merges = new ArrayList<IbmMerge>();
		this.forks = new ArrayList<IbmFork>();
		this.joins = new ArrayList<IbmJoin>();
		// TODO: Add support for loops!!
		this.callsToProcess = new ArrayList<IbmCallToProcess>();
		this.callsToTask = new ArrayList<IbmCallToTask>();
		this.callsToService = new ArrayList<IbmCallToService>();
		this.connections = new ArrayList<IbmConnection>();
				
	}

	public List<IbmStartNode> getStartNodes() {
		return startNodes;
	}

	public void setStartNodes(List<IbmStartNode> startNodes) {
		this.startNodes = startNodes;
	}

	public List<IbmEndNode> getEndNodes() {
		return endNodes;
	}

	public void setEndNodes(List<IbmEndNode> endNodes) {
		this.endNodes = endNodes;
	}

	public List<IbmEndNode> getStopNodes() {
		return stopNodes;
	}

	public void setStopNodes(List<IbmEndNode> stopNodes) {
		this.stopNodes = stopNodes;
	}

	public void setJoins(List<IbmJoin> joins) {
		this.joins = joins;
	}

	public List<IbmTask> getTasks() {
		return tasks;
	}

	public void setTasks(List<IbmTask> tasks) {
		this.tasks = tasks;
	}

	public List<IbmHumanTask> getHumanTasks() {
		return humanTasks;
	}

	public void setHumanTasks(List<IbmHumanTask> humanTasks) {
		this.humanTasks = humanTasks;
	}

	public List<IbmProcess> getProcesses() {
		return processes;
	}

	public void setProcesses(List<IbmProcess> processes) {
		this.processes = processes;
	}

	public List<IbmDecision> getDecisions() {
		return decisions;
	}

	public void setDecisions(List<IbmDecision> decisions) {
		this.decisions = decisions;
	}

	public List<IbmMerge> getMerges() {
		return merges;
	}

	public void setMerges(List<IbmMerge> merges) {
		this.merges = merges;
	}

	public List<IbmFork> getForks() {
		return forks;
	}

	public void setForks(List<IbmFork> forks) {
		this.forks = forks;
	}

	public List<IbmJoin> getJoins() {
		return joins;
	}

	public List<IbmCallToProcess> getCallsToProcess() {
		return callsToProcess;
	}

	public void setCallsToProcess(List<IbmCallToProcess> callsToProcess) {
		this.callsToProcess = callsToProcess;
	}

	public List<IbmCallToTask> getCallsToTask() {
		return callsToTask;
	}

	public void setCallsToTask(List<IbmCallToTask> callsToTask) {
		this.callsToTask = callsToTask;
	}

	public List<IbmCallToService> getCallsToService() {
		return callsToService;
	}

	public void setCallsToService(List<IbmCallToService> callsToService) {
		this.callsToService = callsToService;
	}

	public List<IbmConnection> getConnections() {
		return connections;
	}

	public void setConnections(List<IbmConnection> connections) {
		this.connections = connections;
	}
}
