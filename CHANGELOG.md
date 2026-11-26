# Changelog

## Deployment (2026-11-26)

### Fixed
- **github.io deploys failing with "No artifacts named github-pages were found".** The upload worked, but GitHub listed the new artifact a few seconds late and the deploy step gave up first. The workflow now waits 15 seconds between the upload and the deploy. Web 1.23.1 (the shorter Record expense button) was stuck behind this.

### Tests
- Web 105 (1 new: the wait sits between the upload and the deploy). All passing.

## Web app 1.23.1 and iPhone app 1.20.1 – unreleased (2026-11-26)

Android stays at 1.22.0.

### Changed
- **Record expense is as tall as it was before 1.23.0 but shorter in length:** 13px above and below the 24px quill (54px in all, as with the old 30px quill), 14px at the sides (was 20px) and 6px between the quill and the words (was 8px).

### Tests
- Web 104 (the button's test updated). All passing. Not checked in a browser this time: the local web server couldn't be started.

## Web app 1.23.0 and iPhone app 1.20.0 – unreleased (2026-11-26)

Android stays at 1.22.0 (nothing changes there).

### Changed
- **Thinner capitals on the web and iPhone apps.** Cinzel (the engraved capitals of headings, buttons, totals and the Quick add amount) is now drawn at its regular weight everywhere, as Android always has, instead of semibold and bold. The browser's faked bold is switched off too (`font-synthesis: none`), so nothing thickens it.
- **A smaller Record expense button** on the web and iPhone apps: a 24px quill (was 30px) and less padding (8px top and bottom, 20px at the sides). With the thinner words it is now 217 × 44 px.

### Tests
- Web 104 (2 new: every Cinzel rule at weight 400 with no faked bold; the button's sizes). Android unchanged at 211. All passing. Checked in a browser at phone size, by day and at night: the Ledger's headings, total and button.

## 1.22.0 (Android), web app 1.22.0 and iPhone app 1.19.0 – unreleased (2026-11-26)

### Changed
- **Record expense carries the quill glyph** (all three apps), drawn without a badge like the widgets' Quick add: gold on the burgundy button by day, bronze on the gold button at night. It replaces the ✒️ emoji on the web and iPhone apps and Android's generic pencil icon. On Android the widget's quill now points at the same two images instead of keeping copies. The button's words no longer carry an emoji ("Record expense", "Add expense").

### Tests
- Android 211, web 102 (the two "same pen" tests now check the plain words, the glyph on the button and its day and night versions; on the web also that the service worker caches both). All passing. Checked in a browser: the Ledger's button by day and at night.

## 1.21.1 (Android), web app 1.21.1 and iPhone app 1.18.1 – unreleased (2026-11-26)

### Changed
- **The widgets use the app's own icons, never emoji** (Android and iPhone). The heading carries the pouch emblem, the trip line the trip emblem (instead of ✦), and each ward its category's painted emblem (the pouch for the whole month) instead of 🏰, 🍽️ and the like. A ward over its goal turns burgundy (gold by night), with no ⚠.
- **A new quill glyph for Quick add** (`art/glyph-quill-light.svg` / `-dark.svg`): a quill with a plus, bronze by day and gold by night, drawn without a badge like the ✓ and ☆ glyphs. It replaces the "+" in a burgundy circle on both widgets.
- The web app tells the iPhone widget which emblem goes with each ward (`icon`: `cat_rent`, `logo_pouch`…); the widget carries the emblems in its own assets.

### Tests
- Android 211 (3 new: the widget's emblems, quill and no emoji or symbols; two older ones updated), web 102 (the widget test now also checks the emblem names and that no emoji are sent), iPhone 1 new (every emblem the widget can be sent is in its assets, the quill in both looks). All passing; the iPhone tests compile, and with no iPhone simulator here the 6 widget logic tests were run on the Mac against the same source and pass.

### Known issues
- Neither widget has been seen on a phone with the emblems yet.

## 1.21.0 (Android), web app 1.21.0 and iPhone app 1.18.0 – unreleased (2026-11-26)

### Added
- **Trips in Geminio and Apparate.** The CSV gains a last column, `trip`, with the trip's name (empty for everyday expenses). Apparate reads it back: a name matching a trip (case and Greek accents ignored) ties the row to it, and an unknown name becomes a new trip whose dates span its rows (no budget). The preview says so: "Also adds 1 trip: Rome." Older CSVs without the column import as before.
- **The trip on the Android widget.** Under the total, while a trip is on: "✦ Rome · day 3 of 9 · €60.00 of €500.00". The widget also refreshes with the evening alarm, so the day count moves on.
- **An iPhone widget** (the iPhone app only; github.io can't have one). The Ledger on the home screen in the same parchment or castle colours as on Android: the month's total, the trip you are on and up to three ward bars; the small size shows the total, the trip and the first ward, the medium one everything plus a ➕ that opens Quick add. Tapping elsewhere opens the Ledger. The web app works out what it shows (`web/js/widget.js`, same rules as Android's) after every save, settings change and return to the app; the Swift shell saves it in an App Group the widget reads. The widget works out the trip's day itself and refreshes at midnight; in a new month, until the app is opened, it asks you to open the app instead of showing last month as this one.

### Changed
- `ios/build-ipa.sh` signs the app and the widget ad hoc with their entitlements (the App Group) before zipping, so AltStore can see them.

### Tests
- Android 208 (2 new: trips in the CSV round trip, the widget's trip line), web 102 (3 new: CSV trips, the widget content, and that the app hands it over), iPhone 6 new widget tests (the message, the trip's day, the stale month, the phone's time zone, the App Group from the provisioning profile, the widget links). All passing on Android and web. The iPhone tests compile; with no iPhone simulator installed here, 5 of them (everything but the links) were run on the Mac against the same source and pass.

### Known issues
- The iPhone widget is built but not yet seen on a phone: it has not run on a device or simulator. It also needs AltStore to grant the App Group and an extra App ID (see DEPLOYMENT.md).

## 1.20.0 (Android), web app 1.20.0 and iPhone app 1.17.0 – unreleased (2026-11-26)

### Added
- **A month's full list.** "Show all ›" under the Ledger's latest five (or "Open the full list ›" when there are five or fewer) opens a page with every expense of that month, grouped by day with each day's total. When some of them belong to a trip, a toggle shows the month **With portkeys · €95.00** or **Without · €35.00**, and the total and list follow it. (It replaces the Ledger's own Show all / Show only the latest 5.)

### Changed
- **The expense page shows the painted icons**: the Category, Food and Trip lines now start with the category's, food type's and trip's emblem instead of an emoji.
- Tests: Android 206 (2 new), web 99 (2 new). All passing. Checked in a browser: the expense page's icons, and the November list with and without the Rome dinner.

### Known issues
- Not yet checked on a phone.

## 1.19.0 (Android), web app 1.19.0 and iPhone app 1.16.0 – unreleased (2026-11-26)

### Added
- **Trips** ("Portkeys" in wizard words), with their own painted badge (a gold-strapped travelling trunk with a luggage tag on burgundy, `art/emblem-trip.svg`) in the same round, gold-rimmed style as Geminio, Apparate and the tab emblems: on the Settings card, the Annals tab, trip cards, the trip page and the trip chips.
  - **Plan them** in Settings → Trips (the card after Favourites): a name, first and last day, and an optional total budget. Several trips, each editable or deletable (its expenses stay, untied).
  - **Record into them:** Quick add and the expense form show the trips that aren't over yet; the one on today is ticked, tap to untie. "More details" carries the choice. Any expense can be tied to a trip in the form, e.g. flights booked months before.
  - **Expenses keep their category.** A trip dinner stays Dine out and still counts in its month, category and wards.
  - **With or without trips:** a category page with trip expenses offers "With portkeys · €80.00 | Without · €20.00" and lists accordingly.
  - **See them in Annals → Trips:** every trip with dates, where it stands (in 3 days, day 3 of 9, 9 days), total and budget bar. Each opens a page with the total against the budget, the average a day (so far, while it is on), the spend per category and the trip's expenses.
  - **On the Ledger,** while a trip is on, a card shows it: "Rome · day 7 of 9 · €60.00 of €500.00".
  - Expense rows and the expense view name the trip. Trips are stored with the data (`trips`, and `tripId` on an expense); Geminio's CSV doesn't carry trips yet.

### Fixed
- **Android: category pages opened for the wrong span since 1.13.0.** From the Ledger they listed the year or all time, and from Annals the current month; the two were swapped. Now the Ledger opens the month and Annals the year (By year) or all time, as on the web. A new test guards the wiring.

### Tests
- Android 204 (9 new: trip rules, saving and loading, the wiring, the glyph, and the category-period fix), web 97 (5 new; three older tests updated). All passing. Checked in a browser: adding Rome (20–28 Nov, €500) in Settings, its chip ticked in Quick add, recording a €60 Dine out into it, the Ledger card, Dine out "With portkeys €80 / Without €20", Annals → Portkeys and the trip page.

### Known issues
- Not yet checked on a phone. CSV export and import don't include trips.

## 1.18.0 (Android), web app 1.18.0 and iPhone app 1.15.0 – unreleased (2026-11-26)

### Added
- **The end-of-month owl.** The current month's Ledger opens with an **Owl post** card (Last month in plain words) that sums up the month just finished: "October: €1,240.00, 8% less than September, most went to Diagon Alley." (the category's plain name with plain words). **Open October ›** jumps to it; **Dismiss** hides the card until next month (saved with the data as `recapDismissed`). A month with nothing recorded gets no card, and the comparison is left out when the month before is empty.
- **The owl's notification.** On the 1st, with the evening owl, a second notification brings the same line ("🦉 Owl post: October's ledger is closed"); tapping it opens the Ledger. On Android it comes with the 21:30 alarm; in the iPhone app it is scheduled with the evening owls (its own id, so it doesn't replace that evening's reminder). It follows the evening owl's on/off switch. github.io shows the card only, since web apps can't schedule notifications.
- Logic in `monthRecap` / `recapText` (web `summary.js`) and `MonthRecap` (Android), same wording.
- Tests: Android 195 (2 new), web 92 (2 new). All passing. Checked in a browser: the card's wording, and Dismiss surviving a reload.

### Known issues
- The notifications were not tried on a phone (they first fire on 1 December). The iPhone app's Swift change compiles in the IPA build; its unit tests weren't run (no simulator installed).

## 1.17.2 (Android), web app 1.17.2 and iPhone app 1.14.2 – unreleased (2026-11-25)

### Changed
- **No title suggestions when recording.** Quick add and the expense form no longer suggest earlier titles while you type: you write the title yourself or pick a favourite.
- **Quick add is more compact:** a smaller amount, shorter number keys (40 px on the web, 44 dp on Android), smaller category tiles and calendar emblem, and tighter spacing. On a 375×812 phone it now fits without scrolling, favourites included.
- Tests: Android 193 (2 new: no suggestions on either screen, the compact sizes), web 90 (1 new; the suggestions test now checks they are gone, and one size updated). All passing. Checked in a browser: typing "fre" shows nothing, and the sheet fits the screen.

## 1.17.1 (Android), web app 1.17.1 and iPhone app 1.14.1 – unreleased (2026-11-25)

### Changed
- **Favourites are edited only in Settings → Favourites.** Quick add still shows them to pick, but its "Edit ›" link is gone.
- **Settings cards in a new order:** Favourites, Owl reminder, Wizard words, Number format, Theme, Updates (Android only), Geminio, Apparate.
- Tests: Android 191 (1 new: the card order), web 89 (1 new: the card order; the Quick add test now checks there is no Edit link). All passing. Checked in a browser: the Settings cards' order, and no Edit link in Quick add.

## 1.17.0 (Android), web app 1.17.0 and iPhone app 1.14.0 – unreleased (2026-11-25)

### Added
- **A Favourites page** (Settings → Favourites, or Edit › on Quick add's favourites): change a favourite's title, category, food type and split, remove it, or add a new one without starring an expense first. Still up to five; a duplicate or a sixth is refused with a short reason. Rules in `saveFavorite` / `Favorites.save`.
- **Favourites in the full form:** a new expense ("More details", or the full form) shows the favourites above the title; one tap fills in title, category, food type and split.

### Changed
- **Quick add records with a gold ✓ at the top**, a drawn glyph like the magnifying glass, Time-Turner and star (bronze by day, gold at night). Category tiles and favourites now only *select* (the tile glows and the hint reads e.g. "Super market · your share 50% · tap ✓ to record"), so you can change your mind before recording. The ✓ says what is missing ("Type an amount first", "Pick a category first"). "More details ›" moved to the bottom and carries the picked category and split. The separate "Record · …" button is gone.
- **The expense form saves with the same ✓ in its top bar**; the framed Save button at the bottom is gone. This applies to new expenses and to Amend.
- Tests: Android 190 (2 new: the Favourites page rules, the ✓ picture), web 88 (4 new; two older tests updated to the ✓). All passing. Checked in a browser: the ✓ refuses without an amount, then without a category; picking the Groceries favourite selects Super market at 50% and records €12 as €6; editing Freddo to 50% on the Favourites page; the full form filled from a favourite and saved with the top ✓.

### Known issues
- Not yet checked on a phone.

## 1.16.0 (Android), web app 1.16.0 and iPhone app 1.13.0 – unreleased (2026-11-25)

### Added
- **Favourites.** A star on the expense view (a drawn star like Accio's glass and the Time-Turner: bronze by day, gold at night, filled when starred) keeps that expense's title, category, food type and split, never its amount. Quick add shows them in a **Favourites** row: tap one, type the amount, then **Record · <title>** (its category tile glows; tapping another tile saves it there instead). "More details" carries the favourite's category and split to the full form. Up to five, so they fit one row; starring a sixth says to remove one first. **Edit** on the row shows × to remove them. Stored with the data (`favorites`), so older data loads with none.

### Changed
- **Suggestions only fill in the title.** Typing a title still offers earlier ones, but tapping one no longer records a copy with the same amount (Quick add) or copies the amount, category and split (full form): it just fills in the title. The list no longer shows amounts, and it hides once the title matches.
- Tests: Android 188 (4 new: favourites' rules, the cap of five, saving and loading, the star pictures), web 85 (6 new; the storage test now expects an empty favourites list for old data). All passing. Checked in a browser: a suggestion fills the title and records nothing; starring an expense; picking the favourite, typing €3.10 and Record saves it under Day-to-day; Edit → × removes it.

### Known issues
- Not yet checked on a phone.

## 1.15.1 (Android), web app 1.15.0 and iPhone app 1.12.1 – unreleased (2026-11-25)

### Changed
- **Back to this month is a Time-Turner** in the top bar, next to Accio's magnifying glass, instead of the "↩ Back to …" text under the month name. Like the glass it has no badge: bronze on parchment, gold at night. It only appears when you are looking at another month (Ledger, Calendar, Protego), and its label reads "Back to November 2026" for screen readers. **Annals → By year** has the same Time-Turner when you are on another year, bringing you back to the current one ("Back to 2026"); it doesn't show on All time. The pictures are drawn as vector art (`art/glyph-today-light.svg`, `art/glyph-today-dark.svg`) and rendered with a transparent background by `art/svg2png.swift`.
- Tests: Android 184 (1 new: the right shade per theme), web 80 (2 new: the Time-Turner in the top bar, both shades, cached offline, and the Annals year one; two older tests updated). All passing. Checked in a browser in light and dark: hidden on November, shown on October, and it brings you back; on Annals hidden on 2026 and on All time, shown on 2025.

### Known issues
- Not yet checked on a phone.

## 1.15.0 (Android), web app 1.14.0 and iPhone app 1.12.0 – unreleased (2026-11-25)

### Added
- **A ward for the whole month.** In Protego, "Cast a new ward" now offers **Whole month** first (with the coin-pouch emblem), next to the categories: a goal on everything you spend that month, for that month only or from then on, like the category wards. It shows at the top of Protego, as a bar under "Gold spent this month" on the Ledger, and first on the Android widget. It warns at 80% and when broken ("80% of your monthly ward is spent"), once each. Stored as a goal with no category, so saved data and older versions' goals load unchanged.
- **Back to this month.** On the Ledger, Calendar and Protego, when you are looking at another month, a "↩ Back to November 2026" link under the month name jumps straight back to the current month (and, on the Calendar, to today).
- Tests: Android 183 (5 new: the whole-month ward's sums, scope, removal, warnings, saved data, widget line and keys), web 78 (3 new), all passing. Checked in a browser: casting a whole-month ward from November onwards, its bar on the Ledger, and Back to November from September.

### Known issues
- Not yet checked on a phone.

## Housekeeping – 2026-11-24 (no app changes)

- The project folder is now `expensiarmus` (it was `expenses-app`), matching the GitHub repository, and the Gradle project is named `Expensiarmus` (it was `Expenses`; only Android Studio shows it).
- Unchanged on purpose: the Android app ID `com.csymvoul.expenses` (changing it would make Android treat the app as a new one, so updates and data wouldn't carry over) and the signing key's file name.
- Checked: the 178 Android tests pass and the release APK builds and is signed with the same key as 1.14.0.

## 1.14.0 (Android), web app 1.13.0 and iPhone app 1.11.0 – unreleased (2026-11-24)

### Changed
- **Fuel is now Transport**, for every way of getting about: "Fuel, tickets (plane, train, metro, bus), taxis, parking". It stays Floo Powder, keeps its cauldron emblem, colour, goals and every expense already in it (the code is still `FUEL`). Where the category is written as text (the expense view, the Android widget) its emoji is now 🚆 instead of ⛽. Geminio exports now write "Transport"; Apparate still reads "Fuel", so older backups import into Transport.
- "Transport" rather than "Transportation", so it fits on the Quick add tiles next to Entertainment.
- Tests: Android 178 (1 new), web 75 (1 new); the tests that named Fuel now expect Transport. All passing.

### Known issues
- Not yet checked on a phone.

## 1.13.0 (Android), web app 1.12.0 and iPhone app 1.10.0 – unreleased (2026-11-24)

### Changed
- **Category pages follow where you opened them.** Tapping a category in "Where the gold went", or a food type in "Feasts & Food":
  - on the Ledger lists only that month's expenses (headed "November 2026");
  - in Annals → By year lists that year's, grouped by month (headed "2026");
  - in Annals → All time lists all of them, as before (headed "All time").
  The food chips on the Entertainment page stay within the same span, and the hint under the card says what a tap opens ("…to see its expenses for this month"). Done with `inPeriod` (web `queries.js`, Android `ExpenseQueries`), which the category page applies before grouping by month.
- **Home's description** is now "Rent and shared building costs", without the Greek word.
- Tests: Android 177 (2 new), web 74 (2 new; the Home test updated), all passing. Checked in a browser: Ledger → Entertainment and → Delivery show November only, the All chip stays in November, Annals by year shows 2026's months, all time shows everything.

### Known issues
- Not yet checked on a phone.

## Web app 1.11.1 and iPhone app 1.9.1 – unreleased (2026-11-24)

### Fixed
- **Evanesco in dark mode** was a pinkish red with white text instead of gold. It now matches every other dark-mode button (gold with dark text) and stays red in light mode. The same goes for the ward's "From … on" removal button. The colours come from two new theme values, `--danger-bg` and `--on-danger`. Android was not affected (its delete confirmation is already a gold text button).
- Tests: 1 new web test checks the colours in light mode and in both dark modes (phone set to dark, or Dark chosen in Settings); 72 web tests, all passing. Checked in a browser in light and dark.

## 1.12.0 (Android), web app 1.11.0 and iPhone app 1.9.0 – unreleased (2026-11-24)

### Changed
- **Rent is now Home**, for rent and shared building costs (κοινόχρηστα). Only the name and its hint change: the category keeps its code (`RENT`), its castle emblem, colour, nickname, goals and every expense already in it. Geminio exports now write "Home"; Apparate still reads "Rent", so older backups import into Home.
- **Android tab bar:** the gold line now sits right on the top edge of the tab bar, on one solid band. Before, it sat a few pixels higher on a see-through strip, so the page showed through between the line and the tabs.
- Tests: Android 175 (3 new: the Home name and old "Rent" backups, and where the tab-bar line sits), web 71 (1 new), all passing. The export tests now expect "Home".

### Known issues
- The tab-bar change is Android-only and not yet checked on a phone.

## 1.11.1 (Android), web app 1.10.1 and iPhone app 1.8.1 – unreleased (2026-11-24)

### Changed
- **Apparate has its own emblem:** a ledger scroll rising out of a swirling gold portal, next to the Apparate card's title in Settings, like Geminio's scrolls and the owl. Drawn as vector art in the same gold-on-burgundy style (`art/emblem-apparate.svg`) and rendered to a 192-pixel picture like the other emblems.
- Tests: the emblem tests now include it (172 Android, 70 web, all passing).

### Known issues
- Not yet checked on a phone.

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
