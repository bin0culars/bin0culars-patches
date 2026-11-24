package app.bin0culars.patches.reddit.layout.glassnav

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

internal fun hookBarSlide() {
    val method = BottomBarSlideOffsetFingerprint.method
    val maxIndex = BottomBarSlideOffsetFingerprint.instructionMatches[1].index
    val moveResultInst = method.instructions.getOrNull(maxIndex + 1)
        .orThrow("Missing instruction after Math.max call")
    if (moveResultInst.opcode != Opcode.MOVE_RESULT) {
        throw PatchException("Expected MOVE_RESULT after Math.max call, found ${moveResultInst.opcode}")
    }
    val vR = (moveResultInst as? OneRegisterInstruction)?.registerA
        .orThrow("Could not get register from MOVE_RESULT")

    val vF = (method.instructions.subList(0, maxIndex).lastOrNull { it.opcode == Opcode.INT_TO_FLOAT } as? TwoRegisterInstruction)?.registerA
        .orThrow("Could not find INT_TO_FLOAT before Math.max")

    if (vR > 15 || vF > 15) {
        throw PatchException("Registers out of range for non-range invoke: vR=$vR, vF=$vF")
    }

    method.addInstructions(
        maxIndex + 2,
        "invoke-static { v$vR, v$vF }, $BLUR_EXTENSION_CLASS->onBarLift(IF)V",
    )
}
