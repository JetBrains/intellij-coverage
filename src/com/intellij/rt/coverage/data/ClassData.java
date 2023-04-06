/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.rt.coverage.data;


import com.intellij.rt.coverage.util.*;
import org.jetbrains.coverage.gnu.trove.TIntHashSet;
import org.jetbrains.coverage.gnu.trove.TIntProcedure;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Represents a class in coverage engine.
 */
public class ClassData implements CoverageData {
  /**
   * Hits value forced to be in [0, MAX_HITS] to prevent hits overflow.
   */
  private static final int MAX_HITS = 1000000000;

  private final String myClassName;
  private LineData[] myLinesArray;
  private Map<String, Integer> myStatus;
  private String mySource;

  /**
   * Storage for line and branch hits.
   */
  private volatile int[] myHitsMask;
  /**
   * Storage for test tracking data.
   */
  private volatile boolean[] myTraceMask;

  /**
   * Set of lines that were ignored during instrumentation.
   * Storing this lines helps to correctly merge when a class has inline functions.
   */
  private TIntHashSet myIgnoredLines;

  /**
   * This flag shows whether the bytecode this class
   * has been fully analysed in Instrumenter.
   * Alternatively, a class can be created during lines mapping. In this case,
   * this case may have incomplete lines and should be treated as unloaded.
   */
  private boolean myFullyAnalysed = false;

  public ClassData(final String name) {
    myClassName = name;
  }

  public String getName() {
    return myClassName;
  }

  public void save(final DataOutputStream os, DictionaryLookup dictionaryLookup) throws IOException {
    CoverageIOUtil.writeINT(os, dictionaryLookup.getDictionaryIndex(myClassName));
    final Map<String, List<LineData>> sigLines = prepareSignaturesMap(dictionaryLookup, true);
    final Set<String> sigs = sigLines.keySet();
    CoverageIOUtil.writeINT(os, sigs.size());
    for (String sig1 : sigs) {
      CoverageIOUtil.writeUTF(os, sig1);
      final List<LineData> lines = sigLines.get(sig1);
      CoverageIOUtil.writeINT(os, lines.size());
      for (LineData line : lines) {
        line.save(os);
      }
    }
  }

  private Map<String, List<LineData>> prepareSignaturesMap(DictionaryLookup dictionaryLookup, boolean collapseSignatures) {
    final Map<String, List<LineData>> sigLines = new HashMap<String, List<LineData>>();
    if (myLinesArray == null) return sigLines;
    for (final LineData lineData : myLinesArray) {
      if (lineData == null) continue;
      final String methodSignature = lineData.getMethodSignature();
      final String sig = collapseSignatures ? CoverageIOUtil.collapse(methodSignature, dictionaryLookup) : methodSignature;
      List<LineData> lines = sigLines.get(sig);
      if (lines == null) {
        lines = new ArrayList<LineData>();
        sigLines.put(sig, lines);
      }
      lines.add(lineData);
    }
    return sigLines;
  }

  public Map<String, List<LineData>> mapLinesToMethods() {
    return prepareSignaturesMap(null, false);
  }

  public void merge(final CoverageData data) {
    ClassData classData = (ClassData) data;
    mergeLines(classData.myLinesArray);
    for (String o : getMethodSigs()) {
      myStatus.put(o, null);
    }
    if (mySource == null && classData.mySource != null) {
      mySource = classData.mySource;
    }
    if (!isFullyAnalysed() && classData.isFullyAnalysed()) {
      setFullyAnalysed(true);
    }
  }

  private void mergeLines(LineData[] dLines) {
    if (dLines == null) return;
    if (myLinesArray == null || myLinesArray.length < dLines.length) {
      LineData[] lines = new LineData[dLines.length];
      if (myLinesArray != null) {
        System.arraycopy(myLinesArray, 0, lines, 0, myLinesArray.length);
      }
      myLinesArray = lines;
    }
    for (int i = 0; i < dLines.length; i++) {
      final LineData mergedData = dLines[i];
      if (mergedData == null) continue;
      LineData lineData = myLinesArray[i];
      if (lineData == null) {
        if (isIgnoredLine(i)) continue;
        lineData = new LineData(mergedData.getLineNumber(), mergedData.getMethodSignature());
        registerMethodSignature(lineData);
        myLinesArray[i] = lineData;
      }
      lineData.merge(mergedData);
    }
  }

  public void registerMethodSignature(LineData lineData) {
    initStatusMap();
    myStatus.put(lineData.getMethodSignature(), null);
  }

  public LineData getLineData(int line) {
    if (line < 0 || line >= myLinesArray.length) return null;
    return myLinesArray[line];
  }

  public LineData[] getLines() {
    return myLinesArray;
  }

  @SuppressWarnings("unused")
  public boolean containsLine(int line) {
    return 0 <= line && line < myLinesArray.length && myLinesArray[line] != null;
  }

  public Collection<String> getMethodSigs() {
    initStatusMap();
    return myStatus.keySet();
  }

  private void initStatusMap() {
    if (myStatus == null) myStatus = new HashMap<String, Integer>();
  }

  @SuppressWarnings("unused")
  public Integer getStatus(String methodSignature) {
    if (myStatus == null) return null;
    Integer methodStatus = myStatus.get(methodSignature);
    if (methodStatus == null) {
      for (final LineData lineData : myLinesArray) {
        if (lineData != null && methodSignature.equals(lineData.getMethodSignature())) {
          if (lineData.getStatus() != LineCoverage.NONE) {
            methodStatus = (int) LineCoverage.PARTIAL;
            break;
          }
        }
      }
      if (methodStatus == null) methodStatus = (int) LineCoverage.NONE;
      myStatus.put(methodSignature, methodStatus);
    }
    return methodStatus;
  }

