package io.jenkins.plugins.testing;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.*;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class DotCoverStep extends Step implements Serializable {

    public static final String SNAPSHOT_MERGE_SUFFIX = ".merge.cov";
    public static final String CONFIG_XML_NAME = ".DotCoverConfig.xml";
    public static final String IFRAME_NO_JAVASCRIPT = "<iframe id=\"sourceCode\">";
    public static final String IFRAME_ALLOW_JAVASCRIPT = "<iframe sandbox=\"allow-scripts allow-same-origin allow-top-navigation\" id=\"sourceCode\">";
    private static final long serialVersionUID = 1180920115994863516L;
    private static final String JENKINS_FUNCTION_NAME = "dotcover";
    private String vsTestPlatform = DescriptorImpl.DEFAULT_TEST_PLATFORM; // default defined in config.jelly.
    private String vsTestCaseFilter;
    private String vsTestAssemblyFilter = DescriptorImpl.DEFAULT_TEST_ASSEMBLIES_GLOB;
    private String vsTestArgs;
    private String coverageInclude;
    private String coverageClassInclude;
    private String coverageFunctionInclude;
    private String coverageExclude;
    private String processInclude;
    private String processExclude;
    private String outputDir = DescriptorImpl.DEFAULT_OUTPUT_DIR;
    private String htmlReportPath = DescriptorImpl.DEFAULT_HTML_REPORT_PATH;
    private String nDependXmlReportPath = DescriptorImpl.DEFAULT_NDEPEND_REPORT_PATH;
    private String detailedXMLReportPath = DescriptorImpl.DEFAULT_DETAILED_REPORT_PATH;
    private String snapsnotPath = "snapshot.cov";

    @DataBoundConstructor
    public DotCoverStep() {
    }

    @Override
    public StepExecution start(StepContext stepContext) throws IOException, InterruptedException {
        return new DotCoverStepExecution(stepContext, this);
    }

    public String getVsTestPlatform() {
        return vsTestPlatform;
    }

    @DataBoundSetter
    public void setVsTestPlatform(String vsTestPlatform) {
        this.vsTestPlatform = Util.fixEmptyAndTrim(vsTestPlatform);
    }

    public String getVsTestCaseFilter() {
        return vsTestCaseFilter;
    }

    @DataBoundSetter
    public void setVsTestCaseFilter(String vsTestCaseFilter) {
        this.vsTestCaseFilter = Util.fixEmptyAndTrim(vsTestCaseFilter);
    }

    public String getVsTestAssemblyFilter() {
        return vsTestAssemblyFilter;
    }

    @DataBoundSetter
    public void setVsTestAssemblyFilter(String vsTestAssemblyFilter) {
        this.vsTestAssemblyFilter = Util.fixEmptyAndTrim(vsTestAssemblyFilter);
    }

    public String getVsTestArgs() {
        return vsTestArgs;
    }

    @DataBoundSetter
    public void setVsTestArgs(String vsTestArgs) {
        this.vsTestArgs = Util.fixEmptyAndTrim(vsTestArgs);
    }

    public String getCoverageAssemblyInclude() {
        return coverageInclude;
    }

    @DataBoundSetter
    public void setCoverageAssemblyInclude(String coverageInclude) {
        this.coverageInclude = Util.fixEmptyAndTrim(coverageInclude);
    }

    public String getCoverageClassInclude() {
        return coverageClassInclude;
    }

    @DataBoundSetter
    public void setCoverageClassInclude(String coverageClassInclude) {
        this.coverageClassInclude = Util.fixEmptyAndTrim(coverageClassInclude);
    }

    public String getCoverageFunctionInclude() {
        return coverageFunctionInclude;
    }

    @DataBoundSetter
    public void setCoverageFunctionInclude(String coverageFunctionInclude) {
        this.coverageFunctionInclude = Util.fixEmptyAndTrim(coverageFunctionInclude);
    }

    public String getCoverageExclude() {
        return coverageExclude;
    }

    @DataBoundSetter
    public void setCoverageExclude(String coverageExclude) {
        this.coverageExclude = Util.fixEmptyAndTrim(coverageExclude);
    }

    public String getProcessInclude() {
        return processInclude;
    }

    @DataBoundSetter
    public void setProcessInclude(String processInclude) {
        this.processInclude = Util.fixEmptyAndTrim(processInclude);
    }

    public String getProcessExclude() {
        return processExclude;
    }

    @DataBoundSetter
    public void setProcessExclude(String processExclude) {
        this.processExclude = Util.fixEmptyAndTrim(processExclude);
    }

    public String getHtmlReportPath() {
        return htmlReportPath;
    }

    @DataBoundSetter
    public void setHtmlReportPath(String htmlReportPath) {
        this.htmlReportPath = Util.fixEmptyAndTrim(htmlReportPath);
    }

    public String getNDependXmlReportPath() {
        return nDependXmlReportPath;
    }

    @DataBoundSetter
    public void setNDependXmlReportPath(String nDependXmlReportPath) {
        this.nDependXmlReportPath = Util.fixEmptyAndTrim(nDependXmlReportPath);
    }

    public String getDetailedXMLReportPath() {
        return detailedXMLReportPath;
    }

    @DataBoundSetter
    public void setDetailedXMLReportPath(String detailedXMLReportPath) {
        this.detailedXMLReportPath = Util.fixEmptyAndTrim(detailedXMLReportPath);
    }

    public String getOutputDir() {
        return outputDir;
    }

    @DataBoundSetter
    public void setOutputDir(String outputDir) {
        this.outputDir = Util.fixEmptyAndTrim(outputDir);
    }

    public String getSnapshotPath() {
        return snapsnotPath;
    }

    @Extension
    @Symbol("dotCover")
    public static class DescriptorImpl extends StepDescriptor {

        public static final String DEFAULT_TEST_ASSEMBLIES_GLOB = "**/*Test/bin/**/Release/*Test.dll";
        public static final String DEFAULT_TEST_PLATFORM = "x64";
        public static final String DEFAULT_OUTPUT_DIR = "coverage";
        public static final String DEFAULT_HTML_REPORT_PATH = "index.html";
        public static final String DEFAULT_DETAILED_REPORT_PATH = "detailed-report.xml";
        public static final String DEFAULT_NDEPEND_REPORT_PATH = "ndepend-report.xml";

        @Override
        @NonNull
        public String getFunctionName() {
            return JENKINS_FUNCTION_NAME;
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Generate code coverage data and report(s)";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> contexts = new HashSet<>();
            contexts.add(TaskListener.class);
            contexts.add(Launcher.class);
            contexts.add(FilePath.class);
            contexts.add(EnvVars.class);
            return Collections.unmodifiableSet(contexts);
        }
    }

}
