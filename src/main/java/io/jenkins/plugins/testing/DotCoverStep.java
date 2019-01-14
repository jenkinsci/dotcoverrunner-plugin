package io.jenkins.plugins.testing;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class DotCoverStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(DotCoverStep.class.getName());
    private static final String APP_NAME = "dotcover";

    private String testPlatform;
    private String testCaseFilter;
    private String pathToDotCover;

    @DataBoundConstructor
    public DotCoverStep(String testCaseFilter) {
        this.testCaseFilter = testCaseFilter;
    }

    public String getTestCaseFilter() {
        return testCaseFilter;
    }

    public String getTestPlatform() {
        return testPlatform;
    }

    @DataBoundSetter
    public void setTestPlatform(String testPlatform) {
        this.testPlatform = testPlatform;
    }

    public String getPathToDotCover() {
        return pathToDotCover;
    }

    @DataBoundSetter
    public void setPathToDotCover(String pathToDotCover) {
        this.pathToDotCover = pathToDotCover;
    }

    @Override
    public StepExecution start(StepContext stepContext) {
        return new Execution(stepContext, testPlatform, pathToDotCover);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return APP_NAME;
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return APP_NAME;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution {
        private String testPlatform;
        private String dotCoverPath;

        public Execution(StepContext context, String testPlatform, String dotCoverPath) {
            super(context);
            this.dotCoverPath = dotCoverPath;
            this.testPlatform = testPlatform;
        }

        /**
         * Map given workspace to an agent node, or the jenkins instance
         *
         * @param workspace
         * @return
         */
        private static Node workspaceToNode(@Nonnull FilePath workspace) {
            Computer computer = workspace.toComputer();
            Node node = null;
            if (computer != null) node = computer.getNode();
            return (node != null) ? node : Jenkins.get();
        }

        @Override
        protected Object run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            FilePath workspace = getContext().get(FilePath.class);

            PrintStream logger = listener.getLogger();
            // DotCoverInstallation tool = DotCoverInstallation.forNode(node);

            List<FilePath> files = workspace.list();
            logger.println("listing files below: " + workspace);
            for (FilePath path : files) {
                logger.println("File: " + path);
            }

            logger.println("TestRunner running with testPlatform=" + testPlatform + ", dotCoverPath=" + dotCoverPath);
            DotCoverWrapper wrapper = new DotCoverWrapper(dotCoverPath);
            wrapper.execute();
            return null;
        }

    }


}
