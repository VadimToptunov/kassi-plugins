package io.github.vadimtoptunov.kassitestdata.algo

/** Validators for Russian identifiers — ИНН (individual & legal), СНИЛС, ОГРН, ОГРНИП. */
object RuIdChecksums {

    private val INN10_W = intArrayOf(2, 4, 10, 3, 5, 9, 4, 6, 8)
    private val INN12_W11 = intArrayOf(7, 2, 4, 10, 3, 5, 9, 4, 6, 8)
    private val INN12_W12 = intArrayOf(3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8)

    fun isValidInnLegal(value: String): Boolean {
        if (value.length != 10 || !value.all { it in '0'..'9' }) return false
        var sum = 0
        for (i in 0..8) sum += (value[i] - '0') * INN10_W[i]
        return (sum % 11) % 10 == (value[9] - '0')
    }

    fun isValidInnIndividual(value: String): Boolean {
        if (value.length != 12 || !value.all { it in '0'..'9' }) return false
        var s11 = 0
        for (i in 0..9) s11 += (value[i] - '0') * INN12_W11[i]
        var s12 = 0
        for (i in 0..10) s12 += (value[i] - '0') * INN12_W12[i]
        return (s11 % 11) % 10 == (value[10] - '0') && (s12 % 11) % 10 == (value[11] - '0')
    }

    fun isValidSnils(value: String): Boolean {
        if (value.length != 11 || !value.all { it in '0'..'9' }) return false
        var sum = 0
        for (i in 0..8) sum += (value[i] - '0') * (9 - i)
        val c = sum % 101
        return (if (c == 100) 0 else c) == value.substring(9).toInt()
    }

    fun isValidOgrn(value: String): Boolean {
        if (value.length != 13 || !value.all { it in '0'..'9' }) return false
        return ((value.substring(0, 12).toLong() % 11) % 10).toInt() == (value[12] - '0')
    }

    fun isValidOgrnip(value: String): Boolean {
        if (value.length != 15 || !value.all { it in '0'..'9' }) return false
        return ((value.substring(0, 14).toLong() % 13) % 10).toInt() == (value[14] - '0')
    }
}
