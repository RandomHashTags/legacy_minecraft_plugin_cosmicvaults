package me.randomhashtags.cosmicvaults.utils.classes;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class PlayerVault {

    private static HashMap<UUID, PlayerVault> players = new HashMap<>();
    private final UUID uuid;
    private HashMap<Integer, HashMap<ItemStack, ItemStack[]>> vaults = new HashMap<>();

    public PlayerVault(UUID uuid) {
        this.uuid = uuid;
        if(!players.keySet().contains(uuid)) {
            players.put(uuid, this);
        }
    }
    public static PlayerVault get(UUID player) { return players.getOrDefault(player, new PlayerVault(player)); }

    public UUID getUUID() { return uuid; }
    public HashMap<Integer, HashMap<ItemStack, ItemStack[]>> getVaults() { return vaults; }
    public void setVaults(HashMap<Integer, HashMap<ItemStack, ItemStack[]>> vaults) { this.vaults = vaults; }
    public ItemStack[] getVaultItems(int vault) { return vaults.keySet().contains(vault) ? (ItemStack[]) vaults.get(vault).values().toArray()[0] : null; }
    public void setVaultItems(int vault, ItemStack display, ItemStack[] items) { vaults.put(vault, new HashMap<>()); vaults.get(vault).put(display, items); }
}
