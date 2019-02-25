package io.jenkins.plugins.testing;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Serializable;

final class DotCoverStepConfig implements Serializable {

    public static final String DOTCOVER_TEMP_DIR_NAME = "dotcover-work";
    public static final String HTML_REPORT_NAME = "index.html";
    public static final String NDEPEND_XML_REPORT_NAME = "NDependCoverage.xml";
    public static final String DETAILED_XML_REPORT_NAME = "DetailedCoverage.xml";
    public static final String SNAPSHOT_NAME = "snapshot.cov";
    public static final String SNAPSHOT_MERGE_SUFFIX = ".merge.cov";
    public static final String OUTPUT_DIR_NAME = "DotCover";
    public static final String IFRAME_NO_JAVASCRIPT = "<iframe id=\"sourceCode\">";
    public static final String IFRAME_ALLOW_JAVASCRIPT = "<iframe sandbox=\"allow-scripts allow-same-origin allow-top-navigation\" id=\"sourceCode\">";
    public static final String CONFIG_XML_NAME = ".DotCoverConfig.xml";

    private static final long serialVersionUID = 6113092202485062423L;

    private final String vsTestPlatform;
    private final String vsTestCaseFilter;
    private final String vsTestArgs;
    private final String testAssemblyPath;
    private final String processInclude;
    private final String coverageInclude;
    private final String coverageClassInclude;
    private final String coverageFunctionInclude;
    private final String coverageAssemblyExclude;
    private final String processExclude;

    DotCoverStepConfig(@NonNull String vsTestPlatform, String vsTestCaseFilter, String vsTestArgs, @NonNull String testAssemblyPath, String coverageInclude, String coverageClassInclude, String coverageAssemblyExclude, String processInclude, String processExclude, String coverageFunctionInclude) {
        this.vsTestPlatform = vsTestPlatform;
        this.vsTestCaseFilter = vsTestCaseFilter;
        this.vsTestArgs = vsTestArgs;
        this.processInclude = processInclude;
        this.testAssemblyPath = testAssemblyPath;
        this.coverageInclude = coverageInclude;
        this.coverageClassInclude = coverageClassInclude;
        this.coverageFunctionInclude = coverageFunctionInclude;
        this.coverageAssemblyExclude = coverageAssemblyExclude;
        this.processExclude = processExclude;
    }

    public String getVsTestPlatform() {
        return vsTestPlatform;
    }

    public String getVsTestCaseFilter() {
        return vsTestCaseFilter;
    }

    public String getVsTestArgs() {
        return vsTestArgs;
    }

    public String getTestAssemblyPath() {
        return testAssemblyPath;
    }

    public String getProcessInclude() {
        return processInclude;
    }

    public String getCoverageInclude() {
        return coverageInclude;
    }

    public String getCoverageClassInclude() {
        return coverageClassInclude;
    }

    public String getCoverageFunctionInclude() {
        return coverageFunctionInclude;
    }

    public String getCoverageAssemblyExclude() {
        return coverageAssemblyExclude;
    }

    public String getProcessExclude() {
        return processExclude;
    }

}
