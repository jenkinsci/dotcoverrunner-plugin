package io.jenkins.plugins.testing;

import com.google.common.base.Strings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jenkinsci.plugins.vstest_runner.VsTestInstallation;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

/**
 * Represents one execution of a @{@link DotCoverStep} in a @{@link hudson.model.Run}.
 */
public final class DotCoverStepExecution extends SynchronousNonBlockingStepExecution<DotCoverStep> implements Serializable {

    private static final long serialVersionUID = -1431093121789817171L;
    final FilePath tempDir;
    final FilePath outputDir;
    final DotCoverStep dotCoverStep;
    private final transient PrintStream buildConsole;
    private final transient Launcher launcher;
    private final FilePath workspace;
    private final String dotCoverToolPath;
    private final String agentHtmlReportPath;
    private final String combinedSnapshotPath;
    private final String agentNDependReportPath;
    private final String agentDetailedReportPath;
    private final EnvVars envVars;

    public DotCoverStepExecution(@Nonnull StepContext context, @Nonnull DotCoverStep dotCoverStep) throws IOException, InterruptedException {
        super(context);
        TaskListener listener = context.get(TaskListener.class);
        this.buildConsole = listener.getLogger();
        this.workspace = context.get(FilePath.class);
        this.launcher = context.get(Launcher.class);
        this.dotCoverStep = dotCoverStep;
        this.envVars = context.get(EnvVars.class);
        Node node = workspaceToNode(workspace);
        DotCoverInstallation dotCover = DotCoverInstallation.getDefaultInstallation().forNode(node, listener);
        this.dotCoverToolPath = toAgentPath(workspace.child(dotCover.getHome()));
        createDirIfNeeded(workspace);
        this.tempDir = workspace.child("temp");
        this.outputDir = workspace.child(dotCoverStep.getOutputDir());
        createDirIfNeeded(tempDir, outputDir);
        if (StringUtils.isNotBlank(dotCoverStep.getHtmlReportPath())) {
            agentHtmlReportPath = toAgentPath(outputDir.child(dotCoverStep.getHtmlReportPath()));
        } else {
            agentHtmlReportPath = null;
        }
        if (StringUtils.isNotBlank(dotCoverStep.getNDependXmlReportPath())) {
            agentNDependReportPath = toAgentPath(outputDir.child(dotCoverStep.getNDependXmlReportPath()));
        } else {
            agentNDependReportPath = null;
        }
        if (StringUtils.isNotBlank(dotCoverStep.getDetailedXMLReportPath())) {
            agentDetailedReportPath = toAgentPath(outputDir.child(dotCoverStep.getDetailedXMLReportPath()));
        } else {
            agentDetailedReportPath = null;
        }
        combinedSnapshotPath = toAgentPath(outputDir.child(dotCoverStep.getSnapshotPath()));
    }

    @Override
    protected DotCoverStep run() throws Exception {
        FilePath[] assemblies = workspace.list(dotCoverStep.getVsTestAssemblyFilter());
        if (assemblies.length == 0) {
            return dotCoverStep;
        }
        createCoverageSnapshots(assemblies, buildConsole);
        mergeSnapshots();
        createHTMLReport();
        createNDependReport();
        createDetailedXmlReport();
        return dotCoverStep;
    }

    /**
     * Map workspace to its node or jenkins instance
     *
     * @param workspace The workspace to map
     * @return The node that the workspace is associated with.
     */
    private static Node workspaceToNode(@Nonnull FilePath workspace) {
        Computer computer = workspace.toComputer();
        Node node = null;
        if (computer != null) node = computer.getNode();
        return (node != null) ? node : Jenkins.get();
    }


    private void createCoverageSnapshots(@Nonnull FilePath[] assemblies, @Nonnull PrintStream buildConsole) throws IOException, InterruptedException {
        if (assemblies.length == 0) {
            return;
        }
        DotCoverConfigurationBuilder builder = new DotCoverConfigurationBuilder(this);
        for (FilePath assembly : assemblies) {
            Document config = builder.buildXmlDocument(assembly);
            String assemblyName = assembly.getName();
            String configXmlPath = toAgentPath(outputDir.child(assemblyName + DotCoverStep.CONFIG_XML_NAME));
            buildConsole.println("---------------------------------------------------------------------------------------");
            buildConsole.println("Generating DotCover config xml and writing it to " + configXmlPath);
            buildConsole.println("---------------------------------------------------------------------------------------");
            writeConfig(config, configXmlPath);
            buildConsole.println("---------------------------------------------------------------------------------------");
            buildConsole.println("Running DotCover testing for test assembly: " + assemblyName);
            buildConsole.println("---------------------------------------------------------------------------------------");
            launchDotCover("Cover", configXmlPath); // Generate coverage information
        }
    }

