package app.bin0culars.patches.reddit.misc.globalcontentfilter

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

object IndicatorsElementFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("IndicatorsElement(linkId="),
        string(", indicatorList="),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/util/List;"),
    ),
)
