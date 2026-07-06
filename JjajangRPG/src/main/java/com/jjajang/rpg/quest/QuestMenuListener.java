package com.jjajang.rpg.quest;

import com.jjajang.rpg.gold.GoldManager;
import com.jjajang.rpg.gold.GoldScoreboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class QuestMenuListener implements Listener {

    public static final String TITLE = ChatColor.DARK_PURPLE + "✦ 퀘스트 ✦";
    private static final String JJAJANG_NAME = ChatColor.DARK_RED + "짜장면";

    private final JavaPlugin plugin;
    private final QuestManager qm;
    private final GoldManager gm;
    private final GoldScoreboard sb;

    public QuestMenuListener(JavaPlugin plugin, QuestManager qm, GoldManager gm, GoldScoreboard sb) {
        this.plugin = plugin;
        this.qm = qm;
        this.gm = gm;
        this.sb = sb;
    }

    public static void open(Player player, QuestManager qm) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // 반복 퀘스트: 짜장면처먹기
        List<String> repeatLore = new ArrayList<>();
        repeatLore.add(ChatColor.YELLOW + "[반복 퀘스트]");
        repeatLore.add(ChatColor.GRAY + "짜장면을 먹어보세요!");
        repeatLore.add("");
        repeatLore.add(ChatColor.GOLD + "보상: 523G");
        repeatLore.add(ChatColor.GREEN + "▶ 클릭하여 짜장면 받기");
        inv.setItem(11, makeItem(Material.BROWN_WOOL, ChatColor.DARK_RED + "짜장면처먹기!", repeatLore));

        // 일일 퀘스트: 허스크 5마리
        boolean doneToday = qm.isHuskDoneToday(player.getUniqueId());
        int kills = qm.getHuskKills(player.getUniqueId());
        List<String> dailyLore = new ArrayList<>();
        dailyLore.add(ChatColor.AQUA + "[일일 퀘스트]");
        dailyLore.add(ChatColor.GRAY + "허스크를 5마리 처치하세요.");
        dailyLore.add("");
        if (doneToday) {
            dailyLore.add(ChatColor.GREEN + "✔ 오늘 완료!");
            dailyLore.add(ChatColor.GRAY + "초기화까지: " + ChatColor.YELLOW + qm.timeUntilReset());
        } else {
            dailyLore.add(ChatColor.YELLOW + "진행도: " + kills + " / " + QuestManager.HUSK_GOAL);
            dailyLore.add(ChatColor.GOLD + "보상: 우유 양동이");
            dailyLore.add(ChatColor.GRAY + "초기화까지: " + ChatColor.YELLOW + qm.timeUntilReset());
        }
        inv.setItem(15, makeItem(Material.ZOMBIE_HEAD, ChatColor.YELLOW + "허스크 5마리 잡기", dailyLore));

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

        if (name.equals(ChatColor.DARK_RED + "짜장면처먹기!")) {
            player.closeInventory();
            // 짜장면 아이템 지급
            player.getInventory().addItem(makeJjajang());
            player.sendMessage(ChatColor.DARK_RED + "[퀘스트] 짜장면을 받았습니다! 먹으면 보상 지급.");
        }
    }

    // 짜장면 먹으면 523골드 보상
    @EventHandler
    public void onEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!JJAJANG_NAME.equals(item.getItemMeta().getDisplayName())) return;

        gm.add(player.getUniqueId(), 523);
        sb.update(player);
        player.sendActionBar(Component.text("짜장면처먹기 완료! +523G").color(NamedTextColor.GOLD));
        player.sendMessage(ChatColor.GOLD + "[퀘스트] 짜장면처먹기 완료! +523G");
    }

    // 허스크 처치 카운트
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.HUSK) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        boolean completed = qm.addHuskKill(killer.getUniqueId());
        if (completed) {
            killer.getInventory().addItem(new ItemStack(Material.MILK_BUCKET));
            killer.sendMessage(ChatColor.AQUA + "[일일퀘스트] 허스크 5마리 처치 완료! 우유 양동이 지급!");
            killer.sendActionBar(Component.text("허스크 5마리 처치 완료! 우유 양동이 획득!").color(NamedTextColor.AQUA));
        } else if (!qm.isHuskDoneToday(killer.getUniqueId())) {
            int kills = qm.getHuskKills(killer.getUniqueId());
            killer.sendActionBar(Component.text("허스크 처치: " + kills + "/" + QuestManager.HUSK_GOAL)
                    .color(NamedTextColor.YELLOW));
        }
    }

    public static ItemStack makeJjajang() {
        ItemStack item = new ItemStack(Material.MUSHROOM_STEW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(JJAJANG_NAME);
        meta.setCustomModelData(1002);
        meta.setLore(List.of(ChatColor.GRAY + "짜장RPG 시그니처 짜장면", ChatColor.YELLOW + "먹으면 523G 보상!"));
        item.setItemMeta(meta);
        return item;
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
