package world.ultravanilla.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import world.ultravanilla.UltraVanilla;

import java.awt.Color;

public class ChatColorCommand extends UltraCommand implements CommandExecutor, Listener {

    public ChatColorCommand(UltraVanilla instance) {
        super(instance);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!sender.hasPermission("ultravanilla.chat.chatcolor-prefix")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /chatcolor <color>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            UltraVanilla.set(player, "text-prefix", ChatColor.RESET.toString());
            sender.sendMessage(ChatColor.GREEN + "Chat color reset.");
            return true;
        }

        try {
            if (args[0].startsWith("#") && contrastRatio(Color.decode(args[0])) < 2.0) { // 2.0 because mc chat bg is translucent and can be very dark
                sender.sendMessage(CONTRAST_ERROR);
                return true;
            }

            ChatColor color = ChatColor.of(args[0]);

            if (color == ChatColor.BLACK || color == ChatColor.DARK_GRAY) {
                sender.sendMessage(CONTRAST_ERROR);
                return true;
            }

            UltraVanilla.set(player, "text-prefix", color.toString());
            sender.sendMessage(ChatColor.GREEN + "Chat color set to " + color + args[0] + ChatColor.GREEN + ".");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid color: " + args[0]);
        }

        return true;
    
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPermission("ultravanilla.chat.chatcolor-prefix")) {
            YamlConfiguration config = UltraVanilla.getPlayerConfig(player);
            if (config != null) {
                String prefix = config.getString("text-prefix");
                if (prefix != null && !prefix.equals(ChatColor.RESET.toString())) {
                    UltraVanilla.set(player, "text-prefix", ChatColor.RESET.toString());
                }
            }
        }
    }

    // Contrast ratio calculation based on WCAG guidelines
    private static double contrastRatio(Color c1) {
        double l1 = relativeLuminance(c1);
        double l2 = relativeLuminance(new Color(43, 43, 43)); // mc background estimate

        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);

        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(Color color) {
        double r = channel(color.getRed() / 255.0);
        double g = channel(color.getGreen() / 255.0);
        double b = channel(color.getBlue() / 255.0);

        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(double c) {
        return c <= 0.03928
            ? c / 12.92
            : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static final String CONTRAST_ERROR =
        ChatColor.RED + "The chosen color may be hard to read against the background. Please choose a different color.";
}
