package io.jenkins.plugins.testing;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class DotCoverWrapper {

    private final static String DOTCOVER_EXE_NAME = "dotcover.exe";
    private final Path dotCoverPath;
    private final File dotCoverInstallDir;

    public DotCoverWrapper(String installDir) {
        dotCoverInstallDir = new File(installDir);
        dotCoverPath = Paths.get(dotCoverInstallDir.getAbsolutePath(), DOTCOVER_EXE_NAME);
    }

    public void execute() throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(dotCoverInstallDir);
        builder.command(dotCoverPath.toString());
        Process p = builder.start();
        Executors.newSingleThreadExecutor().execute(new StreamGobbler(p.getInputStream(), System.out::println));
        int exitCode = p.waitFor();
        System.out.println(exitCode);
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }

}
