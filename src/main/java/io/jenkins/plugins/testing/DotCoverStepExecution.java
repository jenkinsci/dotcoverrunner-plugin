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
    private final String vsTestToolPath;
    private final String mandatoryExcludedAssemblies;

    public DotCoverStepExecution(StepContext context, DotCoverStep dotCoverStep) throws IOException, InterruptedException {
        super(context);
        this.context = context;
        this.workspace = context.get(FilePath.class);
        this.dotCoverStep = dotCoverStep;
        Node node = workspaceToNode(workspace);
        this.vsTestToolPath = new File(VsTestInstallation.getDefaultInstallation().forNode(node, context.get(TaskListener.class)).getHome()).getAbsolutePath();
        mandatoryExcludedAssemblies = DotCoverConfiguration.getInstance().getMandatoryExcludedAssemblies();
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

        String finalSnapshot = new File(outputDir.child("snapshot.cov").toURI()).getAbsolutePath();

        try (PrintStream logger = listener.getLogger()) {
            logger.println("Cleaning output directory: " + outputDir);
            cleanOutputDirectory(outputDir);

            FilePath[] assemblies = workspace.list(dotCoverStep.getVsTestAssemblyFilter());

            for (FilePath assembly : assemblies) {
                final String assemblyName = assembly.getName();
                final String snapshotPath = new File(tmpDir.child(assemblyName + ".merge.cov").toURI()).getAbsolutePath();
                final String configXmlPath = new File(outputDir.child(assemblyName + DotCoverStepConfig.CONFIG_XML_NAME).toURI()).getAbsolutePath();
                final DotCoverStepConfig config = prepareDotCoverStepConfig(assembly);
                generateDotCoverConfigXml(config, snapshotPath, outputDirectoryPath, tmpDirectoryPath,configXmlPath);
                launchDotCover("Cover", configXmlPath); // Generate coverage information
            }

            FilePath[] filesToMerge = tmpDir.list("**/*.merge.cov");
            List<String> paths = new ArrayList<>();

            for (FilePath f : filesToMerge) {
                paths.add(new File(f.toURI()).getAbsolutePath());
            }

            String merge = String.join(";", paths);

            launchDotCover("merge", "/Source=" + merge, "/Output=" + finalSnapshot);

            if (isSet(dotCoverStep.getHtmlReportPath())) {
                launchDotCover("Report", "/ReportType=HTML", "/Source=" + finalSnapshot, "/Output=" + htmlReportPath);
                relaxJavaScriptSecurity(htmlReportPath);
            }

            if (isSet(dotCoverStep.getNDependXmlReportPath())) {
                launchDotCover("Report", "/ReportType=NDependXML", "/Source=" + finalSnapshot, "/Output=" + nDependReportPath);
            }

            if (isSet(dotCoverStep.getDetailedXMLReportPath())) {
                launchDotCover("Report", "/ReportType=DetailedXML", "/Source=" + finalSnapshot, "/Output=" + detailedReportPath);
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
        if (isSet(mandatoryExcludedAssemblies))
        {
            assemblies = mandatoryExcludedAssemblies;
            if (isSet(dotCoverStep.getCoverageExclude()))
            {
                assemblies += ";" + dotCoverStep.getCoverageExclude();
            }
        }
        return new DotCoverStepConfig(vsTestToolPath, dotCoverStep.getVsTestPlatform(), dotCoverStep.getVsTestCaseFilter(), dotCoverStep.getVsTestArgs(), assemblyPath, dotCoverStep.getCoverageInclude(), dotCoverStep.getCoverageClassInclude(), assemblies, dotCoverStep.getProcessInclude(), dotCoverStep.getProcessExclude(), dotCoverStep.getCoverageFunctionInclude());
    }

    private void generateDotCoverConfigXml(DotCoverStepConfig dotCoverStepConfig, String snapshotPath, String outputDirectory, String tmpDir, String configXmlPath) throws IOException, InterruptedException {
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