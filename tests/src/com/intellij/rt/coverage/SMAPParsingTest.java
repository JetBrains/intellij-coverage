/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package com.intellij.rt.coverage;

import com.intellij.rt.coverage.data.FileMapData;
import com.intellij.rt.coverage.data.LineMapData;
import com.intellij.rt.coverage.instrumentation.JSR45Util;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class SMAPParsingTest extends TestCase {
  private static final String KOTLIN_SMAP = "SMAP\n" +
      "StoreAwareProjectManager.kt\n" +
      "Kotlin\n" +
      "*S Kotlin\n" +
      "*F\n" +
      "+ 1 StoreAwareProjectManager.kt\n" +
      "com/intellij/configurationStore/StoreAwareProjectManager\n" +
      "+ 2 Standard.kt\n" +
      "kotlin/KotlinPackage/Standard\n" +
      "+ 3 JLangJVM.kt\n" +
      "kotlin/KotlinPackage/JLangJVM\n" +
      "*L\n" +
      "1#1,296:1\n" +
      "24#2:297\n" +
      "45#3,6:298\n" +
      "*E";

  private static final String KOTLIN_SMAP_WITH_INLINE = "SMAP\n" +
      "test.kt\n" +
      "Kotlin\n" +
      "*S Kotlin\n" +
      "*F\n" +
      "+ 1 test.kt\n" +
      "one/TestKt\n" +
      "+ 2 inline.kt\n" +
      "one/coverageinline/InlineKt\n" +
      "*L\n" +
      "1#1,10:1\n" +
      "8#2,6:11\n" +
      "8#2,6:17\n" +
      "*E\n" +
      "*S KotlinDebug\n" +
      "*F\n" +
      "+ 1 test.kt\n" +
      "one/TestKt\n" +
      "*L\n" +
      "8#1,6:11\n" +
      "9#1,6:17\n" +
      "*E";

  /** test-kotlin/src/testData/inline/simpleInline.kt */
  private static final String KOTLIN_SMAP_WITH_INLINE2 = "SMAP\n" +
      "simpleInline.kt\n" +
      "Kotlin\n" +
      "*S Kotlin\n" +
      "*F\n" +
      "+ 1 simpleInline.kt\n" +
      "testData/inline/SimpleInlineKt\n" +
      "*L\n" +
      "1#1,4:1\n" +
      "3#1:5\n" +
      "*E\n" +
      "*S KotlinDebug\n" +
      "*F\n" +
      "+ 1 simpleInline.kt\n" +
      "testData/inline/SimpleInlineKt\n" +
      "*L\n" +
      "4#1:5\n" +
      "*E";

  private static final String KOTLIN_SMAP_WITH_INLINE_WITHOUT_END_SECTION = "SMAP\n" +
      "test.kt\n" +
      "Kotlin\n" +
      "*S Kotlin\n" +
      "*F\n" +
      "+ 1 test.kt\n" +
      "one/TestKt\n" +
      "+ 2 inline.kt\n" +
      "one/coverageinline/InlineKt\n" +
      "*L\n" +
      "1#1,10:1\n" +
      "8#2,6:11\n" +
      "8#2,6:17\n" +
      // no *E here
      "*S KotlinDebug\n" +
      "*F\n" +
      "+ 1 test.kt\n" +
      "one/TestKt\n" +
      "*L\n" +
      "8#1,6:11\n" +
      "9#1,6:17\n" +
      "*E";

  private static final String JSP_SMAP = "SMAP\n" +
      "Hello_jsp.java\n" +
      "JSP\n" +
      "*S JSP\n" +
      "*F\n" +
      "+ 1 Hello.jsp\n" +
      "Hello.jsp\n" +
      "+ 2 greeting.jsp\n" +
      "greeting.jsp\n" +
      "*L\n" +
      "1,5:59\n" +
      "1#2,2:64\n" +
      "3:66,4\n" +
      "6#1,3:70\n" +
      "*E";

  private static final String JSP_SMAP_WITH_SUBDIR = "SMAP\n" +
      "index_jsp.java\n" +
      "JSP\n" +
      "*S JSP\n" +
      "*F\n" +
      "+ 0 index.jsp\n" +
      "index.jsp\n" +
      "+ 1 greeting.jsp\n" +
      "subDir/greeting.jsp\n" +
      "*L\n" +
      "7,9:68\n" +
      "1#1,9:76\n" +
      "10:85,3\n" +
      "11,4:88\n" +
      "15#0,5:91\n" +
      "*E";

  public void testJspSMAPKotlinInline() {
    final FileMapData[] expected = new FileMapData[]{new FileMapData("testData.inline.SimpleInlineKt", "simpleInline.kt",
        new LineMapData[]{
            new LineMapData(1, 1, 1),
            new LineMapData(2, 2, 2),
            new LineMapData(3, 3, 3),
            new LineMapData(4, 4, 4),
            new LineMapData(3, 5, 5)
        })};
    final FileMapData[] datas = JSR45Util.extractLineMapping(KOTLIN_SMAP_WITH_INLINE2, "testData.inline.SimpleInlineKt");
    testJspSmapData(expected, datas);
  }

  public void testJspSMAP() {
    final FileMapData[] expected = new FileMapData[]{new FileMapData("org.apache.jsp.Hello_jsp", "hello.jsp",
        new LineMapData[]{
            new LineMapData(1, 59, 59),
            new LineMapData(2, 60, 60),
            new LineMapData(3, 61, 61),
            new LineMapData(4, 62, 62),
            new LineMapData(5, 63, 63),
            new LineMapData(6, 70, 70),
            new LineMapData(7, 71, 71),
            new LineMapData(8, 72, 72),
        }),
        new FileMapData("org.apache.jsp.greeting_jsp", "greeting.jsp",
            new LineMapData[]{
                new LineMapData(1, 64, 64),
                new LineMapData(2, 65, 65),
                new LineMapData(3, 66, 69)})};

    final FileMapData[] datas = JSR45Util.extractLineMapping(JSP_SMAP, "org.apache.jsp.Hello_jsp");
    testJspSmapData(expected, datas);
  }

  private void testJspSmapData(FileMapData[] expected, FileMapData[] actual) {
    Assert.assertNotNull(actual);
    Assert.assertEquals(expected.length, actual.length);
    for (int i = 0; i < actual.length; i++) {
      final FileMapData data = actual[i];
      final FileMapData expectedData = expected[i];
      Assert.assertEquals(data.toString(), expectedData.toString(), data.toString());
    }
  }

  public void testJspInSubdirSMAP() {
    doTestClassNames(JSP_SMAP_WITH_SUBDIR,
        "org.apache.jsp.index_jsp org.apache.jsp.subDir.greeting_jsp",
        "org.apache.jsp.Hello_jsp");
  }

  public void testKotlinSMAP() {
    doTestClassNames(KOTLIN_SMAP,
        "com.intellij.configurationStore.StoreAwareProjectManager kotlin.KotlinPackage.Standard kotlin.KotlinPackage.JLangJVM",
        "StoreAwareProjectManager");
  }

  public void testKotlinWithInlineSMAP() {
    doTestClassNames(KOTLIN_SMAP_WITH_INLINE,
        "one.TestKt one.coverageinline.InlineKt",
        "one.Test");
  }

  public void testKotlinWithInlineSMAPWithoutEndSection() {
    doTestClassNames(KOTLIN_SMAP_WITH_INLINE_WITHOUT_END_SECTION,
        "one.TestKt one.coverageinline.InlineKt",
        "one.Test");
  }

  private void doTestClassNames(String test, String expectedMergedString, String mainClassname) {
    FileMapData[] fileMapData = JSR45Util.extractLineMapping(test, mainClassname);
    assertNotNull(fileMapData);
    StringBuilder classNames = new StringBuilder();
    for (FileMapData fileData : fileMapData) {
      classNames.append(" ").append(fileData.getClassName());
    }
    Assert.assertEquals(expectedMergedString, classNames.toString().trim());
  }

  public void testRelativePath() {
    final String fileName1 = "view/../greeting.jsp";
    Assert.assertEquals("greeting.jsp", JSR45Util.processRelative(fileName1));

    final String fileName2 = "view/greeting.jsp";
    Assert.assertEquals("view/greeting.jsp", JSR45Util.processRelative(fileName2));

    final String fileName3 = "greeting.jsp";
    Assert.assertEquals("greeting.jsp", JSR45Util.processRelative(fileName3));

  }

  public void testKotlinSourceFiles() {
    assertThat(JSR45Util.parseSourcePaths(KOTLIN_SMAP)).isEqualTo(Arrays.asList(
        "com/intellij/configurationStore/StoreAwareProjectManager.kt",
        "kotlin/KotlinPackage/Standard.kt",
        "kotlin/KotlinPackage/JLangJVM.kt"
    ));
    assertThat(JSR45Util.parseSourcePaths(KOTLIN_SMAP_WITH_INLINE)).isEqualTo(Arrays.asList(
        "one/test.kt",
        "one/coverageinline/inline.kt"
    ));
  }

  public void testJspSourceFiles() {
    assertThat(JSR45Util.parseSourcePaths(JSP_SMAP)).isEqualTo(Arrays.asList(
        "Hello.jsp",
        "greeting.jsp"
    ));
    assertThat(JSR45Util.parseSourcePaths(JSP_SMAP_WITH_SUBDIR)).isEqualTo(Arrays.asList(
        "index.jsp",
        "subDir/greeting.jsp"
    ));
  }
}
