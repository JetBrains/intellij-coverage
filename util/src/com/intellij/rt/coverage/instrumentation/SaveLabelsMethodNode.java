/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation;

import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.tree.LabelNode;
import org.jetbrains.coverage.org.objectweb.asm.tree.MethodNode;

/** With this MethodNode implementation MethodNode and a class using it work with the same Labels. */
public class SaveLabelsMethodNode extends MethodNode {
  public SaveLabelsMethodNode(int access, String name, String desc, String signature, String[] exceptions) {
    super(Opcodes.API_VERSION, access, name, desc, signature, exceptions);
  }

  /**
   *  Unlike super implementation here label is set to new LabelNode, so new Label will not be created.
   */
  @Override
  protected LabelNode getLabelNode(Label label) {
    if (!(label.info instanceof LabelNode)) {
      label.info = new LabelNode(label);
    }
    return (LabelNode)label.info;
  }
}
