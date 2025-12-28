package itf.com.lms.logic.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for a list of TestStep objects.
 * Thread-safe read access by exposing unmodifiable list.
 */
public class TestSequence {

    private final String testItemCode;
    private final List<TestStep> steps = Collections.synchronizedList(new ArrayList<>());

    public TestSequence(String testItemCode) {
        this.testItemCode = testItemCode;
    }

    public void addStep(TestStep step) {
        if (step != null) {
            steps.add(step);
        }
    }

    public TestStep getStep(int index) {
        return steps.get(index);
    }

    public int getTotalSteps() {
        return steps.size();
    }

    public String getTestItemCode() {
        return testItemCode;
    }

    public List<TestStep> getSteps() {
        return Collections.unmodifiableList(new ArrayList<>(steps));
    }
}










