# Expensiarmus

A personal, wizard-themed Android app for tracking monthly spending in euros. Think of it as an enchanted expense report on parchment. Everything is stored on the phone; there is no account, server or internet access.

## What it does

- **Record expenses** with a date, title, amount (€) and one of six categories: Rent, Super market, Entertainment, Bills, Shopping (shoes, clothes, jewellery…), Other.
- **Food subgroups.** An Entertainment expense can be marked as food: *Delivery*, *Dine out* or *Day-to-day* (e.g. a coffee at work).
- **Split expenses.** Mark an expense as shared and set your percentage (50-50 by default). Only your share counts toward totals, charts and goals; the full amount is still shown.
- **Monthly overview.** Total spent, a donut chart and table per category (tap a slice to highlight that category), a food breakdown and the month's ledger. Browse month by month.
- **Calendar.** A month grid showing what you spent each day, with busier days in deeper ink. Tap a day to see its expenses.
- **Category pages.** Tap a category, or a food subgroup, to see all its expenses, grouped by month with monthly totals.
- **Search.** Find expenses by part of the title, by category or food-type name, and/or by picking categories. Case and Greek accents are ignored.
- **Goals.** An overall monthly goal and optional per-category goals, with progress bars that turn amber at 80% and red when over.
- **Evening reminder.** A notification at about 21:30 each day reminds you to record the day's expenses and shows what you've recorded so far. Tap it to go straight to "Record expense". It can be turned off from the ⋮ menu.
- **CSV export** of all expenses for backup or analysis.
- **Wizard look:** parchment, burgundy and gold by day; a candle-lit castle theme in dark mode. Cards are framed in gold with corner sparkles, headings use the Cinzel font, and each category has an emblem and a wizarding nickname (🛍️ Shopping is *Diagon Alley*). The wording is full of spells: Accio to search, Protego for goals, Evanesco to delete.

## How it works

- Kotlin + Jetpack Compose (Material 3), single activity, minimum Android 8.0 (API 26).
- Amounts are stored as whole cents so totals never drift; a split share is rounded half up to the cent.
- All data lives in one JSON file in the app's private storage (`files/expenses.json`). Writes are atomic. An unreadable file is kept as a backup instead of being overwritten.
- The reminder is an inexact `AlarmManager` alarm (no exact-alarm permission needed), re-armed after it fires, after a reboot and after clock/time-zone changes.
- Code layout (`app/src/main/java/com/csymvoul/expenses/`):
  - `model/`: `Expense`, `Category`, `FoodType`, `Goals`, `AppData`
  - `domain/`: `Money`, `MonthlySummary` / `FoodSummary`, `ExpenseQueries` (calendar, category, search), `Reminder` (timing and text), `CsvExporter`
  - `data/`: `ExpenseStore` (JSON file)
  - `reminder/`: alarm scheduling and the notification receiver
  - `ui/`: view model, screens (month, calendar, category, search, add/edit, goals), theme and components
- Unit tests for all of `model/`, `domain/` and `data/` are in `app/src/test/`.
- The Cinzel font is licensed under the SIL Open Font License; see [licenses/Cinzel-OFL.txt](licenses/Cinzel-OFL.txt).

## More

- [DEPLOYMENT.md](DEPLOYMENT.md): building the APK and installing it on the phone
- [CHANGELOG.md](CHANGELOG.md): changes, fixes and known issues
