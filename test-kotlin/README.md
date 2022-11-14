# Coverage tests

## Test sources
Test sources are located in the `testData` package. Each test case is located in a 
separate subpackage. Main class should be `test.kt` for Kotlin or `Test.java` for Java, then it can be detected automatically in the `CoverageTest` test.
Most of the test cases could be configured in source files, but `custom` subpackage is used for special manual test.

## Test configuration
Test configuration is set with comments in source files. The supported settings are:
* `// coverage: [FULL|PARTIAL|NONE]` - line coverage, no such marker means that a line is ignored by coverage agent
* `// patterns: PATTERNS` - include/exclude patterns. Exclude patterns are listed after `-exclude` keyword, `testData\..*` by default
* `// classes: Class1 Class2 ..` - space-separated list of simple class names witch are interesting for this test, by default includes test source file class (TestKt or Test)
* `// extra args: -Dflag=true` - space-separated list of VM options
* `// calculate unloaded: [true|false]` - a flag to include unloaded classes into a coverage report, false by default
* `// markers: [file]` - relative path to a file with `coverage` markers

See runner.kt for source file processing details.

## Generated tests
Most of the test are marked up in source files, so these tests' invocations could be generated automatically with testGeneration.kt code. 
`custom` subpackage is ignored by test generation.

