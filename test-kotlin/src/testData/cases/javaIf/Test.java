/*
 * Copyright 2000-2024 JetBrains s.r.o.
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

package testData.cases.javaIf;

// instructions & branches

public class Test {  // coverage: FULL // stats: 2/2
  void oneBranch1(int x) {
    if (x == 1) {  // coverage: PARTIAL // stats: 3/3 1/2
      System.out.println("case 1");  // coverage: FULL // stats: 4/4
    } else {
      System.out.println("case 2");  // coverage: NONE // stats: 0/3
    }
  }

  void oneBranch2(int x) {
    if (x == 1) {  // coverage: PARTIAL // stats: 3/3 1/2
      System.out.println("case 1");  // coverage: NONE // stats: 0/4
    } else {
      System.out.println("case 2");  // coverage: FULL // stats: 3/3
    }
  }

  void allBranches(int x) {
    if (x == 1) {  // coverage: FULL // stats: 3/3 2/2
      System.out.println("case 1");  // coverage: FULL // stats: 4/4
    } else {
      System.out.println("case 2");  // coverage: FULL // stats: 3/3
    }
  }

  void singleBranch1(int x) {
    if (x == 1) {  // coverage: PARTIAL // stats: 3/3 1/2
      System.out.println("case 1");  // coverage: FULL // stats: 3/3
    }
    System.out.println("case 2");  // coverage: FULL // stats: 3/3
  }

  void singleBranch2(int x) {
    if (x == 1) {  // coverage: PARTIAL // stats: 3/3 1/2
      System.out.println("case 1");  // coverage: NONE // stats: 0/3
    }
    System.out.println("case 2");  // coverage: FULL // stats: 3/3
  }

  void empty(int x) {
    if (x == 1) {  // coverage: NONE // stats: 0/3 0/2
      System.out.println("case 1");  // coverage: NONE // stats: 0/4
    } else {
      System.out.println("case 2");  // coverage: NONE // stats: 0/3
    }
  }

  void and1(boolean a, boolean b) {
    if (a && b) {  // coverage: PARTIAL // stats: 4/4 2/4
      System.out.println("both a and b are true");  // coverage: NONE // stats: 0/4
    } else {
      System.out.println("either a or b is false");  // coverage: FULL // stats: 3/3
    }
  }

  void and2(boolean a, boolean b) {
    if (a && b) {  // coverage: PARTIAL // stats: 2/4 1/4
      System.out.println("both a and b are true");  // coverage: NONE // stats: 0/4
    } else {
      System.out.println("either a or b is false");  // coverage: FULL // stats: 3/3
    }
  }

  void and3(boolean a, boolean b) {
    if (a && b) {  // coverage: PARTIAL // stats: 4/4 2/4
      System.out.println("both a and b are true");  // coverage: FULL // stats: 4/4
    } else {
      System.out.println("either a or b is false");  // coverage: NONE // stats: 0/3
    }
  }

  void fullAnd(boolean a, boolean b) {
    if (a && b) {  // coverage: FULL // stats: 4/4 4/4
      System.out.println("both a and b are true");  // coverage: FULL // stats: 4/4
    } else {
      System.out.println("either a or b is false");  // coverage: FULL // stats: 3/3
    }
  }

  void andAnd0(boolean a, boolean b, boolean c) {
    if (a && b && c) { // coverage: PARTIAL // stats: 2/6 1/6
      System.out.println("All true"); // coverage: NONE // stats: 0/4
    } else {
      System.out.println("Some one is false"); // coverage: FULL // stats: 3/3
    }
  }

  void andAnd1(boolean a, boolean b, boolean c) {
    if (a && b && c) { // coverage: PARTIAL // stats: 4/6 3/6
      System.out.println("All true"); // coverage: NONE // stats: 0/4
    } else {
      System.out.println("Some one is false"); // coverage: FULL // stats: 3/3
    }
  }

  void andAnd2(boolean a, boolean b, boolean c) {
    if (a && b && c) { // coverage: PARTIAL // stats: 6/6 5/6
      System.out.println("All true"); // coverage: NONE // stats: 0/4
    } else {
      System.out.println("Some one is false"); // coverage: FULL // stats: 3/3
    }
  }

  void andAnd3(boolean a, boolean b, boolean c) {
    if (a && b && c) { // coverage: FULL // stats: 6/6 6/6
      System.out.println("All true"); // coverage: FULL // stats: 4/4
    } else {
      System.out.println("Some one is false"); // coverage: FULL // stats: 3/3
    }
  }


  void or1(boolean a, boolean b) {
    if (a || b) {  // coverage: PARTIAL // stats: 2/4 1/4
      System.out.println("either a or b is true");  // coverage: FULL // stats: 4/4
    } else {
      System.out.println("both a and b are false");  // coverage: NONE // stats: 0/3
    }
  }

  void or2(boolean a, boolean b) {
    if (a || b) {  // coverage: PARTIAL // stats: 4/4 2/4
      System.out.println("either a or b is true");  // coverage: FULL // stats: 4/4
    } else {
      System.out.println("both a and b are false");  // coverage: NONE // stats: 0/3
    }
  }

  void or3(boolean a, boolean b) {
    if (a || b) {  // coverage: PARTIAL // stats: 2/4 1/4
      System.out.println("either a or b is true");  // coverage: FULL // stats: 4/4
    } else {
      System.out.println("both a and b are false");  // coverage: NONE // stats: 0/3
    }
  }

  void fullOr(boolean a, boolean b) {
    if (a || b) {  // coverage: FULL // stats: 4/4 4/4
      System.out.println("either a or b is true");  // coverage: FULL // stats: 4/4
    } else {
      System.out.println("both a and b are false");  // coverage: FULL // stats: 3/3
    }
  }

  boolean negation(boolean a) {
    return !a; // coverage: FULL // stats: 2/2
  }

  // condition is eliminated as the bytecode is the same as in the previous method
  boolean manualNegation(boolean a) {
    return a == false ? true : false; // coverage: FULL // stats: 2/2
  }

  boolean andWithoutIf(boolean a, boolean b) {
    return a && b;   // coverage: PARTIAL // stats: 4/4 1/2
  }

  boolean orWithoutIf(boolean a, boolean b) {
    return a || b;   // coverage: PARTIAL // stats: 4/6 1/2
  }

  public static void main(String[] args) {
    Test conditions = new Test();  // coverage: FULL // stats: 4/4

    conditions.oneBranch1(1);  // coverage: FULL // stats: 3/3
    conditions.oneBranch2(2);  // coverage: FULL // stats: 3/3

    conditions.allBranches(1);  // coverage: FULL // stats: 3/3
    conditions.allBranches(2);  // coverage: FULL // stats: 3/3

    conditions.singleBranch1(1);  // coverage: FULL // stats: 3/3
    conditions.singleBranch2(2);  // coverage: FULL // stats: 3/3

    // is not called on purpose
    // conditions.empty(1);

    conditions.and1(true, false);  // coverage: FULL // stats: 4/4
    conditions.and2(false, true);  // coverage: FULL // stats: 4/4
    conditions.and3(true, true);  // coverage: FULL // stats: 4/4

    conditions.fullAnd(true, true); // coverage: FULL // stats: 4/4
    conditions.fullAnd(true, false); // coverage: FULL // stats: 4/4
    conditions.fullAnd(false, true); // coverage: FULL // stats: 4/4
    conditions.fullAnd(false, false); // coverage: FULL // stats: 4/4

    conditions.andAnd0(false, false, false); // coverage: FULL // stats: 5/5

    conditions.andAnd1(false, false, false); // coverage: FULL // stats: 5/5
    conditions.andAnd1(true, false, false); // coverage: FULL // stats: 5/5

    conditions.andAnd2(false, false, false); // coverage: FULL // stats: 5/5
    conditions.andAnd2(true, false, false); // coverage: FULL // stats: 5/5
    conditions.andAnd2(true, true, false); // coverage: FULL // stats: 5/5

    conditions.andAnd3(false, false, false); // coverage: FULL // stats: 5/5
    conditions.andAnd3(true, false, false); // coverage: FULL // stats: 5/5
    conditions.andAnd3(true, true, false); // coverage: FULL // stats: 5/5
    conditions.andAnd3(true, true, true); // coverage: FULL // stats: 5/5

    conditions.or1(true, false);  // coverage: FULL // stats: 4/4
    conditions.or2(false, true);  // coverage: FULL // stats: 4/4
    conditions.or3(true, true);  // coverage: FULL // stats: 4/4

    conditions.fullOr(true, true); // coverage: FULL // stats: 4/4
    conditions.fullOr(true, false); // coverage: FULL // stats: 4/4
    conditions.fullOr(false, true); // coverage: FULL // stats: 4/4
    conditions.fullOr(false, false); // coverage: FULL // stats: 4/4

    conditions.negation(true); // coverage: FULL // stats: 4/4
    conditions.manualNegation(true); // coverage: FULL // stats: 4/4

    conditions.andWithoutIf(true, false);  // coverage: FULL // stats: 5/5
    conditions.orWithoutIf(false, true);  // coverage: FULL // stats: 5/5
  }
}
