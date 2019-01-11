package io.jenkins.plugins.testing;

public enum TestPlatform {

    X86("x86"),
    X64("X64");

    private final String name;

    TestPlatform(String name) {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }
}
