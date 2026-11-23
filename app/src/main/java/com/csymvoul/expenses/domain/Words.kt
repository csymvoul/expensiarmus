package com.csymvoul.expenses.domain

/**
 * The app's wording, with the wizard theme ("Protego", "Accio"…) or in plain words. Chosen in
 * Settings. Same texts as the web app (web/js/words.js).
 */
data class Words(
    val wizard: Boolean,
    val ledgerTab: String,
    val calendarTab: String,
    val annalsTab: String,
    val goalsTab: String,
    val annals: String,
    val goals: String,
    val search: String,
    val fab: String,
    val newExpense: String,
    val editExpense: String,
    val entry: String,
    val amend: String,
    val amount: String,
    val spentThisMonth: String,
    val spentInPrefix: String,
    val spentAllTime: String,
    val whereWent: String,
    val food: String,
    val ledger: String,
    val ledgerEmpty: String,
    val dayHint: String,
    val dayEmpty: String,
    val split: String,
    val save: String,
    val deleteTitle: String,
    val deleteBtn: String,
    val deleteLink: String,
    val keep: String,
    val deleteBodySuffix: String,
    val searchPlaceholder: String,
    val searchHint: String,
    val searchNothing: String,
    val vault: String,
    val newWard: String,
    val aNewWard: String,
    val removeWard: String,
    val goalsIntroPrefix: String,
    val goalsIntroSuffix: String,
    val noGoals: String,
    val whichCategory: String,
    val categoryEmpty: String,
    val exportTitle: String,
    val exportButton: String,
    val exported: String,
    val exportFailed: String,
    val settingsTab: String,
    val reminder: String,
    val reminderSwitch: String,
    val owlNothing: String,
    val owlSome: String,
    val alert80Prefix: String,
    val alert80Suffix: String,
    val alert100Prefix: String,
    val alert100Suffix: String,
    val overGoal: String,
    val nicknames: Boolean,
) {
    fun spentIn(year: Int) = "$spentInPrefix $year"
    fun deleteBody(title: String) = "\"$title\" $deleteBodySuffix"
    fun goalsIntro(month: String) = "$goalsIntroPrefix $month. $goalsIntroSuffix"
    fun alert80(category: String) = "$alert80Prefix $category $alert80Suffix"
    fun alert100(category: String) = "$alert100Prefix $category $alert100Suffix"

    companion object {
        val WIZARD = Words(
            wizard = true,
            ledgerTab = "📜 Ledger", calendarTab = "📅 Calendar", annalsTab = "📊 Annals", goalsTab = "🛡 Protego",
            annals = "📊 Annals", goals = "Protego", search = "Accio",
            fab = "✒️ Record expense", newExpense = "Record expense", editExpense = "Amend expense", entry = "📜 Ledger entry",
            amend = "✏️ Amend expense", amount = "Gold spent", spentThisMonth = "Gold spent this month", spentInPrefix = "Gold spent in",
            spentAllTime = "Gold spent, all time", whereWent = "Where the gold went", food = "Feasts & Food", ledger = "The Ledger",
            ledgerEmpty = "The ledger is blank. Tap Record expense to write the first line.",
            dayHint = "Tap a day and say Revelio.", dayEmpty = "No gold left your vault this day.",
            split = "Split with a fellow wizard", save = "Mischief managed · Save",
            deleteTitle = "Evanesco?", deleteBtn = "Evanesco", deleteLink = "Evanesco · delete", keep = "Keep it",
            deleteBodySuffix = "will vanish from the ledger. This cannot be undone.",
            searchPlaceholder = "Accio expense… (title or category)",
            searchHint = "Say Accio! Type part of a title, or pick one or more vaults.",
            searchNothing = "Revelio found nothing. Try another word.",
            vault = "vault", newWard = "🛡 Cast a new ward", aNewWard = "A new ward", removeWard = "Remove this ward?",
            goalsIntroPrefix = "Cast a ward on the vaults you want to watch in",
            goalsIntroSuffix = "An owl warns you at 80% and when a ward breaks.",
            noGoals = "No wards for this month yet.", whichCategory = "Which vault?",
            categoryEmpty = "This vault is empty. Nothing recorded here yet.",
            exportTitle = "📜 Geminio", exportButton = "Geminio · export CSV", exported = "Geminio! Ledger copied to CSV.",
            exportFailed = "The spell failed: export did not work.",
            settingsTab = "⚙️ Settings", reminder = "🦉 Owl reminder", reminderSwitch = "Send the evening owl",
            owlNothing = "🦉 Owl post: did you forget the ledger?", owlSome = "🦉 Owl post: anything else for the ledger?",
            alert80Prefix = "🦉 80% of your", alert80Suffix = "ward is spent",
            alert100Prefix = "⚡ Your", alert100Suffix = "ward is broken",
            overGoal = "The Protego ward is broken!", nicknames = true,
        )

        val PLAIN = Words(
            wizard = false,
            ledgerTab = "📜 Expenses", calendarTab = "📅 Calendar", annalsTab = "📊 Totals", goalsTab = "🎯 Goals",
            annals = "📊 Totals", goals = "Goals", search = "Search",
            fab = "✒️ Add expense", newExpense = "New expense", editExpense = "Edit expense", entry = "Expense",
            amend = "✏️ Edit expense", amount = "Amount", spentThisMonth = "Spent this month", spentInPrefix = "Spent in",
            spentAllTime = "Spent, all time", whereWent = "By category", food = "Food", ledger = "Expenses",
            ledgerEmpty = "No expenses this month yet.",
            dayHint = "Tap a day to see its expenses.", dayEmpty = "Nothing spent on this day.",
            split = "Split with someone", save = "Save",
            deleteTitle = "Delete expense?", deleteBtn = "Delete", deleteLink = "Delete", keep = "Cancel",
            deleteBodySuffix = "will be deleted. This cannot be undone.",
            searchPlaceholder = "Search by title or category",
            searchHint = "Type part of a title, or pick one or more categories.",
            searchNothing = "Nothing found.",
            vault = "category", newWard = "🎯 Add a goal", aNewWard = "A new goal", removeWard = "Remove this goal?",
            goalsIntroPrefix = "Set goals for the categories you want to watch in",
            goalsIntroSuffix = "You get a warning at 80% and when a goal is passed.",
            noGoals = "No goals for this month yet.", whichCategory = "Which category?",
            categoryEmpty = "Nothing recorded in this category yet.",
            exportTitle = "📤 Export", exportButton = "Export CSV", exported = "Expenses exported.", exportFailed = "Export failed.",
            settingsTab = "⚙️ Settings", reminder = "Daily reminder", reminderSwitch = "Send the daily reminder",
            owlNothing = "Did you record today's expenses?", owlSome = "Anything else to record today?",
            alert80Prefix = "80% of your", alert80Suffix = "goal is spent",
            alert100Prefix = "You've reached your", alert100Suffix = "goal",
            overGoal = "Over the goal!", nicknames = false,
        )

        fun of(wizard: Boolean) = if (wizard) WIZARD else PLAIN

        /** A label without its leading emoji ("⚙️ Settings" → "Settings"), for places that show a painted emblem instead. */
        fun withoutEmoji(label: String): String = label.replace(Regex("^[^\\p{L}\\p{N}]+\\s*"), "")
    }
}
