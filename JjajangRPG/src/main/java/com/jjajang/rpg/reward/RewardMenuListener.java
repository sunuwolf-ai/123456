package com.jjajang.rpg.reward;

import com.jjajang.rpg.cash.CashManager;
import com.jjajang.rpg.item.PlatinumJjajangItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class RewardMenuListener implements Listener {

    public static final String TITLE = ChatColor.GOLD + "✦ 보상함 ✦";

    private static final int SLOT_JJAJJANG_LAUNCH = 13; // 짜장 출시기념 보상

    private final RewardManager rm;
    private final CashManager cm;

    public RewardMenuListener(RewardManager rm, CashManager cm) {
        this.rm = rm;
        this.cm = cm;
    }

    public static void open(Player player, RewardManager rm) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // ── 짜장 출시기념 보상 ───────────────────────────────────────
        boolean claimed = rm.hasClaimed(player.getUniqueId(), RewardManager.REWARD_JJAJJANG_LAUNCH);
        if (claimed) {
            inv.setItem(SLOT_JJAJJANG_LAUNCH, makeItem(Material.GRAY_DYE,
                    ChatColor.GRAY + "[수령완료] 짜장 출시기념 보상",
                    List.of(ChatColor.DARK_GRAY + "이미 수령한 보상입니다.")));
        } else {
            inv.setItem(SLOT_JJAJJANG_LAUNCH, makeItem(Material.BROWN_WOOL,
                    ChatColor.GOLD + "[미수령] 짜장 출시기념 보상",
                    List.of(ChatColor.GRAY + "클릭하여 보상을 수령하세요.",
                            ChatColor.GOLD + "보상: 1,000,000 캐시",
                            ChatColor.LIGHT_PURPLE + "     플래티넘 짜장 ×128",
                            ChatColor.GREEN + "▶ 1회 한정 수령 가능")));
        }

        // 테두리
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

        if (name.equals(ChatColor.GOLD + "[미수령] 짜장 출시기념 보상")) {
            if (rm.hasClaimed(player.getUniqueId(), RewardManager.REWARD_JJAJJANG_LAUNCH)) {
                player.sendMessage(ChatColor.RED + "[보상] 이미 수령한 보상입니다.");
                return;
            }
            rm.claim(player.getUniqueId(), RewardManager.REWARD_JJAJJANG_LAUNCH);

            cm.add(player.getUniqueId(), 1_000_000L);
            giveOrDrop(player, PlatinumJjajangItem.create(64));
            giveOrDrop(player, PlatinumJjajangItem.create(64));

            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "★ " + ChatColor.BOLD + "짜장 출시기념 보상 수령 완료!" + ChatColor.RESET);
            player.sendMessage(ChatColor.GOLD + "  + 1,000,000 캐시");
            player.sendMessage(ChatColor.LIGHT_PURPLE + "  + 플래티넘 짜장 ×128");
            player.sendMessage("");
            player.sendActionBar(Component.text("★ 짜장 출시기념 보상 수령!").color(NamedTextColor.GOLD));
            player.closeInventory();
            open(player, rm);
        }
    }

    private static void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
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
