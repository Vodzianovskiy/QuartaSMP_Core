# ✦ QuartaSMP Core

**QuartaSMP Core** is the main Paper plugin for the QuartaSMP server. It combines player levels, seasonal quests, progression tracking, GUI menus, a level-based shop, admin tools, and PlaceholderAPI support into one clean server core.

Players complete quests, earn experience, level up, unlock seasonal weeks, receive rewards, and spend their levels in the shop on items, permissions, prefixes, and other configurable rewards.

---

## ✨ Features

- 🧭 **Main GUI Menu** — quick access to levels, quests, and the shop with `/quartasmp`, `/qsmp`, or `/q`
- 📈 **Player Level System** — players start at level 1 and gain experience by completing quests
- 🧮 **Configurable XP Formula** — `base-exp + level * multiplier-per-level`, fully adjustable in `config.yml`
- 📚 **Seasonal Quest System** — 12 quest weeks with names, tasks, progress, rewards, and unlock logic
- ✅ **336 Ready-to-Use Quests** — default `quests.yml` includes 12 weeks with 28 tasks each
- 🧩 **Many Task Types** — block breaking, block placing, mob kills, crafting, eating, fishing, brewing, enchanting, trading, taming, breeding, smelting, item delivery, inventory checks, and more
- 🕒 **Flexible Week Unlocking** — supports global season schedule, per-player schedule, sequential unlocking, and all-open mode
- ⏳ **Week Time Limits** — weekly progress can expire after a configurable number of days
- 🔄 **Global or Per-Player Resets** — choose between soft per-player resets and global reset days
- 🎁 **Task & Week Rewards** — give items or run console commands with `%player%` placeholders
- 💎 **Level-Based Shop** — players spend their levels as a hard currency in the shop
- 🛒 **Fully Configurable Shop Items** — item icon, display name, lore, price, required level, commands, and special reward types
- 🧾 **Repeat Purchase Protection** — purchased item IDs are stored in player data
- 🗡️ **Built-In Special Rewards** — for example, `TOP_SWORD` through the internal reward service
- 🧑‍💼 **Admin GUI Panel** — view players, levels, experience, completed weeks, and progress
- 🛠️ **Admin Commands** — reload configs, unlock weeks, set test weeks, and add experience
- 💾 **Automatic Player Data Saving** — saves on quit, plugin shutdown, and scheduled autosaves
- 🔌 **Reload-Friendly Startup** — loads data for players already online after plugin reloads
- 🌐 **PlaceholderAPI Support** — expose player level, experience, formatted level, level prefix, and leaderboard placeholders
- 🏆 **Leaderboard Cache** — top-level placeholders can work with cached offline player data
- 🎨 **MiniMessage Formatting** — HEX colors, gradients, bold, italic, underlined, strikethrough, and rainbow formatting
- 🗂️ **Automatic Config Creation** — `config.yml`, `messages.yml`, `quests.yml`, and `shop.yml` are created automatically
- 🔧 **Safe Config Sync** — missing config keys are added from defaults without overwriting existing user values where supported

---

## 🎮 Gameplay Flow

1. A player opens the main menu with `/q`, `/qsmp`, or `/quartasmp`.
2. The menu shows their level, current XP progress, quests, and shop access.
3. The player opens the quest menu and works on the currently available week.
4. Completing tasks grants XP and optional item rewards.
5. Completing an entire week grants quest points, extra XP, and weekly rewards.
6. XP increases the player's level.
7. Levels can be spent in the shop on rewards, permissions, prefixes, and special items.

---

## 🧩 Quest Task Types

| Type | Goal |
|------|------|
| `DELIVER` | Submit items through the GUI; items are removed from the inventory |
| `HAVE_IN_INVENTORY` | Have required items in the inventory without removing them |
| `CRAFT` | Craft a specific amount of items |
| `KILL` | Kill entities of a specific type |
| `BLOCK_BREAK` | Break a specific amount of blocks |
| `BLOCK_PLACE` | Place a specific amount of blocks |
| `ENCHANT` | Enchant items |
| `FISH` | Catch fish |
| `BREW` | Brew potions |
| `EAT` | Eat specific items |
| `TAME` | Tame animals |
| `SMELT` | Smelt items in a furnace |
| `BUCKET_FILL` | Fill buckets |
| `SHEAR` | Shear sheep |
| `TRADE` | Complete villager trades |
| `BREED` | Breed animals |
| `CONSUME_POTION` | Drink a potion with a specific effect |

---

## 🔧 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/quartasmp` | Open the main QuartaSMP Core menu | `quartasmp.command.use` |
| `/qsmp` | Alias for `/quartasmp` | `quartasmp.command.use` |
| `/q` | Short alias for `/quartasmp` | `quartasmp.command.use` |
| `/quartasmp help` | Show command help | `quartasmp.command.use` |
| `/quartasmp reload` | Reload plugin configuration files | `quartasmp.admin` |
| `/quartasmp admin` | Open the admin GUI panel | `quartasmp.admin` |
| `/quartasmp unlockweek <weekId> [player]` | Unlock a week for a player | `quartasmp.admin` |
| `/quartasmp setweek <weekId> [player]` | Set the current test week for a player | `quartasmp.admin` |
| `/quartasmp addexp <amount> [player]` | Add experience to a player | `quartasmp.admin` |

> Console can also run `q <player>` or `quartasmp <player>` to open the main menu for an online player. This is useful for NPC command actions.

---

## 🔐 Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `quartasmp.command.use` | Allows using `/quartasmp`, `/qsmp`, and `/q` | `true` |
| `quartasmp.admin` | Allows access to all admin tools: reload, admin panel, unlockweek, setweek, addexp | `op` |
| `quartasmp.admin.reload` | Allows reloading plugin configs | `op` |

