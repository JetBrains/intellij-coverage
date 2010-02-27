package com.intellij.rt.coverage.data;


import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.DictionaryLookup;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class ClassData implements CoverageData {
  private final String myClassName;
  private LineData[] myLinesArray;
  private Map myStatus;
  private int[] myLineMask;

  public ClassData(final String name) {
    myClassName = name;
  }

  public String getName() {
    return myClassName;
  }

  public void save(final DataOutputStream os, DictionaryLookup dictionaryLookup) throws IOException {
    CoverageIOUtil.writeINT(os, dictionaryLookup.getDictionaryIndex(myClassName));
    final Map sigLines = prepareSignaturesMap(dictionaryLookup);
    final Set sigs = sigLines.keySet();
    CoverageIOUtil.writeINT(os, sigs.size());
    for (Iterator it = sigs.iterator(); it.hasNext();) {
      final String sig = (String)it.next();
      CoverageIOUtil.writeUTF(os, sig);
      final List lines = (List)sigLines.get(sig);
      CoverageIOUtil.writeINT(os, lines.size());
      for (int i = 0; i < lines.size(); i++) {
        ((LineData)lines.get(i)).save(os, dictionaryLookup);
      }
    }
  }

  private Map prepareSignaturesMap(DictionaryLookup dictionaryLookup) {
    final Map sigLines = new HashMap();
    for (int i = 0; i < myLinesArray.length; i++) {
      final LineData lineData = myLinesArray[i];
      if (lineData == null) continue;
      if (myLineMask != null) {
        lineData.setHits(myLineMask[lineData.getLineNumber()]);
      }
      final String sig = CoverageIOUtil.collapse(lineData.getMethodSignature(), dictionaryLookup);
      List lines = (List)sigLines.get(sig);
      if (lines == null) {
        lines = new ArrayList();
        sigLines.put(sig, lines);
      }
      lines.add(lineData);
    }
    return sigLines;
  }

  public void merge(final CoverageData data) {
    ClassData classData = (ClassData)data;
    if (myLinesArray.length < classData.myLinesArray.length) {
      LineData[] lines = new LineData[classData.myLinesArray.length];
      System.arraycopy(myLinesArray, 0, lines, 0, myLinesArray.length);
      myLinesArray = lines;
    }
    for (int i = 0; i < classData.myLinesArray.length; i++) {
      final LineData mergedData = classData.myLinesArray[i];
      if (mergedData == null) continue;
      final LineData lineData = i < myLinesArray.length ? myLinesArray[i] : null;
      if (lineData != null) {
        lineData.merge(mergedData);
      }
      else {
        registerMethodSignature(mergedData);
        myLinesArray[i] = mergedData;
      }
    }
  }

  public void touchLine(int line) {
    myLineMask[line]++;
  }

  public void touch(int line) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.touch();
    }
  }

  public void touch(int line, int jump, boolean hit) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.touchBrunch(jump, hit);
    }
  }

  public void touch(int line, int switchNumber, int key) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.touchBrunch(switchNumber, key);
    }
  }

  public void registerMethodSignature(LineData lineData) {
    initStatusMap();
    myStatus.put(lineData.getMethodSignature(), null);
  }

  public LineData getLineData(int line) {
    return myLinesArray[line];
  }

  /** @noinspection UnusedDeclaration*/
  public Object[] getLines() {
    return myLinesArray;
  }

  /** @noinspection UnusedDeclaration*/
  public boolean containsLine(int line) {
    return myLinesArray[line] != null;
  }

  /** @noinspection UnusedDeclaration*/
  public Collection getMethodSigs() {
    initStatusMap();
    return myStatus.keySet();
  }

  private void initStatusMap() {
    if (myStatus == null) myStatus = new HashMap();
  }

  public Integer getStatus(String methodSignature) {
    Integer methodStatus = (Integer)myStatus.get(methodSignature);
    if (methodStatus == null) {
      for (int i = 0; i < myLinesArray.length; i++) {
        final LineData lineData = myLinesArray[i];
        if (lineData != null && lineData.getMethodSignature().equals(methodSignature)) {
          if (lineData.getStatus() != LineCoverage.NONE) {
            methodStatus = new Integer(LineCoverage.PARTIAL);
            break;
          }
        }
      }
      if (methodStatus == null) methodStatus = new Integer(LineCoverage.NONE);
      myStatus.put(methodSignature, methodStatus);
    }
    return methodStatus;
  }

  public String toString() {
    return myClassName;
  }

  public void initLineMask(int size) {
    myLineMask = new int[size + 1];
    Arrays.fill(myLineMask, 0);
  }

  public void setLines(LineData[] lines) {
    myLinesArray = lines;
  }
}
