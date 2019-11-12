package me.randomhashtags.cosmicvaults.util;

import me.randomhashtags.cosmicvaults.util.universal.UInventory;
import org.bukkit.inventory.ItemStack;

public class PlayerVault {
    private UInventory inv;
    private ItemStack display;
    private ItemStack[] contents;

    public PlayerVault(UInventory inv, ItemStack display, ItemStack[] contents) {
        this.inv = inv;
        this.display = display;
        this.contents = contents;
    }

    public UInventory getInventory() { return inv; }

    public ItemStack getDisplay() { return display.clone(); }
    public void setDisplay(ItemStack display) { this.display = display; }

    public ItemStack[] getContents() { return contents; }
    public void setContents(ItemStack[] contents) { this.contents = contents; }
}
