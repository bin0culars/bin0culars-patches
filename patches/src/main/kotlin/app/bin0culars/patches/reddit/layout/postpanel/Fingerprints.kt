package app.bin0culars.patches.reddit.layout.postpanel

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.newInstance
import com.android.tools.smali.dexlib2.AccessFlags

object PostDetailPagerPresentationFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/frontpage/presentation/listing/linkpager/refactor/PostDetailPagerScreen;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "L",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            name = "<init>",
            parameters = listOf("Z", "Z", "Z", "Z", "Z", "I"),
        ),
    ),
)

object ModalPresentationFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/emailcollection/screens/addemail/EmailCollectionAddEmailScreen;",
    name = "<init>",
    filters = listOf(
        methodCall(
            name = "<init>",
            parameters = listOf(
                "Z",
                "Lkotlin/jvm/functions/Function0;",
                "Lkotlin/jvm/functions/Function2;",
                "I",
            ),
        ),
    ),
)

object ScreenCreateViewFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/screen/BaseScreen;",
    returnType = "Landroid/view/View;",
    parameters = listOf("Landroid/view/LayoutInflater;", "Landroid/view/ViewGroup;"),
    filters = listOf(
        methodCall(smali = "Lcom/reddit/screen/dialog/ModalBackdropView;->setOnClickedOutside(Lkotlin/jvm/functions/Function0;)V"),
    ),
)

object FeedCardOpenPostFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "L",
        "Lcom/reddit/frontpage/presentation/listing/linkpager/refactor/PostDetailPagerScreen;",
        "Lcom/reddit/listing/common/ListingType;",
        "L",
    ),
    filters = listOf(
        newInstance("Lcom/reddit/frontpage/presentation/detail/transition/BaliDetailScreenChangeHandler;"),
    ),
)
