package com.hamzabilici.yazki.core.turkish

/**
 * "En çok kullanılan kelimeler" listesinden elenecek dolgu kelimeleri ve kısaltmalar.
 * Katlanmış biçimdedir (ç→c, ı→i, ş→s ...).
 */
object StopWords {

    private val WORDS: Set<String> = """
        ve veya ile ama fakat ancak ya da de ki mi mu mi mu ne neden nasil nerede niye
        bir iki bu su o bunu sunu onu buna suna ona bunun sunun onun bunlar sunlar onlar
        ben sen biz siz beni seni bizi sizi bana sana bize size benim senin bizim sizin
        cok az daha en her hic hep tum butun bazi baska kadar gibi icin diye sonra once
        var yok degil evet hayir yani iste sey seyi simdi artik hem bile zaten belki
        olarak olan oldu olur oluyor ol ise ama mi musun misin miyim
        bi bisey birsey bisi cok tamam tmm tm ok okey
        slm nbr naber mrb merhaba sa as selam kib
        null the a an and or to of in is it you i
    """.trimIndent().split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()

    fun isStopWord(foldedWord: String): Boolean =
        foldedWord in WORDS || foldedWord.length < 2 || foldedWord.all { it.isDigit() }
}
