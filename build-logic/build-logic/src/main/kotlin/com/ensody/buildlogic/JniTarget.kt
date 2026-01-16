package com.ensody.buildlogic

import org.jetbrains.kotlin.konan.target.KonanTarget

sealed interface JniTarget {
    val konanTarget: KonanTarget

    enum class Android(override val konanTarget: KonanTarget) : JniTarget {
        ARM64(KonanTarget.ANDROID_ARM64),
        ARM32(KonanTarget.ANDROID_ARM32),
        X64(KonanTarget.ANDROID_X64),
        X86(KonanTarget.ANDROID_X86),
    }

    sealed interface Desktop : JniTarget {
        enum class Linux(override val konanTarget: KonanTarget) : Desktop {
            ARM64(KonanTarget.LINUX_ARM64),
            X64(KonanTarget.LINUX_X64),
        }

        enum class MacOS(override val konanTarget: KonanTarget) : Desktop {
            ARM64(KonanTarget.MACOS_ARM64),
            X64(KonanTarget.MACOS_X64),
        }
        enum class Windows(override val konanTarget: KonanTarget) : Desktop {
            X64(KonanTarget.MINGW_X64),
        }

        companion object {
            val entries: List<JniTarget> by lazy {
                Linux.entries + MacOS.entries + Windows.entries
            }
        }
    }

    companion object {
        val entries: List<JniTarget> by lazy {
            Android.entries + Desktop.entries
        }
    }
}
