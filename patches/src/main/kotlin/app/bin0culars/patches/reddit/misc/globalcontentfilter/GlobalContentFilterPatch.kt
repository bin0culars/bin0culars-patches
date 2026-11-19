package app.bin0culars.patches.reddit.misc.globalcontentfilter

import app.bin0culars.patches.reddit.layout.contenttypefilter.CrosspostElementFingerprint
import app.bin0culars.patches.reddit.layout.contenttypefilter.FeedPageBuildFingerprint
import app.bin0culars.patches.reddit.layout.contenttypefilter.PostElementFingerprint
import app.bin0culars.patches.reddit.layout.contenttypefilter.findChildrenMethod
import app.bin0culars.patches.reddit.layout.contenttypefilter.findDataSourceRegister
import app.bin0culars.patches.reddit.layout.contenttypefilter.findPageConstructorIndex
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

private const val EXTENSION_CLASS = "Lapp/bin0culars/extension/reddit/GlobalContentFilterPatch;"

@Suppress("unused")
val globalContentFilterPatch =
    patch(
        name = "Global content filter",
        description = "Show only SFW or only NSFW posts in every feed and in search. Turn it on in Settings → bin0culars mods.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_REDDIT)
        dependsOn(binocularsModsSettingsPatch)
        extendWith("extensions/extension.mpe")

        execute {
            registerSettings()
            val dataSourceType = hookFeedPages()
            fillIsPostContainerBridge()
            fillChildrenBridge()
            fillFeedTypeBridge(dataSourceType)
            fillIndicatorListBridge()
        }
    }

private fun registerSettings() {
    addBinocularsModsSetting(
        BinocularsModsSetting(
            key = "global_content_filter",
            section = BinocularsModsSection.FEED,
            title = "Global content filter",
            summary =
            "Show only SFW or only NSFW posts in every feed and in search. Hides posts Reddit already sent; it never unblurs or unlocks anything. Some pages show fewer posts, and with Only NSFW a feed can end early if your account gets few NSFW posts. Pull to refresh after changing it.",
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
    val dataSourceRegister = findDataSourceRegister(pageInstructions, injectionIndex, param0Type, p0Register)

    if (listRegister >= 16 || dataSourceRegister >= 16) {
        throw PatchException("Registers must be < 16: list=$listRegister, ds=$dataSourceRegister")
    }

    pageBuildMethod.addInstructions(
        injectionIndex,
        """
            invoke-static { v$listRegister, v$dataSourceRegister }, $EXTENSION_CLASS->filterPage(Ljava/util/List;Ljava/lang/Object;)Ljava/util/List;
            move-result-object v$listRegister
        """.trimIndent(),
    )

    return param0Type
}

private fun fillBridge(name: String, smali: String) {
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
