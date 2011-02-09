package com.intellij.rt.coverage.data;

/**
 * User: anna
 * Date: 2/9/11
 */
public class FileMapData {
  private String myClassName;
  private LineMapData[] myLines;

  public FileMapData(String className, LineMapData[] lines) {
    myClassName = className;
    myLines = lines;
  }

  public String getClassName() {
    return myClassName;
  }

  public LineMapData[] getLines() {
    return myLines;
  }

  public String toString() {
    String toString = "";
    for (int i = 0, myLinesLength = myLines.length; i < myLinesLength; i++) {
      LineMapData line = myLines[i];
      if (line != null) {
        toString += "\n" + line.toString();
      }
    }
    return "class name: " + myClassName + "\nlines:" + toString;
  }
}
