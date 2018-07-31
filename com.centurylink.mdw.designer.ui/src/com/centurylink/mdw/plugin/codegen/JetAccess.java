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
package com.centurylink.mdw.plugin.codegen;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.emf.codegen.CodeGen;
import org.eclipse.emf.codegen.jet.JETEmitter;
import org.eclipse.emf.codegen.jet.JETException;
import org.eclipse.emf.codegen.jmerge.JControlModel;
import org.eclipse.emf.codegen.jmerge.JMerger;

/**
 * Encapsulates access to the JET and JMerge packages.
 */
@SuppressWarnings("deprecation")
public class JetAccess {

    private JetConfig config = null;

    public JetConfig getConfig() {
        return config;
    }

    public JetAccess(JetConfig config) {
        this.config = config;
    }

    /**
     * Invokes the JET template specified in the Config. Uses a JETEmitter to
     * translate the template to an implementation class. The translated class
     * is created the hidden .JETEmitters project.
     * 
     * @param monitor
     *            the progress monitor to use (may be null) - 3 units are used
     * @return the source code text generated by the JET template
     */
    public String generate(IProgressMonitor monitor) throws CoreException, JETException {
        monitor = createIfNull(monitor);

        JetConfig config = getConfig();
        JETEmitter emitter = new JETEmitter(config.getTemplateFullUri(),
                getClass().getClassLoader());
        emitter.addVariable(config.getClasspathVariable(), config.getPluginId());

        IProgressMonitor sub = new SubProgressMonitor(monitor, 2);
        Object[] params = null;
        if (config.getSettings() != null) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("model", config.getModel());
            map.put("settings", config.getSettings());
            params = new Object[] { map };
        }
        else {
            params = new Object[] { config.getModel() };
        }

