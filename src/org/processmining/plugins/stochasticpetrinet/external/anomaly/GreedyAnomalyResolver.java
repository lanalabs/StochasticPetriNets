package org.processmining.plugins.stochasticpetrinet.external.anomaly;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import intervalTree.Interval;
import intervalTree.IntervalTree;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.stochasticpetrinet.external.interaction.Record;

import java.util.*;

/**
 * Greedily resolves anomalies in Sensor data.
 * <p>
 * Assumptions: (for now)
 * - 1:1 relation between patient and treating resource
 * - only one treatment at a given time
 *
 * @author Andreas Rogge-Solti
 */
public class GreedyAnomalyResolver {

    public SortedMultiset<Record> getImputedRecords(SortedMultiset<Record> data) {
        SortedMultiset<Record> imputedRecords = TreeMultiset.create();

        Map<String, Map<String, DescriptiveStatistics>> meanTimesOfResourcesPerActivity = new HashMap<String, Map<String, DescriptiveStatistics>>();
        Map<String, DescriptiveStatistics> meanTimesPerActivity = new HashMap<String, DescriptiveStatistics>();

        Record[] allRecords = data.toArray(new Record[data.size()]);
        Map<Record, Integer> recordIds = new HashMap<Record, Integer>();

        IntervalTree<Record> recordIntervals = new IntervalTree<Record>();
        Set<Integer> unmappedIds = new HashSet<Integer>();

        Set<Pair<Integer, Integer>> mapping = new HashSet<Pair<Integer, Integer>>();

        int anomalies = 0;

        IntervalTree<Record> mappableRecords = new IntervalTree<Record>();

        for (int i = 0; i < allRecords.length; i++) {
            Record r = allRecords[i];
            recordIds.put(r, i);
            if (r.isAnomaly()) {
                anomalies++;
                mappableRecords.addInterval(r.getStartTime(), r.getEndTime(), r);
            } else {
                if (r.isActivityRecord()) {
                    unmappedIds.add(i);
                    recordIntervals.addInterval(new Interval<Record>(r.getStartTime(), r.getEndTime(), r));
                } else {
                    imputedRecords.add(r);
                }
            }
        }
        mappableRecords.build();

        // one pass to filter for clear mappings:
        for (Record r : data) {
            int rId = recordIds.get(r);
            if (unmappedIds.contains(rId)) {
                // find all other mapping candidates: (real activities, with same activity & location and overlapping times)
                List<Record> intersectingRecords = recordIntervals.get(r.getStartTime(), r.getEndTime());
                for (Record r2 : intersectingRecords) {
                    int r2Id = recordIds.get(r2);
                    if (r2Id != rId) {
                        if (unmappedIds.contains(r2Id) && (r.temporallyContains(r2) || r2.temporallyContains(r))) {
                            if (r.getActivity().equals(r2.getActivity()) && r.getLocation().equals(r2.getLocation())) {
                                // bind these two!
                                System.out.println("mapping\t " + r + "\nand\t " + r2 + "\n---------");
                                if (r.isAnomaly()) {
                                    System.out.println("Debug me!");
                                }
                                mapping.add(new Pair<Integer, Integer>(rId, r2Id));
                                unmappedIds.remove(rId);
                                unmappedIds.remove(r2Id);
                                imputedRecords.add(r);
                                imputedRecords.add(r2);
                                if (!meanTimesPerActivity.containsKey(r.getActivity())) {
                                    meanTimesPerActivity.put(r.getActivity(), new DescriptiveStatistics());
                                }
                                meanTimesPerActivity.get(r.getActivity()).addValue(r.getIntersectTime(r2));

                                String resource = getResourceId(r, r2);
                                if (!meanTimesOfResourcesPerActivity.containsKey(resource)) {
                                    meanTimesOfResourcesPerActivity.put(resource, new HashMap<String, DescriptiveStatistics>());
                                }
                                if (!meanTimesOfResourcesPerActivity.get(resource).containsKey(r.getActivity())) {
                                    meanTimesOfResourcesPerActivity.get(resource).put(r.getActivity(), new DescriptiveStatistics());
                                }
                                meanTimesOfResourcesPerActivity.get(resource).get(r.getActivity()).addValue(r.getIntersectTime(r2));
                            }
                        }
                    }
                }
            }
        }

        SortedMultiset<Record> unmappedRecords = TreeMultiset.create();

        // second pass to identify records to map
        for (Record r : data) {
            int rId = recordIds.get(r);
            if (unmappedIds.contains(rId)) {
                if (r.isActivityRecord()) {
                    unmappedRecords.add(r);
                }
            }
        }
        int unmappedActivities = unmappedRecords.size();

        int mappedActivities = 0;
        // third pass to map in a greedy way:
        for (Record unmappedRecord : unmappedRecords) {
            List<Record> mappableCandidates = mappableRecords.get(unmappedRecord.getStartTime(), unmappedRecord.getEndTime());
            Map<Record, Double> candidateScores = computeScores(unmappedRecord, mappableCandidates, meanTimesOfResourcesPerActivity);
            Record best = getBest(candidateScores);
            if (best != null) {
                mappedActivities++;
                System.out.println("Resolving anomaly mapping\t " + unmappedRecord + "\nwith\t\t\t\t " + best + "\n---------");
                long recordStartTime = best.getStartTime();
                long averageDuration = getAverageDuration(meanTimesPerActivity, meanTimesOfResourcesPerActivity, best.getId(), unmappedRecord.getActivity());
                long duration = Math.min(averageDuration, best.getIntersectTime(unmappedRecord));
                Interval<Object> overlap = new Interval<Object>(Math.max(best.getStartTime(), unmappedRecord.getStartTime()), Math.min(best.getEndTime(), unmappedRecord.getEndTime()), null);

                Record imputedRecord = new Record(best.getId(), overlap.getEnd() - duration, overlap.getEnd(), unmappedRecord.getActivity(), unmappedRecord.getLocation(), true);
                System.out.println("imputed records : " + imputedRecords.size());
                imputedRecords.add(imputedRecord);
                System.out.println("imputed records : " + imputedRecords.size());

                imputedRecords.add(unmappedRecord);

                List<Interval<Record>> intervals = mappableRecords.getIntervals(best.getStartTime() + 1);
                for (Interval<Record> interval : intervals) {
                    if (interval.getData().equals(best)) {
                        interval.setStart(imputedRecord.getEndTime());
                        best.setStartTime(imputedRecord.getEndTime());
                        mappableRecords.build();
                        if (best.getStartTime() == best.getEndTime()) {
                            intervals.remove(interval);
                            break;
                        }
                    }
                }

                if (imputedRecord.getStartTime() > best.getStartTime()) {
                    Record uptoRecord = new Record(best.getId(), recordStartTime, imputedRecord.getStartTime(), Record.IDLE, "hallway", true);
                    imputedRecords.add(uptoRecord);
                }
            }
        }

        System.out.println("mapped " + mappedActivities + " out of " + unmappedActivities);

        return imputedRecords;
    }

