package io.jenkins.plugins.testing;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;

@Extension @Symbol("dotcoverExcludes")
public class DotCoverConfiguration extends GlobalConfiguration implements Serializable {

    private static final long serialVersionUID = 6197163805395054799L;
    private String mandatoryExcludedAssemblies;

    @DataBoundConstructor
    public DotCoverConfiguration()
    {
        super();
        load();
    }

    public static DotCoverConfiguration getInstance()
    {
        return GlobalConfiguration.all().get(DotCoverConfiguration.class);
    }

    @SuppressWarnings("unused")
    public String getMandatoryExcludedAssemblies()
    {
        return mandatoryExcludedAssemblies;
    }

    @DataBoundSetter @SuppressWarnings("unused")
    public void setMandatoryExcludedAssemblies(String mandatoryExcludedAssemblies)
    {
        if (this.mandatoryExcludedAssemblies != mandatoryExcludedAssemblies) {
            this.mandatoryExcludedAssemblies = mandatoryExcludedAssemblies;
            save();
        }
    }



}
