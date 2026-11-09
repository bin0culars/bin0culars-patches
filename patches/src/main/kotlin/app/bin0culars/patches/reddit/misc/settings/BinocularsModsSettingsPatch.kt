package app.bin0culars.patches.reddit.misc.settings

import app.bin0culars.patches.reddit.shared.Constants.COMPATIBILITY_REDDIT
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceXml
import app.morphe.patcher.util.findFreeRegister
import app.morphe.patcher.util.p0Register

private const val EXTENSION_CLASS = "Lapp/bin0culars/extension/reddit/settings/BinocularsMods;"

@Suppress("unused")
val binocularsModsSettingsPatch =
    patch(
        description = "Adds the bin0culars mods settings section.",
    ) {
        compatibleWith(COMPATIBILITY_REDDIT)

        extendWith("extensions/extension.mpe")

        execute {
            resourceXml("res/values/strings.xml").use { strings ->
                strings.resource("label_content_policy").text = "bin0culars mods"
                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_mods_title")
                    text = "bin0culars mods"
                }
                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_tablet_grid_title")
                    text = "Tablet grid feed"
                }
                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_tablet_grid_summary")
                    text =
                        "Shows Home, Popular and community feeds as a 2–3 column grid on tablets and other wide screens. Phones are unchanged."
                }
                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_restart_hint")
                    text = "Restart Reddit to apply changes."
                }
            }

            StartUrlActivityFingerprint.let {
                val index = it.instructionMatches.last().index
                it.method.apply {
                    val p0Register = p0Register
                    val free = findFreeRegister(index, p0Register + 1, p0Register + 2)
                    addInstructionsWithLabels(
                        index,
                        """
                            invoke-static { p1, p2 }, $EXTENSION_CLASS->openIfOurs(Landroid/app/Activity;Landroid/net/Uri;)Z
                            move-result v$free
                            if-eqz v$free, :ignore
                            return-void
                            :ignore
                            nop
                        """,
                    )
                }
            }
        }
    }
