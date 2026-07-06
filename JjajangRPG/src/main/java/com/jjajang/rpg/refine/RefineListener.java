package com.jjajang.rpg.refine;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.jjajang.rpg.stats.StatListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class RefineListener implements Listener {

    private final JavaPlugin plugin;
    private StatListener statListener;

    public RefineListener(JavaPlugin plugin) { this.plugin = plugin; }

    public void setStatListener(StatListener sl) { this.statListener = sl; }

    public static void open(Player player) {
        player.openInventory(RefineGUI.build(player, null));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!RefineGUI.TITLE.equals(event.getView().getTitle())) return;

        int slot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        if (slot == RefineGUI.ITEM_SLOT) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Inventory top = player.getOpenInventory().getTopInventory();
                ItemStack placed = top.getItem(RefineGUI.ITEM_SLOT);
                if (placed != null && placed.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                    top.setItem(RefineGUI.ITEM_SLOT, null);
                    placed = null;
                }
                if (placed == null || placed.getType().isAir()) {
                    RefineGUI.showEmpty(top);
                } else {
                    RefineGUI.updateContents(top, placed, player);
                }
            }, 1L);
            return;
        }

        if (slot == RefineGUI.ROLL_BTN && slot < topSize) {
            event.setCancelled(true);
            handleRoll(player, event.getView().getTopInventory());
            return;
        }

        // shift-click from player inventory → item slot
        if (slot >= topSize) {
            if (event.isShiftClick()) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && RefineManager.isRefineable(clicked)) {
                    event.setCancelled(true);
                    Inventory top = event.getView().getTopInventory();
                    ItemStack existing = top.getItem(RefineGUI.ITEM_SLOT);
                    if (existing != null && existing.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                        player.getInventory().setItem(event.getSlot(), existing);
                    } else {
                        player.getInventory().setItem(event.getSlot(), null);
                    }
                    top.setItem(RefineGUI.ITEM_SLOT, clicked.clone());
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        RefineGUI.updateContents(top, top.getItem(RefineGUI.ITEM_SLOT), player), 1L);
                    return;
                }
            }
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!RefineGUI.TITLE.equals(event.getView().getTitle())) return;
        int topSize = event.getView().getTopInventory().getSize();
        for (int s : event.getRawSlots()) {
            if (s < topSize && s != RefineGUI.ITEM_SLOT) { event.setCancelled(true); return; }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!RefineGUI.TITLE.equals(event.getView().getTitle())) return;
        ItemStack left = event.getInventory().getItem(RefineGUI.ITEM_SLOT);
        if (left != null && !left.getType().isAir() && left.getType() != Material.GRAY_STAINED_GLASS_PANE) {
            event.getInventory().setItem(RefineGUI.ITEM_SLOT, null);
            returnItem(player, left);
        }
    }

    private void returnItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        for (ItemStack drop : leftover.values())
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        if (statListener != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                statListener.applyAttributes(player), 1L);
        }
    }

    private void handleRoll(Player player, Inventory top) {
        ItemStack item = top.getItem(RefineGUI.ITEM_SLOT);
        if (item == null || item.getType().isAir() || item.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        if (!RefineManager.isRefineable(item)) return;

        if (!DanmujiItem.consume(player, 1)) {
            player.sendMessage(ChatColor.RED + "단무지가 부족합니다!");
            return;
        }

        String result = RefineManager.roll(plugin, item);
        top.setItem(RefineGUI.ITEM_SLOT, item);

        if ("CEILING_UP".equals(result)) {
            PotentialGrade g = RefineManager.getGrade(item);
            player.sendMessage(ChatColor.GOLD + "[세공] ✦ 천장 달성! 등급 업그레이드: " + (g != null ? g.color + g.displayName : ""));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        } else {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "[세공] 잠재능력이 재롤링되었습니다.");
            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
        }

        RefineGUI.updateContents(top, item, player);
    }
}
