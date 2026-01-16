// `UsesKotlinNativeBundleBuildService` and `KotlinNativeProvider` are `internal` KGP APIs
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.ensody.buildlogic

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeFromToolchainProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.KotlinNativeProvider
import org.jetbrains.kotlin.gradle.targets.native.toolchain.UsesKotlinNativeBundleBuildService
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import kotlin.reflect.KClass

abstract class ClangKonanTask<T : ClangKonanTask<T>>(
    @get:Input val konanTarget: KonanTarget,
    taskType: KClass<T>,
) : AbstractExecTask<T>(taskType.java), UsesKotlinNativeBundleBuildService {

    @get:Nested
    internal val kotlinNativeProvider: Provider<KotlinNativeProvider> = project.provider {
        KotlinNativeFromToolchainProvider(project, konanTarget, kotlinNativeBundleBuildService)
    }

    @get:OutputDirectory
    val outputDirectory: DirectoryProperty = objectFactory.directoryProperty().convention(
        project.layout.dir(
            project.provider {
                temporaryDirFactory.create()!!
            },
        ),
    )

    protected abstract fun setup()

    final override fun exec() {
        val provider = kotlinNativeProvider.get()

        // recreate output directory
        outputDirectory.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }

        // clang will produce output into the current working directory
        workingDir(outputDirectory.get())

        // run clang vis K/N toolchain
        executable(File(provider.bundleDirectory.get(), "bin/run_konan").absolutePath)
        args("clang", "clang++", konanTarget, "-v")

        setup()

        super.exec()
    }
}
