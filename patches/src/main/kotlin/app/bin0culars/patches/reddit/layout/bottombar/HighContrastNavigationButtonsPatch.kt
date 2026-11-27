package app.bin0culars.patches.reddit.layout.bottombar

import app.bin0culars.patches.reddit.misc.settings.BinocularsModsSection.NAVIGATION
import app.bin0culars.patches.reddit.misc.settings.BinocularsModsSetting
import app.bin0culars.patches.reddit.misc.settings.addBinocularsModsSetting
import app.bin0culars.patches.reddit.misc.settings.binocularsModsSettingsPatch
import app.bin0culars.patches.reddit.shared.Constants.COMPATIBILITY_REDDIT
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS = "Lapp/bin0culars/extension/reddit/HighContrastNavigationButtonsPatch;"

@Suppress("unused")
val highContrastNavigationButtonsPatch =
    patch(
        name = "High-contrast navigation buttons",
        description = "Shows the unselected buttons of the navigation bar in black or white instead of grey. Turn it on in Settings → bin0culars mods.",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_REDDIT)
        dependsOn(binocularsModsSettingsPatch)
        extendWith("extensions/extension.mpe")

        execute {
            addBinocularsModsSetting(
                BinocularsModsSetting(
                    key = "high_contrast_navigation_buttons",
                    section = NAVIGATION,
                    title = "High-contrast navigation buttons",
                    summary = "Shows unselected navigation bar buttons in black in light mode and white in dark mode, instead of grey. Restart the app to apply.",
                    default = false,
                ),
            )

            val matches = BottomNavButtonColorFingerprint.matchAll()
            if (matches.isEmpty()) {
                throw PatchException("BottomNavButtonColorFingerprint did not match any methods.")
            }

            val labelClass = BottomNavButtonLabelFingerprint.instructionMatches[1].methodReference.definingClass
            val labelColorMatch = Fingerprint(
                definingClass = labelClass,
                name = "invoke",
                returnType = "Ljava/lang/Object;",
                parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;"),
                filters = unselectedButtonColorFilters,
            ).match()

            (matches + labelColorMatch).forEach { match ->
                val colorMatch = match.instructionMatches.last()
                val register = (colorMatch.instruction as OneRegisterInstruction).registerA

                match.method.addInstructions(
                    colorMatch.index + 1,
                    """
                        invoke-static/range { v$register .. v${register + 1} }, $EXTENSION_CLASS->getUnselectedButtonColor(J)J
                        move-result-wide v$register
                    """,
                )
            }
        }
    }
