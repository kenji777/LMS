package itf.com.lms.logic.test;

import java.util.List;

import itf.com.lms.logic.data.TestSpec;

/**
 * Static utility to evaluate measurements against a spec.
 */
public final class TestEvaluator {

    private TestEvaluator() {
    }

    public static TestResult evaluate(TestSpec spec, List<Double> measurements) {
        if (spec == null || measurements == null || measurements.isEmpty()) {
            return new TestResult(false, 0, "No data");
        }

        double sum = 0;
        for (Double d : measurements) {
            if (d != null) {
                sum += d;
            }
        }
        double avg = sum / measurements.size();

        boolean passed = avg >= spec.getLowerLimit() && avg <= spec.getUpperLimit();
        String reason = passed ? "Within limits" : "Out of range";
        return new TestResult(passed, avg, reason);
    }
}










