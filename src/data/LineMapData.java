package com.intellij.rt.coverage.data;

/**
* User: anna
* Date: 2/9/11
*/
public class LineMapData {
  private int mySourceLineNumber;
  private int myTargetMinLine;
  private int myTargetMaxLine;

  public LineMapData(int mySourceLineNumber, int myTargetMinLine, int myTargetMaxLine) {
    this.mySourceLineNumber = mySourceLineNumber;
    this.myTargetMinLine = myTargetMinLine;
    this.myTargetMaxLine = myTargetMaxLine;
  }

  public int getTargetMinLine() {
    return myTargetMinLine;
  }

  public int getTargetMaxLine() {
    return myTargetMaxLine;
  }

  public int getSourceLineNumber() {
    return mySourceLineNumber;
  }

  public String toString() {
    return "src: " + mySourceLineNumber + ", min: " + myTargetMinLine + ", max: " + myTargetMaxLine;
  }
}
