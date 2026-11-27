package app.bin0culars.patches.reddit.layout.commentemojireply

import app.bin0culars.patches.reddit.layout.autoscroll.MainActivityResumeFingerprint
import app.bin0culars.patches.reddit.misc.settings.BinocularsModsSection.POSTS
import app.bin0culars.patches.reddit.misc.settings.BinocularsModsSetting
import app.bin0culars.patches.reddit.misc.settings.addBinocularsModsSetting
import app.bin0culars.patches.reddit.misc.settings.binocularsModsSettingsPatch
import app.bin0culars.patches.reddit.shared.Constants.COMPATIBILITY_REDDIT
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.mutableClassDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceXml
import app.morphe.patcher.util.p0Register
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

@Suppress("unused")
val commentEmojiReplyPatch =
    patch(
        name = "Comment emoji reply",
        description = "Press and hold a comment to reply with an emoji. Turn it on or off in Settings → bin0culars mods.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_REDDIT)
        dependsOn(binocularsModsSettingsPatch)
        extendWith("extensions/extension.mpe")

        execute {
            addBinocularsModsSetting(
                BinocularsModsSetting(
                    key = "comment_emoji_reply",
                    section = POSTS,
                    title = "Emoji reply on long-press",
                    summary = "Press and hold a comment to reply with an emoji. The emoji is posted right away; some communities remove very short replies.",
                    default = true,
                ),
            )

            CommentGestureConstructorFingerprint.method.apply {
                val p3Reg = p0Register + 3
                val invoke =
                    if (p3Reg > 15) {
                        "invoke-static/range { p3 .. p3 }, $EXTENSION_CLASS->wrapLongPress(Lkotlin/jvm/functions/Function0;)Lkotlin/jvm/functions/Function0;"
                    } else {
                        "invoke-static { p3 }, $EXTENSION_CLASS->wrapLongPress(Lkotlin/jvm/functions/Function0;)Lkotlin/jvm/functions/Function0;"
                    }
                addInstructions(
                    0,
                    """
                        $invoke
                        move-result-object p3
                    """.trimIndent(),
                )
            }

            val repoClass = mutableClassDefBy(CommentRepositoryCreateMarkdownFingerprint.classDef)
            val constructors = repoClass.methods.filter { it.name == "<init>" }
            if (constructors.isEmpty()) {
                throw PatchException("Could not find constructor in ${repoClass.type}")
            }
            for (ctor in constructors) {
                val p0Reg = ctor.p0Register
                val invoke =
                    if (p0Reg > 15) {
                        "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS->setCommentRepository(Ljava/lang/Object;)V"
                    } else {
                        "invoke-static { p0 }, $EXTENSION_CLASS->setCommentRepository(Ljava/lang/Object;)V"
                    }
                val superInitIndex = ctor.instructions.indexOfFirst { instruction ->
                    if (instruction.opcode != Opcode.INVOKE_DIRECT &&
                        instruction.opcode != Opcode.INVOKE_DIRECT_RANGE
                    ) {
                        return@indexOfFirst false
                    }
                    val methodRef = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                    if (methodRef?.name != "<init>") {
                        return@indexOfFirst false
                    }
                    val firstArg = when (instruction) {
                        is FiveRegisterInstruction -> if (instruction.registerCount > 0) instruction.registerC else -1
                        is RegisterRangeInstruction -> if (instruction.registerCount > 0) instruction.startRegister else -1
                        else -> -1
                    }
                    firstArg == p0Reg
                }
                if (superInitIndex == -1) {
                    throw PatchException("Could not find super <init> call in ${repoClass.type}-><init>")
                }
                ctor.addInstructions(superInitIndex + 1, invoke)
            }

            val contMatch = CommentRepositoryCreateMarkdownFingerprint.instructionMatches.first()
            val contTypeRef = contMatch.getInstruction<ReferenceInstruction>().reference as TypeReference
            val createMarkdownContClassName = contTypeRef.type
                .removePrefix("L")
                .removeSuffix(";")
                .replace('/', '.')
            patchStringStub("createMarkdownContClassName", createMarkdownContClassName)

            val replyEventClassName = ReceiveCommentReplyEventFingerprint.classDef.type
                .removePrefix("L")
                .removeSuffix(";")
                .replace('/', '.')
            patchStringStub("replyEventClassName", replyEventClassName)

            MainActivityResumeFingerprint.method.addInstructions(
                0,
                "invoke-static { p0 }, $EXTENSION_CLASS->onActivityResumed(Landroid/app/Activity;)V",
            )
        }

        finalize {
            resourceXml("res/values/strings.xml").use { strings ->
                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_emoji_reply_sent")
                    text = "Replied %s"
                }
                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_emoji_reply_failed")
                    text = "Couldn't post the reply"
                }
                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_emoji_reply_more")
                    text = "More emojis"
                }
            }
        }
    }

private fun patchStringStub(
    methodName: String,
    value: String,
) {
    val stubMethod = Fingerprint(definingClass = EXTENSION_CLASS, name = methodName).method
    for (i in stubMethod.instructions.indices.reversed()) {
        stubMethod.removeInstruction(i)
    }
    stubMethod.addInstructions(
        0,
        """
            const-string v0, "$value"
            return-object v0
        """.trimIndent(),
    )
}
