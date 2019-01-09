package io.jenkins.plugins.testing;

import hudson.Extension;
import hudson.model.Label;
import hudson.model.TaskListener;

import hudson.util.ComboBoxModel;
import jenkins.model.Jenkins;
import org.apache.log4j.Level;
import org.apache.log4j.lf5.LogLevel;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.kohsuke.stapler.DataBoundSetter;

import sun.rmi.runtime.Log;

public class TestRunnerStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(TestRunnerStep.class.getName());

    private String testPlatform;
    private String testCaseFilter;
    private String pathToDotCover;

    @DataBoundConstructor
    public TestRunnerStep(String testCaseFilter)
    {
        this.testCaseFilter = testCaseFilter;
    }

    public String getTestCaseFilter()

    { return testCaseFilter; }

    public String getTestPlatform()
    {
        return testPlatform;
    }

    public String getPathToDotCover()
    {
        return pathToDotCover;
    }


    @DataBoundSetter
    public void setPathToDotCover(String pathToDotCover)

    { this.pathToDotCover = pathToDotCover;
        LOGGER.info("Setting path to dot cover" + pathToDotCover);

    }

    @Override
    public StepExecution start(StepContext stepContext) {
        return new Execution(stepContext, testPlatform, pathToDotCover);
    }

    @DataBoundSetter
    public void setTestPlatform(String testPlatform) {
        this.testPlatform = testPlatform;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "testrunner";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "testrunner";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }



    }

    public static class Execution extends SynchronousNonBlockingStepExecution
    {

        private String testPlatform;
        private String dotCoverPath;

        public Execution(StepContext context, String testPlatform, String dotCoverPath)
        {
            super(context);
            this.dotCoverPath = dotCoverPath;
            this.testPlatform = testPlatform;

        }

        @Override
        protected Object run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            PrintStream logger = listener.getLogger();
            logger.println("TestRunner running with testPlatform=" + testPlatform + ", dotCoverPath=" + dotCoverPath);
            DotCoverWrapper wrapper = new DotCoverWrapper(dotCoverPath);
            wrapper.execute();
            return null;
        }
    }







}
