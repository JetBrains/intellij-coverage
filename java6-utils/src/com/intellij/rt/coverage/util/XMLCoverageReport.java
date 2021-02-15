/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.data.BranchData;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class XMLCoverageReport {
  private static final String NEW_LINE = System.getProperty("line.separator");
  private final Map<String, List<LineData>> myFiles = new HashMap<String, List<LineData>>();
  private XMLStreamWriter myOut;

  public void write(FileOutputStream fOut, ProjectData project) throws IOException {
    XMLOutputFactory factory = XMLOutputFactory.newInstance();
    try {
      myOut = factory.createXMLStreamWriter(new BufferedOutputStream(fOut));
      myFiles.clear();
      writeProject(project);
    } catch (XMLStreamException e) {
      throw wrapIOException(e);
    } finally {
      try {
        if (myOut != null) {
          myOut.flush();
          myOut.close();
          myOut = null;
        } else {
          fOut.close();
        }
      } catch (XMLStreamException e) {
        ErrorReporter.reportError("Error closing file.", e);
      }
    }
  }

  private void newLine() throws XMLStreamException {
    myOut.writeCharacters(NEW_LINE);
  }

  private void writeProject(ProjectData project) throws XMLStreamException {
    myOut.writeStartDocument();
    newLine();
    myOut.writeStartElement("report");
    myOut.writeAttribute("name", "Intellij Coverage Report");
    newLine();

    HashMap<String, List<ClassData>> packages = mapClassesToPackages(project);

    for (Map.Entry<String, List<ClassData>> packageEntry : packages.entrySet()) {
      String packageName = packageEntry.getKey();
      List<ClassData> classes = packageEntry.getValue();
      writePackage(packageName, classes);
    }

    myOut.writeEndElement();
    newLine();
    myOut.writeEndDocument();
    newLine();
  }

  private void writePackage(String packageName, List<ClassData> classes) throws XMLStreamException {
    myOut.writeStartElement("package");
    myOut.writeAttribute("name", packageName);
    newLine();
    myFiles.clear();
    for (ClassData classData : classes) {
      writeClass(classData);
    }
    for (Map.Entry<String, List<LineData>> fileEntry : myFiles.entrySet()) {
      String fileName = fileEntry.getKey();
      List<LineData> lines = fileEntry.getValue();
      writeFile(fileName, lines);
    }
    myOut.writeEndElement();
    newLine();
  }

  private void writeFile(String fileName, List<LineData> lines) throws XMLStreamException {
    myOut.writeStartElement("sourcefile");
    myOut.writeAttribute("name", fileName);
    newLine();
    for (LineData lineData : lines) {
      if (lineData == null) continue;
      writeLine(lineData);
    }
    myOut.writeEndElement();
    newLine();
  }

  private void writeClass(ClassData classData) throws XMLStreamException {
    myOut.writeStartElement("class");
    myOut.writeAttribute("name", classData.getName());
    String sourceName = classData.getSource();
    if (sourceName != null && sourceName.length() > 0) {
      myOut.writeAttribute("sourcefilename", classData.getSource());
      newLine();
      List<LineData> lines = myFiles.get(sourceName);
      List<LineData> newLines = new ArrayList<LineData>();
      LineData[] linesArray = (LineData[]) classData.getLines();
      if (linesArray != null) {
        newLines.addAll(Arrays.asList(linesArray));
      }
      if (lines == null) {
        myFiles.put(sourceName, newLines);
      } else {
        lines.addAll(newLines);
      }
    } else {
      newLine();
    }
    Map<String, List<LineData>> methods = classData.mapLinesToMethods();
    for (Map.Entry<String, List<LineData>> methodEntry : methods.entrySet()) {
      writeMethod(methodEntry.getKey(), methodEntry.getValue());
    }
    myOut.writeEndElement();
    newLine();
  }

  private void writeMethod(String signature, List<LineData> lines) throws XMLStreamException {
    myOut.writeStartElement("method");
    int nameIndex = signature.indexOf('(');
    String name = signature.substring(0, nameIndex);
    String descriptor = signature.substring(nameIndex);
    myOut.writeAttribute("name", name);
    myOut.writeAttribute("desc", descriptor);
    newLine();

    int totalLines = 0;
    int coveredLines = 0;
    int totalBranches = 0;
    int coveredBranches = 0;
    for (LineData lineData : lines) {
      if (lineData == null) continue;
      totalLines++;
      coveredLines += lineData.getHits() > 0 ? 1 : 0;
      BranchData branchData = lineData.getBranchData();
      if (branchData == null) continue;
      totalBranches += branchData.getTotalBranches();
      coveredBranches += branchData.getCoveredBranches();
    }

    writeCounter("LINE", totalLines, coveredLines);
    writeCounter("BRANCH", totalBranches, coveredBranches);

    myOut.writeEndElement();
    newLine();
  }

  private void writeLine(LineData lineData) throws XMLStreamException {
    myOut.writeEmptyElement("line");
    myOut.writeAttribute("nr", Integer.toString(lineData.getLineNumber()));

    BranchData branchData = lineData.getBranchData();
    int totalBranches = branchData == null ? 0 : branchData.getTotalBranches();
    int coveredBranches = branchData == null ? 0 : branchData.getCoveredBranches();
    myOut.writeAttribute("cb", Integer.toString(coveredBranches));
    myOut.writeAttribute("mb", Integer.toString(totalBranches - coveredBranches));
    int lineCovered = lineData.getHits() > 0 ? 1 : 0;
    myOut.writeAttribute("ci", Integer.toString(lineCovered));
    myOut.writeAttribute("mi", Integer.toString(1 - lineCovered));
    newLine();
  }

  private void writeCounter(String type, int total, int covered) throws XMLStreamException {
    myOut.writeEmptyElement("counter");
    myOut.writeAttribute("type", type);
    myOut.writeAttribute("covered", Integer.toString(covered));
    myOut.writeAttribute("missed", Integer.toString(total - covered));
    newLine();
  }

  private static HashMap<String, List<ClassData>> mapClassesToPackages(ProjectData project) {
    HashMap<String, List<ClassData>> packages = new HashMap<String, List<ClassData>>();
    for (ClassData classData : project.getClasses().values()) {
      String className = classData.getName();
      int indexOfName = className.lastIndexOf('.');
      String packageName = indexOfName < 0 ? className : className.substring(0, indexOfName);
      List<ClassData> packageClasses = packages.get(packageName);
      if (!packages.containsKey(packageName)) {
        packageClasses = new ArrayList<ClassData>();
        packages.put(packageName, packageClasses);
      }
      packageClasses.add(classData);
    }
    return packages;
  }

  private IOException wrapIOException(Throwable t) {
    IOException e = new IOException(t.getClass().getSimpleName() + ": " + t.getMessage());
    e.setStackTrace(t.getStackTrace());
    return e;
  }
}
