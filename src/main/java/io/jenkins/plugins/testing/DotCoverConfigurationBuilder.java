package io.jenkins.plugins.testing;

import com.google.common.base.Strings;
import hudson.FilePath;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class DotCoverConfigurationBuilder {

    private final DotCoverStepExecution execution;
    private final DotCoverStep step;
    private final String vsTestToolPath;
    private final String outputDirPath;
    private final String tempDirPath;

    public DotCoverConfigurationBuilder(@Nonnull DotCoverStepExecution execution) throws IOException, InterruptedException {
        this.execution = execution;
        this.step = execution.dotCoverStep;
        this.vsTestToolPath = execution.getVsTestToolPath();
        this.outputDirPath = execution.toAgentPath(execution.outputDir);
        this.tempDirPath = execution.toAgentPath(execution.tempDir);

    }

    public Document buildXmlDocument(FilePath assemblyPath) throws IOException, InterruptedException {
        ArgumentListBuilder vsTestArgsBuilder = new ArgumentListBuilder();
        vsTestArgsBuilder.add("/platform:" + step.getVsTestPlatform());
        vsTestArgsBuilder.add("/logger:trx");
        vsTestArgsBuilder.add(execution.toAgentPath(assemblyPath));

        if (StringUtils.isNotBlank(step.getVsTestCaseFilter())) {
            vsTestArgsBuilder.add("/testCaseFilter:" + step.getVsTestCaseFilter());
        }

        if (StringUtils.isNotBlank(step.getVsTestArgs())) {
            vsTestArgsBuilder.add(step.getVsTestArgs().split(" "));
        }

        Document document = DocumentHelper.createDocument();
        Element analyseParams = document.addElement("AnalyseParams");
        Element targetExecutable = analyseParams.addElement("TargetExecutable");
        targetExecutable.addText(vsTestToolPath);

        Element targetArguments = analyseParams.addElement("TargetArguments");
        targetArguments.addText(vsTestArgsBuilder.toString());

        Element targetWorkingDir = analyseParams.addElement("TargetWorkingDir");
        targetWorkingDir.addText(outputDirPath);

        Element tempDir = analyseParams.addElement("TempDir");
        tempDir.addText(tempDirPath);

        String snapshotName = assemblyPath.getName() + DotCoverStep.SNAPSHOT_MERGE_SUFFIX;
        String snapshotPath = execution.toAgentPath(execution.tempDir.child(snapshotName));
        Element output = analyseParams.addElement("Output");
        output.addText(snapshotPath);
        Element filters = analyseParams.addElement("Filters");
        Element processFilters = analyseParams.addElement("ProcessFilters");

        Element includeFilters = filters.addElement("IncludeFilters");
        Element excludeFilters = filters.addElement("ExcludeFilters");

        boolean isProcessIncludeSet = StringUtils.isNotBlank(step.getProcessInclude());
        boolean isProcessExcludeSet = StringUtils.isNotBlank(step.getProcessExclude());

        if (isProcessIncludeSet || isProcessExcludeSet) {

            if (isProcessIncludeSet) {
                processFilter(processFilters.addElement("IncludeFilters"), step.getProcessInclude());
            }
            if (isProcessExcludeSet) {
                processFilter(processFilters.addElement("ExcludeFilters"), step.getProcessExclude());
            }
        }

        if (StringUtils.isNotBlank(step.getCoverageAssemblyInclude())) {
            for (String assemblyName : step.getCoverageAssemblyInclude().split(";")) {
                if (StringUtils.isNotBlank(assemblyName)) {
                    Element filterEntry = includeFilters.addElement("FilterEntry");
                    filterEntry.addElement("ModuleMask").addText(assemblyName);
                    filterEntry.addElement("ClassMask").addText("*");
                    filterEntry.addElement("FunctionMask").addText("*");
                }
            }
        }

        if (StringUtils.isNotBlank(step.getCoverageClassInclude())) {
            for (String className : step.getCoverageClassInclude().split(";")) {
                if (StringUtils.isNotBlank(className)) {
                    Element filterEntry = includeFilters.addElement("FilterEntry");
                    filterEntry.addElement("ModuleMask").addText("*");
                    filterEntry.addElement("ClassMask").addText(className);
                    filterEntry.addElement("FunctionMask").addText("*");
                }
            }
        }

        if (StringUtils.isNotBlank(step.getCoverageFunctionInclude())) {
            for (String method : step.getCoverageFunctionInclude().split(";")) {
                if (StringUtils.isNotBlank(method)) {
                    Element filterEntry = includeFilters.addElement("FilterEntry");
                    filterEntry.addElement("ModuleMask").addText("*");
                    filterEntry.addElement("ClassMask").addText("*");
                    filterEntry.addElement("FunctionMask").addText(method);
                }
            }
        }

        String mandatoryExcludedAssemblies = DotCoverConfiguration.getInstance().getMandatoryExcludedAssemblies();
        if (StringUtils.isNotBlank(mandatoryExcludedAssemblies) || StringUtils.isNotBlank(step.getCoverageExclude())) {
            String stepExcludedAssemblies = step.getCoverageExclude();
            String excludedAssemblies = "";
            if (StringUtils.isNotBlank(stepExcludedAssemblies)) {
                excludedAssemblies += stepExcludedAssemblies;
            }
            if (StringUtils.isNotBlank(mandatoryExcludedAssemblies)) {
                excludedAssemblies += mandatoryExcludedAssemblies;
            }

            for (String assembly : excludedAssemblies.split(";")) {
                if (StringUtils.isNotBlank(assembly)) {
                    Element filterEntry = excludeFilters.addElement("FilterEntry");
                    filterEntry.addElement("ModuleMask").addText(assembly);
                }
            }
        }
        return document;
    }

    private void processFilter(Element parentElement, String input) {
        if (Strings.isNullOrEmpty(input)) return;
        for (String token : input.split(";")) {
            if (StringUtils.isNotBlank(token)) {
                parentElement.addElement("ProcessMask").addText(token);
            }
        }
    }
}
