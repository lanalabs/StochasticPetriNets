package org.processmining.plugins.temporal.miner;

import gnu.trove.TLongCollection;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

public class TemporalProfileOptimizer {

    private Random random;

    private double alpha = 0.7; // weight used in the score function for the mean duration

    public TemporalProfileOptimizer() {
        this.random = new Random();
    }

    public Petrinet getLocallyOptimalModel(TemporalProfile profile) {
        Petrinet net = PetrinetFactory.newPetrinet("prototype net");

        // For each event-event pair, try to find the optimal choice between sequential and parallel
        TemporalRelations relations = new TemporalRelations(profile.getEventCount());
        TemporalRelations best = (TemporalRelations) relations.clone();
        // evaluate relations:
        double bestScore = getScore(profile, relations);
        System.out.println("score: " + bestScore);
        long count = 0;
        long maxCount = 1000;
        do {
            relations = randomMove(best, profile.getEventCount());
            double currentScore = getScore(profile, relations);
            if (currentScore > bestScore) {
                bestScore = currentScore;
                count = 0;
                best = (TemporalRelations) relations.clone();
            }
            count++;
            System.out.println("current config (" + currentScore + ")");
            System.out.println(relations);
            System.out.println("current best (" + bestScore + ")");
        } while (count < maxCount);
        System.out.println("final config: ");
        System.out.println(best);
        return net;
    }

    private TemporalRelations randomMove(TemporalRelations relations, int eventCount) {
        // randomly permute one pair in the relations:
        TemporalRelations nextRelations = (TemporalRelations) relations.clone();
        nextRelations.randomlyPermuteOne();
        return nextRelations;
    }

    private double getScore(TemporalProfile profile, TemporalRelations relations) {
        // calculate distributions of every event

        OnlineNormalEstimator procedure = new OnlineNormalEstimator();

        int eventCount = profile.getEventCount();
        for (int eventId = 0; eventId < eventCount; eventId++) {
            List<TemporalRelation> relationsOfEvent = getRelationsOfEvent(eventId, relations, eventCount);
            BitSet sequenceSet = TemporalProfile.getBitSet(relationsOfEvent, TemporalRelation.SEQUENCE);
            TLongCollection durations = profile.getDurationsForEvent(eventId, sequenceSet);
            durations.forEach(procedure);

        }
        return alpha * (1 / (Math.abs(procedure.mean()) + 1)) + (1 - alpha) / (Math.log(2 + procedure.getCountBelowZero()));
    }

    private List<TemporalRelation> getRelationsOfEvent(int eventId, TemporalRelations relations, int eventCount) {
        List<TemporalRelation> eventRelations = new ArrayList<TemporalRelation>();
        for (int i = 0; i < eventCount; i++) {
            if (i < eventId) {
                TemporalRelation rel = relations.get(eventId, i);
                if (rel.equals(TemporalRelation.REVERSE_SEQUENCE)) {
                    eventRelations.add(TemporalRelation.SEQUENCE);
                } else if (rel.equals(TemporalRelation.SEQUENCE)) {
                    eventRelations.add(TemporalRelation.REVERSE_SEQUENCE);
                } else {
                    eventRelations.add(rel);
                }
            } else {
                TemporalRelation rel = relations.get(i, eventId);
                eventRelations.add(rel);
            }
        }
        return eventRelations;
    }


}
