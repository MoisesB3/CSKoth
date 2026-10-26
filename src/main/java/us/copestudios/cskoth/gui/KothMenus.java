package us.copestudios.cskoth.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.copestudios.copecore.CopeCore;
import us.copestudios.copecore.gui.ItemBuilder;
import us.copestudios.copecore.gui.Menu;
import us.copestudios.copecore.gui.MenuClick;
import us.copestudios.copecore.text.Messages;
import us.copestudios.copecore.text.Text;
import us.copestudios.cskoth.CSKoth;
import us.copestudios.cskoth.Koth;
import us.copestudios.cskoth.KothGame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * The admin menus (/cskoth), on CopeCore's {@link Menu}. Same screens as 1.x:
 *
 * <pre>
 * Main ── Create KOTH (type a name)
 *      └─ KOTH list ── Manage &lt;koth&gt; ── Start / Stop
 *                                      ├─ Settings  (capture time, points)
 *                                      ├─ Rewards   ── Item rewards (drag items in)
 *                                      │            └─ Command rewards (add / right-click to remove)
 *                                      ├─ Schedule  (daily start, duration, always on, respawn delay)
 *                                      ├─ Zone      (grow, shrink, set with the wand or "here", size)
 *                                      └─ Delete    (asks to confirm - new)
 * </pre>
 *
 * <p>1.x recognised its menus by their title, so renaming a title in messages.yml broke them (and a chest renamed
 * "KOTH List" acted like the menu). CopeCore menus are recognised by the menu object itself. All texts are in
 * messages.yml under "menu". Numbers: left +, right -, shift ×10, Q to type a value.</p>
 */
public final class KothMenus {

    private KothMenus() {
    }

    public static Menu main(CSKoth plugin, Player viewer) {
        return new MainMenu(plugin, viewer);
    }

    // =====================================================================
    //  Shared look
    // =====================================================================

    /** Base: plugin + messages + "item from messages.yml". */
    private abstract static class Base extends Menu {
        protected final CSKoth koths;
        protected final Messages messages;

        Base(CSKoth plugin, Player viewer, int rows, String titleKey, String... pairs) {
            super(plugin, viewer, rows, plugin.messages().format(titleKey, pairs));
            this.koths = plugin;
            this.messages = plugin.messages();
        }

        /** An item named menu.&lt;key&gt;.name with lore menu.&lt;key&gt;.lore (+ extra lines). */
        protected ItemStack item(Material material, String key, List<String> extra, String... pairs) {
            List<String> lore = new ArrayList<>();
            for (String line : messages.templates("menu." + key + ".lore")) {
                lore.add(Text.replace(line, pairs));
            }
            if (extra != null) {
                lore.addAll(extra);
            }
            return ItemBuilder.of(material).name(messages.format("menu." + key + ".name", pairs)).lore(lore)
                    .hideFlags().build();
        }

        protected String yesNo(boolean value) {
            return messages.format(value ? "menu.on" : "menu.off");
        }

        /** A number button that saves koths.yml after each change. */
        protected void number(MenuClick click, String labelKey, double current, double step, double min, double max,
                              DoubleConsumer apply) {
            adjustNumber(click, Text.plain(messages.format(labelKey)), current, step, min, max, true, value -> {
                apply.accept(value);
                koths.koths().saveAll();
            });
        }

        /** The KOTH still exists? (Another admin may have deleted it.) Otherwise back to the list. */
        protected boolean gone(Koth koth) {
            if (koths.koths().get(koth.name()) == koth) {
                return false;
            }
            messages.send(viewer, "koth.not-found", "koth", koth.name());
            new ListMenu(koths, viewer, 0).open();
            return true;
        }
    }

    // =====================================================================
    //  Main
    // =====================================================================

    private static final class MainMenu extends Base {
        MainMenu(CSKoth plugin, Player viewer) {
            super(plugin, viewer, 3, "menu.main.title");
        }

        @Override
        protected void build() {
            set(11, item(Material.EMERALD, "main.create", null), click ->
                    CopeCore.prompts().text(viewer, messages.format("menu.main.create.question"), name -> {
                        Koth koth = koths.create(viewer, name.trim());
                        if (koth != null) {
                            new ManageMenu(koths, viewer, koth).open();
                        } else {
                            open();
                        }
                    }, this::open));
            set(13, item(Material.BOOK, "main.list", null, "count", String.valueOf(koths.koths().all().size())),
                    click -> new ListMenu(koths, viewer, 0).open());
            closeButton(15);
            fillEmpty();
        }
    }

    // =====================================================================
    //  List
    // =====================================================================

    private static final class ListMenu extends Base {
        private final int page;

