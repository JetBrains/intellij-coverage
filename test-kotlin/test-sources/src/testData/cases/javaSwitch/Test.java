/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package testData.cases.javaSwitch;

// instructions & branches

public class Test { // coverage: FULL // stats: 2/2

  void singleBranchSwitch1(int x) {
    switch (x) { // coverage: PARTIAL // branches: 1/2 // stats: 2/2
      case 1: {
        System.out.println("Case 1"); // coverage: FULL // stats: 3/3
        break;
      }
      case 2: {
        System.out.println("Case 2"); // coverage: NONE // stats: 0/3
        break;
      }
    }
  }

  void singleBranchSwitch2(int x) {
    switch (x) { // coverage: PARTIAL // branches: 1/2 // stats: 2/2
      case 1: {
        System.out.println("Case 1"); // coverage: NONE // stats: 0/3
        break;
      }
      case 2: {
        System.out.println("Case 2"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  void defaultBranchSwitch(int x) {
    switch (x) { // coverage: PARTIAL // branches: 0/2 // stats: 2/2
      case 1: {
        System.out.println("Case 1"); // coverage: NONE // stats: 0/3
        break;
      }
      case 2: {
        System.out.println("Case 2"); // coverage: NONE // stats: 0/3
        break;
      }
      default: {
        System.out.println("Default"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  void fullyCoveredSwitch(int x) {
    switch (x) { // coverage: PARTIAL // branches: 2/2 // stats: 2/2
      case 1: {
        System.out.println("Case 1"); // coverage: FULL // stats: 3/3
        break;
      }
      case 2: {
        System.out.println("Case 2"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  void fullyCoveredSwitchWithDefault(int x) {
    switch (x) { // coverage: FULL // branches: 2/2 // stats: 2/2
      case 1: {
        System.out.println("Case 1"); // coverage: FULL // stats: 3/3
        break;
      }
      case 2: {
        System.out.println("Case 2"); // coverage: FULL // stats: 3/3
        break;
      }
      default: {
        System.out.println("Default"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  void fullyCoveredSwitchWithoutDefault(int x) {
    switch (x) { // coverage: PARTIAL // branches: 2/2 // stats: 2/2
      case 1: {
        System.out.println("Case 1"); // coverage: FULL // stats: 3/3
        break;
      }
      case 2: {
        System.out.println("Case 2"); // coverage: FULL // stats: 3/3
        break;
      }
      default: {
        System.out.println("Default"); // coverage: NONE // stats: 0/3
        break;
      }
    }
  }

  void switchWithFallThrough(int x) {
    switch (x) { // coverage: PARTIAL // branches: 1/2 // stats: 2/2
      case 1: {
        System.out.println("Case 1"); // coverage: FULL // stats: 3/3
      }
      case 2: {
        System.out.println("Case 2"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  void stringSwitch(String s) {
    switch (s) { // coverage: PARTIAL // branches: 1/7 // stats: 29/29
      case "A": {
        System.out.println("Case A"); // coverage: FULL // stats: 3/3
        break;
      }
      case "B": {
        System.out.println("Case B"); // coverage: NONE // stats: 0/3
        break;
      }
      case "C": {
        System.out.println("Case C"); // coverage: NONE // stats: 0/3
        break;
      }
      case "D": {
        System.out.println("Case D"); // coverage: NONE // stats: 0/3
        break;
      }
      case "E": {
        System.out.println("Case E"); // coverage: NONE // stats: 0/3
        break;
      }
      case "F": {
        System.out.println("Case F"); // coverage: NONE // stats: 0/3
        break;
      }
      case "G": {
        System.out.println("Case G"); // coverage: NONE // stats: 0/3
        break;
      }
    }
  }

  void fullStringSwitch(String s) {
    switch (s) { // coverage: FULL // branches: 2/2 // stats: 14/14
      case "A": {
        System.out.println("Case A"); // coverage: FULL // stats: 3/3
        break;
      }
      case "B": {
        System.out.println("Case B"); // coverage: FULL // stats: 3/3
        break;
      }
      default: {
        System.out.println("Default"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }

  void stringSwitchSameHashCode(String s) {
    switch (s) { // coverage: FULL // branches: 2/2 // stats: 18/18
      case "Aa": {
        System.out.println("Case A"); // coverage: FULL // stats: 3/3
        break;
      }
      case "BB": {
        System.out.println("Case B"); // coverage: FULL // stats: 3/3
        break;
      }
      default: {
        System.out.println("Default"); // coverage: FULL // stats: 3/3
        break;
      }
    }
  }


  public static void main(String[] args) {
    Test switches = new Test(); // coverage: FULL // stats: 4/4

    switches.singleBranchSwitch1(1); // coverage: FULL // stats: 3/3
    switches.singleBranchSwitch2(2); // coverage: FULL // stats: 3/3
    switches.defaultBranchSwitch(3); // coverage: FULL // stats: 3/3

    switches.fullyCoveredSwitch(1); // coverage: FULL // stats: 3/3
    switches.fullyCoveredSwitch(2); // coverage: FULL // stats: 3/3

    switches.fullyCoveredSwitchWithDefault(1); // coverage: FULL // stats: 3/3
    switches.fullyCoveredSwitchWithDefault(2); // coverage: FULL // stats: 3/3
    switches.fullyCoveredSwitchWithDefault(3); // coverage: FULL // stats: 3/3

    switches.fullyCoveredSwitchWithoutDefault(1); // coverage: FULL // stats: 3/3
    switches.fullyCoveredSwitchWithoutDefault(2); // coverage: FULL // stats: 3/3

    switches.switchWithFallThrough(1); // coverage: FULL // stats: 3/3

    switches.stringSwitch("A"); // coverage: FULL // stats: 3/3
    switches.fullStringSwitch("A"); // coverage: FULL // stats: 3/3
    switches.fullStringSwitch("B"); // coverage: FULL // stats: 3/3
    switches.fullStringSwitch("C"); // coverage: FULL // stats: 3/3

    switches.stringSwitchSameHashCode("Aa"); // coverage: FULL // stats: 3/3
    switches.stringSwitchSameHashCode("BB"); // coverage: FULL // stats: 3/3
    switches.stringSwitchSameHashCode("C"); // coverage: FULL // stats: 3/3
  }
}
