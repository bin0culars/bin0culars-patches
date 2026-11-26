package app.bin0culars.patches.reddit.layout.automod

import app.morphe.patcher.Fingerprint

object SetCommentsToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("SetComments(comments="),
)

object SetCommentTreesToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("SetCommentTrees(commentTrees="),
)

object AppendCommentTreesToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("AppendCommentTrees(commentTrees="),
)

object CommentTreeToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    strings = listOf("CommentTree(rootCommentId="),
)
