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
package com.centurylink.mdw.plugin.actions;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.project.LocalCloudProjectWizard;
import com.centurylink.mdw.plugin.project.RemoteWorkflowProjectWizard;
import com.centurylink.mdw.plugin.workspace.WorkspaceConfig;
import com.centurylink.mdw.plugin.workspace.WorkspaceConfigWizard;

public class ProjectAction extends BasePulldownAction {
    public static final String MENU_SEL_NEW_LOCAL_PROJECT = "New Local Project";
    public static final String MENU_SEL_NEW_REMOTE_PROJECT = "New Remote Project";
    public static final String MENU_SEL_NEW_CLOUD_PROJECT = "New Cloud Project";
    public static final String MENU_SEL_CONFIGURE_WORKSPACE = "Configure Workspace";

    /**
     * populates the plugin action menu (the mdw icon) with its items
     */
    public void populateMenu(Menu menu) {
        // new local project
        MenuItem item = new MenuItem(menu, SWT.NONE);
        item.setText(MENU_SEL_NEW_CLOUD_PROJECT + "...");
        item.setImage(MdwPlugin.getImageDescriptor("icons/cloud_project.gif").createImage());
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newCloudProject();
            }
        });

        // new remote project
        item = new MenuItem(menu, SWT.NONE);
        item.setText(MENU_SEL_NEW_REMOTE_PROJECT + "...");
        item.setImage(MdwPlugin.getImageDescriptor("icons/remote_project.gif").createImage());
        item.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                accessRemoteWorkflowProject();
            }
        });

        // configure workspace
        if (checkUrl(MdwPlugin.getSettings().getWorkspaceSetupUrl())) {
            // separator
            item = new MenuItem(menu, SWT.SEPARATOR);

            item = new MenuItem(menu, SWT.NONE);
            item.setText(MENU_SEL_CONFIGURE_WORKSPACE + "...");
            item.setImage(MdwPlugin.getImageDescriptor("icons/config.gif").createImage());
            item.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    configureWorkspace();
                }
            });
        }
    }

    public void newCloudProject() {
        LocalCloudProjectWizard cloudProjectWizard = new LocalCloudProjectWizard();
        cloudProjectWizard.init(PlatformUI.getWorkbench(), null);
        new WizardDialog(getActiveWindow().getShell(), cloudProjectWizard).open();
    }

    public void accessRemoteWorkflowProject() {
        RemoteWorkflowProjectWizard remoteWorkflowProjectWizard = new RemoteWorkflowProjectWizard();
        new WizardDialog(getActiveWindow().getShell(), remoteWorkflowProjectWizard).open();
    }

    public void configureWorkspace() {
        Shell shell = getActiveWindow().getShell();

        WorkspaceConfig model = new WorkspaceConfig(MdwPlugin.getSettings());
        WorkspaceConfigWizard workspaceConfigWizard = new WorkspaceConfigWizard(model);
        workspaceConfigWizard.setNeedsProgressMonitor(true);
        WizardDialog dialog = new WizardDialog(shell, workspaceConfigWizard);
        dialog.create();
        dialog.open();
    }

    private boolean checkUrl(String urlString) {
        InputStream is = null;

        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.connect();
            is = connection.getInputStream();
        }
        catch (MalformedURLException ex) {
            return false;
        }
        catch (IOException ex) {
            return false;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException ex) {
                }
            }
        }
        return true;
    }
}