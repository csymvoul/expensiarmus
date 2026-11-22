# Changelog

## 1.0 – 2026-11-22

### Added
- Add, edit and delete expenses: date, title, amount in EUR, category (Rent, Super market, Entertainment, Bills, Other).
- Food subgroups for Entertainment expenses: Delivery, Dine out, Day-to-day.
- Split expenses with a percentage share (default 50-50); totals and goals use only your share.
- Monthly view with month switching, total, category donut chart and table, food breakdown and expense list.
- Overall monthly goal and per-category goals with progress bars (amber at 80%, red when over).
- CSV export of all expenses.
- Light/dark mode and dynamic colours on Android 12+.
- 45 unit tests (model, money parsing/formatting, monthly and food summaries, CSV, storage): all passing.

### Known issues / not yet done
- Only unit tests so far. The UI has not been run on an emulator or a real phone yet.
- No import: a CSV export can't be loaded back into the app. Phone backups (Android's auto-backup) do include the data.
- The CSV uses commas. Excel set to a Greek locale may expect semicolons and put everything in one column; use Data → From Text/CSV, or open the file in Google Sheets.
- English only.