---

## 📊 PlaceholderAPI Placeholders

The plugin registers two PlaceholderAPI identifiers: **`qcore`** and **`core`**. Both can be used.

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%qcore_level%` / `%core_level%` | Player level | `12` |
| `%qcore_lvl%` / `%core_lvl%` | Level alias | `12` |
| `%qcore_player_level%` / `%core_player_level%` | Level alias | `12` |
| `%qcore_exp%` / `%core_exp%` | Current player XP | `450` |
| `%qcore_experience%` / `%core_experience%` | XP alias | `450` |
| `%qcore_level_formatted%` / `%core_level_formatted%` | Formatted level | `12 ур.` |
| `%qcore_level_prefix%` / `%core_level_prefix%` | Level prefix | `&7[&b12 LVL&7]` |
| `%qcore_top_level_1%` ... `%qcore_top_level_10%` | Top players by level | `Steve - 15` |

---

## ⚙️ Configuration

The plugin uses several YAML files inside `plugins/QuartaSMP_Core/`.

| File | Purpose |
|------|---------|
| `config.yml` | Main menu, leveling, week unlock modes, schedules, and autosave settings |
| `messages.yml` | Messages, translated material/entity names, task descriptions, shop messages, quest messages |
| `quests.yml` | Quest weeks, tasks, task rewards, and weekly completion rewards |
| `shop.yml` | Shop items, prices, level requirements, reward commands, and special reward types |

### Key `config.yml` Sections

| Section | Description |
|---------|-------------|
| `menu` | Main GUI title, size, materials, slots, and actions |
| `leveling` | Maximum level, XP per quest point, and XP formula |
| `shop-menu` | Shop GUI title, size, border, and background |
| `quests.weeks-unlock-mode` | Week unlock mode: `global-schedule`, `schedule`, `sequential`, or `all-open` |
| `quests.global-schedule` | Global season start and interval between week unlocks |
| `quests.week-unlock-schedule` | Per-player week unlock schedule based on first join time |
| `quests.reset-mode` | Progress reset mode: `per-player` or `global` |
| `quests.week-time-limit-days` | Number of days allowed to complete a week |
| `player-data.auto-save-interval-minutes` | Player data autosave interval |

---

## 🛒 Shop

The shop uses **player levels** as currency.

Each item in `shop.yml` can contain:

| Field | Description |
|-------|-------------|
| `display-name` | Item display name using MiniMessage |
| `material` | GUI icon material |
| `price-level` | Price in player levels |
| `required-level` | Minimum level required to view/buy the item |
| `lore` | Extra item description |
| `commands` | Console commands executed on purchase; `%player%` is replaced with the player name |
| `reward-type` | Internal special reward type, for example `TOP_SWORD` |

Default shop examples include:

- 🏷️ `Season Pass` prefix
- 🛡️ Private-zone flag permissions through LuckPerms commands
- 🗡️ Special diamond sword
- 🟣 Ender pearls
- 📘 Mending book

---

## 📁 Player Data

For each player, the plugin stores:

- UUID
- level
- current XP
- task progress per week
- completed weeks
- week start timestamps
- first join timestamp
- purchased shop item IDs

Data is loaded on join and saved on quit, plugin shutdown, and scheduled autosaves.

---

## 📦 Dependencies

| Plugin / API | Type | Purpose |
|--------------|------|---------|
| [Paper 1.21.x](https://papermc.io/) | **Required** | Minecraft server API |
| Java 21 | **Required** | Runtime and build Java version |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Optional / Softdepend | Level, XP, prefix, and leaderboard placeholders |
| [LuckPerms](https://luckperms.net/) | Optional | Only required if shop items execute `lp user ...` permission commands |

---

## 🚀 Installation

1. Build the plugin or download the compiled `.jar`.
2. Place `QuartaSMP_Core-1.0-SNAPSHOT.jar` into the Paper server `plugins/` folder.
3. Make sure the server runs on **Java 21**.
4. Start the server.
5. After the first startup, configure the files inside `plugins/QuartaSMP_Core/`:
   - `config.yml`
   - `messages.yml`
   - `quests.yml`
   - `shop.yml`
6. Run `/quartasmp reload` or restart the server.

---

## 🏗️ Building from Source

The project uses **Maven**.

```bash
mvn clean package
```

The compiled plugin will be created here:

```text
target/QuartaSMP_Core-1.0-SNAPSHOT.jar
```

---

## 🧪 Quick Admin Start

```text
/q
/quartasmp help
/quartasmp admin
/quartasmp addexp 1000 PlayerName
/quartasmp unlockweek week_2 PlayerName
/quartasmp setweek week_3 PlayerName
/quartasmp reload
```

---

## 🎨 MiniMessage

Most plugin text supports MiniMessage:

```text
<gradient:#FF6B6B:#4ECDC4>Beautiful gradient</gradient>
<color:#FFD700>HEX color</color>
<bold>Bold text</bold>
<italic>Italic text</italic>
<underlined>Underlined text</underlined>
<rainbow>Rainbow text</rainbow>
```

Used in:

- GUI titles
- item display names
- item lore
- messages
- task descriptions
- quest and shop rewards

---

## 📝 Notes

- `shop.yml` is overwritten from the JAR on plugin load in the current implementation, so default shop items always stay updated.
- `quests.yml` and `messages.yml` are softly synchronized: missing keys are added while existing values are kept.
- If PlaceholderAPI is not installed, the plugin still works, but `%qcore_*%` and `%core_*%` placeholders will not be available.
- If shop items execute LuckPerms commands, LuckPerms must be installed separately.

---

## 👤 Author

**_Nightwarden**