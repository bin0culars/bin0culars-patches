package app.bin0culars.patches.reddit.misc.globalcontentfilter

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.anyInstruction
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.instanceOf
import app.morphe.patcher.newInstance
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

object RedditCachedFeedPagerFingerprint : Fingerprint(
    filters = listOf(
        newInstance("Lcom/reddit/feeds/impl/domain/paging/RedditCachedFeedPager\$load\$2;"),
    ),
    custom = { _, classDef -> !classDef.type.contains('$') },
)

object RedditFeedPagerFingerprint : Fingerprint(
    filters = listOf(
        anyInstruction(
            checkCast("Lcom/reddit/feeds/impl/domain/paging/RedditFeedPager\$fetchAndPersistLinks\$1;"),
            instanceOf("Lcom/reddit/feeds/impl/domain/paging/RedditFeedPager\$fetchAndPersistLinks\$1;"),
            newInstance("Lcom/reddit/feeds/impl/domain/paging/RedditFeedPager\$fetchAndPersistLinks\$1;"),
        ),
    ),
    custom = { _, classDef -> !classDef.type.contains('$') },
)

object SearchTypeaheadListFeedElementFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    filters = listOf(
        string(", id="),
        string("SearchTypeaheadListFeedElement(identifier="),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/lang/String;"),
    ),
)
