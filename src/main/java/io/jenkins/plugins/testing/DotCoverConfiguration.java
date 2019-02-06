package io.jenkins.plugins.testing;

import hudson.Extension;
import java.io.Serializable;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

@Extension
public class DotCoverConfiguration extends GlobalConfiguration implements Serializable {

    private static final long serialVersionUID = 6197163805395054799L;
    private String mandatoryExcludedAssemblies;

    @DataBoundConstructor
    public DotCoverConfiguration() {
        super();
        load();
    }

    public static DotCoverConfiguration getInstance() {
        return GlobalConfiguration.all().get(DotCoverConfiguration.class);
    }

    public String getMandatoryExcludedAssemblies() {
        return mandatoryExcludedAssemblies;
    }

    @DataBoundSetter
    public void setMandatoryExcludedAssemblies(String mandatoryExcludedAssemblies) {
        this.mandatoryExcludedAssemblies = mandatoryExcludedAssemblies;
        save();
    }

}
