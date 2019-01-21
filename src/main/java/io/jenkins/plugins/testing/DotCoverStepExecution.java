package io.jenkins.plugins.testing;

import com.google.common.base.Strings;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;
import jenkins.model.Jenkins;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jenkinsci.plugins.vstest_runner.VsTestInstallation;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class DotCoverStepExecution extends SynchronousNonBlockingStepExecution<DotCoverStep> implements Serializable {

    private static final long serialVersionUID = -1431093121789817171L;
    private final StepContext context;
    private final DotCoverStep dotCoverStep;
    private final FilePath workspace;

    public DotCoverStepExecution(StepContext context, DotCoverStep dotCoverStep) throws IOException, InterruptedException {
        super(context);
        this.context = context;
        this.workspace = context.get(FilePath.class);
        this.dotCoverStep = dotCoverStep;
    }

    @Override
    protected DotCoverStep run() throws Exception {
        TaskListener listener = context.get(TaskListener.class);
        FilePath tmpDir = workspace.createTempDir(DotCoverStepConfig.DOTCOVER_TEMP_DIR, "tmp");
        FilePath outputDir = workspace.child(DotCoverStepConfig.OUTPUT_DIR_NAME);

        String htmlReportPath = new File(outputDir.child(DotCoverStepConfig.HTML_REPORT_NAME).toURI()).getAbsolutePath();
        String nDependReportPath = new File(outputDir.child(DotCoverStepConfig.NDEPEND_XML_REPORT_NAME).toURI()).getAbsolutePath();
        String detailedReportPath = new File(outputDir.child(DotCoverStepConfig.DETAILED_XML_REPORT_NAME).toURI()).getAbsolutePath();

        String finalSnapshot = new File(outputDir.child("snapshot.cov").toURI()).getAbsolutePath();

        cleanOutputDirectory(outputDir);

        try (PrintStream logger = listener.getLogger()) {

            FilePath[] assemblies = workspace.list(dotCoverStep.getVsTestAssemblyFilter());

            for (FilePath assembly : assemblies) {
                DotCoverStepConfig config = prepareDotCoverStepConfig(listener, assembly);
                String snapshotPath = new File(tmpDir.child(assembly.getName() + ".merge.cov").toURI()).getAbsolutePath();
                generateDotCoverConfigXml(config, snapshotPath);
                launchDotCover("Cover", snapshotPath); // Generate coverage information
            }

            FilePath[] filesToMerge = tmpDir.list("**/*.merge.cov");
            List<String> paths = new ArrayList<>();

            for (FilePath f : filesToMerge)
            {
                paths.add(new File(f.toURI()).getAbsolutePath());
            }

            String merge = String.join(";", paths);

            launchDotCover("merge", "/Source=" + merge, "/Output=" + finalSnapshot);

            if (isSet(dotCoverStep.getHtmlReportPath())) {
                launchDotCover("Report", "/ReportType=HTML", "/Source=" + finalSnapshot, "/Output=" + htmlReportPath);
                relaxJavaScriptSecurity(htmlReportPath);
            }

            if (isSet(dotCoverStep.getNDependXmlReportPath()))
            {
                launchDotCover("Report", "/ReportType=NDependXML", "/Source=" + finalSnapshot, "/Output=" + nDependReportPath);
            }

            launchDotCover("Report", "/ReportType=DetailedXML", "/Source=" + finalSnapshot, "/Output=" + detailedReportPath);
        }
        return dotCoverStep;
    }

    private void relaxJavaScriptSecurity(String htmlReportPath) throws IOException {
        final Path report = Paths.get(htmlReportPath);
        final Charset utf8 = StandardCharsets.UTF_8;
        String content = new String(Files.readAllBytes(report), utf8);
        content = content.replaceAll(DotCoverStepConfig.IFRAME_NO_JAVASCRIPT, DotCoverStepConfig.IFRAME_ALLOW_JAVASCRIPT);
        Files.write(report, content.getBytes());
    }

    private DotCoverStepConfig prepareDotCoverStepConfig(TaskListener listener, FilePath testAssembly) throws IOException, InterruptedException {
        String testAssemblies = new File(testAssembly.toURI()).getAbsolutePath();

        String solutionFilePath = null;

        if (!isSet(dotCoverStep.getGetSolutionDir())) {
            solutionFilePath = inferSolutionFilePathOrDie();
        }

        Node node = workspaceToNode(workspace);
        String vsTestToolPath = new File(VsTestInstallation.getDefaultInstallation().forNode(node, listener).getHome()).getAbsolutePath();

        FilePath tempDir = workspace.createTempDir(DotCoverStepConfig.DOTCOVER_TEMP_DIR, "tmp");
        String tempDirPath = new File(tempDir.toURI()).getAbsolutePath();
        String dotCoverSnapshotPath = new File(tempDir.child(DotCoverStepConfig.SNAPSHOT_NAME).toURI()).getAbsolutePath();

        FilePath outputDir = workspace.child(DotCoverStepConfig.OUTPUT_DIR_NAME);
        String outputDirPath = new File(outputDir.toURI()).getAbsolutePath();
        String dotCoverConfigXmlPath = new File(outputDir.child(DotCoverStepConfig.CONFIG_NAME).toURI()).getAbsolutePath();

        String htmlReportPath = new File(outputDir.child(DotCoverStepConfig.HTML_REPORT_NAME).toURI()).getAbsolutePath();
        String nDependReportPath = new File(outputDir.child(DotCoverStepConfig.NDEPEND_XML_REPORT_NAME).toURI()).getAbsolutePath();
        String detailedReportPath = new File(outputDir.child(DotCoverStepConfig.DETAILED_XML_REPORT_NAME).toURI()).getAbsolutePath();

        String mandatoryExcludedAssemblies = DotCoverConfiguration.getInstance().getMandatoryExcludedAssemblies();

        String assembliesToExclude = (isSet(mandatoryExcludedAssemblies)) ? dotCoverStep.getCoverageExclude() + ";" + mandatoryExcludedAssemblies : dotCoverStep.getCoverageExclude();

        return new DotCoverStepConfig(solutionFilePath, tempDirPath, outputDirPath, dotCoverConfigXmlPath, dotCoverSnapshotPath, vsTestToolPath, dotCoverStep.getVsTestPlatform(), dotCoverStep.getVsTestCaseFilter(), dotCoverStep.getVsTestArgs(), testAssemblies, htmlReportPath, nDependReportPath, detailedReportPath, dotCoverStep.getCoverageInclude(), dotCoverStep.getCoverageClassInclude(), assembliesToExclude, dotCoverStep.getProcessInclude(), dotCoverStep.getProcessExclude(), dotCoverStep.getCoverageFunctionInclude());
    }

    private String inferSolutionFilePathOrDie() throws IOException, InterruptedException {
        FilePath[] solutionFiles = workspace.list("**/*.sln");
        if (solutionFiles.length != 1) // TODO check for nulls before returning zero-index, check for zero solution files.
        {
            throw new IllegalStateException("More than one solution (.sln) file present. You need to specify which one you want via the solutiondir attribute.");
        }
        return new File(solutionFiles[0].toURI()).getAbsolutePath();
    }

    private void generateDotCoverConfigXml(DotCoverStepConfig dotCoverStepConfig, String snapshotPath) throws IOException, InterruptedException {
        StringBuilder vsTestArgs = new StringBuilder();
        vsTestArgs.append("/platform:");
        vsTestArgs.append(dotCoverStepConfig.getVsTestPlatform());
        vsTestArgs.append(' ');
        vsTestArgs.append("/logger:trx");
        vsTestArgs.append(' ');

        vsTestArgs.append(dotCoverStepConfig.getTestAssemblyPath());
        vsTestArgs.append(' ');

        if (isSet(dotCoverStepConfig.getVsTestCaseFilter())) {
            vsTestArgs.append("/testCaseFilter:");
            vsTestArgs.append(dotCoverStepConfig.getVsTestCaseFilter());
        }

        if (isSet(dotCoverStepConfig.getVsTestArgs())) {
            vsTestArgs.append(dotCoverStepConfig.getVsTestArgs());
        }

        Document document = DocumentHelper.createDocument();

        Element analyseParams = document.addElement("AnalyseParams");

        Element targetExecutable = analyseParams.addElement("TargetExecutable");
        targetExecutable.addText(dotCoverStepConfig.getVsTestPath());

        Element targetArguments = analyseParams.addElement("TargetArguments");
        targetArguments.addText(vsTestArgs.toString());

        Element targetWorkingDir = analyseParams.addElement("TargetWorkingDir");
        targetWorkingDir.addText(dotCoverStepConfig.getOutputDirectory());

        Element tempDir = analyseParams.addElement("TempDir");
        tempDir.addText(dotCoverStepConfig.getTempDirectory());

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

        try (OutputStream out = new FilePath(new File(snapshotPath)).write()) {
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
        final TaskListener listener = context.get(TaskListener.class);
        PrintStream logger = listener.getLogger();
        final FilePath workspace = context.get(FilePath.class);
        final Node node = workspaceToNode(workspace);
        final DotCoverInstallation dotCover = DotCoverInstallation.getDefaultInstallation().forNode(node, listener);

        ArgumentListBuilder builder = new ArgumentListBuilder();
        builder.addQuoted(dotCover.getHome());
        builder.add(arguments);

        final int exitCode = workspace.createLauncher(listener)
                .launch()
                .cmds(builder)
                .stdout(logger)
                .start()
                .join();

        if (exitCode != 0) {
            throw new IllegalStateException("The launcher exited with a non-zero exit code. Exit code: " + exitCode);
        }
    }

    private void cleanOutputDirectory(FilePath outputDir) throws IOException, InterruptedException {
        if (outputDir.exists()) {
            outputDir.deleteRecursive();
        }
        outputDir.mkdirs();
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

}