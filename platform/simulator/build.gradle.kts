import org.gradle.api.tasks.testing.Test

plugins {
    id("java-library")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    testLogging.showStandardStreams = true
}

tasks.register<JavaExec>("exportTraces") {
    group = "application"
    description = "Export the tested scheduler and request/reply scenarios for the offline viewer."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.amberverma.distsys.simulator.TraceExamples")
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    args(layout.buildDirectory.dir("traces").get().asFile.absolutePath)
    outputs.dir(layout.buildDirectory.dir("traces"))
}
