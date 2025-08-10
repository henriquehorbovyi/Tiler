plugins {
    kotlin("jvm") version "2.1.21"
}

group = "com.henriquehorbovyi.tiler"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.formdev:flatlaf:3.2.1")
    implementation("org.json:json:20240303")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}