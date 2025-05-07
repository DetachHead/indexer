import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.powerAssert)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.detekt)
}

repositories { mavenCentral() }

dependencies {
  api(libs.directoryWatcher)
  implementation(libs.coroutines)
  testImplementation(libs.coroutinesTest)
  testImplementation(kotlin("test"))
}

tasks.test {
  useJUnitPlatform()
  testLogging { exceptionFormat = TestExceptionFormat.FULL }
}

kotlin {
  jvmToolchain(22)
  explicitApi()
}
