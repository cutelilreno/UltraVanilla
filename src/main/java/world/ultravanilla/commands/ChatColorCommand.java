package world.ultravanilla.commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import world.ultravanilla.UltraVanilla;

public class ChatColorCommand extends UltraCommand implements CommandExecutor {

    public ChatColorCommand(UltraVanilla instance) {
        super(instance);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        if (!sender.hasPermission("ultravanilla.chat.color")) {
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
            ChatColor color = ChatColor.of(args[0]);
            UltraVanilla.set(player, "text-prefix", color.toString());
            sender.sendMessage(ChatColor.GREEN + "Chat color set to " + color + args[0] + ChatColor.GREEN + ".");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid color: " + args[0]);
        }

        return true;
    
    }
}
