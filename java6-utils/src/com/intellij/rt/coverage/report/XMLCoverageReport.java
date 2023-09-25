/*
 * Copyright 2000-2023 JetBrains s.r.o.
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
import com.intellij.rt.coverage.data.instructions.ClassInstructions;
import com.intellij.rt.coverage.data.instructions.LineInstructions;
import com.intellij.rt.coverage.util.ArrayUtil;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.ErrorReporter;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TIntObjectProcedure;

import javax.xml.stream.*;
import java.io.*;
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
  private static final String REPORT_TAG = "report";
  private static final String NAME_TAG = "name";
  private static final String IJ_REPORT_NAME = "Intellij Coverage Report";
  public static final String PACKAGE_TAG = "package";
  public static final String SOURCEFILE_TAG = "sourcefile";
  public static final String CLASS_TAG = "class";
  public static final String METHOD_TAG = "method";
  public static final String DESC_TAG = "desc";
  public static final String LINE_TAG = "line";
  public static final String LINE_NUMBER_TAG = "nr";
  public static final String MISSED_INSTRUCTIONS_TAG = "mi";
  public static final String COVERED_INSTRUCTIONS_TAG = "ci";
  public static final String MISSED_BRANCHES_TAG = "mb";
  public static final String COVERED_BRANCHES_TAG = "cb";
  public static final String COUNTER_TAG = "counter";
  public static final String TYPE_TAG = "type";
  public static final String MISSED_TAG = "missed";
  public static final String COVERED_TAG = "covered";
  private static final String SOURCEFILE_NAME_TAG = "sourcefilename";
  private final Map<String, List<LineData>> myFiles = new HashMap<String, List<LineData>>();
  private XMLStreamWriter myOut;
  private XMLStreamReader myIn;

  /**
   * Check whether a file may be an XML coverage report of a suitable format.
   */
  public static boolean canReadFile(File file) {
    InputStream is = null;
    XMLStreamReader in = null;
    try {
      XMLInputFactory factory = XMLInputFactory.newInstance();
      is = new BufferedInputStream(new FileInputStream(file));
      in = factory.createXMLStreamReader(is);
      while (in.hasNext()) {
        if (in.next() == XMLStreamReader.START_ELEMENT) {
          if (!REPORT_TAG.equals(in.getLocalName())) return false;
          while (in.hasNext()) {
            if (in.next() == XMLStreamReader.START_ELEMENT) {
              if (PACKAGE_TAG.equals(in.getLocalName()) && in.getAttributeCount() >= 1 && NAME_TAG.equals(in.getAttributeLocalName(0))) {
                return true;
              }
            }
          }
        }
      }
    } catch (Throwable ignored) {
    } finally {
      CoverageIOUtil.close(is);
      if (in != null) {
        try {
          in.close();
        } catch (XMLStreamException ignored) {
        }
      }
    }
    return false;
  }

  public XMLProjectData read(InputStream fIn) throws IOException {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLProjectData report = new XMLProjectData();
    try {
      myIn = factory.createXMLStreamReader(new BufferedInputStream(fIn));
      while (myIn.hasNext()) {
        int event = myIn.next();
        if (event == XMLStreamReader.START_ELEMENT) {
          String name = myIn.getLocalName();
          if (REPORT_TAG.equals(name)) {
            readProject(report);
          }
        }
      }
      return report;
    } catch (XMLStreamException e) {
      throw wrapIOException(e);
    } finally {
      if (myIn != null) {
        try {
          myIn.close();
        } catch (XMLStreamException ignored) {
        }
        myIn = null;
      }
      fIn.close();
    }
  }

  private void readProject(XMLProjectData report) throws XMLStreamException {
    while (myIn.hasNext()) {
      int event = myIn.next();
      if (event == XMLStreamReader.START_ELEMENT) {
        String name = myIn.getLocalName();
        if (PACKAGE_TAG.equals(name) && myIn.getAttributeCount() >= 1 && NAME_TAG.equals(myIn.getAttributeLocalName(0))) {
          String packageName = myIn.getAttributeValue(0);
          readPackage(report, packageName);
        }
      }
    }
  }

  private void readPackage(XMLProjectData report, String packageName) throws XMLStreamException {
    while (myIn.hasNext()) {
      int event = myIn.next();
      if (event == XMLStreamReader.START_ELEMENT) {
        String name = myIn.getLocalName();
        if (CLASS_TAG.equals(name)) {
          String className = getAttribute(NAME_TAG);
          if (className != null) {
            readClass(report, ClassNameUtil.convertToFQName(className), getAttribute(SOURCEFILE_NAME_TAG));
          }
        } else if (SOURCEFILE_TAG.equals(name)) {
          String fileName = getAttribute(NAME_TAG);
          if (fileName != null) {
            String path = packageName.isEmpty() ? fileName : packageName + "/" + fileName;
            readFile(report, path);
          }
        }
      } else if (event == XMLStreamReader.END_ELEMENT) {
        if (PACKAGE_TAG.equals(myIn.getLocalName())) break;
      }
    }
  }

  private void readFile(XMLProjectData report, String path) throws XMLStreamException {
    XMLProjectData.FileInfo file = new XMLProjectData.FileInfo(path);
    report.addFile(file);
    while (myIn.hasNext()) {
      int event = myIn.next();
      if (event == XMLStreamReader.START_ELEMENT) {
        String name = myIn.getLocalName();
        if (LINE_TAG.equals(name)) {
          int lineNumber = Integer.parseInt(getAttribute(LINE_NUMBER_TAG));
          int mi = Integer.parseInt(getAttribute(MISSED_INSTRUCTIONS_TAG));
          int ci = Integer.parseInt(getAttribute(COVERED_INSTRUCTIONS_TAG));
          int mb = Integer.parseInt(getAttribute(MISSED_BRANCHES_TAG));
          int cb = Integer.parseInt(getAttribute(COVERED_BRANCHES_TAG));

          XMLProjectData.LineInfo lineInfo = new XMLProjectData.LineInfo(lineNumber, mi, ci, mb, cb);
          file.lines.add(lineInfo);
        }
      } else if (event == XMLStreamReader.END_ELEMENT) {
        if (SOURCEFILE_TAG.equals(myIn.getLocalName())) break;
      }
    }
  }

  private void readClass(XMLProjectData report, String className, String fileName) throws XMLStreamException {
    int mi = 0, ci = 0, mb = 0, cb = 0, mm = 0, cm = 0, ml = 0, cl = 0;
    while (myIn.hasNext()) {
      int event = myIn.next();
      if (event == XMLStreamReader.START_ELEMENT) {
        String name = myIn.getLocalName();
        if (METHOD_TAG.equals(name)) {
          readMethod();
        } else if (COUNTER_TAG.equals(name)) {
          String type = getAttribute(TYPE_TAG);
          if (LINE_COUNTER.equals(type)) {
            ml = Integer.parseInt(getAttribute(MISSED_TAG));
            cl = Integer.parseInt(getAttribute(COVERED_TAG));
          } else if (INSTRUCTION_COUNTER.equals(type)) {
            mi = Integer.parseInt(getAttribute(MISSED_TAG));
            ci = Integer.parseInt(getAttribute(COVERED_TAG));
          } else if (METHOD_COUNTER.equals(type)) {
            mm = Integer.parseInt(getAttribute(MISSED_TAG));
            cm = Integer.parseInt(getAttribute(COVERED_TAG));
          } else if (BRANCH_COUNTER.equals(type)) {
            mb = Integer.parseInt(getAttribute(MISSED_TAG));
            cb = Integer.parseInt(getAttribute(COVERED_TAG));
          }
        }
      } else if (event == XMLStreamReader.END_ELEMENT) {
        if (CLASS_TAG.equals(myIn.getLocalName())) break;
      }
    }
    XMLProjectData.ClassInfo classInfo = new XMLProjectData.ClassInfo(className, fileName, ml, cl, mi, ci, mb, cb, mm, cm);
    report.addClass(classInfo);
  }

  private void readMethod() throws XMLStreamException {
    while (myIn.hasNext()) {
      int event = myIn.next();
      if (event == XMLStreamReader.END_ELEMENT && METHOD_TAG.equals(myIn.getLocalName())) {
        break;
      }
    }
  }

  private String getAttribute(String attributeName) {
    String value = null;
    for (int i = 0; i < myIn.getAttributeCount(); i++) {
      if (attributeName.equals(myIn.getAttributeLocalName(i))) {
        value = (myIn.getAttributeValue(i));
        break;
      }
    }
    return value;
  }

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
        }
        fOut.close();
      } catch (XMLStreamException e) {
        ErrorReporter.info("Error closing file.", e);
      }
    }
  }

  private void newLine() throws XMLStreamException {
    myOut.writeCharacters(NEW_LINE);
  }

  private void writeProject(ProjectData project) throws XMLStreamException {
    myOut.writeStartDocument();
    newLine();
    myOut.writeStartElement(REPORT_TAG);
    myOut.writeAttribute(NAME_TAG, IJ_REPORT_NAME);
    newLine();

    final HashMap<String, List<ClassData>> packages = mapClassesToPackages(project, true);

    final Counter counter = new Counter();
    for (Map.Entry<String, List<ClassData>> packageEntry : packages.entrySet()) {
      String packageName = packageEntry.getKey();
      List<ClassData> classes = packageEntry.getValue();
      final Counter packageCounter = writePackage(project, packageName, classes);
      counter.add(packageCounter);
    }
    writeCounter(counter, INSTRUCTION_MASK | LINE_MASK | BRANCH_MASK | METHOD_MASK | CLASS_MASK);
    myOut.writeEndElement();
    newLine();
    myOut.writeEndDocument();
  }

  private Counter writePackage(ProjectData project, String packageName, List<ClassData> classes) throws XMLStreamException {
    myOut.writeStartElement(PACKAGE_TAG);
    myOut.writeAttribute("name", ClassNameUtil.convertToInternalName(packageName));
    newLine();
    myFiles.clear();
    final Counter counter = new Counter();
    final Map<LineData, Counter> lineCounters = new HashMap<LineData, Counter>();
    for (ClassData classData : classes) {
      final Counter classCounter = writeClass(project, classData, lineCounters);
      counter.add(classCounter);
    }
    for (Map.Entry<String, List<LineData>> fileEntry : myFiles.entrySet()) {
      writeFile(fileEntry.getKey(), fileEntry.getValue(), lineCounters);
    }

    writeCounter(counter, INSTRUCTION_MASK | LINE_MASK | BRANCH_MASK | METHOD_MASK | CLASS_MASK);

    myOut.writeEndElement();
    newLine();

    return counter;
  }

  private void writeFile(String fileName, List<LineData> lines, Map<LineData, Counter> lineCounters) throws XMLStreamException {
    myOut.writeStartElement(SOURCEFILE_TAG);
    myOut.writeAttribute(NAME_TAG, fileName);
    newLine();
    final TIntObjectHashMap<Counter> groupedLines = new TIntObjectHashMap<Counter>();
    for (LineData lineData : lines) {
      if (lineData == null) continue;
      final int lineNumber = lineData.getLineNumber();
      Counter lineCounter = groupedLines.get(lineNumber);
      if (lineCounter == null) {
        lineCounter = new Counter();
        groupedLines.put(lineNumber, lineCounter);
      }
      final Counter counter = lineCounters.get(lineData);
      if (counter != null) {
        lineCounter.add(counter);
      }
    }

    final List<LineCounter> groupedLinesList = new ArrayList<LineCounter>();
    groupedLines.forEachEntry(new TIntObjectProcedure<Counter>() {
      public boolean execute(int lineNumber, Counter counter) {
        groupedLinesList.add(new LineCounter(lineNumber, counter));
        return true;
      }
    });

    Collections.sort(groupedLinesList, new Comparator<LineCounter>() {
      public int compare(LineCounter o1, LineCounter o2) {
        return o1.line - o2.line;
      }
    });


    final Counter counter = new Counter();
    for (LineCounter lineCounter : groupedLinesList) {
      writeLine(lineCounter.counter, lineCounter.line);
      counter.add(lineCounter.counter);
    }
    writeCounter(counter, INSTRUCTION_MASK | LINE_MASK | BRANCH_MASK);
    myOut.writeEndElement();
    newLine();
  }

  private Counter writeClass(ProjectData project, ClassData classData, Map<LineData, Counter> lineCounters) throws XMLStreamException {
    final ClassInstructions classInstructions = project.getInstructions().get(classData.getName());
    myOut.writeStartElement(CLASS_TAG);
    final String className = ClassNameUtil.convertToInternalName(classData.getName());
    myOut.writeAttribute("name", className);
    String sourceName = classData.getSource();
    if (sourceName != null && !sourceName.isEmpty()) {
      myOut.writeAttribute(SOURCEFILE_NAME_TAG, sourceName);
      newLine();
      List<LineData> lines = myFiles.get(sourceName);
      if (lines == null) {
        lines = new ArrayList<LineData>();
        myFiles.put(sourceName, lines);
      }
      LineData[] linesArray = (LineData[]) classData.getLines();
      if (linesArray != null) {
        for (LineData line : linesArray) {
          if (line == null) continue;
          lines.add(line);
        }
      }
    } else {
      newLine();
    }
    final Counter counter = new Counter();
    Map<String, List<LineData>> methods = classData.mapLinesToMethods();
    for (Map.Entry<String, List<LineData>> methodEntry : methods.entrySet()) {
      final Counter methodCounter = writeMethod(classInstructions, methodEntry.getKey(), methodEntry.getValue(), lineCounters);
      counter.add(methodCounter);
    }
    counter.totalClasses = 1;
    if (counter.coveredMethods > 0) counter.coveredClasses = 1;
    writeCounter(counter, INSTRUCTION_MASK | LINE_MASK | BRANCH_MASK | METHOD_MASK);
    myOut.writeEndElement();
    newLine();
    return counter;
  }

  private Counter writeMethod(ClassInstructions classInstructions, String signature, List<LineData> lines, Map<LineData, Counter> lineCounters) throws XMLStreamException {
    myOut.writeStartElement(METHOD_TAG);
    int nameIndex = signature.indexOf('(');
    String name = signature.substring(0, nameIndex);
    String descriptor = signature.substring(nameIndex);
    myOut.writeAttribute(NAME_TAG, name);
    myOut.writeAttribute(DESC_TAG, descriptor);
    newLine();

    final Counter counter = new Counter();
    final LineInstructions[] instructions = classInstructions == null ? null : classInstructions.getlines();
    for (LineData lineData : lines) {
      if (lineData == null) continue;
      final LineInstructions lineInstructions = ArrayUtil.safeLoad(instructions, lineData.getLineNumber());
      final Counter lineCounter = getLineCounter(lineInstructions, lineData);
      lineCounters.put(lineData, lineCounter);
      counter.add(lineCounter);
    }
    counter.totalMethods = 1;
    if (counter.coveredLines > 0) counter.coveredMethods = 1;
    writeCounter(counter, INSTRUCTION_MASK | LINE_MASK | BRANCH_MASK);

    myOut.writeEndElement();
    newLine();
    return counter;
  }

  private void writeLine(Counter counter, int lineNumber) throws XMLStreamException {
    myOut.writeEmptyElement(LINE_TAG);
    myOut.writeAttribute(LINE_NUMBER_TAG, Integer.toString(lineNumber));

    myOut.writeAttribute(MISSED_INSTRUCTIONS_TAG, Integer.toString(counter.totalInstructions - counter.coveredInstructions));
    myOut.writeAttribute(COVERED_INSTRUCTIONS_TAG, Integer.toString(counter.coveredInstructions));
    myOut.writeAttribute(MISSED_BRANCHES_TAG, Integer.toString(counter.totalBranches - counter.coveredBranches));
    myOut.writeAttribute(COVERED_BRANCHES_TAG, Integer.toString(counter.coveredBranches));
    newLine();
  }

  private Counter getLineCounter(LineInstructions lineInstructions, LineData lineData) {
    final Counter counter = new Counter();
    counter.totalLines = 1;
    counter.coveredLines = lineData.getHits() > 0 ? 1 : 0;

    final BranchData branchData = lineData.getBranchData();
    counter.totalBranches = branchData == null ? 0 : branchData.getTotalBranches();
    counter.coveredBranches = branchData == null ? 0 : branchData.getCoveredBranches();

    if (lineInstructions != null) {
      final BranchData instructionsData = lineInstructions.getInstructionsData(lineData);
      counter.totalInstructions = instructionsData.getTotalBranches();
      counter.coveredInstructions = instructionsData.getCoveredBranches();
    } else {
      counter.totalInstructions = 1;
      counter.coveredInstructions = counter.coveredLines;
    }
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
    myOut.writeEmptyElement(COUNTER_TAG);
    myOut.writeAttribute(TYPE_TAG, type);
    myOut.writeAttribute(MISSED_TAG, Integer.toString(total - covered));
    myOut.writeAttribute(COVERED_TAG, Integer.toString(covered));
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

  public static HashMap<String, List<ClassData>> mapClassesToPackages(ProjectData project, boolean useClassNameIfEmpty) {
    HashMap<String, List<ClassData>> packages = new HashMap<String, List<ClassData>>();
    final List<ClassData> classes = new ArrayList<ClassData>(project.getClassesCollection());
    Collections.sort(classes, new Comparator<ClassData>() {
      @Override
      public int compare(ClassData o1, ClassData o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    for (ClassData classData : classes) {
      if (!shouldIncludeClass(classData)) continue;
      String className = classData.getName();
      int indexOfName = className.lastIndexOf('.');
      String packageName = indexOfName < 0 ? (useClassNameIfEmpty ? className : "") : className.substring(0, indexOfName);
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

  private static class LineCounter {
    private final int line;
    private final Counter counter;

    private LineCounter(int line, Counter counter) {
      this.line = line;
      this.counter = counter;
    }
  }
}
