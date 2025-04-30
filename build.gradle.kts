plugins {
  alias(libs.plugins.kotlin) apply false
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.detekt)
}

group = "io.github.detachhead"

version = "1.0-SNAPSHOT"

repositories { mavenCentral() }
