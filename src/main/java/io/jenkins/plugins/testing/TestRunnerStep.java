package io.jenkins.plugins.testing;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

public class TestRunnerStep extends Step {

    @Extension
    public static class Desc extends StepDescriptor
    {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
           return Collections.unmodifiableSet(Collections.singleton(TaskListener.class));
        }

        /** Returns the DSL method name used by jenkins.
         * @return
         */
        @Override
        public String getFunctionName() {
            return "testRunner";
        }
    }


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

        }

        @Override
        protected Object run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            PrintStream logger = listener.getLogger();
            logger.println("TestRunner running with testPlatform=" + testPlatform);
            return null;
        }
    }







}
