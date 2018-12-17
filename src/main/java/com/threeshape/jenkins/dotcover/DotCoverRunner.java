package com.threeshape.jenkins.dotcover;

import hudson.Plugin;

import java.util.logging.Logger;

public class DotCoverRunner extends Plugin {

    public DotCoverRunner()
    {

    }

    private static final Logger log = Logger.getLogger(DotCoverRunner.class.getName());

    public void start()
    {
        log.info("Starting DotCover plugin");
    }
}
