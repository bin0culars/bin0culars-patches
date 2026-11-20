package app.bin0culars.patches.reddit.misc.globalcontentfilter

import app.bin0culars.patches.reddit.layout.contenttypefilter.listRegisterOf
import app.bin0culars.patches.reddit.layout.contenttypefilter.methodRef
import app.bin0culars.patches.reddit.misc.samesubredditmedia.RecommendedMediaRequestFingerprint
import app.bin0culars.patches.reddit.misc.samesubredditmedia.RecommendedVideosRequestFingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.Opcode

internal fun hookFullscreenMedia() {
    val methods = listOf(
        RecommendedMediaRequestFingerprint.method,
        RecommendedVideosRequestFingerprint.method,
    )
    for (method in methods) {
        val instructions = method.instructions.toList()
        val index = instructions.indexOfFirst { instr ->
            (instr.opcode == Opcode.INVOKE_DIRECT_RANGE || instr.opcode == Opcode.INVOKE_DIRECT) &&
                instr.methodRef()?.let { ref ->
                    ref.definingClass == "Lcom/reddit/domain/model/listing/Listing;" &&
                        ref.name == "<init>" &&
                        ref.parameterTypes.firstOrNull()?.toString() == "Ljava/util/List;"
                } == true
        }
        if (index == -1) {
            throw PatchException("Could not find Listing constructor call in ${method.name}")
        }
        val listRegister = listRegisterOf(instructions[index])
        if (listRegister >= 16) {
            throw PatchException("List register must be < 16, got $listRegister in ${method.name}")
        }
        method.addInstructions(
            index,
            """
                invoke-static { v$listRegister }, $EXTENSION_CLASS->filterMediaLinks(Ljava/util/List;)Ljava/util/List;
                move-result-object v$listRegister
            """.trimIndent(),
        )
    }
}

internal fun fillSearchPostNsfwBridge() {
    val postInfoType = SearchPostInfoFingerprint.classDef.type
    val isNsfwField = SearchPostInfoFingerprint.instructionMatches.last().fieldReference

    val postElementType = SearchDynamicPostElementFingerprint.classDef.type
    val postElementField = SearchDynamicPostElementFingerprint.classDef.fields.firstOrNull { it.type == postInfoType }
        ?: throw PatchException("Could not find field of type $postInfoType in SearchDynamicPostElement")

    val mediaElementType = SearchDynamicMediaElementFingerprint.classDef.type
    val mediaElementField = SearchDynamicMediaElementFingerprint.classDef.fields.firstOrNull { it.type == postInfoType }
        ?: throw PatchException("Could not find field of type $postInfoType in SearchDynamicMediaElement")

    val smali = """
        if-eqz p0, :not_post
        instance-of p1, p0, $postElementType
        if-nez p1, :is_post
        instance-of p1, p0, $mediaElementType
        if-nez p1, :is_media
        :not_post
        const/4 p1, -0x1
        return p1
        :is_post
        check-cast p0, $postElementType
        iget-object p0, p0, $postElementType->${postElementField.name}:$postInfoType
        goto :check_post
        :is_media
        check-cast p0, $mediaElementType
        iget-object p0, p0, $mediaElementType->${mediaElementField.name}:$postInfoType
        :check_post
        if-eqz p0, :null_post
        iget-boolean p1, p0, $postInfoType->${isNsfwField.name}:Z
        return p1
        :null_post
        const/4 p1, 0x0
        return p1
    """.trimIndent()
    fillBridge("searchPostNsfw", smali)
}

internal fun fillSearchCommunityNsfwBridge() {
    val communityType = SearchCommunityFingerprint.classDef.type
    val over18Field = SearchCommunityFingerprint.instructionMatches.last().fieldReference

    val communityElementType = SearchDynamicCommunityElementFingerprint.classDef.type
    val communityElementField = SearchDynamicCommunityElementFingerprint.classDef.fields.firstOrNull { it.type == communityType }
        ?: throw PatchException("Could not find field of type $communityType in SearchDynamicCommunityElement")

    val smali = """
        if-eqz p0, :not_community
        instance-of p1, p0, $communityElementType
        if-eqz p1, :not_community
        check-cast p0, $communityElementType
        iget-object p0, p0, $communityElementType->${communityElementField.name}:$communityType
        if-eqz p0, :null_or_false
        iget-object p0, p0, $communityType->${over18Field.name}:Ljava/lang/Boolean;
        if-eqz p0, :null_or_false
        invoke-virtual {p0}, Ljava/lang/Boolean;->booleanValue()Z
        move-result p1
        return p1
        :null_or_false
        const/4 p1, 0x0
        return p1
        :not_community
        const/4 p1, -0x1
        return p1
    """.trimIndent()
    fillBridge("searchCommunityNsfw", smali)
}

internal fun fillLinkOver18Bridge() {
    val smali = """
        if-eqz p0, :not_link
        instance-of p1, p0, Lcom/reddit/domain/model/Link;
        if-eqz p1, :not_link
        check-cast p0, Lcom/reddit/domain/model/Link;
        invoke-virtual {p0}, Lcom/reddit/domain/model/Link;->getOver18()Z
        move-result p1
        return p1
        :not_link
        const/4 p1, -0x1
        return p1
    """.trimIndent()
    fillBridge("linkOver18", smali)
}

internal fun fillTypeaheadSectionIdBridge() {
    val listType = SearchTypeaheadListFeedElementFingerprint.classDef.type
    val listIdField = SearchTypeaheadListFeedElementFingerprint.instructionMatches.last().fieldReference
    val suggestionType = SearchTypeaheadSuggestionFeedElementFingerprint.classDef.type
    val suggestionIdField = SearchTypeaheadSuggestionFeedElementFingerprint.instructionMatches.last().fieldReference

    val smali = """
        if-eqz p0, :null_id
        instance-of p1, p0, $listType
        if-eqz p1, :try_suggestion
        check-cast p0, $listType
        iget-object p0, p0, $listType->${listIdField.name}:Ljava/lang/String;
        return-object p0
        :try_suggestion
        instance-of p1, p0, $suggestionType
        if-eqz p1, :null_id
        check-cast p0, $suggestionType
        iget-object p0, p0, $suggestionType->${suggestionIdField.name}:Ljava/lang/String;
        return-object p0
        :null_id
        const/4 p0, 0x0
        return-object p0
    """.trimIndent()
    fillBridge("typeaheadSectionId", smali)
}
