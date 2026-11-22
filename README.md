# Expensiarmus

A personal, wizard-themed app for tracking monthly spending in euros: a native Android app, a web app anyone can add to their iPhone home screen, and an iPhone app for personal use (installed with AltStore). Think of it as an enchanted expense report on parchment. Everything is stored on the phone; there is no account, server or internet access.

## What it does

- **Record expenses** with a date, title, amount (€) and one of seven categories: Rent, Super market, Entertainment, Bills, Shopping (shoes, clothes, jewellery…), Fuel (petrol, diesel, LPG), Other.
- **Food subgroups.** An Entertainment expense can be marked as food: *Delivery*, *Dine out* or *Day-to-day* (e.g. a coffee at work).
- **Split expenses.** Mark an expense as shared and set your percentage (50-50 by default). Only your share counts toward totals, charts and goals; the full amount is still shown.
- **Expense view.** Tapping an expense shows it read-only (amounts, date, category, food type, split); Amend opens the edit form.
- **Monthly overview.** Total spent, a donut chart and table per category (tap a slice to highlight that category), a food breakdown and the month's ledger. Browse month by month.
- **Annals.** Totals per year and for all time: a month-by-month or year-by-year bar chart, monthly average, comparison with the previous year, biggest expense, and the category and food breakdowns for the period.
- **Calendar.** A month grid showing what you spent each day, with busier days in deeper ink. Move through months with the month switcher at the top. Tap a day to see its expenses.
- **Category pages.** Tap a category, or a food subgroup, to see all its expenses, grouped by month with monthly totals.
- **Search.** Find expenses by part of the title, by category or food-type name, and/or by picking categories. Case and Greek accents are ignored.
- **Goals.** An overall monthly goal and an optional Entertainment goal, with progress bars that turn amber at 80% and red when over.
- **Evening reminder.** A notification at about 21:30 each day reminds you to record the day's expenses and shows what you've recorded so far. Tap it to go straight to "Record expense". It can be turned off from the ⋮ menu, ⋮ → Send a test owl now shows it immediately, and a banner warns if notifications are switched off for the app.
- **CSV export** of all expenses for backup or analysis.
- **Wizard look:** parchment, burgundy and gold by day; a candle-lit castle theme in dark mode. Cards are framed in gold with corner sparkles, headings use the Cinzel font, and each category has an emblem and a wizarding nickname (🛍️ Shopping is *Diagon Alley*). The wording is full of spells: Accio to search, Protego for goals, Evanesco to delete.

## iPhone: the web app

The same app also runs as a web app at **https://csymvoul.github.io/expenses-app/**. Open it in Safari, tap **Share → Add to Home Screen**, and it gets its own icon, opens full screen and works offline. It has the same features and look as the Android app, except the 21:30 reminder: iPhones don't let web apps schedule notifications, so the app's 🦉 Owl reminder screen explains how to set a daily 21:30 reminder in the iPhone Reminders app. Data stays on the phone (in Safari's storage for the app), and nothing syncs between phones.

## iPhone: the app (personal use)

`ios/` holds a small native iPhone app that bundles the same web app and adds what a web page can't do: a real **21:30 notification** (same wording as on Android, scheduled for the next 30 evenings and refreshed whenever the data changes or the app opens), CSV export through the iPhone share sheet, and a backup copy of the data in the app's Documents folder (visible in the Files app). It is meant to be installed on one's own iPhone with a free Apple ID through AltStore; see [DEPLOYMENT.md](DEPLOYMENT.md).

## How it works

- Kotlin + Jetpack Compose (Material 3), single activity, minimum Android 8.0 (API 26).
- Amounts are stored as whole cents so totals never drift; a split share is rounded half up to the cent.
- All data lives in one JSON file in the app's private storage (`files/expenses.json`). Writes are atomic. An unreadable file is kept as a backup instead of being overwritten.
- The reminder is an exact `AlarmManager` alarm (inexact only if Android doesn't allow exact ones), re-armed after it fires, after a reboot, after clock/time-zone changes and when the exact-alarm permission changes.
- Code layout (`app/src/main/java/com/csymvoul/expenses/`):
  - `model/`: `Expense`, `Category`, `FoodType`, `Goals`, `AppData`
  - `domain/`: `Money`, `MonthlySummary` / `FoodSummary`, `PeriodSummary` (year and all time), `ExpenseQueries` (calendar, category, search), `Reminder` (timing and text), `CsvExporter`
  - `data/`: `ExpenseStore` (JSON file)
  - `reminder/`: alarm scheduling and the notification receiver
  - `ui/`: view model, screens (month, calendar, annals, category, search, add/edit, goals), theme and components
- Unit tests for all of `model/`, `domain/` and `data/` are in `app/src/test/`.
- The web app (`web/`) is plain HTML, CSS and JavaScript modules with no build step. Its logic (`web/js/model.js`, `money.js`, `summary.js`, `queries.js`, `csv.js`, `store.js`) mirrors the Kotlin code and uses the same data format; `app.js` draws the screens. It stores data in `localStorage` and works offline through a service worker (`web/sw.js`). Its tests run with Node's built-in test runner (`web/test/`). `web/js/reminder.js` works out the 21:30 reminders for the iPhone app.
- The iPhone app (`ios/`) is SwiftUI with a `WKWebView` that serves the bundled `web/` folder from an `expensiarmus://` address. The page talks to Swift through a message handler (`Shell.swift`): `schedule` (notifications, `OwlScheduler.swift`), `save` (backup file, `Backup.swift`) and `share` (share sheet). The Xcode project is generated from `ios/project.yml` with XcodeGen.
- The Cinzel font is licensed under the SIL Open Font License; see [licenses/Cinzel-OFL.txt](licenses/Cinzel-OFL.txt).

## More

- [DEPLOYMENT.md](DEPLOYMENT.md): building the APK and installing it on the phone
- [CHANGELOG.md](CHANGELOG.md): changes, fixes and known issues
