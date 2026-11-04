package com.hamzabilici.yazki.data

/**
 * Sohbetin türü. Analiz aynıdır; tür, karnede hangi kartların ve sayaçların anlamlı olduğunu belirler
 * (ör. arkadaş grubunda "En romantik" çıkmaz, aile grubunda "Hayırlı cumalar" öne çıkar).
 */
enum class ChatType(val isGroup: Boolean) {
    FRIEND(isGroup = false),
    FRIEND_GROUP(isGroup = true),
    PARTNER(isGroup = false),
    FAMILY(isGroup = true),
    WORK_SCHOOL(isGroup = true),
    ;

    companion object {
        /** Katılımcı sayısına göre ilk öneri; kullanıcı değiştirebilir. */
        fun suggestFor(participants: Int): ChatType = if (participants > 2) FRIEND_GROUP else FRIEND

        fun fromName(name: String?): ChatType? = entries.firstOrNull { it.name == name }
    }
}

/**
 * Her sohbete ayrı analiz ayarları.
 *
 * | countProfanity | censor | Sonuç |
 * |---|---|---|
 * | açık | açık | "En çok küfür eden" ve sayılar görünür, küfürlerin kendisi hiçbir yerde görünmez |
 * | açık | kapalı | Sayılar + kişinin en sık küfürü karnede; kelime listelerinde küfürler de çıkar |
 * | kapalı | açık | Küfür kartı/satırı yok, kelime listeleri küfürsüz |
 * | kapalı | kapalı | Küfür kartı/satırı yok, kelime listelerinde küfürler çıkabilir |
 */
data class ChatOptions(
    val type: ChatType,
    /** Küfürler sayılıp karnede gösterilsin mi? */
    val countProfanity: Boolean = true,
    /** Açıkken küfürlerin kendisi hiçbir yerde görünmez; yalnızca sayıları gösterilir. */
    val censor: Boolean = true,
) {
    companion object {
        fun defaultFor(participants: Int) = ChatOptions(ChatType.suggestFor(participants))
    }
}
