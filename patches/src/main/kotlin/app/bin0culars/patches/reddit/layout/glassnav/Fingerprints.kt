package app.bin0culars.patches.reddit.layout.glassnav

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method

internal const val EXTENSION_CLASS = "Lapp/bin0culars/extension/reddit/GlassNavPatch;"

internal fun <T> T?.orThrow(msg: String): T = this ?: throw PatchException(msg)

internal val EXPECTED_CONSTRUCTOR_PARAMS = listOf("Z", "Z", "Z", "F", "Z")

internal fun isBottomNavButtonMethod(method: Method): Boolean {
    val p = method.parameterTypes
    return method.returnType == "V" &&
        p.size == 13 &&
        p[1].toString() == "Lkotlin/jvm/functions/Function0;" &&
        p[2].toString() == "Ljava/lang/String;" &&
        p[3].toString() == "Z"
}

object BottomNavBarFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    definingClass = "Lcom/reddit/widget/bottomnav/a;",
    returnType = "V",
    parameters = listOf("L", "L", "F", "L", "L", "I"),
    filters = listOf(
        string("bottom_nav"),
    ),
)

object SideNavRailFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    definingClass = "Lcom/reddit/widget/bottomnav/a;",
    returnType = "V",
    parameters = listOf(
        "Landroidx/compose/runtime/internal/a;",
        "L",
        "L",
        "L",
        "Lkotlin/jvm/functions/Function2;",
        "L",
        "I",
    ),
    filters = listOf(
        literal(0x42c00000),
    ),
)

object SideNavRailContentFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;", "Ljava/lang/Object;", "Ljava/lang/Object;"),
    filters = listOf(
        literal(0x43fa0000),
        literal(0x42800000),
    ),
)

object NavScreenContentStateToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("NavScreenContentState(showBottomNav="),
    ),
)

object LottieDrawFingerprint : Fingerprint(
    definingClass = "Lcom/airbnb/lottie/compose/LottieAnimationKt\$LottieAnimation\$2;",
    name = "invoke",
    returnType = "V",
    parameters = listOf("L"),
)

object NavContentBoxFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        literal(0x42c00000),
        methodCall(definingClass = "Landroidx/compose/ui/b;", name = "c"),
    ),
    custom = { method, _ ->
        val p = method.parameterTypes.map { it.toString() }
        (p.size == 8 || p.size == 9) && p.take(4) == listOf("Z", "Z", "Z", "Z") &&
            p.contains("Landroidx/compose/runtime/internal/a;") && p.last() == "I"
    },
)

object BottomBarSlideOffsetFingerprint : Fingerprint(
    name = "invoke",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Number;", name = "floatValue"),
        methodCall(definingClass = "Ljava/lang/Math;", name = "max"),
        opcode(Opcode.NEG_INT),
        opcode(Opcode.INT_TO_LONG),
        literal(0xffffffffL),
    ),
    custom = { _, classDef ->
        classDef.fields.any { it.type == "Lcom/reddit/launch/bottomnav/BottomNavScreen;" }
    },
)

object BottomNavButtonFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/widget/bottomnav/a;",
    returnType = "V",
    filters = listOf(
        string("bottom_nav_button"),
    ),
    custom = { method, _ -> isBottomNavButtonMethod(method) },
)

object BottomBarSlidePlaceFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        methodCall(definingClass = "Lkotlin/jvm/functions/Function0;", name = "invoke"),
        methodCall(definingClass = "Ljava/lang/Number;", name = "floatValue"),
        opcode(Opcode.SUB_FLOAT_2ADDR),
        opcode(Opcode.IGET, location = MatchAfterImmediately()),
        opcode(Opcode.INT_TO_FLOAT, location = MatchAfterImmediately()),
        opcode(Opcode.MUL_FLOAT_2ADDR, location = MatchAfterImmediately()),
    ),
)
