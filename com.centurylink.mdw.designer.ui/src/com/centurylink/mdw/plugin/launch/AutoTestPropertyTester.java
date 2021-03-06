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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.ResourceWrapper;
import com.centurylink.mdw.plugin.designer.model.CucumberTest;
import com.centurylink.mdw.plugin.designer.model.WorkflowPackage;
import com.centurylink.mdw.plugin.project.WorkflowProjectManager;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;

public class AutoTestPropertyTester extends PropertyTester {
    // TODO: support multiple selections
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        ResourceWrapper resourceWrapper = new ResourceWrapper((IAdaptable) receiver);
        try {
            WorkflowProject workflowProject = resourceWrapper.getOwningWorkflowProject();
            if ("canLaunchAutoTest".equals(property)) { // currently only for
                                                        // debug
                IFile file = resourceWrapper.getFile();
                if (file != null
                        && ("test".equals(file.getFileExtension())
                                || "postman".equals(file.getFileExtension()))
                        && file.exists() && !file.isDerived() && workflowProject != null) {
                    WorkflowPackage pkg = workflowProject.getPackage((IFolder) file.getParent());
                    if (pkg != null)
                        return pkg.getAsset(file) != null;
                }

            }
            else if ("canLaunchAutomatedTests".equals(property)) {
                IFolder folder = resourceWrapper.getFolder();
                if (folder != null) {
                    workflowProject = WorkflowProjectManager.getInstance()
                            .getWorkflowProject(folder.getProject());
                    if (workflowProject != null && workflowProject.isInitialized()) {
                        WorkflowPackage pkg = workflowProject.getPackage(folder);
                        if (pkg != null)
                            return !pkg.getTestCases().isEmpty();
                    }
                }
                else {
                    IProject project = resourceWrapper.getProject();
                    if (project != null) {
                        WorkflowProject proj = WorkflowProjectManager.getInstance()
                                .getWorkflowProject(project);
                        if (proj != null && proj.isInitialized()) {
                            return !proj.getTestCases().isEmpty();
                        }
                    }
                }
            }
            else if ("canLaunchCucumberTest".equals(property)) {
                if (resourceWrapper.getOwningJavaProject() != null) {
                    IFile file = resourceWrapper.getFile();
                    if (file != null && "feature".equals(file.getFileExtension()) && file.exists()
                            && !file.isDerived()) {
                        // exclude mdw automated gherkin tests
                        if (workflowProject != null && workflowProject.isInitialized()) {
                            ResourceWrapper parentWrapper = new ResourceWrapper(
                                    (IAdaptable) file.getParent());
                            IFolder folder = parentWrapper.getFolder();
                            if (folder != null && workflowProject.getPackage(folder) != null)
                                return false;
                        }
                        return true;
                    }
                }
            }
            else if ("canLaunchCucumberTests".equals(property)
                    && resourceWrapper.getOwningJavaProject() != null) {
                IFolder folder = resourceWrapper.getFolder();
                if (folder != null) {
                    if (workflowProject != null && workflowProject.isInitialized()
                            && workflowProject.getPackage(folder) != null)
                        return false;
                    List<CucumberTest> tests = new ArrayList<>();
                    CucumberTest.findTests(folder, tests);
                    return !tests.isEmpty();
                }
                else {
                    return false;
                }
            }

        }
        catch (CoreException ex) {
            PluginMessages.log(ex);
        }

        return false;
    }
}
