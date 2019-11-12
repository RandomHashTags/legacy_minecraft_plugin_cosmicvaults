package me.randomhashtags.cosmicvaults;

import me.randomhashtags.cosmicvaults.util.CVPlayer;
import org.bukkit.plugin.java.JavaPlugin;

public final class CosmicVaults extends JavaPlugin {
    public static CosmicVaults getPlugin;

    private CosmicVaultsAPI api;

    @Override
    public void onEnable() {
        getPlugin = this;
        enable();
    }

    @Override
    public void onDisable() {
        disable();
    }

    public void enable() {
        saveDefaultConfig();
        api = CosmicVaultsAPI.getCosmicVaultsAPI();
        api.load();
        getCommand("playervault").setExecutor(api);
    }
    public void disable() {
        api.unload();
        CVPlayer.unloadAllPlayerData();
    }

    public void reload() {
        disable();
        enable();
    }
}
