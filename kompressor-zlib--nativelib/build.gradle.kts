import com.android.build.gradle.BaseExtension
import com.ensody.buildlogic.setupBuildLogic
import com.ensody.buildlogic.shell
import com.ensody.nativebuilds.addJvmNativeBuilds
import com.ensody.nativebuilds.cinterops
import org.gradle.kotlin.dsl.findByType
import kotlin.apply

plugins {
    id("com.ensody.build-logic.conditionalandroid")
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.publish")
    id("com.ensody.nativebuilds")
}

setupBuildLogic {
    kotlin {
        sourceSets.commonMain.dependencies {
            api(project(":kompressor-core"))
        }
        sourceSets.commonTest.dependencies {
            implementation(project(":kompressor-test"))
        }
        sourceSets.jvmMain {
            resources.srcDir(file("build/nativebuilds-desktop"))
        }
        sourceSets["nonJsMain"].dependencies {
            api(libs.nativebuilds.zlib.core)
        }

        cinterops(libs.nativebuilds.zlib.headers) {
            definitionFile.set(file("src/nativeMain/cinterop/lib.def"))
        }
    }

    addJvmNativeBuilds(
        libs.nativebuilds.zlib.core,
    )

    tasks.register("assembleZigJni") {
        dependsOn("unzipNativeBuilds")
        inputs.file("src/jvmMain/build.zig")
        inputs.dir("build/nativebuilds")
        inputs.dir("src/jvmCommonMain/jni")
        val outputPath = file("build/nativebuilds-desktop")
        outputs.dir(outputPath)
        doLast {
            outputPath.deleteRecursively()
            shell(
                "zig build -p ../../build/nativebuilds-desktop/jni",
                workingDir = file("src/jvmMain"),
                inheritIO = true,
            )
            outputPath.walkBottomUp().filter { it.extension == "pdb" }.forEach { it.delete() }
        }
    }

    tasks.named("jvmProcessResources") {
        dependsOn("assembleZigJni")
    }

    extensions.findByType<BaseExtension>()?.apply {
        externalNativeBuild {
            cmake {
                path = file("src/androidMain/CMakeLists.txt")
            }
        }
        // Android unit tests run on the host, so integrate the native shared libs for the host system
        sourceSets {
            named("test") {
                resources.srcDir(file("build/nativebuilds-desktop"))
            }
        }

        // Needed for Android unit tests to access the native shared libs for the host system
        tasks.named("preBuild") {
            dependsOn("assembleZigJni")
        }
    }
}
