package us.copestudios.cskoth;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.copestudios.copecore.command.CommandContext;
import us.copestudios.copecore.command.CommandFailure;
import us.copestudios.copecore.command.CopeCommand;
import us.copestudios.copecore.text.Messages;
import us.copestudios.copecore.text.Text;
import us.copestudios.cskoth.gui.KothMenus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /cskoth (alias /koth).
 *
 * <pre>
 * Everyone (cskoth.use):
 *   /cskoth list                   KOTHs and which are running
 *   /cskoth next                   the next scheduled KOTH
 *   /cskoth points [koth]          points per player (per KOTH, or the total)
 *   /cskoth top                    most captures                         (new)
 * Admins (cskoth.admin):
 *   /cskoth  or  /cskoth menu      the menu
 *   /cskoth create &lt;name&gt;         new KOTH (wand selection, or a box around you)
 *   /cskoth setzone &lt;koth&gt;        new zone (wand selection, or "here" twice) (new)
 *   /cskoth start &lt;koth&gt; [minutes]   (console too; minutes is new)
 *   /cskoth stop &lt;koth&gt;
 *   /cskoth delete &lt;koth&gt;                                              (now a command too)
 *   /cskoth reload                                                       (new)
 * </pre>
 *
 * <p>1.x opened the menu - with create and delete buttons - for every player.</p>
 */
final class KothCommand {

    private final CSKoth plugin;
    private final Messages messages;

    KothCommand(CSKoth plugin) {
        this.plugin = plugin;
        this.messages = plugin.messages();
    }

    private Koth koth(CommandContext ctx, int index) {
        Koth koth = plugin.koths().get(ctx.arg(index));
        if (koth == null) {
            throw new CommandFailure("koth.not-found", "koth", Text.escape(ctx.arg(index)));
        }
        return koth;
    }

    void register() {
        CopeCommand command = new CopeCommand(plugin, "cskoth", messages);

        // "/cskoth": the menu for admins, the help page for everyone else.
        command.defaultAction(ctx -> {
            if (ctx.isPlayer() && ctx.sender().hasPermission(CSKoth.ADMIN)) {
                KothMenus.main(plugin, ctx.player()).open();
            } else {
                command.sendHelp(ctx.sender(), ctx.label());
            }
        });

        command.sub("menu").aliases("gui").permission(CSKoth.ADMIN).playerOnly()
                .descriptionKey("command.help.menu").description("open the KOTH menu")
                .executes(ctx -> KothMenus.main(plugin, ctx.player()).open());

        command.sub("list").permission(CSKoth.USE)
                .descriptionKey("command.help.list").description("list KOTHs")
                .executes(this::list);

        command.sub("next").permission(CSKoth.USE)
                .descriptionKey("command.help.next").description("the next scheduled KOTH")
                .executes(ctx -> plugin.koths().showNext(ctx.sender()));

        command.sub("points").permission(CSKoth.USE).usage("[koth]")
                .descriptionKey("command.help.points").description("points per player")
                .tab(ctx -> plugin.names())
                .executes(this::points);

        command.sub("top").aliases("leaderboard").permission(CSKoth.USE)
                .descriptionKey("command.help.top").description("most captures")
                .executes(ctx -> top(ctx.sender()));

        command.sub("create").permission(CSKoth.ADMIN).playerOnly().usage("<name>").minArgs(1)
                .descriptionKey("command.help.create").description("make a KOTH")
                .executes(ctx -> plugin.create(ctx.player(), ctx.arg(0)));

        command.sub("setzone").aliases("resize").permission(CSKoth.ADMIN).playerOnly().usage("<koth>").minArgs(1)
                .descriptionKey("command.help.setzone").description("set the zone")
                .tab(ctx -> plugin.names())
                .executes(ctx -> plugin.askZone(ctx.player(), koth(ctx, 0), null));

        command.sub("start").permission(CSKoth.ADMIN).usage("<koth> [minutes]").minArgs(1)
                .descriptionKey("command.help.start").description("start a KOTH")
                .tab(ctx -> ctx.size() <= 1 ? plugin.names() : List.of("15", "30", "60"))
                .executes(ctx -> {
                    Koth koth = koth(ctx, 0);
                    int minutes = ctx.has(1) ? ctx.integer(1, 1, 10_080) : 0;
                    if (!plugin.koths().start(koth, KothGame.Reason.MANUAL, minutes)) {
                        throw new CommandFailure("koth.already-running", "koth", koth.name());
                    }
                    ctx.reply("koth.you-started", "koth", koth.name());
                });

        command.sub("stop").permission(CSKoth.ADMIN).usage("<koth>").minArgs(1)
                .descriptionKey("command.help.stop").description("stop a KOTH")
                .tab(ctx -> plugin.koths().running().stream().map(game -> game.koth().name()).toList())
                .executes(ctx -> {
                    Koth koth = koth(ctx, 0);
                    if (!plugin.koths().stop(koth.name(), true)) {
                        throw new CommandFailure("koth.not-running", "koth", koth.name());
                    }
                    ctx.reply("koth.you-stopped", "koth", koth.name());
                });

        command.sub("delete").aliases("remove").permission(CSKoth.ADMIN).usage("<koth>").minArgs(1)
                .descriptionKey("command.help.delete").description("delete a KOTH")
                .tab(ctx -> plugin.names())
                .executes(ctx -> {
                    Koth koth = koth(ctx, 0);
                    plugin.koths().delete(koth);
                    ctx.reply("koth.deleted", "koth", koth.name());
                });

        command.sub("reload").permission(CSKoth.ADMIN)
                .descriptionKey("command.help.reload").description("reload the files")
                .executes(ctx -> {
                    long started = System.currentTimeMillis();
                    plugin.reload();
                    ctx.reply("generic.reloaded", "plugin", plugin.getName(),
                            "ms", String.valueOf(System.currentTimeMillis() - started));
                });

        command.register();
    }

