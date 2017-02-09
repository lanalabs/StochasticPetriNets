package org.processmining.plugins.temporal.model;

import org.deckfour.xes.classification.XEventClass;

public class TemporalNode {

    private String name;

    private XEventClass eventClass;

    private int countInLog;

    public TemporalNode(String name, XEventClass eClass) {
        this(name, eClass, 0);
    }

    public TemporalNode(String name, XEventClass eClass, int count) {
        this.name = name;
        this.eventClass = eClass;
        this.countInLog = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public XEventClass getEventClass() {
        return eventClass;
    }

    public void setEventClass(XEventClass eventClass) {
        this.eventClass = eventClass;
    }

    public int getCountInLog() {
        return countInLog;
    }

    public void setCountInLog(int countInLog) {
        this.countInLog = countInLog;
    }

}
