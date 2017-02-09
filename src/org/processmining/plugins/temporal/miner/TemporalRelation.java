package org.processmining.plugins.temporal.miner;

public enum TemporalRelation {
    SEQUENCE, REVERSE_SEQUENCE, INDEPENDENT, EXCLUSIVE;

    public String toString() {
        switch (this) {
            case INDEPENDENT:
                return "||";
            case EXCLUSIVE:
                return " #";
            case SEQUENCE:
                return "->";
            case REVERSE_SEQUENCE:
                return "<-";
            default:
                return "??";
        }
    }

}
