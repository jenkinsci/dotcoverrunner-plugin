package io.jenkins.plugins.testing;

import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DifferenceEvaluator;

public class IgnoreTextDifferenceEvaluator implements DifferenceEvaluator {

    private final ComparisonResult evaluation;

    public IgnoreTextDifferenceEvaluator(ComparisonResult evaluation) {
        this.evaluation = evaluation;
    }

    @Override
    public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
        if (outcome == ComparisonResult.EQUAL)
            return outcome;
        final Node controlNode = comparison.getControlDetails().getTarget();
        if (controlNode instanceof Text) {
            return evaluation;
        }
        return outcome;
    }
}
