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
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import com.intellij.rt.coverage.util.ErrorReporter;
import com.intellij.rt.coverage.util.classFinder.ClassEntry;
import com.intellij.rt.coverage.util.classFinder.ClassFinder;
import com.intellij.rt.coverage.util.classFinder.ClassPathEntry;
import org.jetbrains.coverage.gnu.trove.TIntHashSet;
import org.jetbrains.coverage.gnu.trove.TIntProcedure;
import org.jetbrains.coverage.org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class KotlinInlineVisitingFilter extends MethodVisitingFilter {
  private static final String INLINE_FUNCTION_PREFIX = "$i$f$";
  private static final boolean ourCheckInlineSignatures =
      "true".equals(System.getProperty("idea.coverage.check.inline.signatures"));

  /**
   * A correct descriptor of inlined function is unknown is the class with the method definition is not loaded,
   * so a default descriptor is used.
   */
  private static final String DEFAULT_DESC = "()V";
  private static final String UNKNOWN_DESC = "(?)?";
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
          lineData.setMethodSignature(range.myName + (ourCheckInlineSignatures ? UNKNOWN_DESC : DEFAULT_DESC));
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
        final ClassReader reader = new ClassReader(classEntry.getClassInputStream());

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