    private void list(CommandContext ctx) {
        if (plugin.koths().all().isEmpty()) {
            throw new CommandFailure("list.empty");
        }
        messages.sendRaw(ctx.sender(), "list.header");
        for (Koth koth : plugin.koths().all()) {
            KothGame game = plugin.koths().game(koth.name());
            String status = game == null ? messages.format("list.inactive") : messages.format("list.active");
            String coords = koth.area().center() == null ? "?" : koth.area().center().getBlockX() + ", "
                    + koth.area().center().getBlockY() + ", " + koth.area().center().getBlockZ();
            messages.sendRaw(ctx.sender(), "list.entry", "koth", koth.name(), "status", status,
                    "time", KothGame.clock(koth.captureTime()), "points", String.valueOf(koth.capturePoints()),
                    "coords", coords, "world", koth.area().world());
        }
    }

    private void points(CommandContext ctx) {
        if (!ctx.has(0)) {
            // Total points of the sender + top 10 by points.
            if (ctx.isPlayer()) {
                Stats.Entry own = plugin.stats().of(ctx.player().getUniqueId());
                ctx.reply("points.own", "points", String.valueOf(own.points()),
                        "captures", String.valueOf(own.captures()));
            }
            top(ctx.sender());
            return;
        }
        Koth koth = koth(ctx, 0);
        Map<UUID, Integer> points = plugin.stats().pointsFor(koth.name());
        messages.sendRaw(ctx.sender(), "points.header", "koth", koth.name());
        if (points.isEmpty()) {
            messages.sendRaw(ctx.sender(), "points.none");
            return;
        }
        int rank = 1;
        for (Map.Entry<UUID, Integer> entry : points.entrySet()) {
            if (rank > 10) {
                break;
            }
            messages.sendRaw(ctx.sender(), "points.entry", "rank", String.valueOf(rank++),
                    "player", nameOf(entry.getKey()), "points", String.valueOf(entry.getValue()));
        }
    }

    private void top(CommandSender to) {
        List<Stats.Entry> top = plugin.stats().top(10);
        messages.sendRaw(to, "top.header");
        if (top.isEmpty()) {
            messages.sendRaw(to, "top.none");
            return;
        }
        int rank = 1;
        for (Stats.Entry entry : top) {
            messages.sendRaw(to, "top.entry", "rank", String.valueOf(rank++), "player", nameOf(entry.player()),
                    "captures", String.valueOf(entry.captures()), "points", String.valueOf(entry.points()));
        }
    }

    private static String nameOf(UUID id) {
        Player online = org.bukkit.Bukkit.getPlayer(id);
        if (online != null) {
            return online.getName();
        }
        String name = org.bukkit.Bukkit.getOfflinePlayer(id).getName();
        return name == null ? "?" : name;
    }
}
