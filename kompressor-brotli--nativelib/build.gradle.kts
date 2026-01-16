import com.android.build.gradle.BaseExtension
import com.ensody.buildlogic.CompileJni
import com.ensody.buildlogic.JniTarget
import com.ensody.buildlogic.setupBuildLogic
import com.ensody.buildlogic.shell
import com.ensody.nativebuilds.addJvmNativeBuilds
import com.ensody.nativebuilds.cinterops

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
            api(libs.nativebuilds.brotli.common)
            api(libs.nativebuilds.brotli.dec)
            api(libs.nativebuilds.brotli.enc)
        }

        cinterops(libs.nativebuilds.brotli.headers) {
            definitionFile.set(file("src/nativeMain/cinterop/lib.def"))
        }
    }

    addJvmNativeBuilds(
        libs.nativebuilds.brotli.common,
        libs.nativebuilds.brotli.dec,
        libs.nativebuilds.brotli.enc,
    )

//    tasks.register("assembleZigJni") {
//        dependsOn("unzipNativeBuilds")
//        inputs.file("src/jvmMain/build.zig")
//        inputs.dir("build/nativebuilds")
//        inputs.dir("src/jvmCommonMain/jni")
//        val outputPath = file("build/nativebuilds-desktop")
//        outputs.dir(outputPath)
//        doLast {
//            outputPath.deleteRecursively()
//            shell(
//                "zig build -p ../../build/nativebuilds-desktop/jni",
//                workingDir = file("src/jvmMain"),
//                inheritIO = true,
//            )
//            outputPath.walkBottomUp().filter { it.extension == "pdb" }.forEach { it.delete() }
//        }
//    }

    tasks.register<CompileJni>("winJni", JniTarget.Desktop.Linux.ARM64).configure {
        inputFiles.from("jvmCommonMain/jni", "../jni/common/src")
        includeDirs.from("build/nativebuilds/brotli-headers-iosarm64/include", "../jni/common/include")
        linkPaths.add("build/nativebuilds/brotli-libbrotlicommon-jvm/jni/$targetName")
        linkPaths.add("build/nativebuilds/brotli-libbrotlidec-jvm/jni/$targetName")
        linkPaths.add("build/nativebuilds/brotli-libbrotlienc-jvm/jni/$targetName")
        linkLibraries.add("brotlicommon")
        linkLibraries.add("brotlidec")
        linkLibraries.add("brotlienc")
        outputLibraryName.set("libbrotli-jni")
        outputDirectory.set(file("build/nativebuilds-desktop/jni/$targetName"))
    }

    tasks.named("jvmProcessResources") {
//        dependsOn("assembleZigJni")
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
//            dependsOn("assembleZigJni")
        }
    }
}
