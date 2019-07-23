package me.randomhashtags.cosmicvaults.utils;

import me.randomhashtags.cosmicvaults.CosmicVaults;
import me.randomhashtags.cosmicvaults.utils.universal.UInventory;
import me.randomhashtags.cosmicvaults.utils.universal.UMaterial;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CVPlayer {
    private static final String s = File.separator, folder = CosmicVaults.getPlugin.getDataFolder() + s + "_player data";
    public static final HashMap<UUID, CVPlayer> players = new HashMap<>();
    private static final CosmicVaults api = CosmicVaults.getPlugin;

    private UUID uuid;
    public File file = null;
    private YamlConfiguration yml = null;

    private boolean isLoaded = false;
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
    public static CVPlayer get(UUID player) { return players.getOrDefault(player, new CVPlayer(player)); }
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
                    final UMaterial um = UMaterial.match(is);
                    yml.set("vaults." + i + ".items." + a + ".item", um.name());
                    yml.set("vaults." + i + ".items." + a + ".amount", is.getAmount());
                    final short dura = is.getDurability();
                    if(dura != 0) yml.set("vaults." + i + ".items." + a + ".durability", dura);
                    if(is.hasItemMeta()) {
                        final ItemMeta m = is.getItemMeta();
                        if(m.hasDisplayName()) yml.set("vaults." + i + ".items." + a + ".name", m.getDisplayName());
                        final List<String> l = new ArrayList<>();
                        if(m.hasEnchants()) {
                            String b = "VEnchants{";
                            final Map<Enchantment, Integer> enchants = m.getEnchants();
                            final int es = enchants.size();
                            for(int s = 0; s < es; s++) {
                                final Enchantment e = (Enchantment) enchants.keySet().toArray()[s];
                                b = b.concat(e.getName() + enchants.get(e) + (s != es-1 ? ";" : ""));
                            }
                            l.add(b + "}");
                        }
                        if(m.hasLore()) l.addAll(m.getLore());
                        if(!l.isEmpty()) yml.set("vaults." + i + ".items." + a + ".lore", l);
                    }
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
            file = null;
            yml = null;
            players.remove(uuid);
            uuid = null;
            vaults = null;
            vaultDisplay = null;
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
    private void loadVaults() {
        if(vaults == null) vaults = new HashMap<>();
        final CosmicVaults cv = CosmicVaults.getPlugin;
        final int max = getMaxVaultPerms(), def = cv.getDefaultSizeOfVault();
        final String deft = cv.getDefaultVaultTitle();
        final Player player = Bukkit.getPlayer(uuid);
        for(int i = 1; i <= max; i++) {
            if(!vaults.containsKey(i)) {
                vaults.put(i, new UInventory(player, yml.getInt("vaults." + i + ".size", def), yml.getString("vaults." + i + ".title", deft.replace("{VAULT_NUMBER}", Integer.toString(i)))));
                if(yml.get("vaults." + i + ".items") != null) {
                    final ConfigurationSection c = yml.getConfigurationSection("vaults." + i + ".items");
                    if(c != null) {
                        final Inventory inv = vaults.get(i).getInventory();
                        for(String s : c.getKeys(false)) {
                            inv.setItem(Integer.parseInt(s), api.d(yml, "vaults." + i + ".items." + s));
                        }
                    }
                }
            }
        }
    }
    public HashMap<Integer, UInventory> getVaults() {
        loadVaults();
        return vaults;
    }
    public UInventory getVault(int vault) {
        loadVaults();
        return vaults.getOrDefault(vault, null);
    }
    private void loadVaultDisplays() {
        if(vaultDisplay == null) {
            vaultDisplay = new HashMap<>();
            final ItemStack is = api.defaultPvsDisplay;
            final List<String> lore = new ArrayList<>();
            for(int i = 1; i <= getMaxVaultPerms(); i++) {
                if(yml.get("vaults." + i) != null) {
                    vaultDisplay.put(i, api.d(yml, "vaults." + i + ".display"));
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
    }
    public HashMap<Integer, ItemStack> getVaultDisplays() {
        loadVaultDisplays();
        return vaultDisplay;
    }
    public ItemStack getDisplay(int vault) {
        loadVaultDisplays();
        return vaultDisplay.getOrDefault(vault, new ItemStack(Material.BARRIER));
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
                CVPlayer.get(UUID.fromString(f.getName().split("\\.yml")[0])).load();
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
