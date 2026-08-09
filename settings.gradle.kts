import org.gradle.api.initialization.resolve.RepositoriesMode

rootProject.name = "distributed-systems-labs"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

include("platform:simulator")
