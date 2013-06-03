package com.intellij.rt.coverage;

import com.intellij.rt.coverage.util.ClassNameUtil;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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

    // allowing to exclude inner class without explicit $ sign
    doTestExcludeFilter("com.product.QQQ$AAA", toPatterns(new String[]{".*AAA"}), true);
    doTestExcludeFilter("com.product.QQQ$$$AAA$AAA", toPatterns(new String[]{".*AAA"}), true);
    doTestExcludeFilter("com.product.QQQ$AAB", toPatterns(new String[]{".*AAA"}), false);
    doTestExcludeFilter("com.product.QQQ$$$AAB$AAA", toPatterns(new String[]{".*AAA"}), true);
    doTestExcludeFilter("com.product.QQQ$$$AAA$AAB", toPatterns(new String[]{".*AAA"}), true);
    doTestExcludeFilter("com.product.QQQ", toPatterns(new String[]{".*AAA"}), false);

    doTestExcludeFilter("com.product.AAA", toPatterns(new String[]{".*AAA\\$1"}), false);
    doTestExcludeFilter("com.product.AAA$1", toPatterns(new String[]{".*AAA\\$1"}), true);
    doTestExcludeFilter("com.product.AAA", toPatterns(new String[]{".*\\$.*"}), false);
    doTestExcludeFilter("com.product.AAA$1", toPatterns(new String[]{".*\\$.*"}), true);
    doTestExcludeFilter("com.product.AAA$AAA", toPatterns(new String[]{".*\\$.*"}), true);
    doTestExcludeFilter("com.product.AAA$QQQ", toPatterns(new String[]{".*\\$.*"}), true);
    doTestExcludeFilter("com.product.AAA$QQQ$1", toPatterns(new String[]{".*\\$.*"}), true);
  }

  private void doTestExcludeFilter(String className, List excludePatterns, boolean expected) throws Exception {
    assertEquals(expected, ClassNameUtil.shouldExclude(className, excludePatterns));
  }

  private List toPatterns(String regexs[]) {
    List res = new ArrayList(regexs.length);
    for (int i = 0; i < regexs.length; ++i) {
      res.add(Pattern.compile(regexs[i] + "(\\$.*)?"));
    }
    return res;
  }
}
