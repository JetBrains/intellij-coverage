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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Generator {
    public static void main(String[] argv) throws IOException {
        generateLongClass("tests/testData/coverage/longClass/Test.java");
    }

    public static void generateLongClass(String path) throws IOException {
        File longClass = new File(path);
        FileWriter writer = new FileWriter(longClass);
        writer.write("public class Test {\n");
        writer.write("public static void main(String[] argv) {\n");
        writer.write("int v = 0;\n");
        for (int i = 0; i < 32000; i++) {
            writer.write("\n");
        }
        for (int i = 0; i < 1000; i++) {
            writer.write("v++;\n");
            writer.write("v--;\n");
        }
        writer.write("}\n");
        writer.write("}\n");
        writer.close();
    }
} 