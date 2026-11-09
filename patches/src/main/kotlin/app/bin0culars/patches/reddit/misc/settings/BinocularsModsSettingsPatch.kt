package app.bin0culars.patches.reddit.misc.settings

import app.bin0culars.patches.reddit.shared.Constants.COMPATIBILITY_REDDIT
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
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
            registeredSettings.clear()

            resourceXml("res/values/strings.xml").use { strings ->
                strings.resource("label_content_policy").text = "bin0culars mods"
                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_mods_title")
                    text = "bin0culars mods"
                }
                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_back")
                    text = "Back"
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

        finalize {
            resourceXml("res/values/strings.xml").use { strings ->
                for (setting in registeredSettings) {
                    strings.root.appendChild("string") {
                        setAttribute("name", "bin0culars_${setting.key}_title")
                        text = setting.title
                    }
                    strings.root.appendChild("string") {
                        setAttribute("name", "bin0culars_${setting.key}_summary")
                        text = setting.summary
                    }
                    for (choice in setting.choices) {
                        strings.root.appendChild("string") {
                            setAttribute("name", "bin0culars_${setting.key}_choice_${choice.value}")
                            text = choice.label
                        }
                    }
                }

                val usedSections = registeredSettings.map { it.section }.distinct()
                for (section in usedSections) {
                    strings.root.appendChild("string") {
                        setAttribute("name", "bin0culars_section_${section.id}")
                        text = section.title
                    }
                }

                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_restart_hint")
                    text = "Restart Reddit to apply your changes."
                }
                strings.root.appendChild("string") {
                    setAttribute("name", "bin0culars_restart")
                    text = "Restart"
                }
            }

            val encoded = registeredSettings.joinToString("\n") { setting ->
                val type = if (setting.choices.isNotEmpty()) "C" else "S"
                val defaultValue = if (type == "S") {
                    if (setting.default) "1" else "0"
                } else {
                    setting.defaultChoice ?: ""
                }
                val choices = setting.choices.joinToString(",") { it.value }
                "${setting.key}|${setting.section.id}|${setting.parentKey ?: ""}|$type|$defaultValue|$choices"
            }

            val escaped = encoded
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")

            Fingerprint(definingClass = EXTENSION_CLASS, name = "registryData").method.addInstructions(
                0,
                """
                    const-string v0, "$escaped"
                    return-object v0
                """,
            )
        }
    }
