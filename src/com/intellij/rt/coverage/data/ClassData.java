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


import com.intellij.rt.coverage.util.ArrayUtil;
import com.intellij.rt.coverage.util.CoverageIOUtil;
import com.intellij.rt.coverage.util.DictionaryLookup;
import com.intellij.rt.coverage.util.ErrorReporter;
import org.jetbrains.coverage.gnu.trove.TIntHashSet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class ClassData implements CoverageData {
  private final String myClassName;
  private LineData[] myLinesArray;
  private Map<String, Integer> myStatus;
  /** Storage for line hits in sampling mode. */
  private int[] myLineMask;
  private String mySource;

  /** Storage for line and branch hits in new tracing mode. */
  private volatile int[] myHitsMask;
  /** Storage for test tracking data. */
  private volatile boolean[] myTraceMask;

  /**
   * Set of lines that were ignored during instrumentation.
   * Storing this lines helps to correctly merge when a class has inline functions.
   */
  private TIntHashSet myIgnoredLines;

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
    for (Object sig1 : sigs) {
      final String sig = (String) sig1;
      CoverageIOUtil.writeUTF(os, sig);
      final List<LineData> lines = sigLines.get(sig);
      CoverageIOUtil.writeINT(os, lines.size());
      for (Object line : lines) {
        ((LineData) line).save(os);
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
      lineData.touchBranch(jump, hit);
    }
  }

  public void touch(int line, int switchNumber, int key) {
    final LineData lineData = getLineData(line);
    if (lineData != null) {
      lineData.touchBranch(switchNumber, key);
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
  public Collection<String> getMethodSigs() {
    initStatusMap();
    return myStatus.keySet();
  }

  private void initStatusMap() {
    if (myStatus == null) myStatus = new HashMap<String, Integer>();
  }

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

  public void initLineMask(LineData[] lines) {
    if (myLineMask == null) {
      myLineMask = new int[myLinesArray != null ? Math.max(lines.length, myLinesArray.length) : lines.length];
      if (myLinesArray != null) {
        for (int i = 0; i < myLinesArray.length; i++) {
          final LineData data = myLinesArray[i];
          if (data != null) {
            myLineMask[i] = data.getHits();
          }
        }
      }
    } else {
      if (myLineMask.length < lines.length) {
        int[] lineMask = new int[lines.length];
        System.arraycopy(myLineMask, 0, lineMask, 0, myLineMask.length);
        myLineMask = lineMask;
      }
      for (int i = 0; i < lines.length; i++) {
        if (lines[i] != null) {
          myLineMask[i] += lines[i].getHits();
        }
      }
    }
  }

  public void setLines(LineData[] lines) {
    if (myLinesArray == null) {
      myLinesArray = lines;
    } else {
      mergeLines(lines);
    }
  }

  public static int maxSourceLineNumber(LineMapData[] linesMap) {
    int max = 0;
    for (final LineMapData mapData : linesMap) {
      if (mapData != null) {
        max = Math.max(max, mapData.getSourceLineNumber());
      }
    }
    return max;
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
    if (linesMap == null) return;
    if (sourceClassData == targetClassData && sourceClassData.myLinesArray == null) return;
    final LineData[] sourceLines = getSourceLinesArray(linesMap, sourceClassData, targetClassData);
    try {
      final LineData[] targetLines = targetClassData.myLinesArray;
      for (final LineMapData mapData : linesMap) {
        if (mapData == null) continue;
        final int sourceLineNumber = mapData.getSourceLineNumber();
        if (!sourceClassData.isIgnoredLine(sourceLineNumber) && ArrayUtil.safeLoad(sourceLines, sourceLineNumber) == null) {
          final LineData targetLineData = ArrayUtil.safeLoad(targetLines, mapData.getTargetMinLine());
          if (targetLineData != null) {
            final LineData source = new LineData(mapData.getSourceLineNumber(), targetLineData.getMethodSignature());
            ArrayUtil.safeStore(sourceLines, sourceLineNumber, source);
          }
        }
        for (int targetLineNumber = mapData.getTargetMinLine(); targetLineNumber <= mapData.getTargetMaxLine(); targetLineNumber++) {
          final LineData source = ArrayUtil.safeLoad(sourceLines, sourceLineNumber);
          final LineData target = ArrayUtil.safeLoad(targetLines, targetLineNumber);
          if (target == null) continue;
          if (source != null) {
            source.merge(target);
          }
          if (sourceClassData != targetClassData || targetLineNumber != sourceLineNumber) {
            targetLines[targetLineNumber] = null;
          }
        }
      }
    } catch (Throwable e) {
      ErrorReporter.reportError("Error creating line mappings for " + targetClassData.getName(), e);
      return;
    }
    sourceClassData.myLinesArray = sourceLines;
  }

  /**
   * Return lines array of <code>sourceClassData</code> if it fits mapping, or enlarge it.
   */
  private static LineData[] getSourceLinesArray(LineMapData[] linesMap, ClassData sourceClassData, ClassData targetClassData) {
    final int maxMappedSourceLineNumber = maxSourceLineNumber(linesMap);
    if (targetClassData == sourceClassData) {
      return sourceClassData.myLinesArray;
    } else if (sourceClassData.myLinesArray == null) {
      return new LineData[1 + maxMappedSourceLineNumber];
    } else if (sourceClassData.myLinesArray.length >= 1 + maxMappedSourceLineNumber) {
      return sourceClassData.myLinesArray;
    } else {
      final LineData[] sourceLines = new LineData[1 + maxMappedSourceLineNumber];
      System.arraycopy(sourceClassData.myLinesArray, 0, sourceLines, 0, sourceClassData.myLinesArray.length);
      return sourceLines;
    }
  }

  /**
   * Remove all lines that are generated by inline.
   * Do not touch lines that are mapped to itself.
   */
  public void dropMappedLines(FileMapData[] mappings) {
    final LineData[] lines = myLinesArray;
    for (FileMapData mapData : mappings) {
      final boolean isThisClass = myClassName.equals(mapData.getClassName());
      for (LineMapData lineMapData : mapData.getLines()) {
        final int sourceLineNumber = lineMapData.getSourceLineNumber();
        for (int i = lineMapData.getTargetMinLine(); i <= lineMapData.getTargetMaxLine() && i < lines.length; i++) {
          if (isThisClass && i == sourceLineNumber) continue;
          lines[i] = null;
        }
      }
    }
  }

  public void setSource(String source) {
    this.mySource = source;
  }

  public String getSource() {
    return mySource;
  }

  public synchronized void createHitsMask(int size) {
    if (myHitsMask != null && myHitsMask.length >= size) return;
    int[] newMask = new int[size];
    if (myHitsMask != null) {
      System.arraycopy(newMask, 0, myHitsMask, 0, myHitsMask.length);
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

  public int[] getLineMask() {
    return myLineMask;
  }

  public int[] getHitsMask() {
    return myHitsMask;
  }

  public boolean[] getTraceMask() {
    return myTraceMask;
  }

  public void setTraceMask(boolean[] traceMask) {
    myTraceMask = traceMask;
  }

  public void applyLinesMask() {
    if (myLineMask == null) return;
    final int size = myLineMask.length;
    for (LineData lineData : myLinesArray) {
      if (lineData == null) continue;
      if (lineData.getLineNumber() >= size) continue;
      lineData.setHits(myLineMask[lineData.getLineNumber()]);
    }
  }

  public void applyBranches() {
    if (myHitsMask == null) return;
    try {
      for (LineData lineData : myLinesArray) {
        if (lineData == null) continue;
        int lineId = lineData.getId();
        if (lineId != -1) {
          lineData.setHits(myHitsMask[lineId]);
        }

        JumpData[] jumps = lineData.getJumps();
        if (jumps != null) {
          for (JumpData jumpData : jumps) {
            if (jumpData == null) continue;
            int trueId = jumpData.getId(true);
            if (trueId != -1) {
              jumpData.setTrueHits(jumpData.getTrueHits() + myHitsMask[trueId]);
            }
            int falseId = jumpData.getId(false);
            if (falseId != -1) {
              jumpData.setFalseHits(jumpData.getFalseHits() + myHitsMask[falseId]);
            }
          }
        }

        SwitchData[] switches = lineData.getSwitches();
        if (switches != null) {
          for (SwitchData switchData : switches) {
            if (switchData == null) continue;
            int defaultId = switchData.getId(-1);
            if (defaultId != -1) {
              switchData.setDefaultHits(switchData.getDefaultHits() + myHitsMask[defaultId]);
            }
            int[] keys = switchData.getKeys();
            int[] hits = switchData.getHits();

            for (int i = 0; i < hits.length; i++) {
              int caseId = switchData.getId(i);
              if (caseId == -1) continue;
              hits[i] += myHitsMask[caseId];
            }
            switchData.setKeysAndHits(keys, hits);
          }
        }
      }
    } catch (Throwable e) {
      ErrorReporter.reportError("Unexpected error during applying branch data to class " + getName(), e);
    }
  }

  public void setIgnoredLines(TIntHashSet ignoredLines) {
    if (ignoredLines != null && !ignoredLines.isEmpty()) {
      myIgnoredLines = ignoredLines;
    }
  }

  public boolean isIgnoredLine(final int line) {
    return myIgnoredLines != null && myIgnoredLines.contains(line);
  }
}
