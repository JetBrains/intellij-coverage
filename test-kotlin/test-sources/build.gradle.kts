dependencies {
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  val kotlinVersion: String? by project
  val coroutinesVersion = if (kotlinVersion?.startsWith("1.5") == true)  "1.5.2" else "1.6.4"
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-test")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
  implementation("net.bytebuddy:byte-buddy:1.11.12")
  implementation("net.bytebuddy:byte-buddy-agent:1.11.12")
  implementation("org.jmockit:jmockit:1.49")
  compileOnly("org.projectlombok:lombok:1.18.24")
  annotationProcessor("org.projectlombok:lombok:1.18.24")
  implementation("junit:junit:4.13.2")

  // test runtime API
  implementation(project(":instrumentation"))
  implementation(project(":java6-utils"))
  implementation(project(":offline-runtime:api"))


  testImplementation(project(":test-kotlin:test-utils"))
  testImplementation(project(":tests"))
  testImplementation("junit:junit:4.13.2")
  testImplementation(project(":reporter"))
  testImplementation(project(":reporter:offline"))
}

tasks.register<Test>("lineStatusTests") {
  group = "verification"
  filter {
    includeTestsMatching("com.intellij.rt.coverage.CoverageRunTest")
    includeTestsMatching("com.intellij.rt.coverage.caseTests.InstructionsBranchesTest")
    includeTestsMatching("com.intellij.rt.coverage.caseTests.BranchesTest")
    includeTestsMatching("com.intellij.rt.coverage.caseTests.LineSignatureTest")
    includeTestsMatching("com.intellij.rt.coverage.caseTests.OfflineInstrumentationTest")
  }
}
