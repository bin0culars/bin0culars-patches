package app.bin0culars.patches.reddit.layout.hometabs

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

object PopularHomeTabFlagFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/reddit/feeds/domain/features/ForYouFollowingVariant;",
            name = "FOR_YOU_NAME",
        ),
        methodCall(
            definingClass = "Lcom/reddit/session/Session;",
            name = "isLoggedOut",
        ),
        methodCall(
            definingClass = "Lcom/reddit/session/Session;",
            name = "isIncognito",
        ),
    ),
)

object ForYouTabsFlagFingerprint : Fingerprint(
    classFingerprint = PopularHomeTabFlagFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/reddit/feeds/domain/features/ForYouFollowingVariant;",
            name = "isEnabled",
        ),
    ),
)
