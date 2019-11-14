package me.randomhashtags.cosmicvaults.util;

import me.randomhashtags.cosmicvaults.util.universal.UInventory;
import me.randomhashtags.cosmicvaults.util.universal.UMaterial;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlayerVault {
    private UInventory inv;
    private ItemStack display;

    public PlayerVault(UInventory inv, ItemStack display) {
        this.inv = inv;
        this.display = display;
    }

    public UInventory getUInventory() { return inv; }
    public Inventory getInventory() { return inv.getInventory(); }

    public ItemStack getDisplay() { return display.clone(); }
    public void setDisplay(ItemStack display) { this.display = display; }
    public void setDisplayMaterial(UMaterial material) {
        final ItemMeta m = display.getItemMeta();
        display.setType(material.getMaterial());
        display.setItemMeta(m);
    }

    public ItemStack[] getContents() { return getInventory().getContents(); }
    public void setContents(ItemStack[] contents) { getInventory().setContents(contents); }
}
