/*
 * User: anna
 * Date: 26-Feb-2010
 */
package com.intellij.rt.coverage.util;

import com.intellij.rt.coverage.data.LineData;
import gnu.trove.TIntObjectHashMap;

public class LinesUtil {
  public static LineData[] calcLineArray(final int maxLineNumber, final TIntObjectHashMap lines) {
    final LineData[] linesArray = new LineData[maxLineNumber + 1];
    for(int line = 1; line <= maxLineNumber; line++) {
      final LineData lineData = (LineData) lines.get(line);
      if (lineData != null) {
        lineData.fillArrays();
      }
      linesArray[line] = lineData;
    }
    return linesArray;
  }
}