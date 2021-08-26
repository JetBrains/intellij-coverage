/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.visiting;

import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import com.intellij.rt.coverage.util.ErrorReporter;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

import java.util.*;

public class KotlinInlineVisitingFilter extends MethodVisitingFilter {
  private static final String INLINE_FUNCTION_PREFIX = "$i$f$";

  /**
   * A correct descriptor of inlined function is unknown is the class with the method definition is not loaded,
   * so a default descriptor is used.
   */
  private static final String DEFAULT_DESC = "()V";
  private Map<Label, Integer> myLines;
  private List<InlineRange> myInlineRanges;
  private String myName;

  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void initFilter(MethodVisitor methodVisitor, Instrumenter context, String name, String desc) {
    super.initFilter(methodVisitor, context, name, desc);
    myLines = new HashMap<Label, Integer>();
    myInlineRanges = new ArrayList<InlineRange>();
    myName = name;
  }

  @Override
  public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
    super.visitLocalVariable(name, descriptor, signature, start, end, index);
    if (!name.startsWith(INLINE_FUNCTION_PREFIX)) return;
    final String inlineMethodName = name.substring(INLINE_FUNCTION_PREFIX.length());
    myInlineRanges.add(new InlineRange(inlineMethodName, start, end));
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    myLines.put(start, line);
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    if (myInlineRanges.isEmpty()) return;
    try {
      Collections.sort(myInlineRanges);
      final TreeMap<Integer, Integer> lines = new TreeMap<Integer, Integer>();
      for (Map.Entry<Label, Integer> entry : myLines.entrySet()) {
        lines.put(entry.getKey().getOffset(), entry.getValue());
      }
      for (InlineRange range : myInlineRanges) {
        for (int line : lines.subMap(range.myStart.getOffset(), range.myEnd.getOffset()).values()) {
          final LineData lineData = myContext.getLineData(line);
          if (lineData == null) continue;
          // in case of visiting an inline method definition
          if (range.myName.equals(myName)) continue;
          lineData.setMethodSignature(range.myName + DEFAULT_DESC);
        }
      }
    } catch (Throwable e) {
      ErrorReporter.reportError("Error during inline ranges collection", e);
    }
  }

  public static boolean isInlineMethod(String methodName, String variableName) {
    return variableName.equals(INLINE_FUNCTION_PREFIX + methodName);
  }

  private static class InlineRange implements Comparable<InlineRange> {
    private final String myName;
    private final Label myStart;
    private final Label myEnd;

    private InlineRange(String name, Label start, Label end) {
      myName = name;
      myStart = start;
      myEnd = end;
    }

    public int compareTo(InlineRange o) {
      final int startDiff = myStart.getOffset() - o.myStart.getOffset();
      if (startDiff == 0) {
        return -(myEnd.getOffset() - o.myEnd.getOffset());
      }
      return startDiff;
    }
  }
}
