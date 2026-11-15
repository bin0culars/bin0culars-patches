package app.bin0culars.patches.reddit.layout.autoscroll

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

object TabSelectedFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lcom/reddit/launch/bottomnav/BottomNavTab;", "Z"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/reddit/launch/bottomnav/BottomNavScreenPresenter\$selectAndHandleTabSelection\$1;",
            name = "<init>",
        ),
    ),
)

object NavBarStateChangeFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/launch/bottomnav/BottomNavScreen\$onNavBarStateChange\$1;",
    name = "<init>",
)

object MasonryInnerFeedFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        methodCall(
            returnType = "Landroidx/compose/foundation/lazy/staggeredgrid/b;",
        ),
    ),
)

object MainActivityResumeFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/launch/main/MainActivity;",
    name = "onResume",
    returnType = "V",
    parameters = listOf(),
)

object MainActivityPauseFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/launch/main/MainActivity;",
    name = "onPause",
    returnType = "V",
    parameters = listOf(),
)