  public String toString() {
    return myClassName;
  }

  public void setLines(LineData[] lines) {
    if (myLinesArray == null) {
      myLinesArray = lines;
    } else {
      if (isFullyAnalysed()) {
        mergeLines(lines);
      } else {
        final LineData[] incompleteData = myLinesArray;
        myLinesArray = null;
        myStatus = null;
        mergeLines(lines);
        mergeLines(incompleteData);
      }
    }
    setFullyAnalysed(true);
  }

  /**
   * Apply line mappings: move hits from original line in bytecode to the mapped line.
   *
   * @param linesMap        line mappings from target class to source class
   * @param sourceClassData the class to which the mapped lines are moved
   * @param targetClassData the class which initially contains the mapped lines,
   *                        at the end of this method all mapped lines in this class are set to null
   */
  public static void checkLineMappings(LineMapData[] linesMap, ClassData sourceClassData, ClassData targetClassData) {
    sourceClassData.myLinesArray = new BasicLineMapper().mapLines(linesMap, sourceClassData, targetClassData);
  }

  public void dropMappedLines(FileMapData[] mappings) {
    LineMapper.dropMappedLines(mappings, myLinesArray, myClassName);
  }

  public void setSource(String source) {
    this.mySource = source;
  }

  public String getSource() {
    return mySource;
  }

  public boolean isFullyAnalysed() {
    return myFullyAnalysed;
  }

  public void setFullyAnalysed(boolean value) {
    myFullyAnalysed = value;
  }

  public synchronized void createHitsMask(int size) {
    if (myHitsMask != null && myHitsMask.length >= size) return;
    int[] newMask = new int[size];
    if (myHitsMask != null) {
      System.arraycopy(myHitsMask, 0, newMask, 0, myHitsMask.length);
    }
    myHitsMask = newMask;
  }

  public synchronized void createTraceMask(int size) {
    if (myTraceMask != null && myTraceMask.length >= size) return;
    final boolean[] newMask = new boolean[size];
    if (myTraceMask != null) {
      System.arraycopy(newMask, 0, myTraceMask, 0, myTraceMask.length);
    }
    myTraceMask = newMask;
  }

  public int[] getHitsMask() {
    return myHitsMask;
  }

  public void setHitsMask(int[] hits) {
    myHitsMask = hits;
  }

  public boolean[] getTraceMask() {
    return myTraceMask;
  }

  public void setTraceMask(boolean[] traceMask) {
    myTraceMask = traceMask;
  }

  public static int trimHits(int hits) {
    if (0 <= hits && hits <= MAX_HITS) return hits;
    return MAX_HITS;
  }

  public void applyHits() {
    final int[] hits = myHitsMask;
    if (hits == null) return;

    for (int i = 0; i < hits.length; ++i) {
      if (hits[i] < 0 || hits[i] > MAX_HITS) {
        hits[i] = MAX_HITS;
      }
    }
    try {
      for (LineData lineData : myLinesArray) {
        if (lineData == null) continue;
        int lineId = lineData.getId();
        if (lineId != -1) {
          lineData.setHits(lineData.getHits() + hits[lineId]);
          hits[lineId] = 0;
        }

        JumpData[] jumps = lineData.getJumps();
        if (jumps != null) {
          for (JumpData jumpData : jumps) {
            if (jumpData == null) continue;
            int trueId = jumpData.getId(true);
            if (trueId != -1) {
              jumpData.setTrueHits(jumpData.getTrueHits() + hits[trueId]);
              hits[trueId] = 0;
            }
            int falseId = jumpData.getId(false);
            if (falseId != -1) {
              jumpData.setFalseHits(jumpData.getFalseHits() + hits[falseId]);
              hits[falseId] = 0;
            }
          }
        }

        SwitchData[] switches = lineData.getSwitches();
        if (switches != null) {
          for (SwitchData switchData : switches) {
            if (switchData == null) continue;
            int defaultId = switchData.getId(-1);
            if (defaultId != -1) {
              switchData.setDefaultHits(switchData.getDefaultHits() + hits[defaultId]);
              hits[defaultId] = 0;
            }
            int[] switchHits = switchData.getHits();
            for (int i = 0; i < switchHits.length; i++) {
              int caseId = switchData.getId(i);
              if (caseId == -1) continue;
              switchHits[i] += hits[caseId];
              hits[caseId] = 0;
            }
            switchData.setKeysAndHits(switchData.getKeys(), switchHits);
          }
        }
      }
    } catch (Throwable e) {
      ErrorReporter.reportError("Unexpected error during applying hits data to class " + getName(), e);
    }
  }

  public void dropIgnoredLines() {
    if (myIgnoredLines == null) return;
    myIgnoredLines.forEach(new TIntProcedure() {
      public boolean execute(int line) {
        ArrayUtil.safeStore(myLinesArray, line, null);
        return true;
      }
    });
  }

  public void setIgnoredLines(TIntHashSet ignoredLines) {
    if (ignoredLines != null && !ignoredLines.isEmpty()) {
      myIgnoredLines = ignoredLines;
    }
  }

  public boolean isIgnoredLine(final int line) {
    return myIgnoredLines != null && myIgnoredLines.contains(line);
  }

  private static class BasicLineMapper extends LineMapper<LineData> {

    @Override
    protected LineData createNewLine(LineData targetLine, int line) {
      return new LineData(line, targetLine.getMethodSignature());
    }

    @Override
    protected LineData[] createArray(int size) {
      return new LineData[size];
    }

    @Override
    protected LineData[] getLines(ClassData classData) {
      return classData.myLinesArray;
    }
  }
}
