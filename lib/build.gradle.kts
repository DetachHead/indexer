plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.powerAssert)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.detekt)
}

repositories { mavenCentral() }

dependencies {
  implementation(libs.directoryWatcher)
  implementation(libs.coroutines)
  testImplementation(libs.coroutinesTest)
  testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

kotlin {
  jvmToolchain(22)
  explicitApi()
}