        ListMenu(CSKoth plugin, Player viewer, int page) {
            super(plugin, viewer, 6, "menu.list.title");
            this.page = page;
        }

        @Override
        protected void build() {
            List<Koth> all = new ArrayList<>(koths.koths().all());
            paginate(all, page, GRID, koth -> {
                boolean running = koths.koths().isRunning(koth.name());
                return item(running ? Material.BEACON : Material.LODESTONE, "list.koth", null,
                        "koth", koth.name(), "status", messages.format(running ? "menu.running" : "menu.stopped"),
                        "time", KothGame.clockText(koth.captureTime()), "points", String.valueOf(koth.capturePoints()));
            }, (koth, click) -> new ManageMenu(koths, viewer, koth).open(),
                    45, 53, newPage -> new ListMenu(koths, viewer, newPage).open());
            backButton(49, () -> new MainMenu(koths, viewer).open());
            fillEmpty();
        }
    }

    // =====================================================================
    //  Manage one KOTH
    // =====================================================================

    private static final class ManageMenu extends Base {
        private final Koth koth;

        ManageMenu(CSKoth plugin, Player viewer, Koth koth) {
            super(plugin, viewer, 4, "menu.manage.title", "koth", koth.name());
            this.koth = koth;
        }

        @Override
        protected void build() {
            boolean running = koths.koths().isRunning(koth.name());
            set(10, item(running ? Material.RED_WOOL : Material.LIME_WOOL, running ? "manage.stop" : "manage.start", null,
                    "koth", koth.name()), click -> {
                if (gone(koth)) {
                    return;
                }
                if (koths.koths().isRunning(koth.name())) {
                    koths.koths().stop(koth.name(), true);
                } else {
                    koths.koths().start(koth, KothGame.Reason.MANUAL, 0);
                }
                refresh();
            });
            set(12, item(Material.COMPARATOR, "manage.settings", null,
                    "time", String.valueOf(koth.captureTime()), "points", String.valueOf(koth.capturePoints())),
                    click -> new SettingsMenu(koths, viewer, koth).open());
            set(14, item(Material.CHEST, "manage.rewards", null,
                    "items", String.valueOf(koth.itemRewards().size()), "commands", String.valueOf(koth.commandRewards().size())),
                    click -> new RewardsMenu(koths, viewer, koth).open());
            set(16, item(Material.CLOCK, "manage.schedule", null,
                    "scheduled", yesNo(koth.scheduled()), "time", koth.startTimeText(), "always", yesNo(koth.alwaysOn())),
                    click -> new ScheduleMenu(koths, viewer, koth).open());
            set(20, item(Material.GOLDEN_AXE, "manage.zone", null,
                    "size", koth.area().sizeX() + "x" + koth.area().sizeY() + "x" + koth.area().sizeZ()),
                    click -> new ZoneMenu(koths, viewer, koth).open());
            set(22, item(Material.ENDER_PEARL, "manage.teleport", null), click -> {
                if (koth.area().center() != null) {
                    close();
                    viewer.teleport(koth.area().center());
                }
            });
            set(24, item(Material.TNT, "manage.delete", null), click -> new ConfirmDeleteMenu(koths, viewer, koth).open());
            backButton(31, () -> new ListMenu(koths, viewer, 0).open());
            fillEmpty();
        }
    }

    // =====================================================================
    //  Settings: capture time, points
    // =====================================================================

    private static final class SettingsMenu extends Base {
        private final Koth koth;

        SettingsMenu(CSKoth plugin, Player viewer, Koth koth) {
            super(plugin, viewer, 3, "menu.settings.title", "koth", koth.name());
            this.koth = koth;
        }

        @Override
        protected void build() {
            set(11, item(Material.CLOCK, "settings.time", numberHint(10), "time", String.valueOf(koth.captureTime()),
                    "clock", KothGame.clockText(koth.captureTime())),
                    click -> number(click, "menu.settings.time.label", koth.captureTime(), 10, 1, 86_400,
                            value -> koth.captureTime((int) value)));
            set(15, item(Material.EXPERIENCE_BOTTLE, "settings.points", numberHint(1), "points", String.valueOf(koth.capturePoints())),
                    click -> number(click, "menu.settings.points.label", koth.capturePoints(), 1, 0, 100_000,
                            value -> koth.capturePoints((int) value)));
            backButton(22, () -> new ManageMenu(koths, viewer, koth).open());
            fillEmpty();
        }
    }

    // =====================================================================
    //  Rewards
    // =====================================================================

    private static final class RewardsMenu extends Base {
        private final Koth koth;

