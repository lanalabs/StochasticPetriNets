package org.processmining.plugins.alignment;

import com.timeseries.Bit;
import com.timeseries.TimeSeries;
import com.util.DistanceFunction;
import com.util.ManhattanDistance;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.alignment.model.CaseTimeSeries;

public class TimeSeriesComparer {

    /**
     * Returns the upper right triangular matrix
     * [  , 1, 2, 3, 4]
     * [  ,  , 5, 6, 7]
     * [  ,  ,  , 8, 9]
     * [  ,  ,  ,  ,10]
     * [  ,  ,  ,  ,  ]
     *
     * @param context
     * @param caseTimeSeries
     * @return
     */
    public static double[] getDistances(PluginContext context, CaseTimeSeries caseTimeSeries) {
        int size = caseTimeSeries.getTimeSeriesList().size();
        DistanceFunction<Bit> distFn = new ManhattanDistance<>();
        // exploit symmetry
        double[] matrix = new double[size * (size - 1) / 2];
        int pos = 0;
        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {
                TimeSeries<Bit> tsI = caseTimeSeries.getTimeSeries(i);
                TimeSeries<Bit> tsJ = caseTimeSeries.getTimeSeries(j);
                double distance = com.dtw.FastDTW.getWarpDistBetween(tsI, tsJ, distFn);
                final double dist = matrix[pos++] = distance;
                if (j == size - 1)
                    System.out.println("i=\t" + i + ", j=\t" + j + ", dist=\t" + dist);
            }
        }
        return matrix;
    }
}
