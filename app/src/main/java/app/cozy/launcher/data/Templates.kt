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
        TemplateInfo("picnic", "Picnic list", "Gingham top, strawberry ticks", "Meadow", "picnic"),
        TemplateInfo("garden", "Garden journal", "Lined, with a flower border", "Meadow", "garden"),
        TemplateInfo("clouddot", "Cloud dotted", "Dots under little clouds", "Meadow", "clouddot"),
        TemplateInfo("recipe", "Recipe card", "Ingredients and method", "Meadow", "recipe"),
    )

    fun info(id: String) = all.firstOrNull { it.id == id } ?: all.first()

    val paperNames = mapOf(
        "blank" to "Blank paper",
        "lined" to "Lined paper",
        "dotted" to "Dotted paper",
        "grid" to "Grid paper",
        "cornell" to "Cornell paper",
        "picnic" to "Picnic paper",
        "garden" to "Garden paper",
        "clouddot" to "Cloud paper",
        "recipe" to "Recipe card",
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
            "picnic" -> {
                title = "Picnic"
                blocks = listOf(
                    TextBlock(text = "To pack", kind = TextKind.HEADING),
                    CheckBlock(), CheckBlock(), CheckBlock(), CheckBlock(),
                    TextBlock(text = "Who's coming", kind = TextKind.HEADING),
                    TableBlock(rows = listOf(listOf("Name", "Bringing", "Coming?"), listOf("", "", ""), listOf("", "", ""))),
                    TextBlock(),
                )
            }
            "garden" -> {
                title = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))
                blocks = listOf(
                    TextBlock(text = "Today", kind = TextKind.HEADING),
                    TextBlock(),
                    TextBlock(text = "Little joys", kind = TextKind.HEADING),
                    BulletBlock(), BulletBlock(), BulletBlock(),
                )
            }
            "recipe" -> {
                title = "Recipe"
                blocks = listOf(
                    TextBlock(text = "Ingredients", kind = TextKind.HEADING),
                    BulletBlock(), BulletBlock(), BulletBlock(),
                    TextBlock(text = "Method", kind = TextKind.HEADING),
                    TextBlock(),
                    TextBlock(text = "Notes", kind = TextKind.SUBHEADING),
                    TextBlock(),
                )
            }
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
