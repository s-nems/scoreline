import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.errorprone)
    alias(libs.plugins.nullaway)
}

group = "io.github.snems"
version = "0.1.0-SNAPSHOT"

val javaVersion = libs.versions.java.get().toInt()
val javaRelease = libs.versions.javaRelease.get().toInt()

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(libs.slf4j.api)
    errorprone(libs.errorprone.core)
    errorprone(libs.nullaway)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.slf4j.simple)
}

nullaway {
    annotatedPackages.add("io.github.snems.scoreline")
    error()
}

tasks.compileTestJava {
    // Tests pass null on purpose to exercise the null checks, so only Error Prone runs here.
    options.errorprone.nullaway {
        disable()
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Built with the toolchain above, for the oldest LTS release that has every language feature used here.
    options.release = javaRelease
    options.encoding = "UTF-8"
    // The exceptions of this library are never serialised, so they declare no serialVersionUID.
    options.compilerArgs.addAll(listOf("-Xlint:all,-serial", "-Werror"))
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).apply {
        // Comments say what a signature cannot, so a missing @param or @return is not an error.
        addBooleanOption("Xdoclint:all,-missing", true)
        addBooleanOption("Werror", true)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
    }
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

spotless {
    java {
        // Eclipse is the formatter that keeps a record with one component per line.
        eclipse().configFile("gradle/java-format.prefs")
        importOrder("\\#", "")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("gradleScripts") {
        target("*.gradle.kts")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
