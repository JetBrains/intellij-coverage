## How to use IntelliJCoverage agent in your project?

* in IDEA: [run with coverage action](https://www.jetbrains.com/help/idea/running-test-with-coverage.html)
* in TeamCity: [add coverage report to your build](https://www.jetbrains.com/help/teamcity/intellij-idea.html)
* in Gradle projects: use [Kover plugin](https://github.com/Kotlin/kotlinx-kover)
* run coverage agent in console, more detains in this tutorial

## How to run IntelliJCoverage agent in console

### Dependencies:
All the dependencies could be found in the Maven Central repository

* `org.jetbrains.intellij.deps:intellij-coverage-agent` 
  * we will call the jar `intellij-coverage-agent-1.0.716.jar`. **It is important to keep the exact name as it is stated in the Maven repository**
* `org.jetbrains.intellij.deps:intellij-coverage-reporter` (we will call it `reporter.jar`) with dependencies
  * `org.jetbrains.intellij.deps:coverage-report` (we will call it `builder.jar`)
  * `org.freemarker:freemarker` (we will call it `freemarker.jar`)
  * `org.jetbrains:annotations` (we will call it `annotations.jar`)

### Example project 

Let's assume you want to collect coverage report of your application which is already built into a `project.jar`.
As an example we will consider an app that outputs solutions for a number of square equations.

The application can be started as
```
java -cp project.jar example.TestKt
```
where `example.TestKt` is our main class.
As a result we will see an output:
```
root1 = -0.87+1.30i and root2 = -0.87-1.30i
root1 = root2 = -1.00
```

### Collect coverage
Firstly, we need to run our application with the IJCoverage agent.
Coverage agent requires to pass a list of arguments. 
The result of its work is a file with binary coverage report. 
Let's start with a configuration file `config.args`. 
Please write the following information to the configuration file:
```
report.ic
false
false
false
false
<include1>
<include2>
...
-exclude
<exclude1>
<exclude2>
...
```
Here `report.ic` is a path to a file with a binary report, `<include>` and `<exclude>` should be replaced with include/exclude regex if needed.

To run your app with coverage agent, you need to add `javaagent` argument and mention agent location and configuration file location.
You could also add `-Didea.new.tracing.coverage=true` option to run faster coverage.
```
java -cp project.jar -javaagent:intellij-coverage-agent-1.0.716.jar=config.args -Didea.new.tracing.coverage=true example.TestKt
```
After that you will see output:
```
---- IntelliJ IDEA coverage runner ---- 
branch coverage ...
include patterns:
exclude patterns:
root1 = -0.87+1.30i and root2 = -0.87-1.30i
root1 = root2 = -1.00;
Class transformation time: 0.370297627s for 72 classes or 0.005143022597222223s per class
```
Collected coverage is written to the `report.ic` file.

### Generate XML report
Now we can transform binary report to an XML report with the `reporter.jar`. 
Please write the following JSON configuration into a file `config.json`:
```
{
  "reports": [
        { "ic": "report.ic" }
  ],
  "xml": "report.xml",
  "modules": [
    {
      "output": [
        "project.jar"
      ]
    }
  ],
  "include": {
    "classes": [
      "example.*"
    ]
  }
}
```

Now you can generate an XML report with the following command:
```
java -cp reporter.jar:intellij-coverage-agent-1.0.716.jar:builder.jar com.intellij.rt.coverage.report.Main config.json
```
Generated XML report is written to the `report.xml` file.
<details>
  <summary>An example of an XML report</summary>

```
<?xml version="1.0" ?>
<report name="Intellij Coverage Report">
<package name="example">
<class name="example/TestKt" sourcefilename="test.kt">
<method name="findRoots" desc="(DDD)Ljava/lang/String;">
<counter type="INSTRUCTION" missed="48" covered="104"/>
<counter type="BRANCH" missed="1" covered="5"/>
<counter type="LINE" missed="3" covered="8"/>
</method>
<method name="main" desc="()V">
<counter type="INSTRUCTION" missed="0" covered="59"/>
<counter type="BRANCH" missed="0" covered="0"/>
<counter type="LINE" missed="0" covered="6"/>
</method>
<counter type="INSTRUCTION" missed="48" covered="163"/>
<counter type="BRANCH" missed="1" covered="5"/>
<counter type="LINE" missed="3" covered="14"/>
<counter type="METHOD" missed="0" covered="2"/>
</class>
<sourcefile name="test.kt">
<line nr="7" mi="0" ci="2" mb="0" cb="0"/>
<line nr="8" mi="0" ci="18" mb="0" cb="0"/>
<line nr="9" mi="0" ci="10" mb="0" cb="0"/>
<line nr="12" mi="0" ci="20" mb="0" cb="0"/>
<line nr="13" mi="0" ci="7" mb="0" cb="0"/>
<line nr="14" mi="0" ci="2" mb="0" cb="0"/>
<line nr="20" mi="0" ci="10" mb="0" cb="0"/>
<line nr="23" mi="0" ci="4" mb="1" cb="1"/>
<line nr="24" mi="11" ci="0" mb="0" cb="0"/>
<line nr="25" mi="11" ci="0" mb="0" cb="0"/>
<line nr="26" mi="26" ci="0" mb="0" cb="0"/>
<line nr="29" mi="0" ci="8" mb="0" cb="4"/>
<line nr="30" mi="0" ci="8" mb="0" cb="0"/>
<line nr="31" mi="0" ci="21" mb="0" cb="0"/>
<line nr="35" mi="0" ci="8" mb="0" cb="0"/>
<line nr="36" mi="0" ci="9" mb="0" cb="0"/>
<line nr="38" mi="0" ci="36" mb="0" cb="0"/>
<counter type="INSTRUCTION" missed="48" covered="163"/>
<counter type="BRANCH" missed="1" covered="5"/>
<counter type="LINE" missed="3" covered="14"/>
</sourcefile>
<counter type="INSTRUCTION" missed="48" covered="163"/>
<counter type="BRANCH" missed="1" covered="5"/>
<counter type="LINE" missed="3" covered="14"/>
<counter type="METHOD" missed="0" covered="2"/>
<counter type="CLASS" missed="0" covered="1"/>
</package>
<counter type="INSTRUCTION" missed="48" covered="163"/>
<counter type="BRANCH" missed="1" covered="5"/>
<counter type="LINE" missed="3" covered="14"/>
<counter type="METHOD" missed="0" covered="2"/>
<counter type="CLASS" missed="0" covered="1"/>
</report>
```

</details>

### Generate HTML report
You can generate an HTML report with the `reporter.jar` too, but you will need to change the configuration file.
Please change `config.json` to:
```
{
  "reports": [
        { "ic": "report.ic" }
  ],
  "html": "html",
  "modules": [
    {
      "output": [
        "project.jar"
      ],
      "sources": [
        "<PATH TO SOURCE ROOT>"
      ]
    }
  ],
  "include": {
    "classes": [
      "example.*"
    ]
  }
}
```
Here we need to specify an output directory `html` and sources root to add sources into the HTML report.
You can generate an HTML report with the following command:
```
java -cp reporter.jar:intellij-coverage-agent-1.0.716.jar:builder.jar:json.jar:annotations.jar:freemarker.jar com.intellij.rt.coverage.report.Main config.json
```
As a result in `html` directory you will get an HTML report in `index.html` file.

![](https://user-images.githubusercontent.com/31644752/161734145-f5d0dffc-b830-4147-8768-c39d59aa1693.png)
