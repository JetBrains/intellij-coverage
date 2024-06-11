# Coverage tests

## Test sources
Test sources are located in the `testData` package. Each test case is located in a 
separate subpackage. Main class should be `test.kt` for Kotlin or `Test.java` for Java, then it can be detected automatically in the [CoverageTest.kt](src/com/intellij/rt/coverage/CoverageTest.kt) test.
Most of the test cases could be configured in source files, but `custom` subpackage is used for special manual test.

## Test configuration
Test configuration is set with comments in source files. The supported settings are:
* `// coverage: [FULL|PARTIAL|NONE]` - line coverage, no such marker means that a line is ignored by coverage agent
* `// patterns: PATTERNS` - include/exclude patterns. Exclude patterns are listed after `-exclude` keyword, test package is used by default
* `// classes: Class1 Class2 ..` - space-separated list of simple class names witch are interesting for this test, by default includes all classes
* `// class: Class1`, `// classs: Class2` - when specified, these classes must be a complete list of available classes in the report
* `// extra args: -Dflag=true` - space-separated list of VM options
* `// calculate unloaded: [true|false]` - a flag to include unloaded classes into a coverage report, false by default
* `// markers: [file]` - relative path to a file with `coverage` markers
* `// test: ...` - list of tests that are covering a line in test tracking mode, see [TestTrackingTest.kt](src/com/intellij/rt/coverage/caseTests/TestTrackingTest.kt)
* `// branches: COVERED_BRANCHES/TOTAL_BRANCHES`
* `// instructions & branches` - enable instruction and branch coverage counters testing, see [InstructionsBranchesTest.kt](src/com/intellij/rt/coverage/caseTests/InstructionsBranchesTest.kt)
  * `// stats: COVERED_INSTRUCTIONS/TOTAL_INSTRUCTIONS [COVERED_BRANCHES/TOTAL_BRANCHES]` - instructions and branch coverage

See [runner.kt](src/com/intellij/rt/coverage/runner.kt) for source file processing details.

## Generated tests
Most of the tests are marked up in source files, so these tests' invocations could be generated automatically with [testGeneration.kt](src/com/intellij/rt/coverage/testGeneration.kt) code. 
`custom` subpackage is ignored by test generation.

