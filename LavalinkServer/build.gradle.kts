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

import org.gradle.api.file.DuplicatesStrategy
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.apache.tools.ant.filters.ReplaceTokens
import org.ajoberstar.grgit.Grgit

buildscript {
  repositories {
    mavenLocal()
    maven("https://plugins.gradle.org/m2/")
    maven("https://repo.spring.io/plugins-release")
    maven("https://jitpack.io")
  }

  dependencies {
    classpath(libs.gradle.git)
    classpath(libs.spring.gradle)
    classpath(libs.sonarqube)
    classpath(libs.kotlin.gradle)
    classpath(libs.kotlin.allopen)
    classpath(libs.test.logger)
  }
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  application
  idea
  alias(libs.plugins.spring)
  alias(libs.plugins.gradlegitproperties)
  alias(libs.plugins.grgit)
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.spring)
  alias(libs.plugins.test.logger)
}

description = "Play audio to discord voice channels"
version = versionFromTag()

application {
  mainClass.set("lavalink.server.Launcher")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
  implementation(libs.kotlin.reflect)

  // Audio Sending
  implementation(libs.koe.udpqueue) {
    exclude("com.sedmelluq", "lavaplayer")
    exclude("com.sedmelluq", "lava-common")
  }
  implementation(libs.koe.core) {
    exclude("org.slf4j", "slf4j-api")
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Native Transport
  implementation(libs.netty.epoll.x86)
  implementation(libs.netty.epoll.aarch64)
  implementation(libs.netty.kqueue)

  // Audio Player
  implementation(libs.lavaplayer.common)
  implementation(libs.lavaplayer.main)
  implementation(libs.lavaplayer.iprotator)
  implementation(libs.lavaplayer.thirdparty)
  implementation(libs.lavaplayer.formatxm)

  // Filters
  implementation(libs.lavadsp.main) {
    exclude("com.sedmelluq", "lavaplayer")
  }
  implementation(libs.lavadsp.extended) {
    exclude("com.sedmelluq", "lavaplayer")
  }

  // Spring
  implementation(libs.spring.ws)
  implementation(libs.spring.web) {
    exclude("org.springframework.boot", "spring-boot-starter-tomcat")
  }
  implementation(libs.spring.undertow)

  // Logging and Statistics
  implementation(libs.logback)
  implementation(libs.sentry)
  implementation(libs.prometheus.client)
  implementation(libs.prometheus.hotspot)
  implementation(libs.prometheus.logback)
  implementation(libs.prometheus.servlet)

  // Native System Stuff
  implementation(libs.oshi)

  // Json
  implementation(libs.jsonorg)
  implementation(libs.gson)

  // Test stuff
  compileOnly(libs.spotbugs.annotations)
  testImplementation(libs.spring.test)
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