# Changelog

## 1.2.2 – 2026-11-22

### Changed
- Entertainment's emblem is now food 🍽️ instead of theatre masks 🎭.
- Shopping's emblem is now a shopping bag 🛍️ instead of a gem 💎.

### Added
- Tap a slice of the category chart to highlight it. The slice grows, the others fade, the centre shows that category's emblem, name, amount and share, and its row below lights up. Tap it again, or the centre, to show the total.
- 7 new unit tests for slice hit-testing and the new emblems. 84 in total, all passing.

## 1.2.1 – 2026-11-22

### Fixed
- Dark theme: the month name, the < > arrows, the chart total, card titles and category names showed in black and were hard to read. The transparent screen backgrounds added in 1.2 made the default text colour fall back to black. Text now uses the theme's colours again.

### Changed
- Headings, the month arrows, the chart total and category and food names are gold in dark mode and burgundy in light mode.
- 2 new unit tests check that headings carry the heading colour and body text doesn't. 77 in total, all passing.

## 1.2 – 2026-11-22

### Changed
- Ornate wizard frames: cards are parchment sheets with cut corners, a double gold border, sparkles in the corners and diamonds on the edges. Screens sit on a vignetted parchment backdrop, and top bars are underlined with a gold rule.
- Category emblems and nicknames: 🏰 Rent (The Castle Keep), 🛒 Super market (Potion Ingredients), 🎭 Entertainment (Revelry & Feasts; emblem changed to 🍽️ in 1.2.2), ⚡ Bills (Lumos, Aguamenti, Incendio), 💎 Shopping (Diagon Alley), 🎁 Other (Curiosities). Food: 🦉 Delivery (Brought by owl), 🍷 Dine out (Feasting out), ☕ Day-to-day (Daily potions).
- More spells in the wording: Protego (goals), Geminio (CSV export), Accio (search), Revelio (calendar, empty search), Evanesco (delete), "Mischief managed" (save), and owl-post reminders.
- Goal bars are gold-rimmed; going over a goal now says the Protego ward is broken.
- Wax-seal style buttons with cut corners.
- 4 new unit tests (goal description, emblems/nicknames). 75 in total, all passing.

### Known issues / not yet done
- The new look has only been checked by compiling and lint, not on a phone. Emoji emblems depend on the phone's emoji font.

## 1.1 – 2026-11-22

### Added
- Calendar view: each day of the month shows what you spent; tap a day to list its expenses.
- Category pages: tap a category (or a food subgroup) to see all its expenses across months, grouped by month.
- Search by title, category or food type, optionally narrowed to chosen categories; ignores case and Greek accents.
- New **Shopping** category (shoes, clothes, jewellery, etc.).
- Daily reminder notification at about 21:30, which says whether anything was recorded that day and opens "Record expense" when tapped. Toggle in the ⋮ menu. On Android 13+ the app asks for notification permission once.
- 26 new unit tests (calendar grid, daily totals, category grouping, search, compact amounts, reminder timing and text). 71 in total, all passing.

### Changed
- Renamed the app to **Expensiarmus** with a new icon: a parchment expense scroll with a quill and a wax seal, plus a monochrome layer for Android 13 themed icons.
- Wizard theme throughout: parchment/burgundy/gold light theme, candle-lit dark theme, Cinzel headings, serif text and spell-flavoured wording. Wallpaper-based dynamic colours were dropped in favour of the fixed theme.
- The add/edit, goals, category and search screens now return to the screen you came from.
- CSV gains the Shopping category; the export file is named `expensiarmus-<date>.csv`.

### Fixed
- Lint: the CSV byte-order mark is now written as an escape, and night-theme colours have base definitions.

### Known issues / not yet done
- Still only unit tests: the UI and the notification have not been run on an emulator or a phone.
- The reminder time is fixed at 21:30. Android may deliver it a few minutes late, and battery savers on some phones (e.g. Xiaomi, Samsung "deep sleep") may block it unless the app is exempted.
- Lint suggests newer versions of the Compose and AndroidX libraries. They still work and haven't been upgraded yet.
- The package id stays `com.csymvoul.expenses` so that 1.1 installs over 1.0 and keeps your data.

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
