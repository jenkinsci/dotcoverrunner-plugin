package io.jenkins.plugins.testing;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DotCoverStep extends Step implements Serializable {

    private static final long serialVersionUID = 1180920115994863516L;

    private static final String JENKINS_FUNCTION_NAME = "dotcover";

    private String vsTestPlatform;
    private String vsTestCaseFilter = DescriptorImpl.DEFAULT_VSTESTCASEFILTER;
    private String vsTestAssemblyFilter = DescriptorImpl.DEFAULT_TEST_ASSEMBLIES_GLOB;
    private String vsTestArgs;
    private String htmlReportPath;
    private String nDependXmlReportPath;
    private String detailedXMLReportPath;
    private String coverageInclude;
    private String coverageClassInclude;
    private String coverageFunctionInclude;
    private String coverageExclude;
    private String processInclude;
    private String processExclude;

    @DataBoundConstructor
    public DotCoverStep() {
    }

    @Override
    public StepExecution start(StepContext stepContext) throws IOException, InterruptedException {
        return new DotCoverStepExecution(stepContext, this);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public static final String DEFAULT_VSTESTCASEFILTER = "**Test*";
        public static final String DEFAULT_TEST_ASSEMBLIES_GLOB = "**/*Test/bin/**/Release/*Test.dll";

        @Override
        public String getFunctionName() {
            return JENKINS_FUNCTION_NAME;
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Generate code coverage data and report(s)";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            Set<Class<?>> contexts = new HashSet<>();
            contexts.add(TaskListener.class);
            contexts.add(Launcher.class);
            contexts.add(FilePath.class);
            return Collections.unmodifiableSet(contexts);
        }
    }

    public String getVsTestCaseFilter() {
        return vsTestCaseFilter;
    }

    @SuppressWarnings("unused") // Used by Stapler
    @DataBoundSetter
    public void setVsTestCaseFilter(String vsTestCaseFilter) {
        this.vsTestCaseFilter = vsTestCaseFilter;
    }

    public String getVsTestPlatform() {
        return vsTestPlatform;
    }

    @DataBoundSetter
    public void setVsTestPlatform(String vsTestPlatform) {
        this.vsTestPlatform = Util.fixEmptyAndTrim(vsTestPlatform);
    }

    @SuppressWarnings("unused") // Used by Stapler
    public String getVsTestAssemblyFilter() {
        return vsTestAssemblyFilter;
    }

    @SuppressWarnings("unused") // Used by Stapler
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

    public String getCoverageInclude() {
        return coverageInclude;
    }

    @DataBoundSetter
    @SuppressWarnings("unused") // Used by Stapler as defined in config.jelly
    public void setCoverageInclude(String coverageInclude) {
        this.coverageInclude = coverageInclude;
    }

    public String getCoverageClassInclude() {
        return coverageClassInclude;
    }

    @DataBoundSetter
    @SuppressWarnings("unused") // Used by Stapler as defined in config.jelly
    public void setCoverageClassInclude(String coverageClassInclude) {
        this.coverageClassInclude = coverageClassInclude;
    }

    public String getCoverageFunctionInclude() {
        return coverageFunctionInclude;
    }

    @DataBoundSetter
    @SuppressWarnings("unused") // Used by Stapler as defined in config.jelly
    public void setCoverageFunctionInclude(String coverageFunctionInclude) {
        this.coverageFunctionInclude = coverageFunctionInclude;
    }

    public String getCoverageExclude() {
        return coverageExclude;
    }

    @DataBoundSetter
    @SuppressWarnings("unused") // Used by Stapler as defined in config.jelly
    public void setCoverageExclude(String coverageExclude) {
        this.coverageExclude = coverageExclude;
    }

    public String getHtmlReportPath() {
        return htmlReportPath;
    }

    @DataBoundSetter
    @SuppressWarnings("unused") // Used by Stapler as defined in config.jelly
    public void setHtmlReportPath(String htmlReportPath) {
        this.htmlReportPath = htmlReportPath;
    }

    public String getNDependXmlReportPath() {
        return nDependXmlReportPath;
    }

    @DataBoundSetter
    @SuppressWarnings("unused") // Used by Stapler as defined in config.jelly
    public void setNDependXmlReportPath(String nDependXmlReportPath) {
        this.nDependXmlReportPath = nDependXmlReportPath;
    }

    public String getProcessInclude() {
        return processInclude;
    }

    @DataBoundSetter
    @SuppressWarnings("unused") // Used by Stapler as defined in config.jelly
    public void setProcessInclude(String processInclude) {
        this.processInclude = processInclude;
    }

    public String getProcessExclude() {
        return processExclude;
    }

    @DataBoundSetter
    @SuppressWarnings("unused") // Used by Stapler as defined in config.jelly
    public void setProcessExclude(String processExclude) {
        this.processExclude = processExclude;
    }

    public String getDetailedXMLReportPath() {
        return detailedXMLReportPath;
    }

    @DataBoundSetter
    @SuppressWarnings("unused") // Used by Stapler as defined in config.jelly
    public void setDetailedXMLReportPath(String detailedXMLReportPath) {
        this.detailedXMLReportPath = detailedXMLReportPath;
    }


}
