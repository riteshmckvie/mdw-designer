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
package com.centurylink.mdw.plugin.designer.views;

import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;

import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.ActivityImpl;
import com.centurylink.mdw.plugin.designer.model.ExternalEvent;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

public class ProcessExplorerDragSource extends DragSourceAdapter {
    private TreeViewer treeViewer;

    public ProcessExplorerDragSource(TreeViewer treeViewer) {
        this.treeViewer = treeViewer;
    }

    @Override
    public void dragFinished(DragSourceEvent event) {
        if (!event.doit)
            return;

        // everything is done in the drop target
    }

    @Override
    public void dragSetData(DragSourceEvent event) {
        if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
            IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
            List<?> elements = selection.toList();
            StringBuilder data = new StringBuilder();
            for (int i = 0; i < elements.size(); i++) {
                Object element = elements.get(i);
                if (element instanceof WorkflowProcess || element instanceof ExternalEvent
                        || element instanceof ActivityImpl || element instanceof WorkflowAsset) {
                    data.append(element.toString());
                    if (i < elements.size() - 1)
                        data.append("#");
                }
            }
            event.data = data.toString();
        }
    }

    @Override
    public void dragStart(DragSourceEvent event) {
        boolean allowed = true;
        if (!treeViewer.getSelection().isEmpty()) {
            IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();
            List<?> elements = selection.toList();
            for (int i = 0; i < elements.size(); i++) {
                if (!isDragAllowed(elements.get(i))) {
                    allowed = false;
                    break;
                }
            }
        }
        event.doit = allowed;
    }

    private boolean isDragAllowed(Object selectedElement) {
        boolean dragArchived = MdwPlugin.getDefault().getPreferenceStore()
                .getBoolean(PreferenceConstants.PREFS_ALLOW_DELETE_ARCHIVED_PROCESSES);

        if (selectedElement instanceof WorkflowProcess) {
            WorkflowProcess processVersion = (WorkflowProcess) selectedElement;
            IEditorPart editor = findOpenEditor(processVersion);
            if (editor != null) {
                String message = "'" + processVersion.getLabel()
                        + "' is currently open in an editor.\nPlease save and close before dragging.";
                MessageDialog.openError(MdwPlugin.getShell(), "Process Explorer", message);
                return false;
            }
            return dragArchived || !processVersion.isArchived();
        }
        else if (selectedElement instanceof ExternalEvent) {
            ExternalEvent externalEvent = (ExternalEvent) selectedElement;
            return dragArchived || !externalEvent.isArchived();
        }
        else if (selectedElement instanceof ActivityImpl) {
            ActivityImpl activityImpl = (ActivityImpl) selectedElement;
            return dragArchived || !activityImpl.isArchived();
        }
        else if (selectedElement instanceof WorkflowAsset) {
            WorkflowAsset asset = (WorkflowAsset) selectedElement;
            IEditorPart tempFileEditor = asset.getFileEditor();
            if (tempFileEditor != null && findOpenEditor(tempFileEditor.getEditorInput()) != null) {
                String message = "'" + asset.getLabel()
                        + "' is currently open in an editor.\nPlease save and close before dragging.";
                MessageDialog.openError(MdwPlugin.getShell(), "Process Explorer", message);
                return false;
            }
            return dragArchived || !asset.isArchived();
        }

        return false;
    }

    public IEditorPart findOpenEditor(IEditorInput workflowElement) {

        return MdwPlugin.getActivePage().findEditor(workflowElement);
    }
}