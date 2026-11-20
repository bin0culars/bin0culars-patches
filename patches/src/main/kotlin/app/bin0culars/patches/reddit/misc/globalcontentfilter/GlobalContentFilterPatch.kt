package app.bin0culars.patches.reddit.misc.globalcontentfilter

import app.bin0culars.patches.reddit.layout.contenttypefilter.CrosspostElementFingerprint
import app.bin0culars.patches.reddit.layout.contenttypefilter.FeedPageBuildFingerprint
import app.bin0culars.patches.reddit.layout.contenttypefilter.PostElementFingerprint
import app.bin0culars.patches.reddit.layout.contenttypefilter.findChildrenMethod
import app.bin0culars.patches.reddit.layout.contenttypefilter.findDataSourceRegister
import app.bin0culars.patches.reddit.layout.contenttypefilter.findPageConstructorIndex
import app.bin0culars.patches.reddit.layout.contenttypefilter.findPageKeyField
import app.bin0culars.patches.reddit.layout.contenttypefilter.listRegisterOf
import app.bin0culars.patches.reddit.misc.settings.BinocularsModsChoice
import app.bin0culars.patches.reddit.misc.settings.BinocularsModsSection
import app.bin0culars.patches.reddit.misc.settings.BinocularsModsSetting
import app.bin0culars.patches.reddit.misc.settings.addBinocularsModsSetting
import app.bin0culars.patches.reddit.misc.settings.binocularsModsSettingsPatch
import app.bin0culars.patches.reddit.shared.Constants.COMPATIBILITY_REDDIT
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.patch

internal const val EXTENSION_CLASS = "Lapp/bin0culars/extension/reddit/GlobalContentFilterPatch;"

@Suppress("unused")
val globalContentFilterPatch =
    patch(
        name = "Global content filter",
        description = "Experimental: show only SFW or only NSFW posts in every feed, in search and in full-screen media. Turn it on in Settings → bin0culars mods.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_REDDIT)
        dependsOn(binocularsModsSettingsPatch)
        extendWith("extensions/extension.mpe")

        execute {
            registerSettings()
            val dataSourceType = hookFeedPages()
            hookFullscreenMedia()
            hookFeedPagers()
            fillIsPostContainerBridge()
            fillChildrenBridge()
            fillFeedTypeBridge(dataSourceType)
            fillIsFirstPageBridge()
            fillIndicatorListBridge()
            fillSearchPostNsfwBridge()
            fillSearchCommunityNsfwBridge()
            fillLinkOver18Bridge()
            fillTypeaheadSectionIdBridge()
        }
    }

private fun registerSettings() {
    addBinocularsModsSetting(
        BinocularsModsSetting(
            key = "global_content_filter",
            section = BinocularsModsSection.FEED,
            title = "Global content filter (experimental)",
            summary =
            "Experimental. Show only SFW or only NSFW posts in every feed, in search and in full-screen media; Only SFW also hides NSFW communities in search. Posts are hidden after Reddit sends them (nothing is unblurred or unlocked). When a page has few matching posts the next one loads automatically, but feeds with very few matches can still look empty. Pull to refresh after changing it, or switch back to Default if a feed stays empty.",
            choices = listOf(
                BinocularsModsChoice("sfw", "Only SFW"),
                BinocularsModsChoice("default", "Default"),
                BinocularsModsChoice("nsfw", "Only NSFW"),
            ),
            defaultChoice = "default",
        ),
    )
}

