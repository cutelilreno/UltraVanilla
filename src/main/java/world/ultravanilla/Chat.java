package world.ultravanilla;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import world.ultravanilla.commands.MuteCommand;
import world.ultravanilla.reference.Palette;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat implements Listener {

    private static final Pattern PING_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{2,})");

    private final UltraVanilla plugin;

    private static final LegacyComponentSerializer BUNGEE_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private final MiniMessage safeMiniMessage = MiniMessage.builder()
            .tags(
                    TagResolver.builder()
                            .resolver(StandardTags.color())
                            .resolver(StandardTags.gradient())
                            .resolver(StandardTags.rainbow())
                            .resolver(StandardTags.reset())
                            .resolver(StandardTags.decorations())
                            .resolver(StandardTags.pride())
                            .resolver(StandardTags.sprite())
                            .resolver(StandardTags.sequentialHead())
                            .build()
            )
            .build();

    public Chat(UltraVanilla plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        plugin.unsetAfk(player);
        
        var config = UltraVanilla.getPlayerConfig(player.getUniqueId());
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Mute
        if (config.getBoolean("muted", false)) {
            player.sendMessage(MuteCommand.COLOR + "You are muted.");
            event.setCancelled(true);
            plugin.getLogger().info(player.getName() + " tried to say: " + message);
            return;
        }

        if (player.hasPermission("ultravanilla.chat.color")) {
            message = Palette.translate(message);
            Component legacyComp = BUNGEE_SERIALIZER.deserialize(message);
            message = MiniMessage.miniMessage().serialize(legacyComp)
                    .replace("\\<", "<")
                    .replace("\\>", ">")
                    .replace("\\\\", "\\");
        } else {
            message = safeMiniMessage.escapeTags(message);
        }

        // Pings
        if (message.contains("@")) {
            Matcher m = PING_PATTERN.matcher(message);
            StringBuilder sb = new StringBuilder();
            
            while (m.find()) {
                String match = m.group(0);
                String name = m.group(1);

                if (name.equals("everyone") && player.hasPermission("ultravanilla.chat.everyone")) {
                    String colorTag = getMMColor(plugin.getConfig().getString("color.chat.ping.everyone"));
                    String replacement = colorTag + name + colorTag.replace("<", "</");
                    m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    for (Audience viewer : event.viewers()) {
                        if (viewer instanceof Player) {
                            plugin.ping((Player) viewer);
                        }
                    }
                } else {
                    boolean matchedAnyone = false;
                    for (Audience viewer : event.viewers()) {
                        if (viewer instanceof Player) {
                            Player recipient = (Player) viewer;
                            String plainDisplayName = PlainTextComponentSerializer.plainText().serialize(recipient.displayName()).toLowerCase();
                            
                            if (recipient.getName().toLowerCase().contains(name.toLowerCase()) || 
                                plainDisplayName.contains(name.toLowerCase())) {
                                matchedAnyone = true;
                                plugin.ping(player, recipient);
                            }
                        }
                    }
                    if (matchedAnyone) {
                        String colorTag = getMMColor(plugin.getConfig().getString("color.chat.ping.user"));
                        String replacement = colorTag + name + colorTag.replace("<", "</");
                        m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    } else {
                        m.appendReplacement(sb, Matcher.quoteReplacement(match));
                    }
                }
            }
            m.appendTail(sb);
            message = sb.toString();
        }

        String textPrefix = config.getString("text-prefix", "§r");
        Component prefixComponent = BUNGEE_SERIALIZER.deserialize(textPrefix);
        Component parsedMessage = parseSafeMiniMessage(message);
        event.message(prefixComponent.append(parsedMessage));

        // Chat format
        boolean donator = player.hasPermission("ultravanilla.donator");
        boolean staff = player.hasPermission("ultravanilla.staff-custom");
        String group = plugin.getRole(player);
        TextColor rankColor = getTextColor(plugin.getConfig().getString("color.rank." + group));
        List<String> renames = plugin.getConfig().getStringList("rename-groups." + group);

        String rank;
        if (renames.isEmpty()) {
            String defaultRank = group.substring(0, 1).toUpperCase() + group.substring(1);
            rank = plugin.getConfig().getString("rename-groups." + group, defaultRank);
        } else {
            int variant = config.getInt("role-variant", 0);
            rank = renames.get(variant);
        }

        TextColor donatorBracketsColor = getTextColor(plugin.getConfig().getString("color.chat.brackets.donator"));
        TextColor staffBracketsColor = getTextColor(plugin.getConfig().getString("color.chat.brackets.staff"));
        TextColor rankBracketsColor = getTextColor(plugin.getConfig().getString("color.chat.brackets.rank", "gray"));
        TextColor nameBracketsColor = getTextColor(plugin.getConfig().getString("color.chat.brackets.name", "gray"));
        TextColor defaultNameColor = getTextColor(plugin.getConfig().getString("color.chat.default-name-color"));
        TextColor staffColor = getTextColor(plugin.getConfig().getString("color.rank.staff"));
        TextColor donatorColor = getTextColor(plugin.getConfig().getString("color.rank.donator"));

        String donatorSymbol = plugin.getConfig().getString("rename-groups.donator", "D");
        String staffSymbol = plugin.getConfig().getString("rename-groups.staff", "S");

        Component prefix = Component.empty();

        if (donator) {
            Component donatorComp = BUNGEE_SERIALIZER.deserialize(Palette.translate(donatorSymbol)).colorIfAbsent(donatorColor);
            prefix = prefix
                    .append(Component.text("[", donatorBracketsColor))
                    .append(donatorComp)
                    .append(Component.text("] ", donatorBracketsColor));
        }

        if (staff) {
            Component staffComp = BUNGEE_SERIALIZER.deserialize(Palette.translate(staffSymbol)).colorIfAbsent(staffColor);
            prefix = prefix
                    .append(Component.text("[", staffBracketsColor))
                    .append(staffComp)
                    .append(Component.text("] ", staffBracketsColor));
        }

        Component rankComponent = BUNGEE_SERIALIZER.deserialize(Palette.translate(rank)).colorIfAbsent(rankColor);
        Component finalPrefix = prefix
                .append(Component.text("[", rankBracketsColor))
                .append(rankComponent)
                .append(Component.text("] ", rankBracketsColor))
                .append(Component.text("<", nameBracketsColor));

        event.renderer((source, sourceDisplayName, msg, viewer) -> finalPrefix
                .append(sourceDisplayName.colorIfAbsent(defaultNameColor))
                .append(Component.text("> ", nameBracketsColor))
                .append(msg));
    }

    private String getMMColor(String hexOrName) {
        return "<" + getTextColor(hexOrName).asHexString() + ">";
    }

    private TextColor getTextColor(String hexOrName) {
        return getTextColor(hexOrName, "white");
    }

    private TextColor getTextColor(String hexOrName, String fallback) {
        String str = hexOrName != null ? hexOrName : fallback;
        TextColor color;
        
        if (str.startsWith("#")) {
            color = TextColor.fromHexString(str);
        } else {
            color = NamedTextColor.NAMES.value(str.toLowerCase());
        }
        
        return color != null ? color : NamedTextColor.WHITE;
    }

    private Component parseSafeMiniMessage(String text) {
        return safeMiniMessage.deserialize(text);
    }
}
