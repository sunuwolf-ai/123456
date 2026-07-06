package com.jjajang.rpg.warp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class WarpMenuListener implements Listener {

    public static final String TITLE = ChatColor.DARK_AQUA + "✦ 워프 ✦";
    private final JavaPlugin plugin;

    public WarpMenuListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        inv.setItem(11, makeItem(Material.BEACON,
                ChatColor.AQUA + "웨이의 건축현장",
                List.of(ChatColor.GRAY + "좌표: -92, 150, 7",
                        ChatColor.YELLOW + "클릭하여 이동")));

        inv.setItem(15, makeItem(Material.GRASS_BLOCK,
                ChatColor.GREEN + "테스트존",
                List.of(ChatColor.GRAY + "좌표: -3, 130, -1",
                        ChatColor.YELLOW + "클릭하여 이동")));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();

        if (name.equals(ChatColor.AQUA + "웨이의 건축현장")) {
            player.closeInventory();
            player.teleport(new Location(player.getWorld(), -92, 150, 7));
            player.sendMessage(ChatColor.AQUA + "[워프] 웨이의 건축현장으로 이동했습니다.");
        } else if (name.equals(ChatColor.GREEN + "테스트존")) {
            player.closeInventory();
            player.teleport(new Location(player.getWorld(), -3, 130, -1));
            player.sendMessage(ChatColor.GREEN + "[워프] 테스트존으로 이동했습니다.");
        }
    }

    private static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack makeBorder() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}
