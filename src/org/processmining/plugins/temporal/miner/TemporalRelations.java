package org.processmining.plugins.temporal.miner;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import java.util.Iterator;
import java.util.Random;

public class TemporalRelations {
    private Table<Integer, Integer, TemporalRelation> relations;

    private static Random random = new Random();

    private int eventCount;

    public TemporalRelations(int eventCount) {
        this.eventCount = eventCount;
        relations = getRandomRelations(eventCount);
    }

    public TemporalRelations(int eventCount, Table<Integer, Integer, TemporalRelation> relations2) {
        this.relations = HashBasedTable.create();
        for (int x = 0; x < eventCount; x++) {
            for (int y = 0; y <= x; y++) {
                this.relations.put(x, y, relations2.get(x, y));
            }
        }
        this.eventCount = eventCount;
    }

    private Table<Integer, Integer, TemporalRelation> getRandomRelations(int eventCount) {
        Table<Integer, Integer, TemporalRelation> relations = HashBasedTable.create();
        // create random configuration:
        for (int x = 0; x < eventCount; x++) {
            for (int y = 0; y <= x; y++) {
                if (x == y) {
                    relations.put(x, y, TemporalRelation.EXCLUSIVE);
                } else {
                    relations.put(x, y, getRandomRelation());
                }
            }
        }
        return relations;
    }

    protected TemporalRelation getRandomRelation() {
        return TemporalRelation.values()[random.nextInt(TemporalRelation.values().length)];
    }

    public String toString() {
        StringBuilder builder = new StringBuilder(" ");
        for (int i = 0; i < eventCount; i++) {
            if (i == 0) builder.append("   ");
            builder.append(i);
            builder.append("  ");
        }
        builder.append("\n");
        for (int row = 0; row < eventCount; row++) {
            for (int column = 0; column < eventCount; column++) {
                if (column == 0) {
                    builder.append(row);
                    builder.append(": ");
                }
                if (relations.contains(column, row)) {
                    builder.append(relations.get(column, row));
                } else {
                    builder.append("  ");
                }
                builder.append(",");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public TemporalRelation get(int column, int row) {
        return relations.get(column, row);
    }

    public void randomlyPermuteOne() {
        Iterator<Cell<Integer, Integer, TemporalRelation>> iter = relations.cellSet().iterator();
        int index = random.nextInt(relations.size());
        int i = 0;
        Cell<Integer, Integer, TemporalRelation> cell = iter.next();
        while (i++ < index) {
            cell = iter.next();
        }
        TemporalRelation rel = cell.getValue();
        TemporalRelation newRel = null;
        do {
            newRel = getRandomRelation();
        } while (rel.equals(newRel));
        relations.put(cell.getRowKey(), cell.getColumnKey(), newRel);
    }

    public Object clone() {
        TemporalRelations clone = new TemporalRelations(eventCount, relations);
        return clone;
    }
}
