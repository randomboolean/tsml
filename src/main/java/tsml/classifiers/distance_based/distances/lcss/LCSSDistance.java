package tsml.classifiers.distance_based.distances.lcss;

import tsml.classifiers.distance_based.distances.BaseDistanceMeasure;
import tsml.classifiers.distance_based.utils.params.ParamHandler;
import tsml.classifiers.distance_based.utils.params.ParamSet;
import weka.core.Instance;
import weka.core.neighboursearch.PerformanceStats;

/**
 * LCSS distance measure.
 * <p>
 * Contributors: goastler
 */
public class LCSSDistance extends BaseDistanceMeasure {

    // delta === warp
    // epsilon === diff between two values before they're considered the same AKA tolerance

    private double epsilon = 0.01;
    private int delta = 0;

    public static String getEpsilonFlag() {
        return "e";
    }

    public static String getDeltaFlag() {
        return "d";
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    @Override
    public double distance(final Instance first,
        final Instance second,
        double limit,
        final PerformanceStats stats) {

        checkData(first, second);

        int aLength = first.numAttributes() - 1;
        int bLength = second.numAttributes() - 1;

        // 22/10/19 goastler - limit LCSS such that if any value in the current window is larger than the limit then we can stop here, no point in doing the extra work
        if(limit != Double.POSITIVE_INFINITY) { // check if there's a limit set
            // if so then reverse engineer the max LCSS distance and replace the limit
            // this is just the inverse of the return value integer rounded to an LCSS distance
            limit = (int) ((1 - limit) * aLength) + 1;
        }

        int[][] lcss = new int[aLength + 1][bLength + 1];

        int warpingWindow = getDelta();
        if(warpingWindow < 0) {
            warpingWindow = aLength + 1;
        }

        for(int i = 0; i < aLength; i++) {
            boolean tooBig = true;
            for(int j = i - warpingWindow; j <= i + warpingWindow; j++) {
                if(j < 0) {
                    j = -1;
                } else if(j >= bLength) {
                    j = i + warpingWindow;
                } else {
                    if(second.value(j) + this.epsilon >= first.value(i) && second.value(j) - epsilon <= first
                        .value(i)) {
                        lcss[i + 1][j + 1] = lcss[i][j] + 1;
                    } else if(lcss[i][j + 1] > lcss[i + 1][j]) {
                        lcss[i + 1][j + 1] = lcss[i][j + 1];
                    } else {
                        lcss[i + 1][j + 1] = lcss[i + 1][j];
                    }
                    // if this value is less than the limit then fast-fail the limit overflow
                    if(tooBig && lcss[i + 1][j + 1] < limit) {
                        tooBig = false;
                    }
                }
            }

            // if no element is lower than the limit then early abandon
            if(tooBig) {
                return Double.POSITIVE_INFINITY;
            }

        }
        //        System.out.println(ArrayUtilities.toString(lcss, ",", System.lineSeparator()));

        int max = -1;
        for(int j = 1; j < lcss[lcss.length - 1].length; j++) {
            if(lcss[lcss.length - 1][j] > max) {
                max = lcss[lcss.length - 1][j];
            }
        }
        return 1 - ((double) max / aLength);
    }

    @Override
    public ParamSet getParams() {
        return super.getParams().add(getEpsilonFlag(), epsilon).add(getDeltaFlag(), delta);
    }

    @Override
    public void setParams(final ParamSet param) {
        ParamHandler.setParam(param, getEpsilonFlag(), this::setEpsilon, Double.class);
        ParamHandler.setParam(param, getDeltaFlag(), this::setDelta, Integer.class);
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(final int delta) {
        this.delta = delta;
    }
}
