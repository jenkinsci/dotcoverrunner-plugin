package io.jenkins.plugins.testing;

import com.google.common.base.Strings;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
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
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class DotCoverStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(DotCoverStep.class.getName());

    private static final String APP_NAME = "dotcover";
    private static final String dotCoverMandatoryExcludeFromCoverage = "*.Core;*.DongleSystem;*.FocusScanShared;*Test*;FluentAssertions;SlimDX;MathNet.Numerics";


    private String testPlatform;
    private String testCaseFilter;
    private String pathToDotCover;

    @DataBoundConstructor
    public DotCoverStep(String testCaseFilter) {
        this.testCaseFilter = testCaseFilter;
    }

    public String getTestCaseFilter() {
        return testCaseFilter;
    }

    public String getTestPlatform() {
        return testPlatform;
    }

    @DataBoundSetter
    public void setTestPlatform(String testPlatform) {
        this.testPlatform = testPlatform;
    }

    public String getPathToDotCover() {
        return pathToDotCover;
    }

    @DataBoundSetter
    public void setPathToDotCover(String pathToDotCover) {
        this.pathToDotCover = pathToDotCover;
    }

    @Override
    public StepExecution start(StepContext stepContext) {
        return new Execution(stepContext, testPlatform, pathToDotCover);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return APP_NAME;
        }

        @Override
        public String getDisplayName() {
            return APP_NAME;
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }
    }

    public static class Execution extends SynchronousNonBlockingStepExecution {
        private StepContext context;
        private String testPlatform;
        private String dotCoverPath;
        private TaskListener listener;

        public Execution(StepContext context, String testPlatform, String dotCoverPath) {
            super(context);
            this.context = context;
            this.dotCoverPath = dotCoverPath;
            this.testPlatform = testPlatform;
        }

        /**
         * Map given workspace to an agent node, or the jenkins instance
         *
         * @param workspace
         * @return
         */
        private static Node workspaceToNode(@Nonnull FilePath workspace) {
            Computer computer = workspace.toComputer();
            Node node = null;
            if (computer != null) node = computer.getNode();
            return (node != null) ? node : Jenkins.get();
        }

        private String getPathToDotCover()
        {
            String path = null;

            return null;
        }

        private List<String> buildArgs()
        {


            List<String> args = new ArrayList<>();
            args.add(getPathToDotCover());
            return args;
        }


        private void generateDotCoverConfig(@Nonnull FilePath tmpDirectory, final String vsTestArgs, final String solutionDir, final String coverageInclude, final String coverageClassInclude, final String coverageFunctionInclude, final String coverageAssemblyExclude, final String processInclude, final String processExclude) throws ParserConfigurationException, IOException, InterruptedException, TransformerException {


            final TaskListener listener = context.get(TaskListener.class);
            final FilePath workspace = context.get(FilePath.class);
            final Node node = workspaceToNode(workspace);
            final VsTestInstallation vsTest = VsTestInstallation.getDefaultInstallation().forNode(node, listener);
            final FilePath dotCoverConfigPath = tmpDirectory.child("DotCoverConfig.xml");
            String home = vsTest.getHome();

            Document document = DocumentHelper.createDocument();

            Element analyseParams = document.addElement("AnalyseParams");

            Element targetExecutable = analyseParams.addElement("TargetExecutable");
            targetExecutable.addText(new File(home).getAbsolutePath());

            Element targetArguments = analyseParams.addElement("TargetArguments");
            targetArguments.addText(vsTestArgs);

            Element targetWorkingDir = analyseParams.addElement("TargetWorkingDir");
            String workingDir = workspace.child(solutionDir).absolutize().toURI().toString();
            targetWorkingDir.addText(workingDir);

            Element tempDir = analyseParams.addElement("TempDir");
            tempDir.addText(tmpDirectory.absolutize().toURI().toString());

            Element output = analyseParams.addElement("Output");
            output.addText(tmpDirectory.child("Snapshot.cov").toURI().toString());

            Element filters = analyseParams.addElement("Filters");

            Element includeFilters = filters.addElement("IncludeFilters");

            Element excludeFilters = filters.addElement("ExcludeFilters");

            Element processFilters = filters.addElement("ProcessFilters");

            processFilter(processFilters.addElement("IncludeFilters"), processInclude); // todo
            processFilter(processFilters.addElement("ExcludeFilters"), processExclude); // todo

            if (isSet(coverageInclude))
            {
                for (String assemblyName : coverageInclude.split(";"))
                {
                    if (isSet(assemblyName))
                    {
                        Element filterEntry = includeFilters.addElement("FilterEntry");
                        filterEntry.addElement( "ModuleMask").addText(assemblyName);
                        filterEntry.addElement( "ClassMask").addText("*");
                        filterEntry.addElement( "FunctionMask").addText("*");
                    }
                }
            }

            if (isSet(coverageClassInclude))
            {
                for (String className : coverageClassInclude.split(";"))
                {
                    if (isSet(className))
                    {
                        Element filterEntry = includeFilters.addElement("FilterEntry");
                        filterEntry.addElement("ModuleMask").addText("*");
                        filterEntry.addElement("ClassMask").addText(className);
                        filterEntry.addElement("FunctionMask").addText("*");
                    }
                }
            }

            if (isSet(coverageFunctionInclude))
            {
                for (String method: coverageFunctionInclude.split(";"))
                {
                    if (isSet(method))
                    {
                        Element filterEntry = includeFilters.addElement("FilterEntry");
                        filterEntry.addElement("ModuleMask").addText("*");
                        filterEntry.addElement("ClassMask").addText("*");
                        filterEntry.addElement("FunctionMask").addText(method);
                    }
                }
            }

            if (isSet(coverageAssemblyExclude))
            {
                for (String assembly: coverageAssemblyExclude.split(";"))
                {
                    if (isSet(assembly))
                    {
                        Element filterEntry = excludeFilters.addElement("FilterEntry");
                        filterEntry.addElement("ModuleMask").addText(assembly);
                    }
                }
            }



            try (OutputStream out = dotCoverConfigPath.write()) {
                OutputFormat format = OutputFormat.createPrettyPrint();
                XMLWriter writer = new XMLWriter(out, format);
                writer.write( document );
                writer.close();
            }
        }

        private void processFilter(Element parentElement, String input)
        {
            if (!isSet(input)) return;

            for (String s: input.split(";"))
            {
                if (isSet(s)) {
                    parentElement.addElement("ProcessMask").addText(s);
                }
            }
        }


        @Override
        protected Object run() throws Exception {
            final TaskListener listener = getContext().get(TaskListener.class);
            PrintStream logger = listener.getLogger();
            final FilePath workspace = getContext().get(FilePath.class);
            final Node node = workspaceToNode(workspace);
            final DotCoverInstallation dotCover = DotCoverInstallation.getDefaultInstallation().forNode(node, listener);

            final Launcher launcher = workspace.createLauncher(listener);

            final FilePath tmpDirectory = workspace.createTempDir("dotcover", "tmp");
            generateDotCoverConfig(tmpDirectory, "vs test args", "Threeshape.Contracts", "assembly1;assembly2", "class1;class2", "somemethod", "assembly3", "sqlserver.exe;mssqlserver.exe", "test.exe;test2.exe");

            ArgumentListBuilder args = new ArgumentListBuilder();

            args.addQuoted(dotCover.getHome());
            args.add("cover");
            workspace.absolutize().toURI();
            Path p = Paths.get(workspace.absolutize().toURI());
            Path toConfig = p.resolve("DotCoverConfig.xml").toAbsolutePath();
            args.add(toConfig.toString());

            final Proc proc  = launcher.launch().cmds(args).stdout(logger).start();
            proc.join();

            return null;
        }
    }

    private static boolean isSet(final String s)
    {
        return !Strings.isNullOrEmpty(s);
    }

}
