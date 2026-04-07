/*
 * Copyright 2000-2026 JetBrains s.r.o.
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

package com.intellij.rt.coverage.report;

import com.intellij.rt.coverage.data.BranchData;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.data.instructions.ClassInstructions;
import com.intellij.rt.coverage.data.instructions.LineInstructions;
import com.intellij.rt.coverage.report.api.Filters;
import com.intellij.rt.coverage.report.data.BinaryReport;
import com.intellij.rt.coverage.util.ArrayUtil;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * Loads binary coverage reports and prints human-readable text statistics directly
 * from {@link ProjectData}, bypassing XML serialization.
 *
 * <p>Usage: {@code java TextCoverageStatistics <report.ic> [output-root ...]}
 */
public class TextCoverageStatistics {

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Usage: TextCoverageStatistics <report.ic> [output-root ...]");
      System.exit(1);
    }

    File reportFile = new File(args[0]);
    if (!reportFile.exists()) {
      System.err.println("File not found: " + reportFile);
      System.exit(1);
    }

    List<File> outputRoots = new ArrayList<File>();
    for (int i = 1; i < args.length; i++) {
      outputRoots.add(new File(args[i]));
    }

    List<BinaryReport> reports = Collections.singletonList(new BinaryReport(reportFile, null));
    ReportLoadStrategy loadStrategy = new ReportLoadStrategy.RawReportLoadStrategy(
        reports, outputRoots, Collections.<File>emptyList(), Filters.EMPTY);
    ProjectData projectData = loadStrategy.getProjectData();

    printStatistics(projectData, System.out);
  }

  public static void printStatistics(ProjectData project, PrintStream out) {
    HashMap<String, List<ClassData>> packages = XMLCoverageReport.mapClassesToPackages(project, true);
    if (packages.isEmpty()) {
      out.println("No coverage data found.");
      return;
    }

    Map<String, int[]> packageCounters = new TreeMap<String, int[]>();
    List<int[]> classRows = new ArrayList<int[]>();
    final List<String> classNames = new ArrayList<String>();
    final List<ClassData> allClassData = new ArrayList<ClassData>();
    int[] totals = new int[10]; // lines, covLines, branches, covBranches, methods, covMethods, instr, covInstr, classes, covClasses

    for (Map.Entry<String, List<ClassData>> entry : packages.entrySet()) {
      String pkg = entry.getKey();
      int[] pkgCounters = new int[8]; // lines, covLines, branches, covBranches, methods, covMethods, instr, covInstr

      for (ClassData classData : entry.getValue()) {
        int[] cls = computeClassCounters(project, classData);
        classNames.add(classData.getName());
        classRows.add(cls);
        allClassData.add(classData);

        for (int i = 0; i < 8; i++) pkgCounters[i] += cls[i];

        totals[8]++; // totalClasses
        if (cls[5] > 0) totals[9]++; // coveredClasses (has covered methods)
      }

      packageCounters.put(pkg, pkgCounters);
      for (int i = 0; i < 8; i++) totals[i] += pkgCounters[i];
    }

    // Overall summary
    out.println("=== Coverage Summary ===");
    out.println();
    printCounter(out, "Instructions", totals[7], totals[6]);
    printCounter(out, "Branches    ", totals[3], totals[2]);
    printCounter(out, "Lines       ", totals[1], totals[0]);
    printCounter(out, "Methods     ", totals[5], totals[4]);
    printCounter(out, "Classes     ", totals[9], totals[8]);
    out.println();

    // Per-package breakdown
    out.println("=== Per-Package Coverage ===");
    out.println();
    out.printf("%-50s %8s %8s %8s %8s%n", "Package", "Lines", "Line%", "Branch", "Branch%");
    out.println(repeat('-', 82));
    for (Map.Entry<String, int[]> entry : packageCounters.entrySet()) {
      int[] c = entry.getValue();
      out.printf("%-50s %4d/%-3d %7s %4d/%-3d %7s%n",
          truncate(entry.getKey(), 50),
          c[1], c[0], percent(c[1], c[0]),
          c[3], c[2], percent(c[3], c[2]));
    }
    out.println();

    // Per-class details
    out.println("=== Per-Class Coverage ===");
    out.println();
    out.printf("%-60s %8s %8s %8s %8s%n", "Class", "Lines", "Line%", "Methods", "Meth%");
    out.println(repeat('-', 92));

    Integer[] indices = new Integer[classNames.size()];
    for (int i = 0; i < indices.length; i++) indices[i] = i;
    Arrays.sort(indices, new Comparator<Integer>() {
      public int compare(Integer a, Integer b) {
        return classNames.get(a).compareTo(classNames.get(b));
      }
    });
    for (int idx : indices) {
      int[] c = classRows.get(idx);
      out.printf("%-60s %4d/%-3d %7s %4d/%-3d %7s%n",
          truncate(classNames.get(idx), 60),
          c[1], c[0], percent(c[1], c[0]),
          c[5], c[4], percent(c[5], c[4]));
    }

    // Per-class line hit counts
    out.println();
    out.println("=== Line Hit Counts ===");
    for (int idx : indices) {
      ClassData classData = allClassData.get(idx);
      Object[] lines = classData.getLines();
      if (lines == null) continue;

      boolean hasLines = false;
      for (Object line : lines) {
        if (line instanceof LineData) {
          hasLines = true;
          break;
        }
      }
      if (!hasLines) continue;

      out.println();
      out.printf("--- %s ---%n", classNames.get(idx));
      out.printf("  %-6s %8s  %s%n", "Line", "Hits", "Branch");
      for (Object line : lines) {
        if (!(line instanceof LineData)) continue;
        LineData ld = (LineData) line;
        BranchData bd = ld.getBranchData();
        String branchInfo = "";
        if (bd != null) {
          branchInfo = bd.getCoveredBranches() + "/" + bd.getTotalBranches();
        }
        out.printf("  %-6d %8d  %s%n", ld.getLineNumber(), ld.getHits(), branchInfo);
      }
    }
  }

  /**
   * Computes coverage counters for a single class directly from ProjectData.
   * Returns [totalLines, coveredLines, totalBranches, coveredBranches,
   *          totalMethods, coveredMethods, totalInstructions, coveredInstructions].
   * Uses the same logic as {@link XMLCoverageReport}.
   */
  private static int[] computeClassCounters(ProjectData project, ClassData classData) {
    int totalLines = 0, coveredLines = 0;
    int totalBranches = 0, coveredBranches = 0;
    int totalMethods = 0, coveredMethods = 0;
    int totalInstructions = 0, coveredInstructions = 0;

    ClassInstructions classInstructions = project.getInstructions().get(classData.getName());
    LineInstructions[] instructions = classInstructions == null ? null : classInstructions.getlines();

    Map<String, List<LineData>> methods = classData.mapLinesToMethods();
    for (Map.Entry<String, List<LineData>> methodEntry : methods.entrySet()) {
      int mLines = 0, mCovLines = 0;
      int mBranches = 0, mCovBranches = 0;
      int mInstr = 0, mCovInstr = 0;

      for (LineData lineData : methodEntry.getValue()) {
        if (lineData == null) continue;
        mLines++;
        if (lineData.getHits() > 0) mCovLines++;

        BranchData branchData = lineData.getBranchData();
        if (branchData != null) {
          mBranches += branchData.getTotalBranches();
          mCovBranches += branchData.getCoveredBranches();
        }

        LineInstructions lineInstr = ArrayUtil.safeLoad(instructions, lineData.getLineNumber());
        if (lineInstr != null) {
          BranchData instrData = lineInstr.getInstructionsData(lineData);
          mInstr += instrData.getTotalBranches();
          mCovInstr += instrData.getCoveredBranches();
        } else {
          mInstr++;
          if (lineData.getHits() > 0) mCovInstr++;
        }
      }

      totalMethods++;
      if (mCovLines > 0) coveredMethods++;
      totalLines += mLines;
      coveredLines += mCovLines;
      totalBranches += mBranches;
      coveredBranches += mCovBranches;
      totalInstructions += mInstr;
      coveredInstructions += mCovInstr;
    }

    return new int[]{totalLines, coveredLines, totalBranches, coveredBranches,
        totalMethods, coveredMethods, totalInstructions, coveredInstructions};
  }

  private static void printCounter(PrintStream out, String label, int covered, int total) {
    out.printf("  %s: %d/%d %s%n", label, covered, total, percent(covered, total));
  }

  private static String percent(int covered, int total) {
    if (total == 0) return "  n/a";
    return String.format("%5.1f%%", 100.0 * covered / total);
  }

  private static String truncate(String s, int maxLen) {
    if (s.length() <= maxLen) return s;
    return s.substring(0, maxLen - 3) + "...";
  }

  private static String repeat(char c, int count) {
    char[] chars = new char[count];
    Arrays.fill(chars, c);
    return new String(chars);
  }
}
