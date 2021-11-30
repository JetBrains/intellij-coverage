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

package com.intellij.rt.coverage.report;

import com.intellij.rt.coverage.data.BranchData;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.ErrorReporter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class XMLCoverageReport {
  private static final String LINE_COUNTER = "LINE";
  private static final String BRANCH_COUNTER = "BRANCH";
  private static final String METHOD_COUNTER = "METHOD";
  private static final String CLASS_COUNTER = "CLASS";
  private static final String INSTRUCTION_COUNTER = "INSTRUCTION";

  private static final int LINE_MASK = 1;
  private static final int BRANCH_MASK = 2;
  private static final int METHOD_MASK = 4;
  private static final int CLASS_MASK = 8;
  private static final int INSTRUCTION_MASK = 16;

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

    final Counter counter = new Counter();
    for (Map.Entry<String, List<ClassData>> packageEntry : packages.entrySet()) {
      String packageName = packageEntry.getKey();
      List<ClassData> classes = packageEntry.getValue();
      final Counter packageCounter = writePackage(packageName, classes);
      counter.add(packageCounter);
    }
    writeCounter(counter, INSTRUCTION_MASK | LINE_MASK | BRANCH_MASK | METHOD_MASK | CLASS_MASK);
    myOut.writeEndElement();
    newLine();
    myOut.writeEndDocument();
    newLine();
  }

  private Counter writePackage(String packageName, List<ClassData> classes) throws XMLStreamException {
    myOut.writeStartElement("package");
    myOut.writeAttribute("name", ClassNameUtil.convertToInternalName(packageName));
    newLine();
    myFiles.clear();
    final Counter counter = new Counter();
    for (ClassData classData : classes) {
      final Counter classCounter = writeClass(classData);
      counter.add(classCounter);
    }
    for (Map.Entry<String, List<LineData>> fileEntry : myFiles.entrySet()) {
      String fileName = fileEntry.getKey();
      List<LineData> lines = fileEntry.getValue();
      writeFile(fileName, lines);
    }

    writeCounter(counter, INSTRUCTION_MASK | LINE_MASK | BRANCH_MASK | METHOD_MASK | CLASS_MASK);

    myOut.writeEndElement();
    newLine();

    return counter;
  }

  private void writeFile(String fileName, List<LineData> lines) throws XMLStreamException {
    myOut.writeStartElement("sourcefile");
    myOut.writeAttribute("name", fileName);
    newLine();
    final Counter counter = new Counter();
    for (LineData lineData : lines) {
      if (lineData == null) continue;
      final Counter lineCounter = writeLine(lineData);
      counter.add(lineCounter);
    }
    writeCounter(counter, INSTRUCTION_MASK | LINE_MASK | BRANCH_MASK);
    myOut.writeEndElement();
    newLine();
  }

  private Counter writeClass(ClassData classData) throws XMLStreamException {
    myOut.writeStartElement("class");
    final String className = ClassNameUtil.convertToInternalName(classData.getName());
    myOut.writeAttribute("name", className);
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
    final Counter counter = new Counter();
    Map<String, List<LineData>> methods = classData.mapLinesToMethods();
    for (Map.Entry<String, List<LineData>> methodEntry : methods.entrySet()) {
      final Counter methodCounter = writeMethod(methodEntry.getKey(), methodEntry.getValue());
      counter.add(methodCounter);
    }
    counter.totalClasses = 1;
    if (counter.coveredMethods > 0) counter.coveredClasses = 1;
    writeCounter(counter, INSTRUCTION_MASK | LINE_MASK | BRANCH_MASK | METHOD_MASK);
    myOut.writeEndElement();
    newLine();
    return counter;
  }

  private Counter writeMethod(String signature, List<LineData> lines) throws XMLStreamException {
    myOut.writeStartElement("method");
    int nameIndex = signature.indexOf('(');
    String name = signature.substring(0, nameIndex);
    String descriptor = signature.substring(nameIndex);
    myOut.writeAttribute("name", name);
    myOut.writeAttribute("desc", descriptor);
    newLine();

    final Counter counter = new Counter();
    for (LineData lineData : lines) {
      if (lineData == null) continue;
      counter.add(getLineCounter(lineData));
    }
    counter.totalMethods = 1;
    if (counter.coveredLines > 0) counter.coveredMethods = 1;
    writeCounter(counter, INSTRUCTION_MASK | LINE_MASK | BRANCH_MASK);

    myOut.writeEndElement();
    newLine();
    return counter;
  }

  private Counter writeLine(LineData lineData) throws XMLStreamException {
    myOut.writeEmptyElement("line");
    myOut.writeAttribute("nr", Integer.toString(lineData.getLineNumber()));

    final Counter counter = getLineCounter(lineData);
    myOut.writeAttribute("mi", Integer.toString(counter.totalInstructions - counter.coveredInstructions));
    myOut.writeAttribute("ci", Integer.toString(counter.coveredInstructions));
    myOut.writeAttribute("mb", Integer.toString(counter.totalBranches - counter.coveredBranches));
    myOut.writeAttribute("cb", Integer.toString(counter.coveredBranches));
    newLine();
    return counter;
  }

  private Counter getLineCounter(LineData lineData) {
    final Counter counter = new Counter();
    counter.totalLines = 1;
    counter.coveredLines = lineData.getHits() > 0 ? 1 : 0;

    final BranchData branchData = lineData.getBranchData();
    counter.totalBranches = branchData == null ? 0 : branchData.getTotalBranches();
    counter.coveredBranches = branchData == null ? 0 : branchData.getCoveredBranches();

    final BranchData instructionsData = lineData.getInstructionsData();
    counter.totalInstructions = instructionsData.getTotalBranches();
    counter.coveredInstructions = instructionsData.getCoveredBranches();
    return counter;
  }

  private void writeCounter(Counter counter, int mask) throws XMLStreamException {
    if ((mask & INSTRUCTION_MASK) != 0) writeCounter(INSTRUCTION_COUNTER, counter.totalInstructions, counter.coveredInstructions);
    if ((mask & BRANCH_MASK) != 0) writeCounter(BRANCH_COUNTER, counter.totalBranches, counter.coveredBranches);
    if ((mask & LINE_MASK) != 0) writeCounter(LINE_COUNTER, counter.totalLines, counter.coveredLines);
    if ((mask & METHOD_MASK) != 0) writeCounter(METHOD_COUNTER, counter.totalMethods, counter.coveredMethods);
    if ((mask & CLASS_MASK) != 0) writeCounter(CLASS_COUNTER, counter.totalClasses, counter.coveredClasses);
  }

  private void writeCounter(String type, int total, int covered) throws XMLStreamException {
    myOut.writeEmptyElement("counter");
    myOut.writeAttribute("type", type);
    myOut.writeAttribute("missed", Integer.toString(total - covered));
    myOut.writeAttribute("covered", Integer.toString(covered));
    newLine();
  }

  private static boolean shouldIncludeClass(ClassData classData) {
    final Object[] lines = classData.getLines();
    if (lines == null) return false;
    for (Object line : lines) {
      if (line != null) return true;
    }
    return false;
  }

  private static HashMap<String, List<ClassData>> mapClassesToPackages(ProjectData project) {
    HashMap<String, List<ClassData>> packages = new HashMap<String, List<ClassData>>();
    for (ClassData classData : project.getClasses().values()) {
      if (!shouldIncludeClass(classData)) continue;
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

  private static class Counter {
    public int totalClasses;
    public int coveredClasses;

    public int totalMethods;
    public int coveredMethods;

    public int totalLines;
    public int coveredLines;

    public int totalInstructions;
    public int coveredInstructions;

    public int coveredBranches;
    public int totalBranches;

    public void add(Counter other) {
      totalClasses += other.totalClasses;
      coveredClasses += other.coveredClasses;
      totalMethods += other.totalMethods;
      coveredMethods += other.coveredMethods;
      totalLines += other.totalLines;
      coveredLines += other.coveredLines;
      totalBranches += other.totalBranches;
      coveredBranches += other.coveredBranches;
      totalInstructions += other.totalInstructions;
      coveredInstructions += other.coveredInstructions;
    }
  }
}
