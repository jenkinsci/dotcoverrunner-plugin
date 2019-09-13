package io.jenkins.plugins.testing;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.annotation.Nonnull;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DotCoverConfiguratonBuilderTest {

    private static FilePath xmlOutputPath = new FilePath(new File("NotUsedInTheseTests.Dll"));
    @Rule
    public JenkinsConfiguredWithCodeRule master = new JenkinsConfiguredWithCodeRule();
    private StepContext stepContext;
    private DotCoverStep dotCoverStep;
    private DotCoverStepExecution dotCoverStepExecution;
    private DotCoverConfigurationBuilder dotCoverConfigurationBuilder;

    @Test
    @ConfiguredWithCode("jenkins_global_excludes.yml")
    public void testWithGlobalExcludeSetThenProducesCorrectDocumentStructure() throws IOException, InterruptedException, SAXException, URISyntaxException {
        String controlXml = readXmlFromFile("DotCoverConfig_withExcludeFilter.xml");

        Document xmlDocument = dotCoverConfigurationBuilder.buildXmlDocument(xmlOutputPath);
        String testXml = createPrettyPrintedXml(xmlDocument);

        Diff diff = getXmlStructureDiff(testXml, controlXml);

        assertNoDifferences(diff);
    }

    @Test
    @ConfiguredWithCode("jenkins_no_global_excludes.yml")
    public void testWithGlobalExcludeUnsetThenProducesCorrectDocumentStructure() throws IOException, InterruptedException, SAXException, URISyntaxException {
        String controlXml = readXmlFromFile("DotCoverConfig_withNoExcludeFilter.xml");

        Document xmlDocument = dotCoverConfigurationBuilder.buildXmlDocument(xmlOutputPath);
        String testXml = createPrettyPrintedXml(xmlDocument);

        Diff diff = getXmlStructureDiff(testXml, controlXml);

        assertNoDifferences(diff);
    }

    @Test
    @ConfiguredWithCode("jenkins_no_global_excludes.yml")
    public void testWithIncludeSetThenProducesCorrectDocumentStructure() throws IOException, URISyntaxException, InterruptedException {
        String controlXml = readXmlFromFile("DotCoverConfig_withIncludeFilters.xml");

        dotCoverStep.setCoverageAssemblyInclude("*");

        Document xmlDocument = dotCoverConfigurationBuilder.buildXmlDocument(xmlOutputPath);
        String testXml = createPrettyPrintedXml(xmlDocument);


        Diff diff = getXmlStructureDiff(testXml, controlXml);

        assertNoDifferences(diff);
    }


    @ConfiguredWithCode("jenkins_no_global_excludes.yml")
    @Test
    public void testWithFilterProcessExcludeSetThenProducesCorrectDocumentStructure() throws IOException, InterruptedException, URISyntaxException {
        String controlXml = readXmlFromFile("DotCoverConfig_withExcludeProcessFilter.xml");

        dotCoverStep.setProcessExclude("*.NET");

        Document xmlDocument = dotCoverConfigurationBuilder.buildXmlDocument(xmlOutputPath);
        String testXml = createPrettyPrintedXml(xmlDocument);

        Diff diff = getXmlStructureDiff(testXml, controlXml);

        assertNoDifferences(diff);
    }

    @ConfiguredWithCode("jenkins_global_excludes.yml")
    @Test
    public void testWithFilterProcessIncludeSetThenProducesCorrectDocumentStructure() throws IOException, InterruptedException, URISyntaxException {
        String controlXml = readXmlFromFile("DotCoverConfig_withIncludeProcessFilter.xml");

        dotCoverStep.setProcessInclude("*.NET");

        Document xmlDocument = dotCoverConfigurationBuilder.buildXmlDocument(xmlOutputPath);
        String testXml = createPrettyPrintedXml(xmlDocument);

        Diff diff = getXmlStructureDiff(testXml, controlXml);

        assertNoDifferences(diff);
    }


    @Before
    public void createMocks() throws IOException, InterruptedException {
        this.dotCoverStep = new DotCoverStep();
        this.stepContext = mock(StepContext.class);

        when(this.stepContext.get(TaskListener.class)).thenReturn(new TaskListener() {
            @Nonnull
            @Override
            public PrintStream getLogger() {
                return System.out;
            }
        });
        Launcher launcher = mock(Launcher.class);
        FilePath workspace = new FilePath(new File("./"));
        when(launcher.isUnix()).thenReturn(false);
        when(this.stepContext.get(FilePath.class)).thenReturn(workspace);
        when(this.stepContext.get(Launcher.class)).thenReturn(launcher);

        this.dotCoverStepExecution = new DotCoverStepExecution(this.stepContext, this.dotCoverStep);
        this.dotCoverConfigurationBuilder = new DotCoverConfigurationBuilder(this.dotCoverStepExecution);
    }

    private void assertNoDifferences(Diff diff) {
        for (Difference difference : diff.getDifferences()) {
            System.out.println(difference);
        }
        assertThat(diff.hasDifferences(), is(false));
    }

    private Diff getXmlStructureDiff(String testXml, String controlXml) {
        return DiffBuilder.compare(controlXml)
                .withTest(testXml)
                .withDifferenceEvaluator(new IgnoreTextDifferenceEvaluator(ComparisonResult.EQUAL))
                .ignoreComments()
                .ignoreWhitespace()
                .checkForIdentical()
                .build();
    }

    private String readXmlFromFile(String resourceName) throws URISyntaxException, IOException {
        URI pathToControlXmlFile = getClass().getResource(resourceName).toURI();
        return new String(Files.readAllBytes(Paths.get(pathToControlXmlFile)));
    }

    private String createPrettyPrintedXml(Document document) throws IOException {
        final OutputFormat format = OutputFormat.createPrettyPrint();
        format.setIndentSize(4);
        final OutputStream outputStream = new ByteArrayOutputStream();
        final XMLWriter xmlWriter = new XMLWriter(outputStream, format);
        xmlWriter.write(document);
        return outputStream.toString();
    }
}