        String result = emitter.generate(sub, params);
        monitor.worked(1);
        return result;
    }

    /**
     * Merges the specified emitterResult with the contents of an existing file
     * (existing file is not modified). The location of the file to merge with
     * is determined by finding or creating the container (folder) for the
     * Config's package in the Config's target folder. The name of the file to
     * merge with is the Config's target file.
     * 
     * @param monitor
     *            the progress monitor to use (may be null)
     * @param emitterResult
     *            generated content to merge with the existing content
     * @return result of merging the generated contents with the existing file
     * @throws CoreException
     *             if an error occurs accessing the contents of the file
     */
    public String merge(IProgressMonitor monitor, String emitterResult)
            throws CoreException, JETException {
        monitor = createIfNull(monitor);

        JetConfig config = getConfig();
        IContainer container = findOrCreateContainer(monitor, config.getTargetFolder(),
                config.getPackageName());
        if (container == null) {
            throw new JETException("Cound not find or create container for package "
                    + config.getPackageName() + " in " + config.getTargetFolder());
        }

        IFile targetFile = container.getFile(new Path(config.getTargetFile()));
        if (!targetFile.exists()) {
            monitor.worked(1);
            return emitterResult;
        }

        JMerger jMerger = new JMerger();
        jMerger.setControlModel(new JControlModel(config.getMergeXmlFullUri()));
        jMerger.setSourceCompilationUnit(jMerger.createCompilationUnitForContents(emitterResult));

        jMerger.setTargetCompilationUnit(
                jMerger.createCompilationUnitForInputStream(targetFile.getContents(true)));
        String oldContents = jMerger.getTargetCompilationUnit().getContents();

        jMerger.merge();
        monitor.worked(1);

        String result = jMerger.getTargetCompilationUnit().getContents();
        if (oldContents.equals(result)) {
            return result;
        }

        if (!targetFile.isReadOnly()) {
            return result;
        }

        // file may be read-only because it is checked out
        // by a VCM component (here we ask permission to change the file)
        if (targetFile.getWorkspace()
                .validateEdit(new IFile[] { targetFile }, new SubProgressMonitor(monitor, 1))
                .isOK()) {

            jMerger.setTargetCompilationUnit(
                    jMerger.createCompilationUnitForInputStream(targetFile.getContents(true)));
            jMerger.remerge();
            return jMerger.getTargetCompilationUnit().getContents();
        }
        return result;
    }

    /**
     * Saves bytes to a location specified by the Config settings. The location
     * of the file to save is determined by finding or creating the container
     * (folder) for the Config's package in the Config's target folder. The name
     * of the file to save is the Config's target file.
     * 
     * @param monitor
     *            the progress monitor to use (may be null) - 2 units are used
     * @param contents
     *            the byte contents of the file to save
     * @throws CoreException
     */
    public IFile save(IProgressMonitor monitor, byte[] contents)
            throws CoreException, JETException {
        monitor = createIfNull(monitor);

        JetConfig config = getConfig();
        IContainer container = findOrCreateContainer(monitor, config.getTargetFolder(),
                config.getPackageName());
        if (container == null) {
            throw new JETException("Cound not find or create container for package "
                    + config.getPackageName() + " in " + config.getTargetFolder());
        }
        IFile targetFile = container.getFile(new Path(config.getTargetFile()));
        IFile result = getWritableTargetFile(targetFile, container, config.getTargetFile());

        InputStream newContents = new ByteArrayInputStream(contents);
        if (result.exists()) {
            result.setContents(newContents, true, true, new SubProgressMonitor(monitor, 1));
        }
        else {
            result.create(newContents, true, new SubProgressMonitor(monitor, 1));
        }
        return result;
    }

    /**
     * Returns a non-null progress monitor.
     * 
     * @param monitor
     *            an existing progress monitor
     * @return a new NullProgressMonitor if the specified monitor is null, or
     *         the existing monitor otherwise
     */
    private IProgressMonitor createIfNull(IProgressMonitor monitor) {
        if (monitor == null) {
            return new NullProgressMonitor();
        }
        return monitor;
    }

    public IContainer findOrCreateContainer(IProgressMonitor progressMonitor,
            String targetDirectory, String packageName) throws CoreException {

        if (packageName == null)
            packageName = "";

        IPath outputPath = new Path(targetDirectory + "/" + packageName.replace('.', '/'));

        IProgressMonitor sub = new SubProgressMonitor(progressMonitor, 2);
        IPath localLocation = null; // use default
        IContainer container = CodeGen.findOrCreateContainer(outputPath, true, localLocation, sub);

        return container;
    }

    /**
     * Returns an IFile that can be written to. If the specified file is
     * read-write, it is returned unchanged. If the specified file is read-only
     * and {@link Config#isForceOverwrite()}returns true, the file is made
     * writable, otherwise a new file is returned in the specified container
     * with filename "." + fileName + ".new".
     * 
     * @param container
     *            container to create the new file in if the specified file
     *            can't be made writable
     * @param targetFile
     *            the file to make writable
     * @param fileName
     *            used to create a new file name if the specified file can't be
     *            made writable
     * @return a IFile that can be written to
     */
    private IFile getWritableTargetFile(IFile targetFile, IContainer container, String fileName) {
        if (targetFile.isReadOnly()) {
            if (getConfig().isForceOverwrite()) {
                targetFile.setReadOnly(false);
            }
            else {
                targetFile = container.getFile(new Path("." + fileName + ".new"));
            }
        }
        return targetFile;
    }

    /**
     * Appends jet-generated content to the end of the JetAccess save file.
     * 
     * @param monitor
     * @param contents
     * @return
     * @throws IOException
     * @throws CoreException
     */
    public IFile append(IProgressMonitor monitor, byte[] contents)
            throws IOException, CoreException, JETException {
        monitor = createIfNull(monitor);

        JetConfig config = getConfig();
        IContainer container = findOrCreateContainer(monitor, config.getTargetFolder(),
                config.getPackageName());
        if (container == null) {
            throw new JETException("Cound not find or create container for package "
                    + config.getPackageName() + " in " + config.getTargetFolder());
        }
        IFile targetFile = container.getFile(new Path(config.getTargetFile()));
        IFile result = getWritableTargetFile(targetFile, container, config.getTargetFile());

        InputStream newContents = new ByteArrayInputStream(contents);
        if (result.exists()) {
            newContents = appendFile(result, new String(contents));
            result.setContents(newContents, true, true, new SubProgressMonitor(monitor, 2));
        }
        else {
            result.create(newContents, true, new SubProgressMonitor(monitor, 1));
        }
        return result;
    }

    /**
     * Merges jet-generated XML into the JetAccess save file based on the
     * specified outer containing element. The generated output is inserted
     * before every occurrence of the closing tag of outerElement.
     * 
     * @param monitor
     *            - 3 units are used
     * @param contents
     * @param outerElement
     * @return
     * @throws IOException
     * @throws CoreException
     */
    public IFile mergeXml(IProgressMonitor monitor, byte[] contents, String outerElement)
            throws IOException, CoreException, JETException {
        monitor = createIfNull(monitor);

        JetConfig config = getConfig();
        IContainer container = findOrCreateContainer(monitor, config.getTargetFolder(),
                config.getPackageName());
        if (container == null) {
            throw new JETException("Cound not find or create container for package "
                    + config.getPackageName() + " in " + config.getTargetFolder());
        }
        IFile targetFile = container.getFile(new Path(config.getTargetFile()));
        IFile result = getWritableTargetFile(targetFile, container, config.getTargetFile());

        InputStream newContents = new ByteArrayInputStream(contents);
        if (result.exists()) {
            newContents = addNewXml(result, new String(contents), outerElement);
            result.setContents(newContents, true, true, new SubProgressMonitor(monitor, 2));
        }
        else {
            result.create(newContents, true, new SubProgressMonitor(monitor, 1));
        }
        return result;
    }

    private InputStream addNewXml(IFile file, String newContents, String outerElement)
            throws IOException, CoreException {
        String mergedContents = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents(true)));
        String inLine = "";
        while ((inLine = reader.readLine()) != null) {
            if (inLine.indexOf("</" + outerElement + ">") >= 0)
                inLine = newContents + "\n" + inLine;

            mergedContents += inLine + "\n";
        }

        reader.close();

        return new ByteArrayInputStream(mergedContents.getBytes());
    }

    private InputStream appendFile(IFile file, String newContents)
            throws IOException, CoreException {
        String mergedContents = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents(true)));
        String inLine = "";
        while ((inLine = reader.readLine()) != null)
            mergedContents += inLine + "\n";
        mergedContents += newContents;

        reader.close();

        return new ByteArrayInputStream(mergedContents.getBytes());
    }
}