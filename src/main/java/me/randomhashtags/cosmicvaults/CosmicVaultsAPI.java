package me.randomhashtags.cosmicvaults;

import com.sun.istack.internal.NotNull;
import me.randomhashtags.cosmicvaults.util.CVPlayer;
import me.randomhashtags.cosmicvaults.util.PlayerVault;
import me.randomhashtags.cosmicvaults.util.universal.UInventory;
import me.randomhashtags.cosmicvaults.util.universal.UMaterial;
import me.randomhashtags.cosmicvaults.util.universal.UVersion;
import me.randomhashtags.cosmicvaults.util.universal.UVersionable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public final class CosmicVaultsAPI implements CommandExecutor, Listener, UVersionable {
    private static CosmicVaultsAPI instance;
    public static CosmicVaultsAPI getCosmicVaultsAPI() {
        if(instance == null) instance = new CosmicVaultsAPI();
        return instance;
    }

    private String prefix, menuTitle, otherPvs;
    private HashMap<Player, Integer> editing, editingName, editingIcon;
    private HashMap<Player, CVPlayer> editingOther;
    private HashMap<Player, Integer> editingOtherVault, editingOtherIcon;
    public ItemStack defaultPvsDisplay;

    private UInventory editIcon;
    private FileConfiguration config;

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        final Player player = sender instanceof Player ? (Player) sender : null;
        if(player != null) {
            final int l = args.length;
            if(l == 0) {
                viewMenu(player);
            } else {
                final String a = args[0];
                final OfflinePlayer o = Bukkit.getOfflinePlayer(a);
                final HashMap<String, String> replacements = new HashMap<>();
                replacements.put("{PREFIX}", prefix);
                switch (l) {
                    case 1:
                        int v = getRemainingInt(a);
                        if(v < 0) {
                            if(!o.isOnline()) {
                                sendStringListMessage(player, config.getStringList("messages.target player has no vaults"), replacements);
                            } else {
                                viewOtherMenu(player, CVPlayer.get(o.getUniqueId()));
                            }
                        } else if(v <= CVPlayer.get(player.getUniqueId()).getMaxVaultPerms()) {
                            viewVault(player, v);
                        } else {
                            replacements.put("{VAULT_NUMBER}", Integer.toString(v));
                            sendStringListMessage(player, config.getStringList("messages.no vault access"), replacements);
                        }
                        break;
                    case 2:
                        if(!o.isOnline()) {
                            sendStringListMessage(player, config.getStringList("messages.target player has no vaults"), replacements);
                        } else {
                            final CVPlayer pdata = CVPlayer.get(o.getUniqueId());
                            final HashMap<Integer, PlayerVault> vaults = pdata.getVaults();
                            if(vaults.size() == 0) {
                                sendStringListMessage(player, config.getStringList("messages.target player has no vaults"), replacements);
                            } else {
                                v = getRemainingInt(args[1]);
                                final PlayerVault vault = vaults.getOrDefault(v, null);
                                if(vault != null) {
                                    viewOtherVault(player, pdata, vault, v);
                                }
                            }
                        }
                        break;
                    default:
                        viewMenu(player);
                        break;
                }
            }
        }
        return true;
    }

    public void load() {
        pluginmanager.registerEvents(this, cosmicvaults);
        config = cosmicvaults.getConfig();

        prefix = ChatColor.translateAlternateColorCodes('&', config.getString("messages.prefix"));
        editing = new HashMap<>();
        editingName = new HashMap<>();
        editingIcon = new HashMap<>();
        editingOther = new HashMap<>();
        editingOtherVault = new HashMap<>();
        editingOtherIcon = new HashMap<>();

        defaultPvsDisplay = d(config, "CosmicVaults.default pvs display");
        menuTitle = ChatColor.translateAlternateColorCodes('&', config.getString("CosmicVaults.titles.pvs"));
        otherPvs = ChatColor.translateAlternateColorCodes('&', config.getString("CosmicVaults.titles.other pvs"));

        editIcon = new UInventory(null, config.getInt("edit icon.size"), ChatColor.translateAlternateColorCodes('&', config.getString("edit icon.title")));
        final ConfigurationSection c = config.getConfigurationSection("edit icon");
        final List<String> selectLore = colorizeListString(config.getStringList("edit icon.select lore"));
        if(c != null) {
            final Inventory i = editIcon.getInventory();
            for(String s : c.getKeys(false)) {
                final ItemStack is = d(config, "edit icon." + s);
                if(is != null) {
                    final ItemMeta m = is.getItemMeta();
                    final List<String> l = new ArrayList<>();
                    if(m.hasLore()) {
                        l.addAll(m.getLore());
                    }
                    l.addAll(selectLore);
                    m.setLore(l);
                    is.setItemMeta(m);
                    i.setItem(Integer.parseInt(s), is);
                }
            }
        }

        for(Player p : Bukkit.getOnlinePlayers()) {
            CVPlayer.get(p.getUniqueId()).load();
        }
    }

    public void unload() {
        for(Player p : new ArrayList<>(editing.keySet())) {
            p.closeInventory();
        }
        for(Player p : new ArrayList<>(editingName.keySet())) {
            p.closeInventory();
        }
        for(Player p : new ArrayList<>(editingIcon.keySet())) {
            p.closeInventory();
        }
        for(Player p : new ArrayList<>(editingOther.keySet())) {
            p.closeInventory();
        }
        HandlerList.unregisterAll(this);
    }


    public int getDefaultSizeOfVault() { return config.getInt("CosmicVaults.size of vault"); }
    public String getDefaultVaultTitle() { return ChatColor.translateAlternateColorCodes('&', config.getString("CosmicVaults.titles.self")); }

    public void viewOtherMenu(@NotNull Player player, @NotNull CVPlayer target) {
        if(hasPermission(player, "CosmicVaults.pv.other.edit")) {
            player.closeInventory();
            final HashMap<Integer, PlayerVault> vaults = target.getVaults();
            final int s = vaults.size(), size = s > 54 ? 54 : s%9 == 0 && s > 0 ? s : ((s+9)/9)*9;
            player.openInventory(Bukkit.createInventory(player, size, otherPvs.replace("{PLAYER}", Bukkit.getOfflinePlayer(target.getUUID()).getName())));
            final Inventory top = player.getOpenInventory().getTopInventory();
            for(int i = 0; i < s && i < config.getInt("CosmicVaults.max size of pvs display"); i++) {
                top.setItem(i, target.getVaultDisplay(i+1));
            }
            player.updateInventory();
            editingOther.put(player, target);
        }
    }
    public void viewMenu(@NotNull Player player) {
        if(hasPermission(player, "CosmicVaults.playervaults")) {
            player.closeInventory();
            final CVPlayer pdata = CVPlayer.get(player.getUniqueId());
            final HashMap<Integer, PlayerVault> vaults = pdata.getVaults();
            final int s = vaults.size(), size = s > 54 ? 54 : s%9 == 0 && s > 0 ? s : ((s+9)/9)*9;
            player.openInventory(Bukkit.createInventory(player, size, menuTitle));
            final Inventory top = player.getOpenInventory().getTopInventory();
            for(int i = 0; i < s && i < config.getInt("CosmicVaults.max size of pvs display"); i++) {
                top.setItem(i, pdata.getVaultDisplay(i+1));
            }
            player.updateInventory();
        }
    }
    public void viewOtherVault(Player player, CVPlayer target, PlayerVault vault, int vaultNumber) {
        if(hasPermission(player, "CosmicVaults.pv.other.view")) {
            player.closeInventory();
            player.openInventory(vault.getInventory());
            editingOther.put(player, target);
            editingOtherVault.put(player, vaultNumber);
        } else {
            final HashMap<String, String> replacements = new HashMap<>();
            replacements.put("{PREFIX}", prefix);
            sendStringListMessage(player, config.getStringList("messages.no permission to view other vault"), replacements);
        }
    }
    public void viewVault(Player player, int number) {
        if(hasPermission(player, "CosmicVaults.pv." + number)) {
            final CVPlayer vaults = CVPlayer.get(player.getUniqueId());
            final PlayerVault vault = vaults.getVault(number);
            player.closeInventory();
            player.openInventory(vault.getInventory());
            player.updateInventory();
            editing.put(player, number);
        }
    }
    public void setVaultContents(OfflinePlayer player, int vault, ItemStack[] contents) {
        CVPlayer.get(player.getUniqueId()).getVaults().get(vault).getInventory().setContents(contents);
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return !(sender instanceof Player) || sender.hasPermission(permission);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void playerChatEvent(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if(editingName.containsKey(player)) {
            event.setCancelled(true);
            final String s = event.getMessage();
            final int vault = editingName.get(player);
            final HashMap<String, String> replacements = new HashMap<>();
            replacements.put("{PREFIX}", prefix);
            replacements.put("{VAULT_NUMBER}", Integer.toString(vault));
            replacements.put("{NAME}", s);
            if(s.equals("cancel")) {
                sendStringListMessage(player, config.getStringList("messages.rename vault cancelled"), replacements);
            } else {
                final CVPlayer pdata = CVPlayer.get(player.getUniqueId());
                final PlayerVault v = pdata.getVault(vault);
                final ItemStack display = v.getDisplay();
                final ItemMeta m = display.getItemMeta();
                m.setDisplayName(s);;
                display.setItemMeta(m);
                v.setDisplay(display);

                sendStringListMessage(player, config.getStringList("messages.rename vault success"), replacements);
            }
            if(config.getBoolean("CosmicVaults.open menu when.rename vault")) {
                viewMenu(player);
            }
            editingName.remove(player);
        }
    }
    @EventHandler
    private void playerJoinEvent(PlayerJoinEvent event) {
        CVPlayer.get(event.getPlayer().getUniqueId());
    }
    @EventHandler
    private void playerQuitEvent(PlayerQuitEvent event) {
        CVPlayer.get(event.getPlayer().getUniqueId()).unload();
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void inventoryCloseEvent(InventoryCloseEvent event) {
        final Player player = (Player) event.getPlayer();
        if(editing.containsKey(player)) {
            setVaultContents(player, editing.get(player), event.getInventory().getContents());
            editing.remove(player);
            if(config.getBoolean("CosmicVaults.open menu when.closed self player vault")) {
                scheduler.scheduleSyncDelayedTask(cosmicvaults, () -> viewMenu(player), 1);
            }
        } else if(editingIcon.containsKey(player)) {
            editingIcon.remove(player);
            if(config.getBoolean("CosmicVaults.open menu when.closed edit icon")) {
                scheduler.scheduleSyncDelayedTask(cosmicvaults, () -> viewMenu(player), 1);
            }
        } else if(editingOther.containsKey(player)) {
            if(hasPermission(player, "CosmicVaults.pv.other.edit")) {
                final CVPlayer t = editingOther.get(player);
                final OfflinePlayer target = t.getOfflinePlayer();
                final boolean vault = editingOtherVault.containsKey(player);
                if(vault) {
                    setVaultContents(target, editingOtherVault.get(player), event.getInventory().getContents());
                }
                if(!target.isOnline()) t.unload();
            }
            editingOther.remove(player);
            editingOtherVault.remove(player);
            editingOtherIcon.remove(player);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void inventoryClickEvent(InventoryClickEvent event) {
        final Player player = (Player) event.getWhoClicked();
        final boolean isMenu = event.getView().getTitle().equals(menuTitle), isEditingIcon = editingIcon.containsKey(player), isEditingOther = editingOther.containsKey(player);
        if(isMenu || isEditingIcon || isEditingOther) {
            event.setCancelled(true);
            player.updateInventory();
            final int r = event.getRawSlot(), vault = editingIcon.get(player);
            final ItemStack c = event.getCurrentItem();
            if(r < 0 || r >= player.getOpenInventory().getTopInventory().getSize() || c == null || c.getType().equals(Material.AIR)) return;
            if(isMenu) {
                final String a = event.getClick().name();
                if(a.contains("LEFT")) {
                    viewVault(player, r+1);
                } else if(a.contains("RIGHT")) {
                    editIcon(player, r+1);
                } else if(a.contains("MIDDLE")) {
                    enterEditName(player, r+1);
                }
            } else if(isEditingIcon) {
                final UMaterial material = UMaterial.match(c);
                CVPlayer.get(player.getUniqueId()).getVault(vault).setDisplayMaterial(material);
                player.closeInventory();
                final HashMap<String, String> replacements = new HashMap<>();
                replacements.put("{PREFIX}", prefix);
                replacements.put("{VAULT_NUMBER}", Integer.toString(vault));
                replacements.put("{MATERIAL}", material.name());
                sendStringListMessage(player, config.getStringList("messages.save edited icon"), replacements);
            } else { // isEditingOther
                final CVPlayer pdata = editingOther.get(player);
                final boolean edit = editingOtherVault.containsKey(player), icon = editingOtherIcon.containsKey(player);
                final String perm = "CosmicVaults.pv.other." + (edit ? "edit" : icon ? "edit.icon" : "view");
                if(!hasPermission(player, perm)) {
                    final HashMap<String, String> replacements = new HashMap<>();
                    replacements.put("{PREFIX}", prefix);
                    sendStringListMessage(player, config.getStringList("messages.no permission to " + (edit ? "edit other vault" : icon ? "edit other vault icon" : "view other vault")), null);
                } else if(!edit && !icon) { // menu
                    player.closeInventory();
                    viewOtherVault(player, pdata, pdata.getVaults().get(r+1), r+1);
                }
            }
        }
    }

    public void enterEditName(Player player, int vault) {
        if(!editingName.containsKey(player)) {
            player.closeInventory();
            editingName.put(player, vault);
            final HashMap<String, String> replacements = new HashMap<>();
            replacements.put("{PLAYER}", player.getName());
            replacements.put("{VAULT_NUMBER}", Integer.toString(vault));
            replacements.put("{PREFIX}", prefix);
            sendStringListMessage(player, config.getStringList("messages.rename vault"), replacements);
            player.updateInventory();
        }
    }
    public void editIcon(Player player, int vault) {
        if(!editingIcon.containsKey(player)) {
            player.openInventory(Bukkit.createInventory(player, editIcon.getSize(), editIcon.getTitle().replace("{VAULT_NUMBER}", Integer.toString(vault))));
            player.getOpenInventory().getTopInventory().setContents(editIcon.getInventory().getContents());
            player.updateInventory();
            editingIcon.put(player, vault);
        }
    }

    public ItemStack d(FileConfiguration config, String path) {
        ItemStack item = null;
        ItemMeta itemMeta = null;
        if(config == null && path != null || config != null && config.get(path + ".item") != null) {
            final String PP = config == null ? path : config.getString(path + ".item");
            String P = PP.toLowerCase();

            int amount = config != null && config.get(path + ".amount") != null ? config.getInt(path + ".amount") : 1;
            if(P.toLowerCase().contains(";amount=")) {
                final Random random = new Random();
                final String A = P.split("=")[1];
                final boolean B = P.contains("-");
                final int min = B ? Integer.parseInt(A.split("-")[0]) : 0;
                amount = B ? min+random.nextInt(Integer.parseInt(A.split("-")[1])-min+1) : Integer.parseInt(A);
                path = path.split(";amount=")[0];
                P = P.split(";")[0];
            }

            boolean enchanted = config != null && config.getBoolean(path + ".enchanted");
            SkullMeta m = null;
            String name = config != null ? config.getString(path + ".name") : null;
            final String[] material = P.toUpperCase().split(":");
            final String mat = material[0];
            final byte data = material.length == 2 ? Byte.parseByte(material[1]) : 0;
            final UMaterial U = UMaterial.match(mat + (data != 0 ? ":" + data : ""));
            try {
                item = U.getItemStack();
                final Material skullitem = UMaterial.PLAYER_HEAD_ITEM.getMaterial(), i = item.getType();
                if(!i.equals(Material.AIR)) {
                    item.setAmount(amount);
                    itemMeta = item.getItemMeta();
                    if(i.equals(skullitem)) {
                        m = (SkullMeta) itemMeta;
                        if(item.getData().getData() == 3) m.setOwner(P.split(":").length == 4 ? P.split(":")[3].split("}")[0] : "RandomHashTags");
                    }
                    (i.equals(skullitem) ? m : itemMeta).setDisplayName(name != null ? ChatColor.translateAlternateColorCodes('&', name) : null);

                    if(enchanted) itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    final HashMap<Enchantment, Integer> enchants = new HashMap<>();
                    List<String> lore = new ArrayList<>();
                    if(config != null && config.get(path + ".lore") != null) {
                        final UVersion uv = UVersion.getUVersion();
                        for(String string : config.getStringList(path + ".lore")) {
                            if(string.toLowerCase().startsWith("venchants{")) {
                                for(String s : string.split("\\{")[1].split("}")[0].split(";")) {
                                    enchants.put(uv.getEnchantment(s), getRemainingInt(s));
                                }
                            } else {
                                lore.add(ChatColor.translateAlternateColorCodes('&', string));
                            }
                        }
                    }
                    (!i.equals(skullitem) ? itemMeta : m).setLore(lore);
                    item.setItemMeta(!item.getType().equals(skullitem) ? itemMeta : m);
                    if(enchanted) item.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
                    if(config != null && config.get(path + ".durability") != null) {
                        item.setDurability((short) config.getInt(path + ".durability"));
                    }
                    for(Enchantment enchantment : enchants.keySet()) {
                        if(enchantment != null) {
                            item.addUnsafeEnchantment(enchantment, enchants.get(enchantment));
                        }
                    }
                }
            } catch(Exception e) {
                System.out.println("UMaterial null itemstack. mat=" + mat + ";data=" + data + ";versionName=" + (U != null ? U.getVersionName() : null) + ";getMaterial()=" + (U != null ? U.getMaterial() : null));
                return null;
            }
        }
        return item;
    }
}
