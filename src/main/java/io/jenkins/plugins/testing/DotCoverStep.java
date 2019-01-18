package io.jenkins.plugins.testing;

import hudson.Extension;
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
import java.util.Set;

public class DotCoverStep extends Step implements Serializable {

    private static final long serialVersionUID = 1180920115994863516L;

    private static final String APP_NAME = "dotcover";
    public static final String DEFAULT_TEST_ASSEMBLIES_GLOB="\\**\\*Test\\bin\\**\\Release\\*Test.dll";

    private String vsTestPlatform;
    private String vsTestCaseFilter = DescriptorImpl.DEFAULT_VSTESTCASEFILTER;
    private String vsTestAssemblyFilter;
    private String coverageInclude;
    private String coverageClassInclude;
    private String coverageFunctionInclude;
    private String coverageExclude;
    private String htmlReportPath;
    private String nDependXmlReportPath;
    private String vsTestArgs;
    private String solutionDir;
    private String processInclude;
    private String processExclude;

    @DataBoundConstructor
    public DotCoverStep() {
        vsTestAssemblyFilter = DEFAULT_TEST_ASSEMBLIES_GLOB;
    }


    @SuppressWarnings("unused") // Used by Stapler
    @DataBoundSetter
    public void setVsTestCaseFilter(String vsTestCaseFilter) {
        this.vsTestCaseFilter = Util.fixEmptyAndTrim(vsTestCaseFilter);
    }

    public String getVsTestCaseFilter() {
        return vsTestCaseFilter;
    }

    public String getVsTestPlatform() {
        return vsTestPlatform;
    }

    @SuppressWarnings("unused") // Used by Stapler
    @DataBoundSetter
    public void setVsTestAssemblyFilter(String vsTestAssemblyFilter)
    {
        this.vsTestAssemblyFilter = vsTestAssemblyFilter;
    }

    @SuppressWarnings("unused") // Used by Stapler
    public String getVsTestAssemblyFilter()
    {
        return vsTestAssemblyFilter;
    }

    @DataBoundSetter @SuppressWarnings("unused") // Used by Stapler as defined in config.jelly
    public void setSolutionDir(String solutionDir)
    {
        this.solutionDir = solutionDir;
    }

    public String getGetSolutionDir()
    {
        return getSolutionDir();
    }

    @DataBoundSetter
    public void setVsTestPlatform(String vsTestPlatform) {
        this.vsTestPlatform = vsTestPlatform;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws IOException, InterruptedException {
        return new DotCoverStepExecution(stepContext, this);
    }


    public String getVsTestArgs() {
        return vsTestArgs;
    }

    public String getCoverageInclude() {
        return coverageInclude;
    }

    @DataBoundSetter
    public void setCoverageInclude(String coverageInclude) {
        this.coverageInclude = coverageInclude;
    }

    public String getCoverageClassInclude() {
        return coverageClassInclude;
    }

    @DataBoundSetter
    public void setCoverageClassInclude(String coverageClassInclude) {
        this.coverageClassInclude = coverageClassInclude;
    }

    public String getCoverageFunctionInclude() {
        return coverageFunctionInclude;
    }

    @DataBoundSetter
    public void setCoverageFunctionInclude(String coverageFunctionInclude) {
        this.coverageFunctionInclude = coverageFunctionInclude;
    }

    public String getCoverageExclude() {
        return coverageExclude;
    }

    @DataBoundSetter
    public void setCoverageExclude(String coverageExclude) {
        this.coverageExclude = coverageExclude;
    }

    public String getHtmlReportPath() {
        return htmlReportPath;
    }

    @DataBoundSetter
    public void setHtmlReportPath(String htmlReportPath) {
        this.htmlReportPath = htmlReportPath;
    }

    public String getNDependXmlReportPath() {
        return nDependXmlReportPath;
    }

    @DataBoundSetter
    public void setNDependXmlReportPath(String nDependXmlReportPath) {
        this.nDependXmlReportPath = nDependXmlReportPath;
    }

    @DataBoundSetter
    public void setVsTestArgs(String vsTestArgs) {
        this.vsTestArgs = vsTestArgs;
    }

    public String getSolutionDir() {
        return solutionDir;
    }

    public String getProcessInclude() {
        return processInclude;
    }

    public void setProcessInclude(String processInclude) {
        this.processInclude = processInclude;
    }

    public String getProcessExclude() {
        return processExclude;
    }

    public void setProcessExclude(String processExclude) {
        this.processExclude = processExclude;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        public static final String DEFAULT_VSTESTCASEFILTER = "**Test*";

        @Override
        public String getFunctionName() {
            return APP_NAME;
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Generate code coverage data and report(s)";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        } //TODO Should include more...

    }

}
