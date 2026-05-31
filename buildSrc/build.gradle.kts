plugins {
  kotlin("jvm") version "2.3.21"
  `kotlin-dsl`
}

kotlin {
  jvmToolchain(25)
}

repositories {
  mavenLocal()
  mavenCentral()
  gradlePluginPortal()
  maven {
    name = "GitHubPackages"
    url = uri("https://maven.pkg.github.com/doki-theme/doki-build-source-jvm")
    credentials {
      username = System.getenv("GITHUB_ACTOR") ?: ""
      password = System.getenv("GITHUB_TOKEN") ?: ""
    }
  }
}

dependencies {
  implementation("org.jsoup:jsoup:1.17.2")
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("io.unthrottled.doki.build.jvm:doki-build-source-jvm:89.0.0")
}
