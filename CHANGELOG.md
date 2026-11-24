# Changelog

## Web app 1.10.0 and iPhone app 1.8.0 – unreleased (2026-11-24)

### Added
- **Apparate · import CSV** on the web app (github.io) and in the iPhone app, the same as in Android 1.11.0: a card in Settings under Geminio opens the file picker (Files on the iPhone), then a confirmation shows how many expenses are new, how many are already in the ledger and which rows couldn't be read. Nothing is added until you tap Add; existing expenses are never changed. The rules are the Android ones (`importCsv` and `importSummary` in `web/js/csv.js`), so a file gives the same result in every app.
- The update check stays Android-only: the iPhone app is updated through AltStore and the web app updates itself.
- Tests: 7 new web tests (the importer's cases from Android, the summary, and the Settings wiring); 70 web tests in all, passing. Checked in a browser: picking a CSV with one new row, one already in the ledger and one bad category shows the right summary, and Add saves only the new one.

### Known issues
- Not yet tried in the iPhone app.

## 1.11.0 (Android only) – unreleased (2026-11-24)

### Added
- **Apparate · import CSV** (Settings, under Geminio; "Import CSV" in plain words). Pick a CSV with the system file picker: a Geminio export, or a spreadsheet with the same columns. Commas or semicolons, dates as 2026-11-22 or 22/11/2026, amounts in either number format, categories and food types by name or code, an empty share counts as 100%. A confirmation shows how many rows are new, how many are already in the ledger, and up to five rows that couldn't be read with the reason ("Line 4: \"2026-11-31\" is not a date."). Nothing is added until you confirm, existing expenses are never changed, and rows match the ledger one for one, so re-importing a backup adds nothing while two identical coffees stay two. Logic in `domain/CsvImporter.kt`.
- **Update check.** Once a day (at app start and with the evening alarm) the app asks GitHub for the latest release. A newer version gets one notification ("🦉 Owl post: Expensiarmus 1.12.0 is out"); tapping it opens Settings → Updates, which downloads the APK into the app's cache and opens Android's installer, where you tap Update. The first time, Android asks to allow installs from Expensiarmus. A switch turns the daily check off; Check now still works. Logic in `domain/AppUpdate.kt`, Android parts in `update/Updater.kt`.
- New permissions: `INTERNET` (only for the update check and the download) and `REQUEST_INSTALL_PACKAGES` (to hand the download to the installer). A `FileProvider` shares only the downloaded APK with the installer.

### Notes
- The web app and the iPhone app are unchanged: the iPhone app is updated through AltStore and the web app updates itself.
- This is the first version that can update itself, so 1.11.0 itself still has to be installed by hand.
- Tests: 8 import tests, 7 update tests (including GitHub's real answer for v1.10.0, kept in `app/src/test/resources/github-latest-release.json`) and 1 settings test; 172 Android tests in all, passing. The download, the notification and the installer hand-off were not run on a phone.

## 1.10.0 (Android), web app 1.9.0 and iPhone app 1.7.0 – unreleased (2026-11-23)

### Added
- **Calendar: the month at a glance.** A card above the grid shows the month's total; the change from the month before (while the month is running, compared with the same days of the previous month, e.g. "▲ 12% more than Oct 1–23"); and three tiles: number of expenses, average a day (so far), and the busiest day with its total. Worked out by `monthGlance` (web `queries.js`, Android `ExpenseQueries`).

### Changed
- **Ledger:** the list at the end of the Ledger tab shows the latest 5 expenses of the month. "Show all N ›" lists the rest; "Show only the latest 5" folds it again. It folds back when you change month.
- **Record expense / Add expense:** both wordings now start with the same ✒️ pen (plain words used ＋ before).
- Tests: 5 new Android tests (156 in all) and 5 new web tests (63 in all) cover the latest-5 list, the month card's sums, averages, busiest day, fair comparison, ties, rounding and empty months, and the shared pen. All passing. Checked in a browser: Ledger with Show all / Show only the latest 5, and the Calendar card in light and dark.

### Known issues
- Not yet checked on an Android phone or in the iPhone app.

## 1.9.5 (Android), web app 1.8.5 and iPhone app 1.6.5 – unreleased (2026-11-23)

### Changed
- **Sparkle clusters removed again:** the gold rules under the top bar and above the tab bar are back to how they were in 1.9.3 (a double line with the one star in the middle). They didn't look right on the phone. `ui/components/RuleEnds.kt` and its tests are gone.
- **Cast a new ward** keeps the painted Protego shield from 1.9.4.
- Tests: the sparkle tests are replaced by one web test that checks the rules have no clusters; the ward-label test moved to `WardButtonTest`. 151 Android and 58 web tests, all passing.

### Notes
- The old `csymvoul/expenses-app` repository (the forwarding page) is archived, so old links keep forwarding.

## 1.9.4 (Android), web app 1.8.4 and iPhone app 1.6.4 – unreleased (2026-11-23)

### Changed
- **Sparkle clusters on the gold rules:** the double gold line under the top bar and above the tab bar now ends at each side in a big four-point star, a small one and two specks; the tab bar's face downwards. They take the theme's gold. Section headings inside pages keep their plain rules.
  - Web: one masked pseudo-element per bar (`.bar::before`, `.tabs::before`) over `var(--gold)`.
  - Android: `GoldRule(sparkleEnds = true)`, with the positions worked out in `ruleEndSparks` (`ui/components/RuleEnds.kt`).
- **Cast a new ward** shows the painted Protego shield instead of the 🛡 emoji (Add a goal in plain words).
- Tests: 4 new Android tests (cluster shape, mirroring, flipping, the ward label; 154 in all) and 2 new web tests (the sparkle rule, with a guard against unescaped quotes in its pictures, and the ward button; 58 in all). One older web test now allows an extra class on a button's frame. All passing. Checked in a browser in light and dark on Protego.

### Known issues
- Not yet checked on an Android phone or in the iPhone app.

## 1.9.3 (Android), web app 1.8.3 and iPhone app 1.6.3 – unreleased (2026-11-23)

### Changed
- **Accio's magnifying glass stands on its own,** without the round badge, on the Ledger's search button and the Accio top bar. It comes in two shades: bronze on the light parchment, bright gold on the dark theme, picked automatically. `art/cut-glyph.swift` lifts the glass off its burgundy background into see-through pictures (`search_light.png`, `search_dark.png`); the badge version is gone.
- Tests: a new Android test checks the right shade per theme (150 in all); the web test checks the glass has no badge, both shades exist and are cached offline, and the CSS uses bronze by day and gold in both dark-theme rules (56 in all). All passing. Checked in a browser in light and dark.

### Known issues
- Not yet checked on an Android phone or in the iPhone app.

## 1.9.2 (Android), web app 1.8.2 and iPhone app 1.6.2 – unreleased (2026-11-23)

### Changed
- **Accio has its own emblem:** a gold magnifying glass (`art/emblem-search.jpg`) on the Ledger's search button and in the Accio page's top bar.
- **Quick add:** the date button shows the painted calendar emblem instead of the 📅 emoji.

### Fixed
- **Owl reminder emblem** in Settings sat slightly to the right of its circle; it is now cut centred on the owl and clock.
- Tests: the Android picture test and the web emblem tests now cover the magnifying glass, and a new web test checks the Quick add date and Accio emblems (149 Android, 56 web tests, all passing). Checked in a browser: Ledger top bar, Quick add, Settings and Accio.

### Known issues
- Not yet checked on an Android phone or in the iPhone app.

## 1.9.1 (Android), web app 1.8.1 and iPhone app 1.6.1 – unreleased (2026-11-23)

### Changed
- **Painted emblems in every top bar:** the Ledger shows the gold coin pouch from the logo next to "Expensiarmus"; Ledger entry, Annals, Protego and Settings show their tab emblem; a category page shows the category's emblem (or the food type's, when one is picked) instead of the old emoji.
- **Annals:** "By year" carries the calendar emblem and "All time" the Annals emblem.
- **Settings:** new painted emblems for Owl reminder (owl on an alarm clock), Geminio (two scrolls) and the theme choices (compass for Like the phone, sun for Light, moon for Dark). The source picture is `art/emblems-settings.jpg`.
- **Dark theme buttons:** the gold main buttons (Record expense, Amend, Geminio, Cast a new ward, Save…) now have burgundy frame lines and sparkles, so the border stands out instead of blending into the gold.
- Tests: 3 new Android tests (labels without emoji, the new pictures, burgundy button lines at night; 149 in all) and 4 new web tests (55 in all), all passing. Two older web tests were updated to the new header and frame markup. Checked in a browser: Ledger and Settings in dark, Annals and a category page in light.

### Known issues
- Not yet checked on an Android phone or in the iPhone app.

## 1.9.0 (Android), web app 1.8.0 and iPhone app 1.6.0 – unreleased (2026-11-23)

### Changed
- **Painted emblems instead of emoji:** every category, food type and tab now has a hand-painted gold-on-burgundy emblem (made with Gemini). They appear as round gold-rimmed badges in the ledger, the category table, Quick add tiles, suggestions, the expense view, category pages and Protego; as small badges at the front of category and food chips; and as rounded squares in the bottom tab bar, faded for the tabs you're not on.
  - Android: the 15 pictures are in `res/drawable-nodpi/emblem_*.png`; `Category.icon`, `FoodType.icon` and `Tab.icon` point to them, and `EmblemBadge` now draws a picture.
  - Web (and so the iPhone app): the pictures are in `web/icons/emblems/`, each category and food type has an `icon`, `TAB_ICONS` covers the tabs, and the service worker caches them all for offline use.
  - The source pictures are `art/emblems-tabs.jpg` and `art/emblems-categories.jpg`; `art/cut-emblems.swift` cuts them out.
- Emoji stay where the category is written out as text: the Category and Food lines on the expense view, the category page title and the Android home-screen widget.
- Tests: 2 new Android tests (every category, food type and tab has its own picture; 146 in all) and 2 new web tests (every emblem exists on disk and is cached offline; 51 in all), all passing. Checked in a browser in light and dark: ledger, tab bar, Quick add and search chips.

### Known issues
- Not yet checked on an Android phone or in the iPhone app.

## 1.8.0 (Android), web app 1.7.0 and iPhone app 1.5.0 – unreleased (2026-11-23)

### Changed
- **New logo:** a gold coin pouch with a sparkling wand on burgundy, used for the Android launcher icon, the iPhone app icon and the web app's home-screen icon and favicon. Android uses a roomier crop so round and squircle icon masks don't cut into the art. The source picture and the script that makes every icon are in `art/`.
- **The GitHub repository is now `csymvoul/expensiarmus`,** and the web app is at https://csymvoul.github.io/expensiarmus/. The old address forwards there, and installed copies on friends' phones move over by themselves; see DEPLOYMENT.md, "Old web address".

### Known issues
- Android 13+ "themed icons" no longer show a monochrome version of the logo; the launcher shows the full-colour icon instead.

## 1.7.2 (Android), web app 1.6.2 and iPhone app 1.4.2 – unreleased (2026-11-23)

### Changed
- **Ledger entry page:** the trash and pencil icons in the top bar are gone. They did the same as the ✏️ Amend expense button and the Evanesco · delete link at the bottom, which stay. The top bar only has Back.
- **Record / Amend expense screen:** the ✓ and 🗑 in the top bar are gone. The framed Save button at the bottom saves, and deleting is on the Ledger entry page.
- 2 new web tests check there's exactly one way to amend and one to delete on the entry page, and only the Save button on the edit screen (49 web tests). 144 Android tests, all passing.

## 1.7.1 (Android), web app 1.6.1 and iPhone app 1.4.1 – unreleased (2026-11-23)

### Changed
- **Every main button has the Record expense look:** a burgundy seal with cut corners, a double gold border, corner sparkles and a shadow. That covers Geminio · export CSV, Cast a new ward, the ward's Save, Mischief managed · Save, Amend expense, and the Evanesco / "From … on" confirmations (in red). Text links such as Cancel stay plain.
  - Web: one shared frame (`.frame` / `.frame-inner`) for all of them.
  - Android: one shared `WizardButton`.
- 1 web test now checks that every main button carries the frame (47 web tests). 144 Android tests, all passing. Checked in a browser: all four main buttons are framed, gold-bordered and shadowed.

## 1.7.0 (Android), web app 1.6.0 and iPhone app 1.4.0 – unreleased (2026-11-23)

### Changed
- **Record expense button:** framed like the cards (cut corners, double gold border, corner sparkles) and floating above the page on a shadow. On the web the shadow is a drop-shadow filter, because the cut-corner clip would cut off an ordinary shadow.
- **Settings is the fifth tab:** 📜 Ledger · 📅 Calendar · 📊 Annals · 🛡 Protego · ⚙️ Settings, with no back button.
- **Geminio (CSV export) moved into Settings,** as its own card. The ⋮ menu is gone; the top bar keeps search.

### Removed
- **"Send a test owl now"** from the menu and from Settings. The evening owl has been checked on both phones.

### Tests
- 1 new Android test (144 in total) and 1 new web test (47 in total), all passing. Checked in a browser at phone size: the framed button and its shadow (also enlarged), five tabs, Settings without a back button, the Geminio card, and no test owl left anywhere.

## iPhone app 1.3.1 and web app 1.5.1 – unreleased (2026-11-23)

### Fixed
- Quick add's number pad keys (digits, "." / "," and "⌫") weren't centred in the iPhone app. The 1.3.0 app was built just before the centring fix went into the web code. The keys now centre with flexbox, which works the same in Safari and the app's web view, and a test guards it. Checked in a browser: every key is exactly centred. The Android number pad uses Material buttons, which centre their content, so it needed no change.

## 1.6.0 (Android), web app 1.5.0 and iPhone app 1.3.0 – unreleased (2026-11-23)

### Added
- **Quick add.** ✒️ (or ＋) opens a sheet with a big number pad: type the amount, tap a category tile (the 7 categories plus 🦉 Delivery, 🍷 Dine out, ☕ Day-to-day), done. The title is optional and defaults to the category or food type. A short **Recorded … · Undo** bar follows. **More details ›** opens the full form already filled in. The date is today, or the selected calendar day.
- **Suggestions from your history.** Typing a title in Quick add or the full form offers earlier expenses ("Fre" → *Freddo espresso · €2.50 · Day-to-day*), most used first, ignoring case and Greek accents. In Quick add, tapping one records it at once (with Undo); in the full form it fills in amount, category, food type and split.
- **Bottom tabs:** 📜 Ledger · 📅 Calendar · 📊 Annals · 🛡 Protego. The Overview/Calendar switch and the links under the monthly total are gone; Protego has its own month switcher.
- **Settings** (⋮ → ⚙️ Settings):
  - reminder on/off and **any time** (no longer fixed at 21:30);
  - **Wizard words** on/off: plain labels (Goals, Search, Delete, Totals…), no nicknames, and plain notification texts;
  - **number format**: €1,234.56 or Greek 1.234,56 €;
  - **theme**: like the phone, light or dark (the iPhone app follows it too).
- **Android home-screen widget:** this month's total, up to three goal bars (closest to their goal first), and ➕ for Quick add. It updates after every change and every 30 minutes.
- Tests: 15 new Android tests (143 in total) and 7 new web tests (45 in total), all passing. Checked in a browser at phone size: Quick add (number pad, tiles, Undo, suggestions, More details), suggestions in the full form, tabs, Settings with plain words, the Greek format, dark theme and a custom time, and the iPhone app's theme, reminder time and test owl.

### Fixed
- Web: after an update, a phone could load the new main script with stale cached copies of other files and show a blank page. The offline worker now bypasses the browser's cache when it installs or refreshes.

### Known issues
- The widget and the Android screens haven't been tried on a phone yet.

## 1.5.0 (Android), web app 1.4.0 and iPhone app 1.2.0 – unreleased (2026-11-23)

### Changed
- **Goals per month.** Protego now shows the wards (category goals) of the month you're viewing. **🛡 Cast a new ward** lets you pick any category, set an amount, and choose **only this month** or **this month and every month after**. A goal for one specific month always wins over an "every month" one, and past months keep their own goals. Tap a ward to change it, or remove it for this month only or from this month on.
- **The "Total per month" goal is gone.** The monthly total card now links to Protego for that month.
- Goals saved before this version are converted: the Entertainment goal becomes an "every month" goal, and the old monthly total and other category goals are dropped (they were unused since 1.4.0).

### Added
- **Goal warnings** for the current month: at **80%** of a category's goal ("🦉 80% of your Entertainment ward is spent: €48.00 of €60.00 in November · €12.00 left") and when it's **reached or passed** ("⚡ Your Entertainment ward is broken: … €5.00 over"). Each comes at most once per category per month, and jumping straight past the goal sends only the second one.
  - Android: a notification ("Goal warnings" channel), or a toast if notifications are off.
  - iPhone app: a notification.
  - github.io: a card at the top of the app, since web pages can't send notifications on iPhone.
- Tests: 21 new Android tests (goal rules, conversion, warnings, wording), 128 in total. 7 new web tests with the same cases, 38 in total. All passing.
- Checked in a browser at phone size: conversion of the old goal, casting, editing and removing wards, both warnings and no repeats, and the iPhone app's notification message.

### Known issues
- Warnings already sent for a month aren't sent again that month, even if you raise the goal and later pass 80% of the new amount.

## Web app 1.3.2 and iPhone app 1.1.2 – unreleased (2026-11-22)

### Changed
- **No zooming:** double-tap and pinch no longer zoom the page.
  - On github.io: `maximum-scale=1, user-scalable=no`, CSS `touch-action: manipulation` (also removes the tap delay), and a small script cancelling Safari's pinch gestures, since Safari ignores `user-scalable=no`.
  - In the iPhone app: the web view is locked at 100% (zoom range 1–1 and its pinch recogniser switched off).
- **Top bar stays at the top** while scrolling, like on Android. It's now fixed rather than sticky, which iPhone Safari and the app's web view didn't always honour, with matching space so no content sits under it.
- 1 new web test guarding the no-zoom settings and the fixed bar. 32 in total, all passing.
- Checked in a browser at phone size: the bar stays put after scrolling, and nothing is hidden under it.

### Known issues
- Safari may change how it treats pinch blocking in future iOS versions. The iPhone app is unaffected.

## 1.4.1 (Android), web app 1.3.1 and iPhone app 1.1.1 – unreleased (2026-11-22)

### Removed
- The ‹ › arrows and the swipe on the calendar card, added in 1.4.0. The month switcher at the top of the screen already moves the calendar through months.

## 1.4.0 (Android), web app 1.3.0 and iPhone app 1.1.0 – unreleased (2026-11-22)

### Changed
- **Goals:** Protego now has just the monthly total goal and an **Entertainment** goal. Goals saved earlier for other categories are ignored and no longer shown.
- **Opening an expense** shows it read-only first: amount (your share and the full amount), date, category, food type and split ("Your share 50% (€18.00) · theirs 50% (€18.00)"). **✏️ Amend expense** opens the edit form, and Evanesco deletes it. After saving, you're back on the updated view; after deleting from the edit form, you're back on the list.

### Added
- **Calendar:** ‹ › arrows on the calendar card to move through months, and swiping left/right on the calendar.
- Tests: 4 new Android tests (Entertainment-only goals, split wording), 107 in total. 2 new web tests, 31 in total. All passing.
- Checked in a browser at phone size: goals, calendar arrows and swipe, view → amend → save → back, and delete from the edit form.

## iPhone app 1.0 and web app 1.2.0 – unreleased (2026-11-22)

### Added
- **Native iPhone app** (`ios/`) for personal use, installed with a free Apple ID through AltStore. It bundles the web app and adds:
  - a real **21:30 notification** with the Android wording. The next 30 evenings are scheduled and refreshed whenever the data changes or the app opens. Tapping it opens Record expense. On/off switch under ⋮ → 🦉 Owl reminder, with a note if notifications are turned off in Settings.
  - CSV export through the iPhone share sheet.
  - a backup of the data in Documents/expensiarmus-data.json, visible in the Files app. It is restored automatically if the web view's storage is ever cleared.
- `web/js/reminder.js`: reminder wording and the evenings to schedule, shared logic for the iPhone app.
- `ios/build-ipa.sh` builds an unsigned `.ipa` for AltStore. The Xcode project is generated from `ios/project.yml` (XcodeGen).
- Tests: 4 new web tests (29 in total) and Swift unit tests for message parsing, safe quoting and file types. The Swift logic checks pass when compiled for macOS; the Xcode test target runs once Xcode is installed.
- Checked in a browser: the web app for friends is unchanged, and with a stand-in for the iPhone app the page sends the right schedule, save and share messages.

### Changed (web app)
- Inside the iPhone app the web app hides the "Add to Home Screen" hint and the evening banner (the real notification replaces it) and does not register its offline worker. Nothing changes on github.io.

### Known issues / not yet done
- Not built yet: Xcode is not installed on the Mac, so the iPhone app has not been compiled for iOS or run.
- The iPhone app's data is separate from the github.io copy in Safari; there is no import yet.

## 1.3.1 (Android) – 2026-11-22

### Fixed
- **The 21:30 owl could arrive hours late or not that evening.** It was scheduled as an inexact alarm, which Android may delay by up to 75% of the time until it is due (many hours for an alarm set a day ahead). It is now an exact alarm that fires at 21:30 even when the phone is idle (`USE_EXACT_ALARM` on Android 13+, which apps installed outside the Play Store get automatically; `SCHEDULE_EXACT_ALARM` on Android 12). If exact alarms are ever not allowed, it falls back to the old behaviour, and the alarm is re-armed when that permission changes.

### Added
- A banner on the main screen when notifications are switched off for the app: *"🦉 Owl reminders are blocked: tap to allow notifications"*. It opens the app's notification settings and disappears once they're allowed.
- ⋮ → **🦉 Send a test owl now** shows the owl immediately, with today's text, to check that notifications get through.
- 2 new unit tests for choosing exact vs. inexact alarms. 103 in total, all passing.

### Known issues
- Some phones (Samsung, Xiaomi and others) still stop alarms for apps under battery restrictions. If the owl doesn't come, set Settings → Apps → Expensiarmus → Battery → Unrestricted.

## 1.3.0 (Android) and web app 1.1.0 – 2026-11-22

### Added
- **⛽ Fuel** category for petrol (gas), diesel and LPG, nicknamed *Floo Powder*. It sits between Shopping and Other everywhere: chips, chart, goals, search, CSV. Bills stays for household water, electricity and heating gas.
- **📊 Annals** screen with totals over longer periods. Open it from ⋮ or from "See the whole year in the Annals" under the monthly total.
  - **By year:** total, your share vs full bills, monthly average, change against the same months of last year (the whole previous year for past years), the number of expenses and the biggest one. A month-by-month bar chart: tap a bar for its total and **Open month ›**. The category pie chart (tap to highlight) and the food breakdown for the year.
  - **All time:** the same over everything, with a year-by-year bar chart where **Open year ›** jumps to that year.
  - The monthly average counts from January, or the first month you recorded anything, up to the current month (December for past years).
- Tests: 16 new Android tests (year and all-time summaries, bar hit-testing, Fuel), 101 in total. 5 new web tests, 25 in total. All passing.

### Changed
- In the category and food lists, the percentage now sits under the amount, so long names like "Entertainment" fit on narrow phones.

## Web app 1.0 – 2026-11-22

### Added
- Expensiarmus as a web app for iPhone (and any browser), published with GitHub Pages at https://csymvoul.github.io/expenses-app/ and installable with Safari's **Add to Home Screen**.
- Same features as Android 1.2.2: expenses with splits and food subgroups, monthly overview with a tappable donut chart, calendar, category pages, Accio search, Protego goals, CSV export (through the iPhone share sheet), the wizard look in light and dark mode, and the same icon.
- Works offline through a service worker. Data stays on the phone in the same format as the Android app.
- 🦉 Owl reminder screen explaining how to set a daily 21:30 reminder in the iPhone Reminders app, plus an in-app owl banner after 21:00 when nothing has been recorded that day.
- GitHub Actions workflow that runs the web tests and deploys `web/` to GitHub Pages on every push to `main`. It uses the current action versions (checkout v7, setup-node v7, configure-pages v6, upload-pages-artifact v5, deploy-pages v5) and Node 22.
- 20 JavaScript unit tests ported from the Kotlin tests (money, splits, summaries, goals, food, calendar, category pages, search, CSV, storage), all passing.
- Checked in a browser at iPhone size, light and dark: all screens, tapping a slice, recording, searching, goals and deleting.

### Known issues / not yet done
- No real scheduled notification on iPhone. Web apps can't schedule local notifications, hence the Reminders workaround.
- Nothing syncs between phones, and there is no import yet: an Android CSV can't be loaded into the web app.
- The offline worker registers on the live site, but offline use hasn't been tried on an actual iPhone yet.
- Two codebases: features added to the Android app need adding to `web/` as well.

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
