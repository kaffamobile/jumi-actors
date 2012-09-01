// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.config;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.util.*;

@NotThreadSafe
public class SuiteConfigurationBuilder {

    private final List<File> classPath;
    private final List<String> jvmOptions;
    private String includedTestsPattern;

    public SuiteConfigurationBuilder() {
        this(new SuiteConfiguration());
    }

    SuiteConfigurationBuilder(SuiteConfiguration src) {
        classPath = new ArrayList<File>(src.classPath());
        jvmOptions = new ArrayList<String>(src.jvmOptions());
        includedTestsPattern = src.includedTestsPattern();
    }

    public SuiteConfiguration freeze() {
        return new SuiteConfiguration(this);
    }


    // getters and setters

    public List<File> classPath() {
        return classPath;
    }

    public SuiteConfigurationBuilder addToClassPath(File file) {
        classPath.add(file);
        return this;
    }

    public String includedTestsPattern() {
        return includedTestsPattern;
    }

    public SuiteConfigurationBuilder includedTestsPattern(String includedTestsPattern) {
        this.includedTestsPattern = includedTestsPattern;
        return this;
    }

    public List<String> jvmOptions() {
        return jvmOptions;
    }

    public SuiteConfigurationBuilder addJvmOptions(String... jvmOptions) {
        this.jvmOptions.addAll(Arrays.asList(jvmOptions));
        return this;
    }
}