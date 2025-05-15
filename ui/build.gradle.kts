@file:OptIn(ExperimentalComposeLibrary::class)

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
  alias(libs.plugins.kotlin)
  alias(libs.plugins.powerAssert)
  alias(libs.plugins.composePlugin)
  alias(libs.plugins.compose)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.detekt)
}

repositories {
  mavenCentral()
  google()
}

dependencies {
  implementation(project(":indexer"))
  implementation(compose.desktop.currentOs)
  implementation(compose.material3)
  implementation(compose.materialIconsExtended)
  implementation(libs.bonsai)
  implementation(libs.bonsaiFileSystem)
  implementation(libs.filekitCore)
  implementation(libs.filekitDialogsCompose)
  testImplementation(kotlin("test"))
  testImplementation(compose.uiTest)
}

compose.desktop {
  application {
    mainClass = "io.github.detachhead.indexer.ui.MainKt"
    nativeDistributions {
      packageName = "indexer"
      packageVersion = "1.0.0"
    }
  }
}

tasks.test {
  useJUnitPlatform()
  testLogging { exceptionFormat = TestExceptionFormat.FULL }
}

kotlin { jvmToolchain(22) }
