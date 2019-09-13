package io.jenkins.plugins.testing;

import hudson.FilePath;
import hudson.model.Result;
import hudson.slaves.DumbSlave;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.File;
import java.net.URL;
import java.util.List;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DotCoverStepExecutionTest {

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsConfiguredWithCodeRule master = new JenkinsConfiguredWithCodeRule();

    private static CpsFlowDefinition makeDotCoverPipeline() {
        CpsFlowDefinition pipeline = new CpsFlowDefinition("node { dotcover () }", true);
        return pipeline;
    }

    @Test
    @ConfiguredWithCode("jenkins_no_global_excludes.yml")
    public void skipDotCoverTasksIfNoMatchingTestDLLs() throws Exception {
        CpsFlowDefinition pipelineDefinition = makeDotCoverPipeline();
        WorkflowJob project = master.createProject(WorkflowJob.class);
        project.setDefinition(pipelineDefinition);
        master.buildAndAssertSuccess(project);
    }


    @Test
    @ConfiguredWithCode("jenkins_no_global_excludes.yml")
    public void testInvalidTestDllFailsBuild() throws Exception // TODO can i get the failure cause somehow?
    {
        String pipeline = "pipeline {\n" +
                "    agent any\n" +
                "    stages {\n" +
                "        stage ('prepare-for-test') {\n" +
                "            steps {\n" +
                "\n" +
                "                    writeFile file: 'EmptyTest.dll', text: ''\n" +
                "                    dotcover vsTestAssemblyFilter: '**/EmptyTest.dll'\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "post {\n" +
                "always {\n" +
                "archiveArtifacts artifacts: '*.xml'\n" +
                "} \n" +
                "} \n" +
                "}\n";

        WorkflowJob project = master.createProject(WorkflowJob.class);
        CpsFlowDefinition pipelineDefinition = new CpsFlowDefinition(pipeline, true);
        project.setDefinition(pipelineDefinition);
        WorkflowRun build = project.scheduleBuild2(0).get(); // schedule right now and wait to complete.
        master.assertBuildStatus(Result.FAILURE, build);
    }

    @Test
    @ConfiguredWithCode("jenkins_no_global_excludes.yml")
    public void foldersCreatedCorrectly() throws Exception {
        final String pipeline = "pipeline {\n" +
                "  agent {label 'test-agent'}\n" +
                "\n" +
                "  stages {\n" +
                "    stage ('coverage') {\n" +
                "      steps {\n" +
                "      dotcover()\n" +
                "      }\n" +
                "    \n" +
                "    }\t\n" +
                "  }\n" +
                "}";

        CpsFlowDefinition definition = new CpsFlowDefinition(pipeline, true);
        DumbSlave slave = master.createOnlineSlave();
        slave.setLabelString("test-agent");
        WorkflowJob project = master.createProject(WorkflowJob.class);
        project.setDefinition(definition);
        master.buildAndAssertSuccess(project);
        FilePath agentWorkspace = slave.getWorkspaceFor(project);
        List<FilePath> subDirectories = agentWorkspace.listDirectories();
        boolean outputDirCreated = false;
        boolean tempDirCreated = false;
        for (FilePath dir : subDirectories) {
            if (dir.getRemote().endsWith("coverage")) {
                outputDirCreated = true;
            } else if (dir.getRemote().endsWith("temp")) {
                tempDirCreated = true;
            }
        }

        Assert.assertTrue("The dotcover output directory was created.", outputDirCreated);
        Assert.assertTrue("the temp dir was created", tempDirCreated);
    }

    @Test
    @ConfiguredWithCode("jenkins_no_global_excludes.yml")
    public void runTestsAndDoCoverageForProjectOk() throws Exception {
        WorkflowJob project = master.createProject(WorkflowJob.class);
        URL zipFile = getClass().getResource("TestAppForDotCover.zip");
        String zipFilePath = new File(zipFile.toURI()).getPath().replace("\\", "/");
        CpsFlowDefinition pipelineDefinition = new CpsFlowDefinition("" +
                "node {\n" +
                "  unzip '" + zipFilePath + "'\n" +
                "  dotcover vsTestAssemblyFilter: '**/*Test*/bin/**/*Test.dll', htmlReportPath: 'reports/my-coverage.html'\n" +
                "}", true);
        project.setDefinition(pipelineDefinition);

        master.buildAndAssertSuccess(project);
        FilePath workspace = master.jenkins.getWorkspaceFor(project);
        FilePath[] snapshots = workspace.list("**/*.cov");
        FilePath[] htmlReport = workspace.list("**/my-coverage.html");
        FilePath[] nDependReport = workspace.list("**/" + DotCoverStep.DescriptorImpl.DEFAULT_NDEPEND_REPORT_PATH);
        FilePath[] detailedXMLReport = workspace.list("**/" + DotCoverStep.DescriptorImpl.DEFAULT_DETAILED_REPORT_PATH);
        String reportContent = htmlReport[0].readToString();

        assertThat(snapshots.length, is(3));
        assertThat(htmlReport.length, is(1));
        assertThat(nDependReport.length, is(1));
        assertThat(detailedXMLReport.length, is(1));
        assertThat(reportContent, containsString("iframe sandbox"));
    }

}
