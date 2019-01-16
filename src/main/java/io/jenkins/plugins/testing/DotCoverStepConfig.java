package io.jenkins.plugins.testing;

public class DotCoverStepConfig {

    private String dotcoverSnapshotPath;

    public DotCoverStepConfig()
    {

    }

    public void setDotcoverSnapshotPath(String dotcoverSnapshotPath)

    { this.dotcoverSnapshotPath = dotcoverSnapshotPath; }

    public String getDotcoverSnapshotPath()
    {
        return dotcoverSnapshotPath;
    }


}
