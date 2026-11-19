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

object SearchPostInfoFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("SearchPostInfo(id="),
        string(", isNsfw="),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN, type = "Z"),
    ),
)

object SearchCommunityFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("SearchCommunity(subredditId="),
        string(", over18="),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/lang/Boolean;"),
    ),
)

object SearchDynamicPostElementFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("SearchDynamicPostElement(post="),
    ),
)

object SearchDynamicMediaElementFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("SearchDynamicMediaElement(post="),
    ),
)

object SearchDynamicCommunityElementFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string("SearchDynamicCommunityElement(id="),
    ),
)
