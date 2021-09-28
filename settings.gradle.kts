rootProject.name = "Lavalink"

pluginManagement {
  val springBootVersion: String by settings
  val gradleGitVersion: String by settings
  val grGitVersion: String by settings
  val kotlinVersion: String by settings
  val testLoggerVersion: String by settings

  plugins {
    id("org.springframework.boot") version springBootVersion
    id("com.gorylenko.gradle-git-properties") version gradleGitVersion
    id("org.ajoberstar.grgit") version grGitVersion
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    id("com.adarshr.test-logger") version testLoggerVersion
  }
}