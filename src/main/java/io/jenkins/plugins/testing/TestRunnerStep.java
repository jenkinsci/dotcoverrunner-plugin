package io.jenkins.plugins.testing;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.logging.Logger;

public class TestRunnerStep extends Step {


    private static final Logger LOGGER = Logger.getLogger(TestRunnerStep.class.getName());

    private String testPlatform;
    private String testCaseFilter;

    @DataBoundConstructor
    public TestRunnerStep(String testPlatform, String testCaseFilter)
    {
        this.testPlatform = testPlatform;
        this.testCaseFilter = testCaseFilter;
    }

    public String getTestPlatform()
    {
        return testPlatform;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new Execution(stepContext, testPlatform);
    }

    public static class Execution extends SynchronousNonBlockingStepExecution
    {

        private String testPlatform;

        public Execution(StepContext context, String testPlatform)
        {
            super(context);
            this.testPlatform = testPlatform;
            LOGGER.info("Execution constructed");
        }

        @Override
        protected Object run() throws Exception {
            LOGGER.info("TestRunner running with args " + testPlatform);
            return null;
        }
    }







}
