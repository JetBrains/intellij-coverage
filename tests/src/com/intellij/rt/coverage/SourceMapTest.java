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

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.instrumentation.SaveHook;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Andrey Titov
 * @since 10/11/13.
 */
public class SourceMapTest extends TestCase {
  public void test_write_read_data() throws IOException {
    final File tempFile = File.createTempFile("write_data", "ideacovtest");
    Map init_str_clData_map = createMap(new String[]{"someclass", "filename.java", "class5", "filename2.java", "class2", "filename0.java"});
    SaveHook.doSaveSourceMap(Collections.emptyMap(), tempFile, init_str_clData_map);

    final Map map = SaveHook.loadSourceMapFromFile(Collections.emptyMap(), tempFile);
    assertEquals(init_str_clData_map.size(), map.size());
    checkMapContainsAll(map, init_str_clData_map);
  }

  public void test_write_read_data_many() throws IOException {
    final File tempFile = File.createTempFile("write_data", "ideacovtest");
    Map init_str_clData_map = createRandomMap();
    SaveHook.doSaveSourceMap(Collections.emptyMap(), tempFile, init_str_clData_map);

    final Map map = SaveHook.loadSourceMapFromFile(Collections.emptyMap(), tempFile);
    checkMapContainsAll(map, init_str_clData_map);
  }

  public void test_write_with_old_file() throws IOException {
    final File tempFile = File.createTempFile("write_data", "ideacovtest");
    Map init_str_clData_map = createMap(new String[]{"someclass", "filename.java", "class5", "filename2.java", "class2", "filename0.java"});
    SaveHook.doSaveSourceMap(Collections.emptyMap(), tempFile, init_str_clData_map);

    Map second_str_clData_map = createMap(new String[]{"someclass1", "filename.java1", "class51", "filename2.java1", "class21", "filename0.java1"});

    SaveHook.saveSourceMap(second_str_clData_map, tempFile);

    final Map map = SaveHook.loadSourceMapFromFile(Collections.emptyMap(), tempFile);
    checkMapContainsAll(map, init_str_clData_map);
    checkMapContainsAll(map, second_str_clData_map);
  }

  public void test_duplicate_class_in_old_file() throws IOException {
    final File tempFile = File.createTempFile("write_data", "ideacovtest");
    Map init_str_clData_map = createMap(new String[]{"someclass", "filename.java", "class5", "filename2.java", "class2", "filename0.java"});
    SaveHook.doSaveSourceMap(Collections.emptyMap(), tempFile, init_str_clData_map);

    Map second_str_clData_map = createMap(new String[]{"someclass", "filename.java", "class51", "filename2.java1", "class21", "filename0.java1"});

    SaveHook.saveSourceMap(second_str_clData_map, tempFile);

    final Map map = SaveHook.loadSourceMapFromFile(Collections.emptyMap(), tempFile);
    assertEquals(init_str_clData_map.size() + second_str_clData_map.size() - 1, map.size());
    checkMapContainsAll(map, init_str_clData_map);
    checkMapContainsAll(map, second_str_clData_map);
  }

  public void test_duplicate_class_with_other_source_in_old_file() throws IOException {
    final File tempFile = File.createTempFile("write_data", "ideacovtest");
    Map init_str_clData_map = createMap(new String[]{"someclass", "filename.java"});
    SaveHook.doSaveSourceMap(Collections.emptyMap(), tempFile, init_str_clData_map);

    Map second_str_clData_map = createMap(new String[]{"someclass", "filename.java1"});

    SaveHook.saveSourceMap(second_str_clData_map, tempFile);

    final Map map = SaveHook.loadSourceMapFromFile(Collections.emptyMap(), tempFile);
    assertEquals(1, map.size());
    assertEquals("filename.java", map.get("someclass"));
  }

  public void test_duplicate_class_with_null_source_in_old_file() throws IOException {
    final File tempFile = File.createTempFile("write_data", "ideacovtest");
    Map init_str_clData_map = createMap(new String[]{"someclass", null});
    SaveHook.doSaveSourceMap(Collections.emptyMap(), tempFile, init_str_clData_map);

    Map second_str_clData_map = createMap(new String[]{"someclass", "filename.java1"});

    SaveHook.saveSourceMap(second_str_clData_map, tempFile);

    final Map map = SaveHook.loadSourceMapFromFile(Collections.emptyMap(), tempFile);
    assertEquals(1, map.size());
    assertEquals("filename.java1", map.get("someclass"));
  }

  public void test_duplicate_class_with_both_null_source() throws IOException {
    final File tempFile = File.createTempFile("write_data", "ideacovtest");
    Map init_str_clData_map = createMap(new String[]{"someclass", null});
    SaveHook.doSaveSourceMap(Collections.emptyMap(), tempFile, init_str_clData_map);

    Map second_str_clData_map = createMap(new String[]{"someclass", null});

    SaveHook.saveSourceMap(second_str_clData_map, tempFile);

    final Map map = SaveHook.loadSourceMapFromFile(Collections.emptyMap(), tempFile);
    assertEquals(0, map.size());
  }

  public void test_duplicate_class_with_null_source_in_new_file() throws IOException {
    final File tempFile = File.createTempFile("write_data", "ideacovtest");
    Map init_str_clData_map = createMap(new String[]{"someclass", "filename.java1"});
    SaveHook.doSaveSourceMap(Collections.emptyMap(), tempFile, init_str_clData_map);

    Map second_str_clData_map = createMap(new String[]{"someclass", null});

    SaveHook.saveSourceMap(second_str_clData_map, tempFile);

    final Map map = SaveHook.loadSourceMapFromFile(Collections.emptyMap(), tempFile);
    assertEquals(1, map.size());
    assertEquals("filename.java1", map.get("someclass"));
  }

  private Map createMap(final String[] strings) {
    HashMap map = new HashMap(strings.length / 2);
    for (int i = 0; i < strings.length; i += 2) {
      final ClassData clData = new ClassData(strings[i]);
      clData.setSource(strings[i + 1]);
      map.put(strings[i], clData);
    }
    return map;
  }

  private Map createRandomMap() {
    int limit = 20000;
    String[] strings = new String[limit];
    for (int i = 0; i < limit; ++i) {
      strings[i] = String.valueOf((char)('a' + Math.floor((Math.random() * ('z' - 'a'))))); // one random letter
    }
    return createMap(strings);
  }

  private void checkMapContainsAll(Map str_str_resultMap, Map init_str_clData_map) {
    for (Iterator entry_it = init_str_clData_map.entrySet().iterator(); entry_it.hasNext(); ) {
      Map.Entry str_clData_entry = (Map.Entry) entry_it.next();
      final String clName = (String) str_str_resultMap.get(str_clData_entry.getKey());
      assertNotNull("Class " + str_clData_entry.getKey() + " wasn't found", clName);
      final String initSourceFilename = ((ClassData) str_clData_entry.getValue()).getSource();
      assertEquals("Class's source filename didn't match. init: " + initSourceFilename + ", result: " + clName, initSourceFilename, clName);
    }
  }
}
