import java.util.Locale
import org.gradle.api.tasks.Exec
import org.gradle.jvm.tasks.Jar

plugins {
    id("java")
    application
}

group = "dev.iancheung"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val jmeVersion = "3.6.1-stable"

dependencies {
    implementation("org.jmonkeyengine:jme3-core:$jmeVersion")
    implementation("com.github.stephengold:Minie:7.7.0")

    runtimeOnly("org.jmonkeyengine:jme3-desktop:$jmeVersion")
    runtimeOnly("org.jmonkeyengine:jme3-lwjgl3:$jmeVersion")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("dev.iancheung.world.Main")
}

tasks.named<Jar>("jar") {
    archiveFileName.set("${project.name}.jar")
}

val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
val isMacOs = osName.contains("mac")
val isWindows = osName.contains("win")
val appName = "Mission Control"
val packageVersion = project.version.toString()
    .replace(Regex("[^0-9.]+"), "")
    .ifBlank { "1.0.0" }
val jpackageExecutable = file("${System.getProperty("java.home")}/bin/jpackage").absolutePath

fun registerJpackageTask(
    taskName: String,
    packageType: String,
    platformName: String,
    isSupportedPlatform: () -> Boolean,
    extraArguments: List<String> = emptyList()
) {
    tasks.register<Exec>(taskName) {
        group = "distribution"
        description = "Build a $platformName desktop package using jpackage."
        dependsOn("installDist")

        doFirst {
            check(isSupportedPlatform()) {
                "Run $taskName on $platformName."
            }
        }

        val inputDir = layout.buildDirectory.dir("install/${project.name}/lib").get().asFile.absolutePath
        val outputDir = layout.buildDirectory.dir("jpackage").get().asFile.absolutePath

        commandLine(
            jpackageExecutable,
            "--type", packageType,
            "--dest", outputDir,
            "--input", inputDir,
            "--name", appName,
            "--app-version", packageVersion,
            "--main-jar", "${project.name}.jar",
            "--main-class", application.mainClass.get(),
            *extraArguments.toTypedArray()
        )
    }
}

registerJpackageTask(
    taskName = "packageMac",
    packageType = "dmg",
    platformName = "macOS",
    isSupportedPlatform = { isMacOs },
    extraArguments = listOf("--java-options", "-XstartOnFirstThread")
)

registerJpackageTask(
    taskName = "packageWindows",
    packageType = "msi",
    platformName = "Windows",
    isSupportedPlatform = { isWindows }
)

tasks.withType<JavaExec> {
    // Check if the operating system is macOS
    if (System.getProperty("os.name").lowercase(Locale.getDefault()).contains("mac")) {
        // Force the JVM to run the main method on the first thread
        jvmArgs("-XstartOnFirstThread")
    }
}

tasks.test {
    useJUnitPlatform()
}