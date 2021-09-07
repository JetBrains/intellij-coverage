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

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.FileMapData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.filters.enumerating.KotlinDefaultArgsBranchFilter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.classFinder.ClassEntry;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import org.jetbrains.coverage.gnu.trove.TIntHashSet;
import org.jetbrains.coverage.gnu.trove.TIntIntHashMap;
import org.jetbrains.coverage.gnu.trove.TIntIntProcedure;
import org.jetbrains.coverage.gnu.trove.TIntProcedure;
import org.jetbrains.coverage.org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class KotlinInlineVisitingFilter extends MethodVisitingFilter {
  private static final String INLINE_FUNCTION_PREFIX = "$i$f$";
  private static final String INLINE_ARGUMENT_PREFIX = "$i$a-$";
  private static final boolean ourCheckInlineSignatures =
      "true".equals(System.getProperty("idea.coverage.check.inline.signatures"));

  /**
   * A correct descriptor of inlined function is unknown is the class with the method definition is not loaded,
   * so a default descriptor is used.
   */
  private static final String DEFAULT_DESC = "()V";
  private static final String UNKNOWN_DESC = "(?)?";
  private TIntIntHashMap myLines;
  private TIntHashSet myLinesSet;
  private Map<Label, Integer> myLabelIds;
  private List<InlineRange> myInlineFunctionRanges;
  private List<InlineRange> myInlineArgumentRanges;
  private String myName;
  private int myLabelCounter;

  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void initFilter(MethodVisitor methodVisitor, Instrumenter context, String name, String desc) {
    super.initFilter(methodVisitor, context, name, desc);
    myLines = new TIntIntHashMap();
    myLinesSet = new TIntHashSet();
    myLabelIds = new HashMap<Label, Integer>();
    myInlineFunctionRanges = new ArrayList<InlineRange>();
    myInlineArgumentRanges = new ArrayList<InlineRange>();
    myName = name;
    myLabelCounter = 0;
  }

  @Override
  public void visitLabel(Label label) {
    super.visitLabel(label);
    myLabelIds.put(label, myLabelCounter++);
  }

  @Override
  public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
    super.visitLocalVariable(name, descriptor, signature, start, end, index);
    if (name.startsWith(INLINE_FUNCTION_PREFIX)) {
      final String inlineMethodName = name.substring(INLINE_FUNCTION_PREFIX.length());
      if (isSameMethod(inlineMethodName)) return;
      myInlineFunctionRanges.add(new InlineRange(inlineMethodName, myLabelIds.get(start), myLabelIds.get(end)));
    }
    if (name.startsWith(INLINE_ARGUMENT_PREFIX)) {
      final int i = name.lastIndexOf('-');
      if (i < 0) return;
      final String inlineMethodName = name.substring(INLINE_ARGUMENT_PREFIX.length(), i);
      if (isSameMethod(inlineMethodName)) return;
      myInlineArgumentRanges.add(new InlineRange("", myLabelIds.get(start), myLabelIds.get(end)));
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    super.visitLineNumber(line, start);
    // ignore repeated line numbers as line can have only one signature
    if (myLinesSet.add(line)) {
      myLines.put(myLabelIds.get(start), line);
    }
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    if (myInlineFunctionRanges.isEmpty()) return;
    if (myName == null) return;
    try {
      final Integer maxLine = getMaxSourceLine();
      if (maxLine == null) return;
      Collections.sort(myInlineFunctionRanges);
      Collections.sort(myInlineArgumentRanges);
      final TreeMap<Integer, Integer> lines = new TreeMap<Integer, Integer>();
      myLines.forEachEntry(new TIntIntProcedure() {
        public boolean execute(int offset, int line) {
          lines.put(offset, line);
          return true;
        }
      });
      for (InlineRange range : myInlineFunctionRanges) {
        for (Map.Entry<Integer, Integer> entry : lines.subMap(range.myStart, range.myEnd).entrySet()) {
          final int line = entry.getValue();
          if (line <= maxLine) continue;
          if (isInside(range, findInlineArgumentRange(entry.getKey()))) continue;
          final LineData lineData = myContext.getLineData(line);
          if (lineData == null) continue;
          lineData.setMethodSignature(range.myName + (ourCheckInlineSignatures ? UNKNOWN_DESC : DEFAULT_DESC));
        }
      }
    } catch (Throwable e) {
      ErrorReporter.reportError("Error during inline ranges collection", e);
    }
  }

  private Integer getMaxSourceLine() {
    final ProjectData project = ProjectData.getProjectData();
    if (project == null) return null;
    final Map<String, FileMapData[]> mappings = project.getLinesMap();
    if (mappings == null) return null;
    final FileMapData[] classMappings = mappings.get(myContext.getClassName());
    if (classMappings == null) return null;
    int maxValue = -1;
    for (FileMapData data : classMappings) {
      if (data == null) continue;
      if (!data.getClassName().equals(myContext.getClassName())) continue;
      maxValue = Math.max(maxValue, ClassData.maxSourceLineNumber(data.getLines()));
    }
    return maxValue == -1 ? null : maxValue;
  }

  private boolean isSameMethod(String name) {
    return myName.equals(name) // definition of inline method
        || myName.equals(name + KotlinDefaultArgsBranchFilter.DEFAULT_ARGS_SUFFIX) // default method
        || myName.equals(name + "-impl"); // InlineOnly method
  }

  private static boolean isInside(InlineRange container, InlineRange content) {
    return container != null && content != null
        && content.myStart >= container.myStart
        && content.myEnd <= container.myEnd;
  }

  private InlineRange findInlineArgumentRange(int offset) {
    int low = 0;
    int high = myInlineArgumentRanges.size() - 1;
    InlineRange result = null;
    while (low <= high) {
      final int mid = (low + high) / 2;
      final InlineRange midValue = myInlineArgumentRanges.get(mid);
      if (offset < midValue.myStart) {
        high = mid - 1;
      } else if (offset >= midValue.myEnd) {
        low = mid + 1;
      } else {
        if (result == null || result.length() > midValue.length()) {
          result = midValue;
        }
        // try to find a nested range
        if (midValue.myStart == offset) {
          high = mid - 1;
        } else {
          low = mid + 1;
        }
      }
    }
    return result;
  }

  public static boolean isInlineMethod(String methodName, String variableName) {
    return variableName.equals(INLINE_FUNCTION_PREFIX + methodName);
  }

  private static class InlineRange implements Comparable<InlineRange> {
    private final String myName;
    private final int myStart;
    private final int myEnd;

    private InlineRange(String name, int start, int end) {
      myName = name;
      myStart = start;
      myEnd = end;
    }

    public int length() {
      return myEnd - myStart;
    }

    @Override
    public String toString() {
      return "InlineRange{name=" + myName + ", start=" + myStart + ", end=" + myEnd + '}';
    }

    public int compareTo(InlineRange o) {
      final int startDiff = myStart - o.myStart;
      if (startDiff == 0) {
        return myEnd - o.myEnd;
      }
      return startDiff;
    }
  }

  /**
   * If an option is turned on, try to load bytecode of classes that have lines with unknown signature and
   * find out the correct signature.
   */
  public static void checkLineSignatures(final ClassData classData, final ClassFinder classFinder) {
    if (!ourCheckInlineSignatures) return;
    final TIntHashSet linesWithIncorrectSignatures = new TIntHashSet();
    for (LineData line : (LineData[]) classData.getLines()) {
      if (line != null && line.getMethodSignature() != null && line.getMethodSignature().endsWith(UNKNOWN_DESC)) {
        linesWithIncorrectSignatures.add(line.getLineNumber());
      }
    }
    if (linesWithIncorrectSignatures.isEmpty()) return;
    final Set<ClassLoader> classLoaders = new HashSet<ClassLoader>(classFinder.getClassloaders());
    classLoaders.add(null);
    for (ClassLoader loader : classLoaders) {
      InputStream is = null;
      try {
        final ClassEntry classEntry = new ClassEntry(classData.getName(), loader);
        is = classEntry.getClassInputStream();
        if (is == null) continue;
        final ClassReader reader = new ClassReader(is);

        reader.accept(new ClassVisitor(Opcodes.API_VERSION) {
          @Override
          public MethodVisitor visitMethod(int access, final String name, final String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.API_VERSION) {
              @Override
              public void visitLineNumber(int line, Label start) {
                super.visitLineNumber(line, start);
                if (linesWithIncorrectSignatures.remove(line)) {
                  final LineData lineData = classData.getLineData(line);
                  lineData.setMethodSignature(name + descriptor);
                }
              }
            };
          }
        }, ClassReader.SKIP_FRAMES);
        break;
      } catch (Throwable ignored) {
      } finally {
        if (is != null) {
          try {
            is.close();
          } catch (IOException ignored) {
          }
        }
      }
    }
    linesWithIncorrectSignatures.forEach(new TIntProcedure() {
      public boolean execute(int line) {
        final LineData lineData = classData.getLineData(line);
        lineData.setMethodSignature(lineData.getMethodSignature().replace(UNKNOWN_DESC, DEFAULT_DESC));
        return true;
      }
    });
  }
}
