package com.nuvio.tv.core.danexus

import com.nuvio.tv.domain.model.MetaCastMember

object DanexusPeopleCredits {
    fun isDirector(member: MetaCastMember): Boolean =
        member.character.equals("Director", true) || member.character.equals("Creator", true)
    fun isWriter(member: MetaCastMember): Boolean =
        member.character.equals("Writer", true) || member.character.equals("Screenplay", true)

    fun visible(cast: List<MetaCastMember>, directors: List<MetaCastMember>): List<MetaCastMember> =
        (directors + cast).filter { it.name.isNotBlank() && !isWriter(it) }
            .distinctBy { it.tmdbId?.takeIf { id -> id > 0 }?.toString() ?: it.name.trim().lowercase() }
}
