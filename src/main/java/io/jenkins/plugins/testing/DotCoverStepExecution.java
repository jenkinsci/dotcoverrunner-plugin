package io.jenkins.plugins.testing;

import com.google.common.base.Strings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
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
    private final StepContext context;
    private final DotCoverStep dotCoverStep;
    private final FilePath workspace;
    private final String mandatoryExcludedAssemblies;

    /**
     * Default constructor. Normally invoked by @{@link DotCoverStep}
     *
     * @param context      The context of this execution.
     * @param dotCoverStep The step that created this execution.
     * @throws IOException          If an IO error occured.
     * @throws InterruptedException If the thread was interrupted, which typically happens if the @{@link hudson.model.Run} is cancelled.
     */
    public DotCoverStepExecution(StepContext context, DotCoverStep dotCoverStep) throws IOException, InterruptedException {
        super(context);
        this.context = context;
        this.workspace = context.get(FilePath.class);
        this.dotCoverStep = dotCoverStep;
        mandatoryExcludedAssemblies = DotCoverConfiguration.getInstance().getMandatoryExcludedAssemblies();
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

    private static boolean isSet(final String s) {
        return !Strings.isNullOrEmpty(s);
    }

    @Override
    protected DotCoverStep run() throws Exception {
        TaskListener listener = context.get(TaskListener.class);
        FilePath tmpDir = workspace.createTempDir(DotCoverStepConfig.DOTCOVER_TEMP_DIR_NAME, "tmp");
        FilePath outputDir = workspace.child(DotCoverStepConfig.OUTPUT_DIR_NAME);

        String htmlReportPath = new File(outputDir.child(DotCoverStepConfig.HTML_REPORT_NAME).toURI()).getAbsolutePath();
        String nDependReportPath = new File(outputDir.child(DotCoverStepConfig.NDEPEND_XML_REPORT_NAME).toURI()).getAbsolutePath();
        String detailedReportPath = new File(outputDir.child(DotCoverStepConfig.DETAILED_XML_REPORT_NAME).toURI()).getAbsolutePath();
        String outputDirectoryPath = new File(outputDir.toURI()).getAbsolutePath();
        String tmpDirectoryPath = new File(tmpDir.toURI()).getAbsolutePath();
        String combinedSnapshotPath = new File(outputDir.child(DotCoverStepConfig.SNAPSHOT_NAME).toURI()).getAbsolutePath();

        try (PrintStream logger = listener.getLogger()) {
            FilePath[] assemblies = workspace.list(dotCoverStep.getVsTestAssemblyFilter());

            for (FilePath assembly : assemblies) {
                String assemblyName = assembly.getName();
                String snapshotPath = new File(tmpDir.child(assemblyName + DotCoverStepConfig.SNAPSHOT_MERGE_SUFFIX).toURI()).getAbsolutePath();
                String configXmlPath = new File(outputDir.child(assemblyName + DotCoverStepConfig.CONFIG_XML_NAME).toURI()).getAbsolutePath();
                DotCoverStepConfig config = prepareDotCoverStepConfig(assembly);
                logger.println("Generating DotCover config xml: " + configXmlPath);
                generateDotCoverConfigXml(config, snapshotPath, outputDirectoryPath, tmpDirectoryPath, configXmlPath);
                logger.println("Running DotCover for test assembly:" + assemblyName);
                launchDotCover("Cover", configXmlPath); // Generate coverage information
            }

            FilePath[] snapshotsToMerge = tmpDir.list("**/*" + DotCoverStepConfig.SNAPSHOT_MERGE_SUFFIX);

            List<String> snapshotPaths = new ArrayList<>();
            for (FilePath filePath : snapshotsToMerge) {
                snapshotPaths.add(new File(filePath.toURI()).getAbsolutePath());
            }
            String mergedSnapshotPaths = String.join(";", snapshotPaths);
            launchDotCover("Merge", "/Source=" + mergedSnapshotPaths, "/Output=" + combinedSnapshotPath);

            if (isSet(dotCoverStep.getHtmlReportPath())) {
                launchDotCover("Report", "/ReportType=HTML", "/Source=" + combinedSnapshotPath, "/Output=" + htmlReportPath);
                relaxJavaScriptSecurity(htmlReportPath);
            }

            if (isSet(dotCoverStep.getNDependXmlReportPath())) {
                launchDotCover("Report", "/ReportType=NDependXML", "/Source=" + combinedSnapshotPath, "/Output=" + nDependReportPath);
            }

            if (isSet(dotCoverStep.getDetailedXMLReportPath())) {
                launchDotCover("Report", "/ReportType=DetailedXML", "/Source=" + combinedSnapshotPath, "/Output=" + detailedReportPath);
            }
        }
        return dotCoverStep;
    }

    private void relaxJavaScriptSecurity(String htmlReportPath) throws IOException {
        Path report = Paths.get(htmlReportPath);
        Charset utf8 = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(report), utf8);
        content = content.replaceAll(DotCoverStepConfig.IFRAME_NO_JAVASCRIPT, DotCoverStepConfig.IFRAME_ALLOW_JAVASCRIPT);
        Files.write(report, content.getBytes());
    }

    private DotCoverStepConfig prepareDotCoverStepConfig(FilePath testAssembly) throws IOException, InterruptedException {
        String assemblyPath = new File(testAssembly.toURI()).getAbsolutePath();
        String assemblies = null;
        if (isSet(mandatoryExcludedAssemblies)) {
            assemblies = mandatoryExcludedAssemblies;
            if (isSet(dotCoverStep.getCoverageExclude())) {
                assemblies += ";" + dotCoverStep.getCoverageExclude();
            }
        }
        return new DotCoverStepConfig(dotCoverStep.getVsTestPlatform(), dotCoverStep.getVsTestCaseFilter(), dotCoverStep.getVsTestArgs(), assemblyPath, dotCoverStep.getCoverageInclude(), dotCoverStep.getCoverageClassInclude(), assemblies, dotCoverStep.getProcessInclude(), dotCoverStep.getProcessExclude(), dotCoverStep.getCoverageFunctionInclude());
    }

    private void generateDotCoverConfigXml(DotCoverStepConfig dotCoverStepConfig, String snapshotPath, String outputDirectory, String tmpDir, String configXmlPath) throws IOException, InterruptedException {
        ArgumentListBuilder vsTestArgsBuilder = new ArgumentListBuilder();
        vsTestArgsBuilder.add("/platform:" + dotCoverStepConfig.getVsTestPlatform());
        vsTestArgsBuilder.add("/logger:trx");
        vsTestArgsBuilder.add(dotCoverStepConfig.getTestAssemblyPath());

        if (isSet(dotCoverStepConfig.getVsTestCaseFilter())) {
            vsTestArgsBuilder.add("/testCaseFilter:" + dotCoverStepConfig.getVsTestCaseFilter());
        }

        if (isSet(dotCoverStepConfig.getVsTestArgs())) {
            vsTestArgsBuilder.add(dotCoverStepConfig.getVsTestArgs().split(" "));
        }

        Document document = DocumentHelper.createDocument();

        Element analyseParams = document.addElement("AnalyseParams");

        Element targetExecutable = analyseParams.addElement("TargetExecutable");
        targetExecutable.addText(getVsTestToolPath());

        Element targetArguments = analyseParams.addElement("TargetArguments");
        targetArguments.addText(vsTestArgsBuilder.toString());

        Element targetWorkingDir = analyseParams.addElement("TargetWorkingDir");
        targetWorkingDir.addText(outputDirectory);

        Element tempDir = analyseParams.addElement("TempDir");
        tempDir.addText(tmpDir);

        Element output = analyseParams.addElement("Output"); // Path to snapshot (.cov) file.
        output.addText(snapshotPath);

        Element filters = analyseParams.addElement("Filters");

        Element includeFilters = filters.addElement("IncludeFilters");

        Element excludeFilters = filters.addElement("ExcludeFilters");

        Element processFilters = filters.addElement("ProcessFilters");

        processFilter(processFilters.addElement("IncludeFilters"), dotCoverStepConfig.getProcessInclude());
        processFilter(processFilters.addElement("ExcludeFilters"), dotCoverStepConfig.getProcessExclude());

        if (isSet(dotCoverStepConfig.getCoverageInclude())) {
            for (String assemblyName : dotCoverStepConfig.getCoverageInclude().split(";")) {
                if (isSet(assemblyName)) {
                    Element filterEntry = includeFilters.addElement("FilterEntry");
                    filterEntry.addElement("ModuleMask").addText(assemblyName);
                    filterEntry.addElement("ClassMask").addText("*");
                    filterEntry.addElement("FunctionMask").addText("*");
                }
            }
        }

        if (isSet(dotCoverStepConfig.getCoverageClassInclude())) {
            for (String className : dotCoverStepConfig.getCoverageClassInclude().split(";")) {
                if (isSet(className)) {
                    Element filterEntry = includeFilters.addElement("FilterEntry");
                    filterEntry.addElement("ModuleMask").addText("*");
                    filterEntry.addElement("ClassMask").addText(className);
                    filterEntry.addElement("FunctionMask").addText("*");
                }
            }
        }

        if (isSet(dotCoverStepConfig.getCoverageFunctionInclude())) {
            for (String method : dotCoverStepConfig.getCoverageFunctionInclude().split(";")) {
                if (isSet(method)) {
                    Element filterEntry = includeFilters.addElement("FilterEntry");
                    filterEntry.addElement("ModuleMask").addText("*");
                    filterEntry.addElement("ClassMask").addText("*");
                    filterEntry.addElement("FunctionMask").addText(method);
                }
            }
        }

        if (isSet(dotCoverStepConfig.getCoverageAssemblyExclude())) {
            for (String assembly : dotCoverStepConfig.getCoverageAssemblyExclude().split(";")) {
                if (isSet(assembly)) {
                    Element filterEntry = excludeFilters.addElement("FilterEntry");
                    filterEntry.addElement("ModuleMask").addText(assembly);
                }
            }
        }

        try (OutputStream out = new FilePath(new File(configXmlPath)).write()) {
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(out, format);
            writer.write(document);
            writer.close();
        }
    }

    private void processFilter(Element parentElement, String input) {
        if (!isSet(input)) return;

        for (String s : input.split(";")) {
            if (isSet(s)) {
                parentElement.addElement("ProcessMask").addText(s);
            }
        }
    }

    public void launchDotCover(String... arguments) throws IOException, InterruptedException {
        TaskListener listener = context.get(TaskListener.class);
        PrintStream logger = listener.getLogger();
        FilePath workspace = context.get(FilePath.class);
        EnvVars envVars = getContext().get(EnvVars.class);
        Node node = workspaceToNode(workspace);
        DotCoverInstallation dotCover = DotCoverInstallation.getDefaultInstallation().forNode(node, listener);

        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.addQuoted(dotCover.getHome());
        builder.add(arguments);

        int exitCode = workspace.createLauncher(listener)
                .launch()
                .cmds(builder)
                .envs(envVars)
                .stdout(logger)
                .stderr(logger)
                .pwd(workspace)
                .start()
                .join();

        if (exitCode != 0) {
            throw new IllegalStateException("The launcher exited with a non-zero exit code. Exit code: " + exitCode);
        }
    }

    private String getVsTestToolPath() throws IOException, InterruptedException {
        EnvVars envVars = getContext().get(EnvVars.class);
        TaskListener listener = getContext().get(TaskListener.class);
        Node node = workspaceToNode(workspace);
        VsTestInstallation installation = VsTestInstallation.getDefaultInstallation();

        if (envVars != null) {
            return installation.forEnvironment(envVars).getVsTestExe();
        }

        if (node != null) {
            return installation.forNode(node, listener).getVsTestExe();
        }

        return installation.getVsTestExe();
    }

}
