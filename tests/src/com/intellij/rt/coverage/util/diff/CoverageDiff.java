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

package com.intellij.rt.coverage.util.diff;

import com.intellij.rt.coverage.data.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** This class is used for comparing coverage for tests. Checks full match of coverage results. */
public class CoverageDiff {
  private final List<DiffElement<ClassData>> myClassesDiff = new ArrayList<DiffElement<ClassData>>();
  private final List<DiffElement<LineData>> myLinesDiff = new ArrayList<DiffElement<LineData>>();
  private final List<DiffElement<JumpData>> myJumpsDiff = new ArrayList<DiffElement<JumpData>>();
  private final List<DiffElement<SwitchData>> mySwitchesDiff = new ArrayList<DiffElement<SwitchData>>();

  private CoverageDiff() {
  }


  public List<DiffElement<ClassData>> getClassesDiff() {
    return myClassesDiff;
  }

  public List<DiffElement<LineData>> getLinesDiff() {
    return myLinesDiff;
  }

  public List<DiffElement<JumpData>> getJumpsDiff() {
    return myJumpsDiff;
  }

  public List<DiffElement<SwitchData>> getSwitchesDiff() {
    return mySwitchesDiff;
  }

  public boolean isEmpty() {
    return myClassesDiff.isEmpty() && myLinesDiff.isEmpty() && myJumpsDiff.isEmpty() && mySwitchesDiff.isEmpty();
  }

  public static CoverageDiff coverageDiff(ProjectData before, ProjectData after) {
    CoverageDiff diff = new CoverageDiff();
    diff.compareProjects(before, after);
    return diff;
  }

  private void compareProjects(ProjectData before, ProjectData after) {
    Map<String, ClassData> beforeClasses = before.getClasses();
    Map<String, ClassData> afterClasses = after.getClasses();
    for (Map.Entry<String, ClassData> entry : beforeClasses.entrySet()) {
      String className = entry.getKey();
      ClassData afterClass = afterClasses.get(className);
      if (afterClass != null) {
        compareClasses(entry.getValue(), afterClass);
        afterClasses.remove(className);
      } else {
        myClassesDiff.add(new DeleteDiffElement<ClassData>(className, entry.getValue()));
      }
    }

    for (Map.Entry<String, ClassData> entry : afterClasses.entrySet()) {
      myClassesDiff.add(new AddDiffElement<ClassData>(entry.getKey(), entry.getValue()));
    }
  }


  private void compareClasses(final ClassData before, final ClassData after) {
    LineData[] beforeLines = (LineData[]) before.getLines();
    LineData[] afterLines = (LineData[]) after.getLines();
    if (afterLines == null && beforeLines == null) return;
    if (afterLines == null) {
      myClassesDiff.add(new DeleteDiffElement<ClassData>(before.getName(), before));
      return;
    } else if (beforeLines == null) {
      myClassesDiff.add(new AddDiffElement<ClassData>(after.getName(), after));
      return;
    }
    compare(beforeLines, afterLines, before.getName(), myLinesDiff, new CompareCallback<LineData>() {
      public void apply(LineData beforeLine, LineData afterLine) {
        compareLines(beforeLine, afterLine, before.getName());
      }
    });
  }

  private void compareLines(final LineData beforeLine, final LineData afterLine, final String className) {
    compareWithEmptyDefault(beforeLine.getJumps(), afterLine.getJumps(), new JumpData[0], className, myJumpsDiff, new CompareCallback<JumpData>() {
      public void apply(JumpData beforeJump, JumpData afterJump) {
        if ((beforeJump.getTrueHits() > 0) != (afterJump.getTrueHits() > 0)
            || (beforeJump.getFalseHits() > 0) != (afterJump.getFalseHits() > 0)) {
          myJumpsDiff.add(new ReplaceDiffElement<JumpData>(className, beforeJump, afterJump));
        }
      }
    });

    compareWithEmptyDefault(beforeLine.getSwitches(), afterLine.getSwitches(), new SwitchData[0], className, mySwitchesDiff, new CompareCallback<SwitchData>() {
      public void apply(SwitchData beforeSwitch, SwitchData afterSwitch) {
        boolean same = (beforeSwitch.getDefaultHits() > 0) != (afterSwitch.getDefaultHits() > 0);
        same &= beforeSwitch.getHits().length == afterSwitch.getHits().length;
        for (int i = 0; i < Math.min(beforeSwitch.getHits().length, afterSwitch.getHits().length); i++) {
          same &= (beforeSwitch.getHits()[i] > 0) != (afterSwitch.getHits()[i] > 0);
        }
        if (same) return;
        mySwitchesDiff.add(new ReplaceDiffElement<SwitchData>(className, beforeSwitch, afterSwitch));
      }
    });

    if (beforeLine.getStatus() != afterLine.getStatus()) {
      myLinesDiff.add(new ReplaceDiffElement<LineData>(className, beforeLine, afterLine));
    }
  }


  private interface CompareCallback<T> {
    void apply(T before, T after);
  }

  private static <T> void compareWithEmptyDefault(T[] before, T[] after, T[] empty, String className, List<DiffElement<T>> diff, CompareCallback<T> callback) {
    compare(before == null ? empty : before, after == null ? empty : after, className, diff, callback);
  }

  private static <T> void compare(T[] before, T[] after, String className, List<DiffElement<T>> diff, CompareCallback<T> callback) {
    int i = 0;
    for (; i < Math.min(before.length, after.length); i++) {
      T beforeElement = before[i];
      T afterElement = after[i];
      if (beforeElement == null && afterElement == null) continue;
      if (beforeElement == null) {
        diff.add(new AddDiffElement<T>(className, afterElement));
        continue;
      } else if (afterElement == null) {
        diff.add(new DeleteDiffElement<T>(className, beforeElement));
        continue;
      }
      callback.apply(beforeElement, afterElement);
    }
    for (int j = i; j < before.length; j++) {
      if (before[j] == null) continue;
      diff.add(new DeleteDiffElement<T>(className, before[j]));
    }
    for (int j = i; j < after.length; j++) {
      if (after[j] == null) continue;
      diff.add(new AddDiffElement<T>(className, after[j]));
    }
  }
}
