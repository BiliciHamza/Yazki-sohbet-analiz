package com.hamzabilici.yazki.ui

import androidx.annotation.StringRes
import com.hamzabilici.yazki.R
import com.hamzabilici.yazki.data.ChatType
import com.hamzabilici.yazki.data.ChatType.FAMILY
import com.hamzabilici.yazki.data.ChatType.FRIEND
import com.hamzabilici.yazki.data.ChatType.FRIEND_GROUP
import com.hamzabilici.yazki.data.ChatType.PARTNER
import com.hamzabilici.yazki.data.ChatType.WORK_SCHOOL
import com.hamzabilici.yazki.core.turkish.PhraseCategory

/** Sohbet türünün görünümü: emoji, ad ve kısa açıklama. */
val ChatType.emoji: String
    get() = when (this) {
        FRIEND -> "🤝"
        FRIEND_GROUP -> "🎉"
        PARTNER -> "💞"
        FAMILY -> "🏡"
        WORK_SCHOOL -> "💼"
    }

@get:StringRes
val ChatType.title: Int
    get() = when (this) {
        FRIEND -> R.string.type_friend
        FRIEND_GROUP -> R.string.type_friend_group
        PARTNER -> R.string.type_partner
        FAMILY -> R.string.type_family
        WORK_SCHOOL -> R.string.type_work
    }

@get:StringRes
val ChatType.subtitle: Int
    get() = when (this) {
        FRIEND -> R.string.type_friend_sub
        FRIEND_GROUP -> R.string.type_friend_group_sub
        PARTNER -> R.string.type_partner_sub
        FAMILY -> R.string.type_family_sub
        WORK_SCHOOL -> R.string.type_work_sub
    }

/** Karne'deki öne çıkan kart türleri. */
enum class HighlightKind {
    TOTAL, TOP_TALKER, STARTER, DAY_STARTER, FASTEST, NIGHT_OWL, LAUGHTER, ADDRESS,
    MORNING, GOOD_NIGHT, HOW_ARE_YOU, THANKS, FRIDAY, LOVE, APOLOGY, OKAY,
    DOUBLE_TEXT, PROFANITY, AFTER_HOURS, QUESTIONS, DOCUMENTS, PHOTOS, VOICE, LINKS,
    STREAK, BUSIEST_DAY, EMOJI, SILENCE,
}

/**
 * Her sohbet türünde hangi kartların hangi sırayla çıkacağı.
 *
 * İlkeler:
 * - **Sevgili & Flört:** romantizm, günaydın / iyi geceler, kim önce yazıyor, kim bekletiyor.
 * - **Arkadaş / Arkadaş grubu:** kahkaha, hitaplar, gece kuşu, küfür; romantizm yok.
 * - **Aile:** günaydın, hayırlı cumalar, fotoğraf ve link paylaşanlar, sevecenlik (romantizm değil).
 * - **İş & Okul:** soru, dosya, mesai dışı mesaj, teşekkür; romantizm ve gece kuşu öne çıkmaz.
 *
 * Verisi olmayan kartlar (sayı 0) zaten listeye girmez.
 */
fun ChatType.highlightOrder(): List<HighlightKind> = when (this) {
    FRIEND -> listOf(
        HighlightKind.TOTAL, HighlightKind.TOP_TALKER, HighlightKind.STARTER, HighlightKind.DAY_STARTER,
        HighlightKind.FASTEST, HighlightKind.LAUGHTER, HighlightKind.ADDRESS, HighlightKind.HOW_ARE_YOU,
        HighlightKind.NIGHT_OWL, HighlightKind.PROFANITY, HighlightKind.STREAK, HighlightKind.BUSIEST_DAY,
        HighlightKind.EMOJI, HighlightKind.DOUBLE_TEXT, HighlightKind.VOICE, HighlightKind.OKAY,
        HighlightKind.APOLOGY, HighlightKind.SILENCE,
    )
    FRIEND_GROUP -> listOf(
        HighlightKind.TOTAL, HighlightKind.TOP_TALKER, HighlightKind.STARTER, HighlightKind.DAY_STARTER,
        HighlightKind.LAUGHTER, HighlightKind.FASTEST, HighlightKind.NIGHT_OWL, HighlightKind.PROFANITY,
        HighlightKind.ADDRESS, HighlightKind.VOICE, HighlightKind.PHOTOS, HighlightKind.LINKS,
        HighlightKind.BUSIEST_DAY, HighlightKind.STREAK, HighlightKind.EMOJI, HighlightKind.OKAY,
        HighlightKind.SILENCE,
    )
    PARTNER -> listOf(
        HighlightKind.TOTAL, HighlightKind.TOP_TALKER, HighlightKind.STARTER, HighlightKind.DAY_STARTER,
        HighlightKind.MORNING, HighlightKind.GOOD_NIGHT, HighlightKind.LOVE, HighlightKind.ADDRESS,
        HighlightKind.FASTEST, HighlightKind.DOUBLE_TEXT, HighlightKind.HOW_ARE_YOU, HighlightKind.APOLOGY,
        HighlightKind.LAUGHTER, HighlightKind.NIGHT_OWL, HighlightKind.STREAK, HighlightKind.BUSIEST_DAY,
        HighlightKind.EMOJI, HighlightKind.VOICE, HighlightKind.PROFANITY, HighlightKind.OKAY,
        HighlightKind.SILENCE,
    )
    FAMILY -> listOf(
        HighlightKind.TOTAL, HighlightKind.TOP_TALKER, HighlightKind.DAY_STARTER, HighlightKind.MORNING,
        HighlightKind.GOOD_NIGHT, HighlightKind.FRIDAY, HighlightKind.LOVE, HighlightKind.PHOTOS,
        HighlightKind.VOICE, HighlightKind.LINKS, HighlightKind.HOW_ARE_YOU, HighlightKind.THANKS,
        HighlightKind.FASTEST, HighlightKind.LAUGHTER, HighlightKind.ADDRESS, HighlightKind.BUSIEST_DAY,
        HighlightKind.STREAK, HighlightKind.EMOJI, HighlightKind.PROFANITY,
    )
    WORK_SCHOOL -> listOf(
        HighlightKind.TOTAL, HighlightKind.TOP_TALKER, HighlightKind.STARTER, HighlightKind.FASTEST,
        HighlightKind.QUESTIONS, HighlightKind.DOCUMENTS, HighlightKind.AFTER_HOURS, HighlightKind.THANKS,
        HighlightKind.MORNING, HighlightKind.OKAY, HighlightKind.LINKS, HighlightKind.APOLOGY,
        HighlightKind.BUSIEST_DAY, HighlightKind.LAUGHTER, HighlightKind.EMOJI, HighlightKind.PROFANITY,
        HighlightKind.SILENCE,
    )
}

/** İstatistik'teki Türkçe sayaç tablosunda bu türde anlamlı olan kategoriler. */
fun ChatType.showsPhrase(c: PhraseCategory): Boolean = when (c) {
    PhraseCategory.LOVE -> this == PARTNER || this == FAMILY
    PhraseCategory.FRIDAY_GREETING -> this == FAMILY
    PhraseCategory.HOW_ARE_YOU, PhraseCategory.ON_MY_WAY -> this != WORK_SCHOOL
    else -> true
}

/** Mesai dışı mesaj satırı yalnızca iş ve okul gruplarında anlamlı. */
val ChatType.showsAfterHours: Boolean get() = this == WORK_SCHOOL
