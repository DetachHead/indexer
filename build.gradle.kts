val coroutinesVersion = "1.9.0"

plugins {
  val kotlinVersion = "2.1.20"
  kotlin("jvm") version kotlinVersion
  kotlin("plugin.power-assert") version kotlinVersion
  id("com.ncorti.ktfmt.gradle") version "0.22.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "io.github.detachhead"

version = "1.0-SNAPSHOT"

repositories { mavenCentral() }

dependencies {
  implementation("io.methvin:directory-watcher:0.19.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
  testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }

kotlin {
  jvmToolchain(16)
  explicitApi()
}
