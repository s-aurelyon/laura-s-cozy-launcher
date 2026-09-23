package app.cozy.launcher.data

import java.time.DayOfWeek
import java.time.LocalDate

/** South African public holidays, including the Monday-after-Sunday rule. */
object Holidays {
    private val cache = HashMap<Int, Map<LocalDate, String>>()

    fun name(date: LocalDate): String? = forYear(date.year)[date]

    fun forYear(year: Int): Map<LocalDate, String> = cache.getOrPut(year) {
        val m = LinkedHashMap<LocalDate, String>()
        m[LocalDate.of(year, 1, 1)] = "New Year's Day"
        m[LocalDate.of(year, 3, 21)] = "Human Rights Day"
        m[LocalDate.of(year, 4, 27)] = "Freedom Day"
        m[LocalDate.of(year, 5, 1)] = "Workers' Day"
        m[LocalDate.of(year, 6, 16)] = "Youth Day"
        m[LocalDate.of(year, 8, 9)] = "Women's Day"
        m[LocalDate.of(year, 9, 24)] = "Heritage Day"
        m[LocalDate.of(year, 12, 16)] = "Day of Reconciliation"
        m[LocalDate.of(year, 12, 25)] = "Christmas Day"
        m[LocalDate.of(year, 12, 26)] = "Day of Goodwill"
        val easter = easterSunday(year)
        m[easter.minusDays(2)] = "Good Friday"
        m[easter.plusDays(1)] = "Family Day"
        val observed = HashMap<LocalDate, String>()
        for ((d, n) in m) {
            if (d.dayOfWeek == DayOfWeek.SUNDAY) {
                val monday = d.plusDays(1)
                if (!m.containsKey(monday)) observed[monday] = "$n (observed)"
            }
        }
        m.putAll(observed)
        m
    }

    /** Anonymous Gregorian algorithm. */
    private fun easterSunday(y: Int): LocalDate {
        val a = y % 19
        val b = y / 100
        val c = y % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(y, month, day)
    }
}
