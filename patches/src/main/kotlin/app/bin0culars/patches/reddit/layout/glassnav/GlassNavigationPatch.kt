package app.bin0culars.patches.reddit.layout.glassnav

import app.bin0culars.patches.reddit.misc.settings.BinocularsModsChoice
import app.bin0culars.patches.reddit.misc.settings.BinocularsModsSection.NAVIGATION
import app.bin0culars.patches.reddit.misc.settings.BinocularsModsSetting
import app.bin0culars.patches.reddit.misc.settings.addBinocularsModsSetting
import app.bin0culars.patches.reddit.misc.settings.binocularsModsSettingsPatch
import app.bin0culars.patches.reddit.shared.Constants.COMPATIBILITY_REDDIT
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.registersUsed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val glassNavigationPatch =
    patch(
        name = "Glass navigation bar",
        description = "Turns the bottom navigation bar (the side rail on tablets) into a floating see-through capsule. " +
            "Turn it on in Settings → bin0culars mods.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_REDDIT)
        dependsOn(binocularsModsSettingsPatch)
        extendWith("extensions/extension.mpe")

        execute {
            addBinocularsModsSetting(
                BinocularsModsSetting(
                    key = "glass_navigation",
                    section = NAVIGATION,
                    title = "Glass navigation bar",
                    summary = "Floating see-through bar with the feed scrolling underneath it. " +
                        "On tablets the side rail gets the same look. " +
                        "To stop it hiding while you scroll, also turn on Keep bottom bar visible.",
                    default = false,
                ),
            )
            addBinocularsModsSetting(
                BinocularsModsSetting(
                    key = "glass_navigation_tint",
                    section = NAVIGATION,
                    parentKey = "glass_navigation",
                    title = "Glass colour",
                    summary = "Auto follows Reddit's light or dark theme.",
                    choices = listOf(
                        BinocularsModsChoice("auto", "Auto"),
                        BinocularsModsChoice("light", "Light"),
                        BinocularsModsChoice("dark", "Dark"),
                    ),
                    defaultChoice = "auto",
                ),
            )
            addBinocularsModsSetting(
                BinocularsModsSetting(
                    key = "glass_navigation_opacity",
                    section = NAVIGATION,
                    parentKey = "glass_navigation",
                    title = "See-through",
                    summary = "Clear shows the most of the page behind the bar, Frosted the least.",
                    choices = listOf(
                        BinocularsModsChoice("clear", "Clear"),
                        BinocularsModsChoice("balanced", "Balanced"),
                        BinocularsModsChoice("frosted", "Frosted"),
                    ),
                    defaultChoice = "balanced",
                ),
            )

            val bottomNavBarMethod = BottomNavBarFingerprint.method
            val bottomNavIndex = BottomNavBarFingerprint.instructionMatches.first().index

            val (drawBehindIndex, drawBehindRef) = findDrawBehind(bottomNavBarMethod, bottomNavIndex)
            val drawBehindInst = bottomNavBarMethod.instructions[drawBehindIndex]
            val vF = (drawBehindInst as? FiveRegisterInstruction)?.registerD
                ?: drawBehindInst.registersUsed.getOrNull(1)
                    .orThrow("Could not find 2nd argument of drawBehind")

            val (surfaceIndex, surfaceInst) = findSurfaceCall(bottomNavBarMethod, bottomNavIndex)
            val vElevation = surfaceInst.startRegister + 2
            val rColor = surfaceInst.startRegister + 3
            val rColorHigh = surfaceInst.startRegister + 4

            val lambdaType = findLambdaClassType(bottomNavBarMethod, drawBehindIndex, vF)
            val scopeRefs = findScopeRefs(lambdaType)

            fillPlaceholders(scopeRefs)

            val themeRefs = findThemeRefs(bottomNavBarMethod, surfaceIndex)

            hookBarSurface(
                bottomNavBarMethod,
                surfaceIndex,
                vElevation,
                rColor,
                rColorHigh,
            )
            hookBarDraw(
                bottomNavBarMethod,
                drawBehindIndex,
                vF,
            )

            hookSideNavRail(SideNavRailFingerprint.method, drawBehindRef.toSmali(), themeRefs)

            hookNavScreenContentState()

            hookBottomNavBackdrop()
        }
    }

