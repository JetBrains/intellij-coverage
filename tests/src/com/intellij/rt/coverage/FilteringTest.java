package com.intellij.rt.coverage;

import com.intellij.rt.coverage.util.ClassNameUtil;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Andrei Titov
 * Date: 5/14/13
 */
public class FilteringTest extends TestCase {
  public void test_excludes() throws Exception {
    doTestExcludeFilter("com.product.AAA", toPatterns(new String[]{"AAA"}), false);
    doTestExcludeFilter("com.product.AAA$1", toPatterns(new String[]{"AAA"}), false);
    doTestExcludeFilter("com.product.AAA", toPatterns(new String[]{"com.product.AAA"}), true);
    doTestExcludeFilter("com.product.AAA", toPatterns(new String[]{".*AAA"}), true);
    doTestExcludeFilter("com.product.AAA$1", toPatterns(new String[]{".*AAA"}), true);
    doTestExcludeFilter("com.product.AAA$QQQ", toPatterns(new String[]{".*AAA"}), true);
    doTestExcludeFilter("com.product.AAA$QQQ$1", toPatterns(new String[]{".*AAA"}), true);

    // This case is not so obvious. But without this we will not be able to exclude "AAA$1" class with ".*AAA" pattern
    doTestExcludeFilter("com.product.QQQ$AAA", toPatterns(new String[]{".*AAA"}), false);

    doTestExcludeFilter("com.product.AAA", toPatterns(new String[]{".*AAA$1"}), false);
    doTestExcludeFilter("com.product.AAA$1", toPatterns(new String[]{".*AAA$1"}), true);
    doTestExcludeFilter("com.product.AAA", toPatterns(new String[]{".*$.*"}), false);
    doTestExcludeFilter("com.product.AAA$1", toPatterns(new String[]{".*$.*"}), true);
    doTestExcludeFilter("com.product.AAA$AAA", toPatterns(new String[]{".*$.*"}), true);
    doTestExcludeFilter("com.product.AAA$QQQ", toPatterns(new String[]{".*$.*"}), true);
    doTestExcludeFilter("com.product.AAA$QQQ$1", toPatterns(new String[]{".*$.*"}), true);
  }

  private void doTestExcludeFilter(String className, List excludePatterns, boolean expected) throws Exception {
    assertEquals(expected, ClassNameUtil.shouldExclude(className, excludePatterns));
  }

  private List toPatterns(String regexs[]) {
    List res = new ArrayList(regexs.length);
    for (int i = 0; i < regexs.length; ++i) {
      res.add(ClassNameUtil.makePattern(regexs[i]));
    }
    return res;
  }
}
