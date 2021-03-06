/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.plugin.launch;

import java.util.Date;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;

import com.centurylink.mdw.designer.testing.TestCase;
import com.centurylink.mdw.plugin.PluginMessages;

/**
 * This is for standalone (non-MDW) tests. For MDW test cases, see
 * GherkinTestCaseLaunch. TODO: Currently this does not add any value.
 */
public class CucumberLaunchListener implements IDebugEventSetListener {
    private ILaunchConfiguration launchConfig;
    private boolean running;

    private Date start;

    public Date getStart() {
        return start;
    }

    private Date end;

    public Date getEnd() {
        return end;
    }

    private String status;

    public String getStatus() {
        return status;
    }

    public CucumberLaunchListener(ILaunchConfiguration launchConfig) {
        this.launchConfig = launchConfig;
    }

    public void handleDebugEvents(DebugEvent[] events) {
        for (DebugEvent event : events) {
            if (event.getSource() instanceof IProcess) {
                IProcess process = (IProcess) event.getSource();
                if (event.getKind() == DebugEvent.CREATE) {
                    process.getStreamsProxy().getOutputStreamMonitor()
                            .addListener(new IStreamListener() {
                                public void streamAppended(String text, IStreamMonitor monitor) {
                                    System.out.print(text);
                                    if (!running) {
                                        running = true;
                                        start = new Date();
                                    }
                                }
                            });
                    process.getStreamsProxy().getErrorStreamMonitor()
                            .addListener(new IStreamListener() {
                                public void streamAppended(String text, IStreamMonitor monitor) {
                                    System.out.print(text);
                                }
                            });
                }
                else if (event.getKind() == DebugEvent.TERMINATE
                        && process.getLaunch().getLaunchConfiguration().equals(launchConfig)
                        && process.isTerminated() && true) {
                    end = new Date();
                    try {
                        int exitCode = process.getExitValue();
                        if (exitCode == 0) {
                            status = TestCase.STATUS_PASS;
                        }
                        else {
                            status = TestCase.STATUS_FAIL;
                        }
                    }
                    catch (DebugException ex) {
                        PluginMessages.log(ex);
                        status = TestCase.STATUS_ERROR;
                    }
                }
            }
        }
    }
}