    private long getAverageDuration(Map<String, DescriptiveStatistics> meanTimesPerActivity,
                                    Map<String, Map<String, DescriptiveStatistics>> meanTimesOfResourcesPerActivity, String id, String activity) {
        if (meanTimesOfResourcesPerActivity.containsKey(id) && meanTimesOfResourcesPerActivity.get(id).containsKey(activity)) {
            return (long) meanTimesOfResourcesPerActivity.get(id).get(activity).getMean();
        }
        return (long) meanTimesPerActivity.get(activity).getMean();
    }

    private Record getBest(Map<Record, Double> candidateScores) {
        Record best = null;
        double maxScore = -1;
        for (Record r : candidateScores.keySet()) {
            if (candidateScores.get(r) > maxScore) {
                maxScore = candidateScores.get(r);
                best = r;
            }
        }
        return best;
    }

    private String getResourceId(Record r, Record r2) {
        if (!r.getId().startsWith("pat")) {
            return r.getId();
        } else {
            return r2.getId();
        }
    }

    private Map<Record, Double> computeScores(Record unmappedRecord, List<Record> mappableCandidates, Map<String, Map<String, DescriptiveStatistics>> meanTimesOfResourcesPerActivity) {
        Map<Record, Double> scores = new HashMap<Record, Double>();
        String resource1 = unmappedRecord.getId();
        for (Record r : mappableCandidates) {
            String resource2 = r.getId();
            if (isPatient(resource1) ^ isPatient(resource2)) {
                String resource = getResourceId(unmappedRecord, r);
                if (meanTimesOfResourcesPerActivity.containsKey(resource) && meanTimesOfResourcesPerActivity.get(resource).containsKey(unmappedRecord.getActivity())) {
                    double meanDur = meanTimesOfResourcesPerActivity.get(resource).get(unmappedRecord.getActivity()).getMean();
                    double intersect = unmappedRecord.getIntersectTime(r);
                    scores.put(r, Math.min(intersect, meanDur) / Math.max(intersect, meanDur));
                    // FIXME!! This should take all the features into account:
                    // 1. Where has the resource been before?
                    // 2. What is the probability of doing this starting from the previous known state?
                } else {
                    scores.put(r, 0.1);
                }
            }
        }
        return scores;
    }

    public static boolean isPatient(String resource1) {
        return resource1.startsWith("pat");
    }
}