        RewardsMenu(CSKoth plugin, Player viewer, Koth koth) {
            super(plugin, viewer, 3, "menu.rewards.title", "koth", koth.name());
            this.koth = koth;
        }

        @Override
        protected void build() {
            set(11, item(Material.DIAMOND, "rewards.items", null, "count", String.valueOf(koth.itemRewards().size())),
                    click -> new ItemRewardsMenu(koths, viewer, koth).open());
            set(15, item(Material.COMMAND_BLOCK, "rewards.commands", null, "count", String.valueOf(koth.commandRewards().size()),
                    "global", String.valueOf(koths.cfg().getStringList("rewards").size())),
                    click -> new CommandRewardsMenu(koths, viewer, koth).open());
            backButton(22, () -> new ManageMenu(koths, viewer, koth).open());
            fillEmpty();
        }
    }

    /** Put the reward items in the top 5 rows. Saved with the button, and also when the menu is closed. */
    private static final class ItemRewardsMenu extends Base {
        private static final int[] SLOTS = range(0, 45);
        private final Koth koth;
        private boolean saved;

        ItemRewardsMenu(CSKoth plugin, Player viewer, Koth koth) {
            super(plugin, viewer, 6, "menu.items.title", "koth", koth.name());
            this.koth = koth;
        }

        @Override
        protected boolean allowsPlayerInventory() {
            return true; // pick items up from your own inventory and put them in
        }

        @Override
        protected void build() {
            for (int i = 0; i < SLOTS.length; i++) {
                set(SLOTS[i], i < koth.itemRewards().size() ? koth.itemRewards().get(i).clone() : null);
            }
            editable(SLOTS);
            ItemStack pane = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
            for (int slot = 45; slot < 54; slot++) {
                set(slot, pane);
            }
            backButton(45, () -> {
                save(false);
                new RewardsMenu(koths, viewer, koth).open();
            });
            set(49, item(Material.LIME_DYE, "items.save", null), click -> {
                save(true);
                new RewardsMenu(koths, viewer, koth).open();
            });
        }

        private void save(boolean tell) {
            if (saved) {
                return;
            }
            saved = true;
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack item : itemsIn(SLOTS)) {
                if (item != null && !item.getType().isAir()) {
                    items.add(item.clone());
                }
            }
            koth.itemRewards(items);
            koths.koths().saveAll();
            messages.send(viewer, tell ? "menu.items.saved" : "menu.items.saved-on-close",
                    "count", String.valueOf(items.size()));
        }