private fun findWidthIndex(method: MutableMethod, literalIndex: Int): Int =
    ((literalIndex + 1) until method.instructions.size).firstOrNull { i ->
        val inst = method.instructions[i]
        val isStatic = inst.opcode == Opcode.INVOKE_STATIC
        val ref = (inst as? ReferenceInstruction)?.reference as? MethodReference
        isStatic && ref != null && isObjectReturningCall(ref, "F")
    }.orThrow("Could not find width instruction after literal")

private fun getTargetRegister(
    instruction: Instruction?,
    expectedOpcode: Opcode,
    description: String,
): Int {
    val inst = instruction.orThrow("Missing $description instruction")
    if (inst.opcode != expectedOpcode) {
        throw PatchException("Expected $expectedOpcode for $description, found ${inst.opcode}")
    }
    return (inst as? OneRegisterInstruction)?.registerA
        .orThrow("Could not read register from $description")
}

private fun hookBarSurface(
    method: MutableMethod,
    surfaceIndex: Int,
    vElevation: Int,
    rColor: Int,
    rColorHigh: Int,
) {
    val barElevationSmali = if (vElevation > 15) {
        """
            invoke-static/range { v$vElevation .. v$vElevation }, $EXTENSION_CLASS->barElevation(F)F
            move-result v$vElevation
        """.trimIndent()
    } else {
        """
            invoke-static { v$vElevation }, $EXTENSION_CLASS->barElevation(F)F
            move-result v$vElevation
        """.trimIndent()
    }

    val barColorSmali = if (rColor > 15 || rColorHigh > 15) {
        """
            invoke-static/range { v$rColor .. v$rColorHigh }, $EXTENSION_CLASS->barColor(J)J
            move-result-wide v$rColor
        """.trimIndent()
    } else {
        """
            invoke-static { v$rColor, v$rColorHigh }, $EXTENSION_CLASS->barColor(J)J
            move-result-wide v$rColor
        """.trimIndent()
    }

    val surfaceSmali = """
        $barElevationSmali
        $barColorSmali
    """.trimIndent()
    method.addInstructions(surfaceIndex, surfaceSmali)
}

private fun hookBarDraw(
    method: MutableMethod,
    drawBehindIndex: Int,
    vF: Int,
) {
    val barDrawSmali = if (vF > 15) {
        """
            invoke-static/range { v$vF .. v$vF }, $EXTENSION_CLASS->barDraw(Lkotlin/jvm/functions/Function1;)Lkotlin/jvm/functions/Function1;
            move-result-object v$vF
        """.trimIndent()
    } else {
        """
            invoke-static { v$vF }, $EXTENSION_CLASS->barDraw(Lkotlin/jvm/functions/Function1;)Lkotlin/jvm/functions/Function1;
            move-result-object v$vF
        """.trimIndent()
    }
    method.addInstructions(drawBehindIndex, barDrawSmali)
}

private fun isRailBoxCall(inst: Instruction, composerType: String): Boolean {
    val params = ((inst as? ReferenceInstruction)?.reference as? MethodReference)?.parameterTypes
    return inst.opcode == Opcode.INVOKE_STATIC_RANGE &&
        params != null && params.size == 7 &&
        params[4].toString() == composerType &&
        params[5].toString() == "I" && params[6].toString() == "I"
}

private fun findRailBoxCall(
    method: MutableMethod,
    widthIndex: Int,
    composerType: String,
): Pair<Int, RegisterRangeInstruction> {
    val idx = ((widthIndex + 1) until method.instructions.size).firstOrNull { i ->
        isRailBoxCall(method.instructions[i], composerType)
    }.orThrow("Could not find Box INVOKE_STATIC_RANGE after widthIndex")
    val inst = (method.instructions[idx] as? RegisterRangeInstruction)
        .orThrow("Box call at $idx is not RegisterRangeInstruction")
    return idx to inst
}

