package com.vitalis.chat;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * /w <ник> <текст...> and /r <текст...> — own whispers, zero placeholders.
 * Messages are Adventure components built in code: nothing to misparse.
 * last{} powers /r (both directions, so ping-pong just works).
 */
public class WCommand implements BasicCommand {
    private final HeavenChat plugin;
    private final boolean reply;
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final Map<UUID, UUID> last = new HashMap<>();

    public WCommand(HeavenChat plugin, boolean reply) {
        this.plugin = plugin;
        this.reply = reply;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender s = stack.getSender();
        if (!(s instanceof Player p)) {
            s.sendMessage("Only players.");
            return;
        }
        if (!p.hasPermission("heavenchat.use")) {
            p.sendMessage(Component.text("Нет доступа.", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        if (reply) {
            if (args.length < 1) {
                p.sendMessage(prefix().append(Component.text("Использование: /r <текст>",
                        net.kyori.adventure.text.format.NamedTextColor.YELLOW)));
                return;
            }
            UUID to = last.get(p.getUniqueId());
            Player t = to == null ? null : Bukkit.getPlayer(to);
            if (t == null || !t.isOnline()) {
                p.sendMessage(prefix().append(Component.text("Некому отвечать.",
                        net.kyori.adventure.text.format.NamedTextColor.GRAY)));
                return;
            }
            send(p, t, join(args, 0));
            return;
        }
        if (args.length < 2) {
            p.sendMessage(prefix().append(Component.text("Использование: /w <ник> <текст>",
                    net.kyori.adventure.text.format.NamedTextColor.YELLOW)));
            return;
        }
        Player t = Bukkit.getPlayerExact(args[0]);
        if (t == null || !t.isOnline()) {
            p.sendMessage(prefix().append(Component.text("Игрок " + args[0] + " не в сети.",
                    net.kyori.adventure.text.format.NamedTextColor.RED)));
            return;
        }
        if (t.getUniqueId().equals(p.getUniqueId())) {
            p.sendMessage(prefix().append(Component.text("Самому себе? Серьёзно?",
                    net.kyori.adventure.text.format.NamedTextColor.GRAY)));
            return;
        }
        long now = System.currentTimeMillis();
        Long until = cooldown.get(p.getUniqueId());
        if (until != null && now < until) {
            p.sendMessage(prefix().append(Component.text("Помедленнее.",
                    net.kyori.adventure.text.format.NamedTextColor.GRAY)));
            return;
        }
        cooldown.put(p.getUniqueId(), now + plugin.pmCooldownMs());
        send(p, t, join(args, 1));
    }

    private void send(Player from, Player to, String text) {
        if (text.isEmpty()) return;
        TitlesIndex.Title title = plugin.titles().titleOf(from.getUniqueId());
        String titleName = title == null ? "" : title.name;
        long mins = plugin.titles().minutes(from.getUniqueId());
        to.sendMessage(PmFormat.incoming(from.getName(), titleName, mins, text));
        try {
            to.playSound(to.getLocation(), plugin.pmSound(), plugin.pmVol(), plugin.pmPitch());
        } catch (Exception ignored) {}
        from.sendMessage(PmFormat.outgoing(to.getName(), text));
        last.put(to.getUniqueId(), from.getUniqueId());
        last.put(from.getUniqueId(), to.getUniqueId());
    }

    private Component prefix() {
        return PmFormat.prefix(plugin.chatPrefix());
    }

    private static String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString().trim();
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (!reply && args.length <= 1) {
            String q = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> out = new ArrayList<>();
            for (Player o : Bukkit.getOnlinePlayers()) {
                if (!o.equals(stack.getSender())
                        && o.getName().toLowerCase(Locale.ROOT).startsWith(q)) out.add(o.getName());
            }
            return out;
        }
        return List.of();
    }
}
