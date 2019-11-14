package me.randomhashtags.cosmicvaults.util;

import me.randomhashtags.cosmicvaults.CosmicVaults;
import me.randomhashtags.cosmicvaults.CosmicVaultsAPI;
import me.randomhashtags.cosmicvaults.util.universal.UInventory;
import me.randomhashtags.cosmicvaults.util.universal.UMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static java.io.File.separator;

public class CVPlayer {
    private static final String folder = CosmicVaults.getPlugin.getDataFolder() + separator + "_player data";
    public static final HashMap<UUID, CVPlayer> players = new HashMap<>();
    private static final CosmicVaultsAPI api = CosmicVaultsAPI.getCosmicVaultsAPI();

    private UUID uuid;
    private File file;
    private YamlConfiguration yml;

    private boolean isLoaded = false;
    private HashMap<Integer, PlayerVault> vaults;
    private HashMap<Integer, ItemStack> vaultDisplay;

    public CVPlayer(UUID uuid) {
        this.uuid = uuid;
        final File f = new File(folder, uuid.toString() + ".yml");
        boolean backup = false;
        if(!players.containsKey(uuid)) {
            if(!f.exists()) {
                try {
                    final File folder = new File(CVPlayer.folder);
                    if(!folder.exists()) {
                        folder.mkdirs();
                    }
                    f.createNewFile();
                    backup = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            file = new File(folder, uuid.toString() + ".yml");
            yml = YamlConfiguration.loadConfiguration(file);
            players.put(uuid, this);
        }
        if(backup) backup();
    }
    public static CVPlayer get(UUID player) { return players.getOrDefault(player, new CVPlayer(player)).load(); }
    public void backup() {
        yml.set("name", Bukkit.getOfflinePlayer(uuid).getName());

        final HashMap<Integer, PlayerVault> vaults = getVaults();
        yml.set("vaults", null);
        for(int i : vaults.keySet()) {
            final PlayerVault vault = vaults.get(i);
            final UInventory u = vault.getUInventory();
            final Inventory inv = u.getInventory();
            final int size = u.getSize();
            yml.set("vaults." + i + ".size", size);
            yml.set("vaults." + i + ".title", u.getTitle());
            yml.set("vaults." + i + ".display", vault.getDisplay().toString());
            for(int a = 0; a < size; a++) {
                final ItemStack is = inv.getItem(a);
                if(is != null) {
                    yml.set("vaults." + i + ".items." + a, is.toString());
                }
            }
        }
        save();
    }
    public CVPlayer load() {
        if(!isLoaded) {
            isLoaded = true;
            return this;
        }
        return players.get(uuid);
    }
    public void unload() {
        if(isLoaded) {
            backup();
            isLoaded = false;
            players.remove(uuid);
        }
    }

    public boolean isLoaded() { return isLoaded; }
    public UUID getUUID() { return uuid; }
    public YamlConfiguration getYaml() { return yml; }

    public int getMaxVaultPerms() {
        int a = 0;
        final Player player = Bukkit.getPlayer(uuid);
        for(int i = 1; i <= 100; i++) {
            if(player.hasPermission("CosmicVaults.pv." + i)) {
                a = i;
            }
        }
        return a;
    }
    public HashMap<Integer, PlayerVault> getVaults() {
        if(vaults == null) {
            vaults = new HashMap<>();
            final int max = getMaxVaultPerms(), def = api.getDefaultSizeOfVault();
            final String deft = api.getDefaultVaultTitle();
            final Player player = Bukkit.getPlayer(uuid);
            for(int i = 1; i <= max; i++) {
                final int size = yml.getInt("vaults." + i + ".size", def);
                final UInventory uinv = new UInventory(player, size, yml.getString("vaults." + i + ".title", deft.replace("{VAULT_NUMBER}", Integer.toString(i))));
                final ItemStack display = yml.getItemStack("vaults." + i + ".display", new ItemStack(Material.EMERALD));
                final PlayerVault vault = new PlayerVault(uinv, display);

                final ItemStack[] contents = new ItemStack[size];
                if(yml.get("vaults." + i + ".items") != null) {
                    final ConfigurationSection c = yml.getConfigurationSection("vaults." + i + ".items");
                    for(String s : c.getKeys(false)) {
                        contents[Integer.parseInt(s)] = yml.getItemStack("vaults." + i + ".items." + s);
                    }
                }
                vault.setContents(contents);
                vaults.put(i, vault);
            }
        }
        return vaults;
    }
    public PlayerVault getVault(int vault) {
        return getVaults().getOrDefault(vault, null);
    }
    public ItemStack getVaultDisplay(int vault) {
        final PlayerVault v = getVault(vault);
        return v != null ? v.getDisplay() : null;
    }
    public void setVaults(HashMap<Integer, PlayerVault> vaults) {
        this.vaults = vaults;
    }
    public ItemStack[] getVaultItems(int vault) {
        return vaults.containsKey(vault) ? vaults.get(vault).getInventory().getContents() : null;
    }
    public void setVaultContents(int vault, ItemStack[] items) {
        if(!vaults.containsKey(vault)) {
            vaults.put(vault, new PlayerVault(new UInventory(null, 54, "Vault #" + vault), null));
        }
        vaults.get(vault).getInventory().setContents(items);
    }


    private void save() {
        try {
            yml.save(file);
            yml = YamlConfiguration.loadConfiguration(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public OfflinePlayer getOfflinePlayer() {
        return uuid != null ? Bukkit.getOfflinePlayer(uuid) : null;
    }

    public static void loadAllPlayerData() {
        try {
            for(File f : new File(folder).listFiles()) {
                CVPlayer.get(UUID.fromString(f.getName().split("\\.yml")[0]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void unloadAllPlayerData() {
        for(CVPlayer p : new ArrayList<>(players.values())) {
            p.unload();
        }
    }
}
