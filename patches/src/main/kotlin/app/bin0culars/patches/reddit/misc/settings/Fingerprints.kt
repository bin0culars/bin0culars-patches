// Adapted from Morphe patches (https://github.com/MorpheApp/morphe-patches), Copyright 2026 Morphe, GPLv3. See NOTICE.

package app.bin0culars.patches.reddit.misc.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object StartUrlActivityFingerprint : Fingerprint(
    isStatic = true,
    parameters = listOf(
        "L",
        "Landroid/app/Activity;",
        "Landroid/net/Uri;",
        "Landroid/os/Bundle;",
        "Z",
        "I",
    ),
    filters = listOf(
        methodCall(smali = "Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;"),
        string("android.intent.action.VIEW"),
        methodCall(
            parameters = listOf(
                "Landroid/app/Activity;",
                "Landroid/net/Uri;",
                "I",
                "Ljava/lang/String;",
                "Landroid/os/Bundle;",
                "Z",
            ),
        ),
    ),
)
