# Expenses

A personal Android app for tracking monthly spending in euros. Everything is stored on the phone; there is no account, server or internet access.

## What it does

- **Record expenses** with a date, title, amount (€) and one of five categories: Rent, Super market, Entertainment, Bills, Other.
- **Food subgroups.** An Entertainment expense can be marked as food: *Delivery*, *Dine out* or *Day-to-day* (e.g. a coffee at work).
- **Split expenses.** Mark an expense as shared and set your percentage (50-50 by default, with 33% and 25% shortcuts or a slider). Only your share counts toward totals, charts and goals; the full amount is still shown.
- **Monthly view.** Browse month by month: total spent, a donut chart and table per category, a food breakdown (bar plus amount, count and % per subgroup), and the list of that month's expenses. Tap an expense to edit or delete it.
- **Goals.** Set an overall monthly goal and optional goals per category. Progress bars show the percentage spent and the amount left or over. They turn amber at 80% and red once over. Goals apply to every month until changed.
- **CSV export** of all expenses (UTF-8, opens in Excel or Google Sheets) for backup or analysis.
- Follows the phone's light/dark mode and, on Android 12+, its wallpaper colours.

## How it works

- Kotlin + Jetpack Compose (Material 3), single activity, minimum Android 8.0 (API 26).
- Amounts are stored as whole cents so totals never drift through rounding. A split share is rounded half up to the cent.
- All data lives in one JSON file in the app's private storage (`files/expenses.json`). Writes go to a temp file first and are swapped in atomically. If the file ever becomes unreadable, it is kept as `expenses.json.corrupt-<time>` rather than overwritten.
- Code layout (`app/src/main/java/com/csymvoul/expenses/`):
  - `model/` – `Expense`, `Category`, `FoodType`, `Goals`, `AppData`
  - `domain/` – `Money` (parsing/formatting), `MonthlySummary` / `FoodSummary` (totals, goal progress), `CsvExporter`
  - `data/` – `ExpenseStore` (JSON file)
  - `ui/` – view model, screens (month, add/edit, goals), donut chart and goal bars
- Unit tests for the model, money handling, summaries, CSV and storage are in `app/src/test/`.

## More

- [DEPLOYMENT.md](DEPLOYMENT.md): building the APK and installing it on the phone
- [CHANGELOG.md](CHANGELOG.md): changes, fixes and known issues
