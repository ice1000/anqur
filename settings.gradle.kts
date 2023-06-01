rootProject.name = "anqur"

dependencyResolutionManagement {
  @Suppress("UnstableApiUsage") repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    mavenLocal()
  }
}

include("base", "cli")