        @Override
        protected void onClose() {
            save(false);
        }
    }

    /** Reward commands: add one (typed in chat), right-click one to remove it. */
    private static final class CommandRewardsMenu extends Base {
        private final Koth koth;

        CommandRewardsMenu(CSKoth plugin, Player viewer, Koth koth) {
            super(plugin, viewer, 6, "menu.commands.title", "koth", koth.name());
            this.koth = koth;
        }

        @Override
        protected void build() {
            List<String> commands = koth.commandRewards();
            for (int i = 0; i < Math.min(45, commands.size()); i++) {
                int index = i;
                set(i, item(Material.PAPER, "commands.entry", null, "number", String.valueOf(i + 1),
                        "command", Text.escape(commands.get(i))), click -> {
                    if (click.isRight() && index < koth.commandRewards().size()) {
                        koth.commandRewards().remove(index);
                        koths.koths().saveAll();
                        messages.send(viewer, "menu.commands.removed");
                        refresh();
                    }
                });
            }
            backButton(45, () -> new RewardsMenu(koths, viewer, koth).open());
            set(49, item(Material.EMERALD, "commands.add", null), click ->
                    CopeCore.prompts().text(viewer, messages.format("menu.commands.question"), command -> {
                        String line = command.trim();
                        if (line.startsWith("/")) {
                            line = line.substring(1);
                        }
                        if (!line.isEmpty()) {
                            koth.commandRewards().add(line);
                            koths.koths().saveAll();
                            messages.send(viewer, "menu.commands.added", "command", Text.escape(line));
                        }
                        open();
                    }, this::open));
            fillEmpty();
        }
    }

    // =====================================================================
    //  Schedule
    // =====================================================================

    private static final class ScheduleMenu extends Base {
        private final Koth koth;

        ScheduleMenu(CSKoth plugin, Player viewer, Koth koth) {
            super(plugin, viewer, 4, "menu.schedule.title", "koth", koth.name());
            this.koth = koth;
        }

        @Override
        protected void build() {
            set(10, item(koth.scheduled() ? Material.LIME_DYE : Material.GRAY_DYE, "schedule.scheduled", null,
                    "value", yesNo(koth.scheduled())), click -> {
                koth.scheduled(!koth.scheduled());
                koths.koths().saveAll();
                refresh();
            });
            set(12, item(Material.CLOCK, "schedule.time", null, "value", koth.startTimeText()), click ->
                    CopeCore.prompts().ask(koths, viewer, messages.format("menu.schedule.time.question"), input -> {
                        String[] parts = input.trim().split("[:.h ]");
                        try {
                            int hour = Integer.parseInt(parts[0]);
                            int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                                return messages.format("menu.schedule.time.invalid");
                            }
                            koth.startTime(hour, minute);
                            koths.koths().saveAll();
                            messages.send(viewer, "menu.schedule.time.set", "koth", koth.name(), "time", koth.startTimeText());
                            open();
                            return null;
                        } catch (NumberFormatException notATime) {
                            return messages.format("menu.schedule.time.invalid");
                        }
                    }, this::open));
            set(14, item(Material.HOPPER, "schedule.duration", numberHint(5), "value", String.valueOf(koth.durationMinutes())),
                    click -> number(click, "menu.schedule.duration.label", koth.durationMinutes(), 5, 1, 1440,
                            value -> koth.durationMinutes((int) value)));
            set(16, item(koth.alwaysOn() ? Material.BEACON : Material.GLASS, "schedule.always-on", null,
                    "value", yesNo(koth.alwaysOn())), click -> {
                koth.alwaysOn(!koth.alwaysOn());
                koths.koths().saveAll();
                refresh();
            });
            set(22, item(Material.REPEATER, "schedule.respawn", numberHint(5), "value", String.valueOf(koth.respawnDelay())),
                    click -> number(click, "menu.schedule.respawn.label", koth.respawnDelay(), 5, 0, 1440,
                            value -> koth.respawnDelay((int) value)));
            backButton(31, () -> new ManageMenu(koths, viewer, koth).open());
            fillEmpty();
        }
    }

    // =====================================================================
    //  Zone
    // =====================================================================

    private static final class ZoneMenu extends Base {
        private final Koth koth;

        ZoneMenu(CSKoth plugin, Player viewer, Koth koth) {
            super(plugin, viewer, 4, "menu.zone.title", "koth", koth.name());
            this.koth = koth;
        }

        @Override
        protected void build() {
            set(11, item(Material.LIME_STAINED_GLASS, "zone.expand", null), click -> {
                koths.resize(koth, click.isShift() ? 5 : 1);
                CopeCore.regions().showOutline(viewer, koth.area());
                messages.send(viewer, "menu.zone.expanded");
                refresh();
            });
            set(13, item(Material.GOLDEN_AXE, "zone.select", null), click ->
                    koths.askZone(viewer, koth, () -> new ZoneMenu(koths, viewer, koth).open()));
            set(15, item(Material.RED_STAINED_GLASS, "zone.shrink", null), click -> {
                if (koths.resize(koth, click.isShift() ? -5 : -1)) {
                    CopeCore.regions().showOutline(viewer, koth.area());
                    messages.send(viewer, "menu.zone.shrunk");
                } else {
                    messages.send(viewer, "menu.zone.too-small");
                }
                refresh();
            });
            set(22, item(Material.MAP, "zone.info", null,
                    "width", String.valueOf(koth.area().sizeX()), "height", String.valueOf(koth.area().sizeY()),
                    "length", String.valueOf(koth.area().sizeZ()), "volume", String.valueOf(koth.area().volume()),
                    "world", koth.area().world()), click -> CopeCore.regions().showOutline(viewer, koth.area()));
            backButton(31, () -> new ManageMenu(koths, viewer, koth).open());
            fillEmpty();
        }
    }

    // =====================================================================
    //  Delete (confirm)
    // =====================================================================

    private static final class ConfirmDeleteMenu extends Base {
        private final Koth koth;

        ConfirmDeleteMenu(CSKoth plugin, Player viewer, Koth koth) {
            super(plugin, viewer, 3, "menu.delete.title", "koth", koth.name());
            this.koth = koth;
        }

        @Override
        protected void build() {
            set(11, item(Material.RED_CONCRETE, "delete.confirm", null, "koth", koth.name()), click -> {
                koths.koths().delete(koth);
                messages.send(viewer, "koth.deleted", "koth", koth.name());
                new ListMenu(koths, viewer, 0).open();
            });
            set(15, item(Material.LIME_CONCRETE, "delete.cancel", null), click -> new ManageMenu(koths, viewer, koth).open());
            fillEmpty();
        }
    }

    private static int[] range(int from, int to) {
        int[] slots = new int[to - from];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = from + i;
        }
        return slots;
    }
}
