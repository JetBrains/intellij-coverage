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
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author anna
 * @since 2/9/11
 */
public class SMAPParsingTest extends TestCase {

  public void test1() {
    final String test =
            "SMAP\n" +
            "Hello_jsp.java\n" +
            "JSP\n" +
            "*S JSP\n" +
            "*F\n" +
            "+ 0 Hello.jsp\n" +
            "Hello.jsp\n" +
            "+ 1 greeting.jsp\n" +
            "greeting.jsp\n" +
            "*L\n" +
            "1,5:59\n" +
            "1#1,2:64\n" +
            "3:66,4\n" +
            "6#0,3:70\n" +
            "*E";
    final FileMapData[] expected = new FileMapData[]{new FileMapData("org.apache.jsp.Hello_jsp",
        new LineMapData[] {
            new LineMapData(1, 59, 59),
            new LineMapData(2, 60, 60),
            new LineMapData(3, 61, 61),
            new LineMapData(4, 62, 62),
            new LineMapData(5, 63, 63),
            new LineMapData(6, 70, 70),
            new LineMapData(7, 71, 71),
            new LineMapData(8, 72, 72),
        }),
    new FileMapData("org.apache.jsp.greeting_jsp",
        new LineMapData[]{
            new LineMapData(1, 64, 64),
            new LineMapData(2, 65, 65),
            new LineMapData(3, 66, 69)})};

    final FileMapData[] datas = JSR45Util.extractLineMapping(test, "org.apache.jsp.Hello_jsp");
    Assert.assertEquals(expected.length, datas.length);
    for (int i = 0; i < datas.length; i++) {
      final FileMapData data = datas[i];
      final FileMapData expectedData = expected[i];
      Assert.assertEquals(data.toString(), expectedData.toString(), data.toString());
    }
  }

  public void testRelativePath() {
    final String fileName1 = "view/../greeting.jsp";
    Assert.assertEquals("greeting.jsp", JSR45Util.processRelative(fileName1));

    final String fileName2 = "view/greeting.jsp";
    Assert.assertEquals("view/greeting.jsp", JSR45Util.processRelative(fileName2));

    final String fileName3 = "greeting.jsp";
    Assert.assertEquals("greeting.jsp", JSR45Util.processRelative(fileName3));

  }

}
