package me.randomhashtags.cosmicvaults.util.universal;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import me.randomhashtags.cosmicvaults.CosmicVaults;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface UVersionable {
    CosmicVaults cosmicvaults = CosmicVaults.getPlugin;
    String version = Bukkit.getVersion();
    BukkitScheduler scheduler = Bukkit.getScheduler();
    PluginManager pluginmanager = Bukkit.getPluginManager();

    default List<String> colorizeListString(@Nullable List<String> input) {
        final List<String> i = new ArrayList<>();
        if(input != null) {
            for(String s : input) {
                i.add(ChatColor.translateAlternateColorCodes('&', s));
            }
        }
        return i;
    }

    default int getRemainingInt(@NotNull String string) {
        string = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', string)).replaceAll("\\p{L}", "").replaceAll("\\s", "").replaceAll("\\p{P}", "").replaceAll("\\p{S}", "");
        return string.isEmpty() ? -1 : Integer.parseInt(string);
    }
    default void sendStringListMessage(@NotNull CommandSender sender, @NotNull List<String> message, @Nullable HashMap<String, String> replacements) {
        for(String s : message) {
            if(replacements != null) {
                for(String r : replacements.keySet()) {
                    s = s.replace(r, replacements.get(r));
                }
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', s));
        }
    }
}
