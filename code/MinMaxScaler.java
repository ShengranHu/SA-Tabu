package TCYB;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;

public class MinMaxScaler {
    double Emin, Emax, min, max;

    public MinMaxScaler(double[] data, double max, double min){
        DoubleSummaryStatistics stat = Arrays.stream(data).summaryStatistics();
        this.Emin = stat.getMin();
        this.Emax = stat.getMax();
        this.min = min;
        this.max = max;
    }

    public double[] transform(double[] data){
        double[] transformed = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            transformed[i] = (data[i] - Emin) / (Emax - Emin) * (max - min);
        }
        return transformed;
    }

    public double transform(double data){
        double transformed = (data - Emin) / (Emax - Emin) * (max - min);
        return transformed;
    }

    public int rev_transform(double transformed){
        double origin = transformed / (max - min) * (Emax - Emin) + Emin;
        return (int) origin;
    }
}
