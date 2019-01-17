package io.jenkins.plugins.testing;

public final class DotCoverStepConfig {

    private String dotcoverSnapshotPath;
    private final String testPlatform;


    public DotCoverStepConfig(String testPlatform)
    {
        this.testPlatform = testPlatform;
    }

    public String getDotcoverSnapshotPath()
    {
        return dotcoverSnapshotPath;
    }


}
