/*
 *  Copyright (c) 2021 Freya Arbjerg and contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 *
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.apache.tools.ant.filters.ReplaceTokens
import org.ajoberstar.grgit.Grgit

buildscript {
  val kotlinVersion = "1.6.0"
  val springBootVersion = "2.6.2"

  repositories {
    mavenLocal()
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.spring.io/plugins-release")
    maven("https://jitpack.io")
  }

  dependencies {
    classpath("com.gorylenko.gradle-git-properties:gradle-git-properties:2.3.1")
    classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    classpath("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:3.3")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    classpath("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
    classpath("com.adarshr:gradle-test-logger-plugin:1.6.0")
  }
}

plugins {
  application
  idea
  id("org.springframework.boot") version "2.6.2"
  id("com.gorylenko.gradle-git-properties") version "2.3.1"
  id("org.ajoberstar.grgit") version "4.1.0"
  kotlin("jvm") version "1.6.0"
  kotlin("plugin.spring") version "1.6.0"
  id("com.adarshr.test-logger") version "1.6.0"
}

group = "lavalink"
description = "Play audio to discord voice channels"
version = versionFromTag()

application {
  mainClass.set("lavalink.server.Launcher")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

repositories {
  mavenCentral()
  jcenter()
  mavenLocal()
  maven("https://jitpack.io")
  maven("https://m2.dv8tion.net/releases")
}

val kotlinVersion = "1.6.0"
val springBootVersion = "2.6.2"
val prometheusVersion = "0.14.1"
val koeVersion = "4835255"

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

  // Audio Sending
  implementation("com.github.davidffa.koe:ext-udpqueue:$koeVersion") {
    exclude("com.sedmelluq", "lavaplayer")
  }
  implementation("com.github.davidffa.koe:core:$koeVersion") {
    exclude("org.slf4j", "slf4j-api")
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Transport
  implementation("io.netty:netty-transport-native-epoll:4.1.72.Final:linux-x86_64")

  // Audio Player
  implementation("com.github.davidffa:lavaplayer-fork:0cb6a3a")
  implementation("com.sedmelluq:lavaplayer-ext-youtube-rotator:0.2.3") {
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Filters
  implementation("com.github.natanbc:lavadsp:0.7.7") {
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Spring
  implementation("org.springframework:spring-websocket:5.3.15")
  implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion") {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation("org.springframework.boot:spring-boot-starter-undertow:$springBootVersion")

  // Logging and Statistics
  implementation("ch.qos.logback:logback-classic:1.2.10")
  implementation("io.sentry:sentry-logback:5.6.0")
  implementation("io.prometheus:simpleclient:$prometheusVersion")
  implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
  implementation("io.prometheus:simpleclient_logback:$prometheusVersion")
  implementation("io.prometheus:simpleclient_servlet:$prometheusVersion")

  // Native System Stuff
  implementation("com.github.oshi:oshi-core:6.0.0")

  // Json
  implementation("org.json:json:20210307")
  implementation("com.google.code.gson:gson:2.8.9")

  // Test stuff
  compileOnly("com.github.spotbugs:spotbugs-annotations:4.5.3")
  testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
}

tasks {
  bootJar {
    archiveFileName.set("Lavalink.jar")
  }

  bootRun {
    dependsOn(compileTestKotlin)
  }

  processResources {
    filesMatching("**/app.properties") {
      val tokens = mapOf(
        "project.version" to project.version,
        "project.groupId" to project.group,
        "project.artifactId" to project.name,
        "env.BUILD_NUMBER" to if (System.getenv("CI") != null) System.getenv("BUILD_NUMBER") else "Unofficial",
        "env.BUILD_TIME" to System.currentTimeMillis().toString()
      )

      filter<ReplaceTokens>("tokens" to tokens)
    }
  }

  build {
    doLast {
      println("Version: $version")
    }
  }

  test {
    useJUnitPlatform()
  }

  withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
  }
}

fun versionFromTag(): String {
  val grgit = Grgit.open(mapOf("currentDir" to rootDir))

  val headTag = grgit.tag.list().find {
    it.commit.id == grgit.head().id
  }

  // Uncommitted changes? -> should be SNAPSHOT
  // Also watch out for false positives in the CI build
  val clean = grgit.status().isClean || System.getenv("CI") != null

  if (!clean) {
    println("Git state is dirty, setting version as snapshot")
  }

  return if (headTag != null && clean) {
    headTag.name
  } else {
    "${grgit.head().id}-SNAPSHOT"
  }
}

//create a simple version file that we will be reading to create appropriate docker tags

fun versionTxt() {
  File("$projectDir/VERSION.txt").appendText("$version\n")
}

versionTxt()