package app.bin0culars.patches.reddit.layout.glassnav

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val BUTTON_PARAM_COUNT = 13
private const val SELECTED_PARAM_INDEX = 3
private const val MODIFIER_PARAM_INDEX = 4
private const val EXPECTED_VARIANT_COUNT = 2
private const val FUNCTION0_TYPE = "Lkotlin/jvm/functions/Function0;"
private const val FUNCTION2_TYPE = "Lkotlin/jvm/functions/Function2;"
private const val STRING_TYPE = "Ljava/lang/String;"
private const val COMPOSABLE_LAMBDA_TYPE = "Landroidx/compose/runtime/internal/a;"

private val EXPECTED_EXACT_PARAMS = mapOf(
    1 to FUNCTION0_TYPE,
    2 to STRING_TYPE,
    3 to "Z",
    5 to FUNCTION0_TYPE,
    6 to STRING_TYPE,
    7 to STRING_TYPE,
    8 to FUNCTION2_TYPE,
    9 to FUNCTION2_TYPE,
    10 to COMPOSABLE_LAMBDA_TYPE,
    12 to "I",
)
private val EXPECTED_OBJECT_INDICES = listOf(0, 4, 11)

private fun isNavButtonVariant(method: Method): Boolean {
    val isStatic = (method.accessFlags and AccessFlags.STATIC.value) != 0
    val params = method.parameterTypes
    return isStatic &&
        method.implementation != null &&
        method.returnType == "V" &&
        params.size == BUTTON_PARAM_COUNT &&
        EXPECTED_EXACT_PARAMS.all { (index, type) -> params[index].toString() == type } &&
        EXPECTED_OBJECT_INDICES.all { index -> params[index].toString().startsWith("L") }
}

private fun hookNavButtonPill(method: MutableMethod) {
    val selectedReg = paramRegisterIndex(method, SELECTED_PARAM_INDEX)
    val modReg = paramRegisterIndex(method, MODIFIER_PARAM_INDEX)
    if (modReg != selectedReg + 1) {
        return
    }
    val modifierType = method.parameterTypes[MODIFIER_PARAM_INDEX].toString()
    val hookCall = "$EXTENSION_CLASS->navItemModifier(ZLjava/lang/Object;)Ljava/lang/Object;"
    method.addInstructions(
        0,
        """
            invoke-static/range { p$selectedReg .. p$modReg }, $hookCall
            move-result-object p$modReg
            check-cast p$modReg, $modifierType
        """.trimIndent(),
    )

    val spaceLiteral = 8f.toRawBits()
    val spaceIndex = method.instructions.indexOfFirst { inst ->
        inst is NarrowLiteralInstruction && inst.narrowLiteral == spaceLiteral
    }.takeIf { it >= 0 }.orThrow("Could not find 8f literal in nav button method")
    val spaceRegister = method.getInstruction<OneRegisterInstruction>(spaceIndex).registerA
    method.addInstructions(
        spaceIndex + 1,
        """
            invoke-static/range { v$spaceRegister .. v$spaceRegister }, $EXTENSION_CLASS->buttonTopSpace(F)F
            move-result v$spaceRegister
        """.trimIndent(),
    )
}

internal fun hookNavPill() {
    val buttonClass = BottomNavButtonFingerprint.classDefOrNull ?: return
    val targets = buttonClass.methods.filter(::isNavButtonVariant)
    if (targets.size != EXPECTED_VARIANT_COUNT) {
        return
    }
    targets.forEach(::hookNavButtonPill)
}