private fun verifyRailScratchRegisters(
    method: MutableMethod,
    boxCallIndex: Int,
    b: Int,
) {
    val checkStart = maxOf(0, boxCallIndex - 12)
    val preBoxInstructions = method.instructions.subList(checkStart, boxCallIndex)
    val writtenRegisters = preBoxInstructions
        .filter { it is OneRegisterInstruction && it.opcode.setsRegister() }
        .map { (it as OneRegisterInstruction).registerA }
        .toSet()

    val hasB1 = b + 1 in writtenRegisters
    val hasB2 = b + 2 in writtenRegisters
    val hasB3 = b + 3 in writtenRegisters
    if (!hasB1 || !hasB2 || !hasB3) {
        throw PatchException(
            "Registers ${b + 1}, ${b + 2}, or ${b + 3} not written in 12 instructions before Box call",
        )
    }
}

private fun hookSideNavRail(
    method: MutableMethod,
    drawBehindRefStr: String,
    themeRefs: ThemeRefs,
) {
    val literalIndex = SideNavRailFingerprint.instructionMatches.first().index
    val widthIndex = findWidthIndex(method, literalIndex)
    val vM = getTargetRegister(
        method.instructions.getOrNull(widthIndex + 1),
        Opcode.MOVE_RESULT_OBJECT,
        "width move-result",
    )
    val vT = getTargetRegister(
        method.instructions.getOrNull(widthIndex + 2),
        Opcode.SGET_OBJECT,
        "scratch sget",
    )

    val composerType = method.parameterTypes[5].toString()
    val (boxCallIndex, boxCallInst) = findRailBoxCall(method, widthIndex, composerType)
    val b = boxCallInst.startRegister
    verifyRailScratchRegisters(method, boxCallIndex, b)

    val railSmali = """
        invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS->railTabs(Ljava/lang/Object;)V
        invoke-static { }, $EXTENSION_CLASS->railDraw()Lkotlin/jvm/functions/Function1;
        move-result-object v$vT
        invoke-static { v$vM, v$vT }, $drawBehindRefStr
        move-result-object v$vM
        sget-object v${b + 1}, ${themeRefs.localFieldRef}
        invoke-virtual { v${b + 4}, v${b + 1} }, ${themeRefs.composerGetRef}
        move-result-object v${b + 1}
        check-cast v${b + 1}, ${themeRefs.themeType}
        iget-object v${b + 1}, v${b + 1}, ${themeRefs.colorsFieldRef}
        invoke-virtual { v${b + 1} }, ${themeRefs.bgColorRef}
        move-result-wide v${b + 2}
        invoke-static { v${b + 2}, v${b + 3} }, $EXTENSION_CLASS->railThemeColor(J)V
    """.trimIndent()

    method.addInstructions(widthIndex + 2, railSmali)
}

private fun hookNavScreenContentState() {
    val stateClass = NavScreenContentStateToStringFingerprint.classDefOrNull ?: return
    val constructorMethod = stateClass.methods.firstOrNull { method ->
        method.name == "<init>" &&
            method.parameterTypes.map { it.toString() } == EXPECTED_CONSTRUCTOR_PARAMS
    }.orThrow("Could not find NavScreenContentState constructor")
    val smali = """
        invoke-static { p2 }, $EXTENSION_CLASS->padContentForBottomNav(Z)Z
        move-result p2
    """.trimIndent()
    constructorMethod.addInstructions(0, smali)
}

private fun hookBottomNavBackdrop() {
    val (method, bgIndex) = findBottomNavBackdrop()
    val bgInst = method.instructions[bgIndex] as? FiveRegisterInstruction
        ?: throw PatchException("Backdrop instruction at $bgIndex is not FiveRegisterInstruction")
    val vD = bgInst.registerD
    val vE = bgInst.registerE

    val smali = if (vD > 15 || vE > 15) {
        """
            invoke-static/range { v$vD .. v$vE }, $EXTENSION_CLASS->barBackdrop(J)J
            move-result-wide v$vD
        """.trimIndent()
    } else {
        """
            invoke-static { v$vD, v$vE }, $EXTENSION_CLASS->barBackdrop(J)J
            move-result-wide v$vD
        """.trimIndent()
    }
    method.addInstructions(bgIndex, smali)
}