private fun hookFeedPages(): String {
    val pageBuildMethod = FeedPageBuildFingerprint.method
    val pageInstructions = pageBuildMethod.instructions.toList()
    val getDataMatch = FeedPageBuildFingerprint.instructionMatches.last()

    val injectionIndex = findPageConstructorIndex(pageInstructions, getDataMatch.index)
    val listRegister = listRegisterOf(pageInstructions[injectionIndex])

    val param0Type = pageBuildMethod.parameterTypes.first().toString()
    val implementation = pageBuildMethod.implementation
        ?: throw PatchException("Method ${pageBuildMethod.name} has no implementation")
    val p0Register = implementation.registerCount - 3
    val requestRegister = implementation.registerCount - 2
    val dataSourceRegister = findDataSourceRegister(pageInstructions, injectionIndex, param0Type, p0Register)

    if (listRegister >= 16 || dataSourceRegister >= 16 || requestRegister >= 16) {
        throw PatchException("Registers must be < 16: list=$listRegister, ds=$dataSourceRegister, req=$requestRegister")
    }

    pageBuildMethod.addInstructions(
        injectionIndex,
        """
            invoke-static { v$listRegister, v$dataSourceRegister, v$requestRegister }, $EXTENSION_CLASS->filterPage(Ljava/util/List;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;
            move-result-object v$listRegister
        """.trimIndent(),
    )

    return param0Type
}

internal fun fillBridge(name: String, smali: String) {
    Fingerprint(definingClass = EXTENSION_CLASS, name = name).method.addInstructions(0, smali)
}

private fun fillIsPostContainerBridge() {
    val postType = PostElementFingerprint.classDef.type
    val crosspostType = CrosspostElementFingerprint.classDef.type
    val smali = """
        if-eqz p0, :not_container
        instance-of p1, p0, $postType
        if-nez p1, :is_container
        instance-of p1, p0, $crosspostType
        if-nez p1, :is_container
        :not_container
        const/4 p1, 0x0
        return p1
        :is_container
        const/4 p1, 0x1
        return p1
    """.trimIndent()
    fillBridge("isPostContainer", smali)
}

private fun fillChildrenBridge() {
    val (parentInterface, method) = findChildrenMethod()
    val childrenMethodName = method.name
    val childrenReturnType = method.returnType

    val smali = """
        instance-of p1, p0, $parentInterface
        if-eqz p1, :null_child
        check-cast p0, $parentInterface
        invoke-interface {p0}, $parentInterface->$childrenMethodName()$childrenReturnType
        move-result-object p0
        return-object p0
        :null_child
        const/4 p0, 0x0
        return-object p0
    """.trimIndent()
    fillBridge("children", smali)
}

private fun fillFeedTypeBridge(dataSourceType: String) {
    val dsClassDef = classDefBy(dataSourceType)
    val feedTypeField = dsClassDef.fields.firstOrNull { it.type == "Lcom/reddit/feeds/data/FeedType;" }
        ?: throw PatchException("Could not find FeedType field in $dataSourceType")

    val smali = """
        check-cast p0, $dataSourceType
        iget-object p0, p0, $dataSourceType->${feedTypeField.name}:Lcom/reddit/feeds/data/FeedType;
        return-object p0
    """.trimIndent()
    fillBridge("feedType", smali)
}

private fun fillIsFirstPageBridge() {
    val requestType = FeedPageBuildFingerprint.method.parameterTypes[1].toString()
    val fieldName = findPageKeyField(requestType)

    val smali = """
        if-eqz p0, :no
        check-cast p0, $requestType
        iget-object p0, p0, $requestType->$fieldName:Ljava/lang/String;
        if-nez p0, :no
        const/4 p1, 0x1
        return p1
        :no
        const/4 p1, 0x0
        return p1
    """.trimIndent()
    fillBridge("isFirstPage", smali)
}

private fun fillIndicatorListBridge() {
    val indicatorsType = IndicatorsElementFingerprint.classDef.type
    val indicatorField = IndicatorsElementFingerprint.instructionMatches.last().fieldReference
    val smali = """
        instance-of p1, p0, $indicatorsType
        if-eqz p1, :none
        check-cast p0, $indicatorsType
        iget-object p0, p0, $indicatorsType->${indicatorField.name}:Ljava/util/List;
        return-object p0
        :none
        const/4 p0, 0x0
        return-object p0
    """.trimIndent()
    fillBridge("indicatorList", smali)
}
