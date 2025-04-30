@file:OptIn(ExperimentalComposeLibrary::class)

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
  implementation(project(":lib"))
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
    mainClass = "io.gitlab.detachhead.indexer.ui.MainKt"
    nativeDistributions {
      // TODO: targetPlatforms?
      packageName = "indexer"
      packageVersion = "1.0.0"
    }
  }
}

tasks.test { useJUnitPlatform() }

kotlin { jvmToolchain(16) }
