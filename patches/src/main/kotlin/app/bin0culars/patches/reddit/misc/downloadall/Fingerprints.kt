package app.bin0culars.patches.reddit.misc.downloadall

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.methodCall

internal const val EXTENSION_CLASS = "Lapp/bin0culars/extension/reddit/DownloadAllPatch;"

object ShareSheetLoadLinkFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/sharing/actions/handler/ActionsScreenEventHandler\$loadLink\$1;",
    name = "invokeSuspend",
    filters = listOf(
        checkCast("Lcom/reddit/domain/model/Link;"),
        methodCall(
            definingClass = "Lcom/reddit/domain/model/Link;",
            name = "getPromoted",
            location = MatchAfterWithin(10),
        ),
    ),
)

object LaunchDownloadActionFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lkotlin/jvm/functions/Function1;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/reddit/sharing/actions/handler/ActionsScreenShareActionHandler\$launchDownloadAction\$1;",
            name = "<init>",
        ),
    ),
)

object LaunchOriginalDownloadFingerprint : Fingerprint(
    definingClass = EXTENSION_CLASS,
    name = "launchOriginalDownload",
)
