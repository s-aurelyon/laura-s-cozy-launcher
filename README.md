# Cozy

A minimalist home screen for Laura's Xiaomi tablet (and the Bigme e-ink reader),
with its own Notes, Reminders, Focus timer and Calendar built in.

## What's inside

- **Home screen**: greeting, the mascot with a little message about the day, six tiles,
  today's reminders and a mini focus timer. The pencil opens **Edit home**, where any tile
  can be renamed, given a new icon, pointed at a built-in app or any installed app, moved
  or removed. The grid button opens **All apps**.
- **Notes**: folders, pinning, search, nine templates (blank, lined, dotted, grid,
  checklist, table, daily page, weekly spread, Cornell), headings and text styles,
  checklists, bullet lists and tables.
  - **Pen**: the stylus always writes. **Hold the pen still for a moment and a text box
    appears with the keyboard.** A quick pen tap on a text box opens it for typing.
    The pen's eraser end (if it has one) erases. Fingers scroll and type, unless the
    pen, highlighter or eraser tool is picked in the toolbar.
- **Reminders**: Today / Scheduled / All / Done, lists, repeats, notifications at the
  chosen time, and links from notes.
- **Focus timer**: focus, short and long breaks, sessions of four, custom times,
  a notification when time's up, and an option to keep the screen awake.
- **Calendar**: a wall-calendar month view with South African public holidays,
  events, reminders and a daily page for any day.
- **Settings** (Edit home → gear): name, colour, animations, e-ink mode for the Bigme,
  week start, and how long the pen hold takes.

Everything is stored privately on the device. Nothing goes online.

## Getting the app onto the tablet (no Android Studio needed)

GitHub builds the app for free:

1. Make a free account at github.com, then create a **new private repository**
   (for example `cozy`).
2. On the new repository's page choose **uploading an existing file**, and drag in
   everything from this folder. Include the hidden `.github` folder (on a Mac, press
   Cmd+Shift+. in Finder to show hidden files). Commit.
3. Open the **Actions** tab. A build called "Build Cozy APK" starts by itself and takes
   about 5 minutes. When it has a green tick, open it and download **Cozy-apk** at the
   bottom of the page. Unzip it to get `Cozy.apk`.
4. Get `Cozy.apk` onto the tablet (Google Drive, email, USB cable), tap it, and allow
   "install unknown apps" when asked.
5. Open Cozy, allow notifications, then Edit home → gear → **Make Cozy the home screen**
   and choose Cozy. (On Xiaomi: Settings › Apps › Default apps › Home app.)

Every time the code changes, a new APK builds the same way. Installing it updates the
app and keeps all her notes, because every build is signed with the same key
(`app/cozy.keystore`). Keep that file, and keep the repository private.

## Building on a computer instead

Open the folder in Android Studio, or run `bash scripts/fetch-fonts.sh` and then
`gradle assembleRelease` with Gradle 8.11 and JDK 17. The fonts are optional; without
them the app uses the system fonts.

## If the first build fails

This project was written without being able to compile it, so the first GitHub build
may stop with an error. Open the failed run, copy the red error lines, and paste them
to Claude (or point Claude Code at the repository) to fix.
