package app.bin0culars.patches.reddit.layout.glassnav

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions

private const val TABS_EXTENSION_CLASS = "Lapp/bin0culars/extension/reddit/GlassTopTabs;"

internal fun hookTopTabs(
    modifierType: String,
    sites: TopTabsSites,
) {
    fillBridge(
        "newWeight",
        """
            new-instance p0, ${sites.weightType}
            invoke-direct { p0, p1, p2 }, ${sites.weightType}-><init>(FZ)V
            return-object p0
        """,
        extensionClass = TABS_EXTENSION_CLASS,
    )
    fillBridge(
        "thenModifier",
        """
            check-cast p0, $modifierType
            check-cast p1, $modifierType
            invoke-interface { p0, p1 }, ${sites.thenRef.toSmali()}
            move-result-object p0
            return-object p0
        """,
        extensionClass = TABS_EXTENSION_CLASS,
    )
    sites.wrapperMethod.addInstructions(
        0,
        """
            invoke-static/range { p${sites.wrapperModReg} .. p${sites.wrapperModReg} }, $SEARCH_EXTENSION_CLASS->tabsModifier(Ljava/lang/Object;)Ljava/lang/Object;
            move-result-object p${sites.wrapperModReg}
            check-cast p${sites.wrapperModReg}, $modifierType
            invoke-static/range { p${sites.wrapperPadReg} .. p${sites.wrapperPadReg} }, $SEARCH_EXTENSION_CLASS->tabsTopPadding(F)F
            move-result p${sites.wrapperPadReg}
        """.trimIndent(),
    )
    sites.tabBarMethod.addInstructions(
        0,
        """
            invoke-static/range { p${sites.evenlyReg} .. p${sites.evenlyReg} }, $SEARCH_EXTENSION_CLASS->tabsEvenly(Z)Z
            move-result p${sites.evenlyReg}
        """.trimIndent(),
    )
}
