// Copyright © 2011, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.jumi.core.commands;

import net.orfjackal.jumi.core.*;

public class AddSuiteListener implements Command {
    private final SuiteListener listener;

    public AddSuiteListener(SuiteListener listener) {
        this.listener = listener;
    }

    public void fireOn(CommandListener target) {
        target.addSuiteListener(listener);
    }
}