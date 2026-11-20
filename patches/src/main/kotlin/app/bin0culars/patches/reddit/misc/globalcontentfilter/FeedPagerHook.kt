package app.bin0culars.patches.reddit.misc.globalcontentfilter

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef

internal fun hookFeedPagers() {
    val cachedPager = RedditCachedFeedPagerFingerprint.classDef
    val feedPager = RedditFeedPagerFingerprint.classDef

    listOf(cachedPager, feedPager).forEach { pager ->
        for (method in pager.methods.filter { it.name == "<init>" }) {
            hookInitMethod(method)
        }
    }

    val retryInterface = findRetryInterface(cachedPager)
        ?: findRetryInterface(feedPager)
        ?: throw PatchException("Could not find retry interface on feed pagers")

    val cachedFeedTypeField = feedTypeFieldName(cachedPager)
    val feedPagerFeedTypeField = feedTypeFieldName(feedPager)

    fillPagerFeedTypeBridge(
        cachedPager.type,
        cachedFeedTypeField,
        feedPager.type,
        feedPagerFeedTypeField,
    )
    fillPagerRetryBridge(retryInterface)
}

private fun feedTypeFieldName(pager: ClassDef): String =
    pager.fields.firstOrNull { it.type == "Lcom/reddit/feeds/data/FeedType;" }?.name
        ?: throw PatchException("Could not find FeedType field in ${pager.type}")

private fun parameterRegisterCount(method: MutableMethod): Int =
    1 + method.parameterTypes.sumOf { type ->
        val first = type.firstOrNull()
        if (first == 'J' || first == 'D') 2 else 1
    }

private fun hookInitMethod(method: MutableMethod) {
    val implementation = method.implementation
        ?: throw PatchException("Method ${method.name} has no implementation")
    val p0 = implementation.registerCount - parameterRegisterCount(method)
    val invokeInstruction = "invoke-static/range { v$p0 .. v$p0 }, $EXTENSION_CLASS->registerPager(Ljava/lang/Object;)V"

    val returnIndices = method.instructions
        .withIndex()
        .filter { it.value.opcode == Opcode.RETURN_VOID }
        .map { it.index }
        .reversed()

    for (index in returnIndices) {
        method.addInstructions(index, invokeInstruction)
    }
}

private fun findRetryInterface(classDef: ClassDef): String? =
    classDef.interfaces.firstOrNull { ifaceType ->
        classDefBy(ifaceType).methods.any { m ->
            m.name == "retry" && m.returnType == "V" && m.parameterTypes.isEmpty()
        }
    }

private fun fillPagerFeedTypeBridge(
    cachedPagerType: String,
    cachedFeedTypeField: String,
    feedPagerType: String,
    feedPagerFeedTypeField: String,
) {
    val smali = """
        if-eqz p0, :null_feed_type
        instance-of p1, p0, $cachedPagerType
        if-eqz p1, :try_feed_pager
        check-cast p0, $cachedPagerType
        iget-object p0, p0, $cachedPagerType->$cachedFeedTypeField:Lcom/reddit/feeds/data/FeedType;
        return-object p0
        :try_feed_pager
        instance-of p1, p0, $feedPagerType
        if-eqz p1, :null_feed_type
        check-cast p0, $feedPagerType
        iget-object p0, p0, $feedPagerType->$feedPagerFeedTypeField:Lcom/reddit/feeds/data/FeedType;
        return-object p0
        :null_feed_type
        const/4 p0, 0x0
        return-object p0
    """.trimIndent()
    fillBridge("pagerFeedType", smali)
}

private fun fillPagerRetryBridge(retryInterface: String) {
    val smali = """
        if-eqz p0, :done
        instance-of p1, p0, $retryInterface
        if-eqz p1, :done
        check-cast p0, $retryInterface
        invoke-interface {p0}, $retryInterface->retry()V
        :done
        return-void
    """.trimIndent()
    fillBridge("pagerRetry", smali)
}
