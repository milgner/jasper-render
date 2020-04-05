val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val jasperreports_version: String by project
val pdfbox_version: String by project

plugins {
    application
    kotlin("jvm") version "1.3.71"
}

group = "jasper_render"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    maven { url = uri("http://jaspersoft.jfrog.io/jaspersoft/third-party-ce-artifacts") }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-metrics:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-auth:$ktor_version")
    implementation("io.ktor:ktor-auth-jwt:$ktor_version")
    implementation("net.sf.jasperreports:jasperreports:$jasperreports_version")
    implementation("org.apache.pdfbox:pdfbox:$pdfbox_version")

    testImplementation("org.apache.pdfbox:preflight:$pdfbox_version")
    testImplementation(platform("io.strikt:strikt-bom:0.25.0"))
    testImplementation("io.strikt:strikt-gradle")
    testImplementation("net.sf.jasperreports:jasperreports-fonts:$jasperreports_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation(kotlin("script-runtime"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")
