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
import java.util.*;

import static java.io.File.separator;

public class CVPlayer {
    private static final String folder = CosmicVaults.getPlugin.getDataFolder() + separator + "_player data";
    public static final HashMap<UUID, CVPlayer> players = new HashMap<>();
    private static final CosmicVaultsAPI api = CosmicVaultsAPI.getCosmicVaultsAPI();

    private UUID uuid;
    private File file;
    private YamlConfiguration yml;

    private boolean isLoaded = false;
    private HashMap<Integer, PlayerVault> vaultz;
    private HashMap<Integer, UInventory> vaults;
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

        final HashMap<Integer, UInventory> vaults = getVaults();
        final HashMap<Integer, ItemStack> displays = getVaultDisplays();
        yml.set("vaults", null);
        for(int i : displays.keySet()) {
            final ItemStack is = displays.get(i);
            final ItemMeta meta = is.getItemMeta();
            final UMaterial m = UMaterial.match(is);
            yml.set("vaults." + i + ".display.item", m.name());
            yml.set("vaults." + i + ".display.amount", is.getAmount());
            if(meta.hasDisplayName()) yml.set("vaults." + i + ".display.name", meta.getDisplayName());
            if(meta.hasLore()) yml.set("vaults." + i + ".display.lore", meta.getLore());
        }
        for(int i : vaults.keySet()) {
            final UInventory u = vaults.get(i);
            final Inventory inv = u.getInventory();
            final int size = u.getSize();
            yml.set("vaults." + i + ".size", size);
            yml.set("vaults." + i + ".title", u.getTitle());
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
    public HashMap<Integer, UInventory> getVaults() {
        if(vaults == null) {
            vaultz = new HashMap<>(); // TODO
            vaults = new HashMap<>();
            final int max = getMaxVaultPerms(), def = api.getDefaultSizeOfVault();
            final String deft = api.getDefaultVaultTitle();
            final Player player = Bukkit.getPlayer(uuid);
            for(int i = 1; i <= max; i++) {
                final UInventory uinv = new UInventory(player, yml.getInt("vaults." + i + ".size", def), yml.getString("vaults." + i + ".title", deft.replace("{VAULT_NUMBER}", Integer.toString(i))));
                vaultz.put(i, new PlayerVault(uinv, null, null));

                vaults.put(i, uinv);
                if(yml.get("vaults." + i + ".items") != null) {
                    final ConfigurationSection c = yml.getConfigurationSection("vaults." + i + ".items");
                    if(c != null) {
                        final Inventory inv = vaults.get(i).getInventory();
                        for(String s : c.getKeys(false)) {
                            inv.setItem(Integer.parseInt(s), yml.getItemStack("vaults." + i + ".items." + s));
                        }
                    }
                }
            }
        }
        return vaults;
    }
    public UInventory getVault(int vault) {
        return getVaults().getOrDefault(vault, null);
    }
    public HashMap<Integer, ItemStack> getVaultDisplays() {
        if(vaultDisplay == null) {
            vaultDisplay = new HashMap<>();
            final ItemStack is = api.defaultPvsDisplay;
            final List<String> lore = new ArrayList<>();
            for(int i = 1; i <= getMaxVaultPerms(); i++) {
                if(yml.get("vaults." + i) != null) {
                    vaultDisplay.put(i, yml.getItemStack("vaults." + i + ".display"));
                } else {
                    final String v = Integer.toString(i);
                    final ItemStack item = is.clone();
                    item.setAmount(i);
                    final ItemMeta meta = item.getItemMeta();
                    lore.clear();
                    meta.setDisplayName(meta.getDisplayName().replace("{VAULT_NUMBER}", v));
                    for(String s : meta.getLore()) {
                        lore.add(s.replace("{VAULT_NUMBER}", v));
                    }
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    vaultDisplay.put(i, item);
                }
            }
        }
        return vaultDisplay;
    }
    public ItemStack getDisplay(int vault) {
        return getVaultDisplays().getOrDefault(vault, new ItemStack(Material.BARRIER));
    }
    public void setDisplayName(int vault, String name) {
        final ItemStack is = getDisplay(vault);
        final ItemMeta m = is.getItemMeta();
        m.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        is.setItemMeta(m);
    }
    public void setIcon(int vault, UMaterial icon) {
        final ItemStack is = getDisplay(vault), d = icon.getItemStack();
        d.setItemMeta(is.getItemMeta());
        vaultDisplay.put(vault, d);
    }
    public void setVaults(HashMap<Integer, UInventory> vaults) {
        this.vaults = vaults;
    }
    public ItemStack[] getVaultItems(int vault) {
        return vaults.containsKey(vault) ? vaults.get(vault).getInventory().getContents() : null;
    }
    public void setVaultItems(int vault, ItemStack[] items) {
        if(!vaults.containsKey(vault)) vaults.put(vault, new UInventory(null, 54, "Vault #" + vault));
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
