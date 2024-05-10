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
  private volatile Object myHitsMask;
  /**
   * Storage for test tracking data.
   */
  private volatile boolean[] myTraceMask;

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

  public Object[] getLines() {
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

  public void resetLines(LineData[] lines) {
    myLinesArray = lines;
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

  public synchronized void createMask(int size, boolean calculateHits) {
    Object current = myHitsMask;
    if (calculateHits) {
      if (current == null) {
        myHitsMask = new int[size];
      } else {
        if (!(current instanceof int[])) throw new IllegalStateException("Int array expected");
        int[] hits = (int[]) current;
        if (hits.length < size) {
          // Overwriting this field may cause incomplete coverage,
          // as the reference to this array is cached in the instrumented class field/condy.
          myHitsMask = ArrayUtil.copy(hits, size);
        }
      }
    } else {
      if (current == null) {
        myHitsMask = new boolean[size];
      } else {
        if (!(current instanceof boolean[])) throw new IllegalStateException("Boolean array expected");
        boolean[] hits = (boolean[]) current;
        if (hits.length < size) {
          // Overwriting this field may cause incomplete coverage,
          // as the reference to this array is cached in the instrumented class field/condy.
          myHitsMask = ArrayUtil.copy(hits, size);
        }
      }
    }
  }

  public synchronized void createTraceMask(int size) {
    if (myTraceMask == null) {
      myTraceMask = new boolean[size];
    } else if (myTraceMask.length < size) {
      myTraceMask = ArrayUtil.copy(myTraceMask, size);
    }
  }

  public Object getHitsMask() {
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
    int[] hits = CommonArrayUtil.getIntArray(myHitsMask);
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
        }

        JumpData[] jumps = lineData.getJumps();
        if (jumps != null) {
          for (JumpData jumpData : jumps) {
            if (jumpData == null) continue;
            int trueId = jumpData.getId(true);
            if (trueId != -1) {
              jumpData.setTrueHits(jumpData.getTrueHits() + hits[trueId]);
            }
            int falseId = jumpData.getId(false);
            if (falseId != -1) {
              jumpData.setFalseHits(jumpData.getFalseHits() + hits[falseId]);
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
            }
            int[] switchHits = switchData.getHits();
            for (int i = 0; i < switchHits.length; i++) {
              int caseId = switchData.getId(i);
              if (caseId == -1) continue;
              switchHits[i] += hits[caseId];
            }
            switchData.setKeysAndHits(switchData.getKeys(), switchHits);
          }
        }
      }
      if (myHitsMask instanceof int[]) {
        Arrays.fill((int[]) myHitsMask, 0);
      } else if (myHitsMask instanceof boolean[]) {
        Arrays.fill((boolean[]) myHitsMask, false);
      }
    } catch (Throwable e) {
      ErrorReporter.warn("Unexpected error during applying hits data to class " + getName(), e);
    }
  }
}
