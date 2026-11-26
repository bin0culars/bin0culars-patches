package app.bin0culars.patches.reddit.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    private val REDDIT_TARGETS_UNTIL_47 =
        listOf(
            AppTarget(
                version = "2026.47.0",
            ),
            AppTarget(
                version = "2026.46.0",
            ),
            AppTarget(
                version = "2026.45.0",
            ),
        )

    val COMPATIBILITY_REDDIT =
        Compatibility(
            name = "Reddit",
            packageName = "com.reddit.frontpage",
            apkFileType = ApkFileType.APKM,
            appIconColor = 0xFF4500,
            targets = listOf(AppTarget(version = "2026.48.0")) + REDDIT_TARGETS_UNTIL_47,
        )

    val COMPATIBILITY_REDDIT_UNTIL_47 =
        Compatibility(
            name = "Reddit",
            packageName = "com.reddit.frontpage",
            apkFileType = ApkFileType.APKM,
            appIconColor = 0xFF4500,
            targets = REDDIT_TARGETS_UNTIL_47,
        )
}