    private void writeConfig(Document config, String configXmlPath) throws IOException, InterruptedException {
        FilePath destination = workspace.child(configXmlPath);
        try (OutputStream out = destination.write()) {
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(out, format);
            writer.write(config);
            writer.close();
        }
    }

    private void mergeSnapshots() throws IOException, InterruptedException {
        FilePath[] snapshotsToMerge = tempDir.list("**/*" + DotCoverStep.SNAPSHOT_MERGE_SUFFIX);
        List<String> snapshotPaths = new ArrayList<>();
        for (FilePath filePath : snapshotsToMerge) {
            snapshotPaths.add(toAgentPath(filePath));
        }
        String mergedSnapshotPaths = String.join(";", snapshotPaths);
        if (!Strings.isNullOrEmpty(mergedSnapshotPaths)) {
            launchDotCover("Merge", "/Source=" + mergedSnapshotPaths, "/Output=" + combinedSnapshotPath);
        }
    }

    private void createDetailedXmlReport() throws IOException, InterruptedException {
        if (!Strings.isNullOrEmpty(dotCoverStep.getDetailedXMLReportPath())) {
            launchDotCover("Report", "/ReportType=DetailedXML", "/Source=" + combinedSnapshotPath, "/Output=" + agentDetailedReportPath);
        }
    }

    private void createNDependReport() throws IOException, InterruptedException {
        if (!Strings.isNullOrEmpty(dotCoverStep.getNDependXmlReportPath())) {
            launchDotCover("Report", "/ReportType=NDependXML", "/Source=" + combinedSnapshotPath, "/Output=" + agentNDependReportPath);
        }
    }

    public int launchDotCover(String... arguments) throws IOException, InterruptedException {
        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.add(dotCoverToolPath);
        builder.add(arguments);

        int exitCode = launcher
                .launch()
                .cmds(builder)
                .envs(envVars)
                .stdout(buildConsole)
                .stderr(buildConsole)
                .pwd(workspace)
                .start()
                .join();

        if (exitCode != 0) {
            throw new IllegalStateException("The launcher exited with a non-zero exit code. Exit code: " + exitCode);
        }
        return exitCode;
    }

    private void createHTMLReport() throws IOException, InterruptedException {
        if (!Strings.isNullOrEmpty(dotCoverStep.getHtmlReportPath())) {
            launchDotCover("Report", "/ReportType=HTML", "/Source=" + combinedSnapshotPath, "/Output=" + agentHtmlReportPath);
            relaxJavaScriptSecurity(agentHtmlReportPath);
        }
    }

    private void relaxJavaScriptSecurity(@Nonnull String htmlReportPath) throws IOException, InterruptedException {
        Charset utf8 = StandardCharsets.UTF_8;
        FilePath report = workspace.child(htmlReportPath);
        String content = report.readToString();
        content = content.replaceAll(DotCoverStep.IFRAME_NO_JAVASCRIPT, DotCoverStep.IFRAME_ALLOW_JAVASCRIPT);
        report.write(content, utf8.toString());
    }

    final String getVsTestToolPath() throws IOException, InterruptedException {
        EnvVars envVars = getContext().get(EnvVars.class);
        TaskListener listener = getContext().get(TaskListener.class);
        Node node = workspaceToNode(workspace);
        VsTestInstallation installation = VsTestInstallation.getDefaultInstallation();

        if (envVars != null) {
            return installation.forEnvironment(envVars).getVsTestExe();
        }
        return installation.forNode(node, listener).getVsTestExe();
    }

    private void createDirIfNeeded(FilePath... dirs) throws IOException, InterruptedException {
        for (FilePath directory : dirs) {
            if (!directory.exists()) {
                directory.mkdirs();
            }
        }
    }

    // Sadly, this is necessary due to limitations in the current implementation of getRemote().
    final String toAgentPath(@Nonnull FilePath filePath) throws IOException, InterruptedException {
        if (launcher.isUnix()) {
            return filePath.getRemote();
        } else {
            return filePath.toURI().getPath().substring(1).replace("\\", "/");
        }
    }

}
