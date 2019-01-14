package io.jenkins.plugins.testing;

import hudson.EnvVars;
import hudson.cli.CLICommandInvoker;
import hudson.model.Computer;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static hudson.cli.CLICommandInvoker.Matcher.succeededSilently;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestRunnerStepTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private WorkflowJob job;
    private WorkflowRun run;

    @Before
    public void setup() throws Exception {
        job = jenkinsRule.jenkins.createProject(WorkflowJob.class, "workflow");
        jenkinsRule.createSlave("dummy1", "a", new EnvVars());
        jenkinsRule.createSlave("dummy2", "a b", new EnvVars());
        jenkinsRule.createSlave("dummy3", "a b c", new EnvVars());
        DumbSlave slave = jenkinsRule.createSlave("dummy4", "a b c d", new EnvVars());
        slave.toComputer().waitUntilOnline();
        CLICommandInvoker command = new CLICommandInvoker(jenkinsRule, "disconnect-node");
        CLICommandInvoker.Result result = command
                .authorizedTo(Computer.DISCONNECT, Jenkins.READ)
                .invokeWithArgs("dummy4");
        assertThat(result, succeededSilently());
        assertThat(slave.toComputer().isOffline(), equalTo(true));
    }

    @Test
    public void test_something_happens() throws Exception {
        job.setDefinition(new CpsFlowDefinition("testRunner(testPlatform: 'x64', testCaseFilter: 'onlySlowOnes')", true));
        run = job.scheduleBuild2(0).get(); // schedule for immediate execution
        jenkinsRule.assertBuildStatus(Result.SUCCESS, run);

        jenkinsRule.assertLogContains("TestRunner running with testPlatform=x64", run);
    }

}
