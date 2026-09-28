import org.gradle.api.tasks.testing.Test

plugins {
    id("java-library")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// The learner's unfinished implementation is compiled by check, but its
// acceptance tests run explicitly until the exercise has been completed.
tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("local-kv-exercise")
    }
}

tasks.register<Test>("exerciseTest") {
    group = "verification"
    description = "Run the local KV coding exercise acceptance tests (the starter intentionally fails)."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("local-kv-exercise")
    }
    shouldRunAfter(tasks.named("test"))
}
