package app.bin0culars.patches.reddit.layout.commentemojireply

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.newInstance
import app.morphe.patcher.string

internal const val EXTENSION_CLASS = "Lapp/bin0culars/extension/reddit/CommentEmojiReplyPatch;"

internal object CommentGestureInvokeFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/comments/elements/usercomment/composables/",
    name = "invoke",
    filters = listOf(
        newInstance("Lcom/reddit/comments/elements/usercomment/composables/UserCommentElement\$commentGestureHandling\$1\$3;"),
    ),
)

internal object CommentGestureConstructorFingerprint : Fingerprint(
    classFingerprint = CommentGestureInvokeFingerprint,
    name = "<init>",
    parameters = listOf(
        "Lkotlin/jvm/functions/Function0;",
        "L",
        "Lkotlin/jvm/functions/Function0;",
        "Lkotlin/jvm/functions/Function0;",
        "Lkotlin/jvm/functions/Function0;",
    ),
)

internal object CommentRepositoryCreateMarkdownFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/comment/data/repository/",
    returnType = "Ljava/lang/Object;",
    filters = listOf(
        newInstance("Lcom/reddit/comment/data/repository/RedditCommentRepository\$createMarkdown\$1;"),
    ),
)

internal object ReceiveCommentReplyEventFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        string("OnReceiveCommentReplyEvent(comment="),
    ),
)
