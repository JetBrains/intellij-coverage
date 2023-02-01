# IntelliJ IDEA Code Coverage Agent [![official JetBrains project](http://jb.gg/badges/official-plastic.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

Apache 2 licensed code coverage engine for JVM. Supports branch coverage and
per-test coverage tracking. 

## How to use IntelliJCoverage agent in your project?

* in IDEA: [run with coverage action](https://www.jetbrains.com/help/idea/running-test-with-coverage.html)
* in TeamCity: [add coverage report to your build](https://www.jetbrains.com/help/teamcity/intellij-idea.html)
* in Gradle projects: use [Kover plugin](https://github.com/Kotlin/kotlinx-kover)

## Compiling

Open the project directory as Gradle project and run `:instrumentation:jar` task. The agent binary is created as `coverage-agent.jar` in the
`dist` directory.

## Contributions

Pull requests are welcome. Please post bug reports and feature requests
to [YouTrack](https://youtrack.jetbrains.com/issues/IDEA), project "IntelliJ IDEA".
