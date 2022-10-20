package org.xbib.net.socket;

import java.util.concurrent.TimeUnit;

public class Metric {
    
    private int count = 0;

    private double sumOfSquaresOfDifferences = 0.0;

    private double average = 0.0;

    private long min = Long.MAX_VALUE;

    private long max = Long.MIN_VALUE;

    public Metric() {
    }

    public int getCount() {
        return count;
    }

    public double getSumOfSquaresOfDifferences() {
        return sumOfSquaresOfDifferences;
    }

    public double getStandardDeviation() {
        return count == 0 ? 0.0 : Math.sqrt(getSumOfSquaresOfDifferences() / getCount());
    }

    public double getAverage() {
        return average;
    }

    public long getMinimum() {
        return min;
    }

    public long getMaximum() {
        return max;
    }

    public void update(long sample) {
        count++;
        double oldAvg = average;
        average += (sample - oldAvg)/ count;
        sumOfSquaresOfDifferences += (sample - getAverage())*(sample - oldAvg);
        min = Math.min(min, sample);
        max = Math.max(max, sample);
    }

    public String getSummary(TimeUnit unit) {
        double nanosPerUnit = TimeUnit.NANOSECONDS.convert(1, unit); 
        return String.format("cnt/min/avg/max/stddev = %d/%.3f/%.3f/%.3f/%.3f",
                getCount(),
                getMinimum()/nanosPerUnit, 
                getAverage()/nanosPerUnit,
                getMaximum()/nanosPerUnit, 
                getStandardDeviation()/nanosPerUnit);
    }
}
