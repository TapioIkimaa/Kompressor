package com.ensody.buildlogic

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.konan.target.HostManager
import javax.inject.Inject

abstract class CompileJni @Inject constructor(
    @get:Input val desktopTarget: JniTarget.Desktop,
) : ClangKonanTask<CompileJni>(desktopTarget.konanTarget, CompileJni::class) {

    init {
        // for cross compilation
        enabled = HostManager().isEnabled(konanTarget)
    }

    @get:Input
    val targetName = konanTarget.name.split("_").run {
        first() + drop(1).joinToString("") { it.uppercaseFirstChar() }
    }

    @get:InputFiles
    val includeDirs: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:InputFiles
    val inputFiles: ConfigurableFileCollection = objectFactory.fileCollection()

    @get:Input
    val linkPaths: ListProperty<String> = objectFactory.listProperty(String::class.java)

    @get:Input
    val linkLibraries: ListProperty<String> = objectFactory.listProperty(String::class.java)

    @get:Input
    val outputLibraryName: Property<String> = objectFactory.property(String::class.java)

    override fun setup() {
        // build a shared library
        args("-shared", "-fPIC")

        // include paths to jni headers
        val jniFiles = listOf(
            "jni/include/share/jni.h",
            "jni/include/unix/jni_md.h",
            "jni/include/windows/jni_md.h",
            "jni/include/LICENSE"
        )
        for (path in jniFiles) {
            val buildPath = project.layout.buildDirectory.file(path).get().asFile
            buildPath.parentFile.mkdirs()
            buildPath.writeTextIfDifferent(CompileJni::class.java.module.getResourceAsStream(path)!!.readAllBytes().decodeToString())
        }

        val hostDirName = when (desktopTarget) {
            is JniTarget.Desktop.Linux, is JniTarget.Desktop.MacOS -> "unix"
            is JniTarget.Desktop.Windows -> "windows"
        }
        for (dir in listOf("share", hostDirName)) {
            args("-I${project.layout.buildDirectory.file("jni/include/$dir").get().asFile.absolutePath}")
        }

        // include dirs to search headers
        args(includeDirs.map { "-I${it.absolutePath}" })

        // link paths/libs
        args(linkPaths.get().map { "-L${project.file(it).absolutePath}" })
        args(linkLibraries.get().map { "-l$it" })

        val fullLibraryName = outputLibraryName.get() + "." + konanTarget.family.dynamicSuffix

        // output path
        args("-o", fullLibraryName)

        // input files
        inputFiles.asFileTree.forEach {
            args(it.absolutePath)
        }
    }
}
