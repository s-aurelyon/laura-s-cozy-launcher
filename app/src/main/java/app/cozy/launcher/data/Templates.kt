package app.cozy.launcher.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class TemplateInfo(val id: String, val name: String, val desc: String, val category: String, val paper: String)

object Templates {
    val all = listOf(
        TemplateInfo("blank", "Blank", "Type or sketch freely", "Paper", "blank"),
        TemplateInfo("lined", "Lined", "Classic notebook", "Paper", "lined"),
        TemplateInfo("dotted", "Dotted", "Bullet-journal style", "Paper", "dotted"),
        TemplateInfo("grid", "Grid", "Squares for layouts", "Paper", "grid"),
        TemplateInfo("checklist", "Checklist", "Tick things off", "Lists", "blank"),
        TemplateInfo("table", "Table", "Rows and columns", "Lists", "blank"),
        TemplateInfo("daily", "Daily page", "Schedule, to-dos, notes", "Planners", "dotted"),
        TemplateInfo("weekly", "Weekly spread", "Seven days at a glance", "Planners", "dotted"),
        TemplateInfo("cornell", "Cornell notes", "Cues, notes, summary", "Planners", "cornell"),
    )

    fun info(id: String) = all.firstOrNull { it.id == id } ?: all.first()

    val paperNames = mapOf(
        "blank" to "Blank paper",
        "lined" to "Lined paper",
        "dotted" to "Dotted paper",
        "grid" to "Grid paper",
        "cornell" to "Cornell paper",
    )

    val paperColors = listOf(0xFFFFFDF8, 0xFFFFFFFF, 0xFFFDEEF1, 0xFFEEF6F0, 0xFFFFF6DC)

    fun create(kind: String, paperColor: Long = 0xFFFFFDF8, folder: String? = null, date: LocalDate = LocalDate.now()): Note {
        val t = info(kind)
        val blocks: List<Block>
        var title = ""
        var day: Long? = null
        when (kind) {
            "checklist" -> blocks = listOf(CheckBlock(), CheckBlock(), CheckBlock())
            "table" -> blocks = listOf(
                TableBlock(rows = listOf(listOf("", "", ""), listOf("", "", ""), listOf("", "", ""))),
                TextBlock(),
            )
            "daily" -> {
                title = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
                day = date.toEpochDay()
                blocks = listOf(
                    TextBlock(text = "Schedule", kind = TextKind.HEADING),
                    BulletBlock(text = "08:00 "),
                    BulletBlock(text = "10:00 "),
                    BulletBlock(text = "12:00 "),
                    BulletBlock(text = "14:00 "),
                    BulletBlock(text = "16:00 "),
                    BulletBlock(text = "18:00 "),
                    TextBlock(text = "To do", kind = TextKind.HEADING),
                    CheckBlock(), CheckBlock(), CheckBlock(),
                    TextBlock(text = "Notes", kind = TextKind.HEADING),
                    TextBlock(),
                )
            }
            "weekly" -> {
                val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                title = "Week of " + monday.format(DateTimeFormatter.ofPattern("d MMMM"))
                val list = mutableListOf<Block>()
                for (i in 0L until 7L) {
                    val d = monday.plusDays(i)
                    list += TextBlock(text = d.format(DateTimeFormatter.ofPattern("EEEE d")), kind = TextKind.SUBHEADING)
                    list += TextBlock()
                }
                blocks = list
            }
            "cornell" -> blocks = listOf(
                TextBlock(text = "Key questions", kind = TextKind.HEADING),
                BulletBlock(),
                TextBlock(text = "Notes", kind = TextKind.HEADING),
                TextBlock(),
                TextBlock(text = "Summary", kind = TextKind.HEADING),
                TextBlock(),
            )
            else -> blocks = listOf(TextBlock())
        }
        return Note(
            title = title,
            folder = folder,
            paper = t.paper,
            paperColor = paperColor,
            template = kind,
            blocks = blocks,
            day = day,
            color = listOf(0xFFF4C2CB, 0xFFCFE8D8, 0xFFD9D0F2, 0xFFF6E2A8).random(),
        )
    }

    fun welcomeNote(): Note = Note(
        title = "Welcome, Laura",
        paper = "dotted",
        template = "dotted",
        pinned = true,
        blocks = listOf(
            TextBlock(text = "This is your cozy notebook. A few little tricks:"),
            BulletBlock(text = "Write or draw anywhere with your pen."),
            BulletBlock(text = "Hold the pen still on the page for a moment and a text box pops up with the keyboard."),
            BulletBlock(text = "Use your finger to scroll and to tap into the text."),
            BulletBlock(text = "The toolbar at the bottom adds checklists, tables and headings."),
            TextBlock(text = "Try it", kind = TextKind.HEADING),
            CheckBlock(text = "Tick this box"),
            CheckBlock(text = "Make your first note from a template"),
        ),
    )
}
