package app.bin0culars.patches.reddit.layout.bottombar

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

object BottomBarStateChangeFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/launch/bottomnav/BottomNavScreen;",
    returnType = "V",
    parameters = listOf("F"),
    filters = listOf(
        newInstance("Lcom/reddit/launch/bottomnav/BottomNavScreen\$onNavBarStateChange\$1;"),
    ),
)

internal val unselectedButtonColorFilters = listOf(
    // Selected color.
    methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = listOf(), returnType = "J"),
    opcode(Opcode.MOVE_RESULT_WIDE, location = MatchAfterImmediately()),
    // Unselected color.
    methodCall(opcode = Opcode.INVOKE_VIRTUAL, parameters = listOf(), returnType = "J", location = MatchAfterWithin(6)),
    opcode(Opcode.MOVE_RESULT_WIDE, location = MatchAfterImmediately()),
)

internal object BottomNavButtonColorFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(string("bottom_nav_button")) + unselectedButtonColorFilters,
)

internal object BottomNavButtonLabelFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        string("bottom_nav_button"),
        methodCall(
            opcode = Opcode.INVOKE_DIRECT,
            name = "<init>",
            parameters = listOf("Ljava/lang/Object;", "Z", "I"),
        ),
        literal(-0x40550ece, location = MatchAfterImmediately()),
    ),
)
