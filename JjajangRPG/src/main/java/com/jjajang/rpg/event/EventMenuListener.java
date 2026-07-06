package com.jjajang.rpg.event;

import com.jjajang.rpg.cash.CashManager;
import com.jjajang.rpg.gold.GoldManager;
import com.jjajang.rpg.gold.GoldScoreboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventMenuListener implements Listener {

    public static final String TITLE_SELECT    = ChatColor.LIGHT_PURPLE + "✦ 이벤트 ✦";
    public static final String TITLE_ATTEND    = ChatColor.GREEN        + "✦ 캐시 출석 이벤트 ✦";
    public static final String TITLE_RAID      = ChatColor.RED          + "✦ 상혁이 레이드 이벤트 ✦";

    private final EventManager em;
    private final GoldManager gm;
    private final GoldScoreboard sb;
    private final CashManager cm;

    public EventMenuListener(EventManager em, GoldManager gm, GoldScoreboard sb, CashManager cm) {
        this.em = em; this.gm = gm; this.sb = sb; this.cm = cm;
    }

    // ── 이벤트 선택 창 ────────────────────────────────────────────
    public static void openSelect(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_SELECT);
        inv.setItem(11, makeItem(Material.CLOCK,
                ChatColor.GREEN + "캐시 출석 이벤트",
                List.of(ChatColor.GRAY + "매일 1회 접속 보상을 받으세요!",
                        ChatColor.YELLOW + "최대 Day7: 5,000C",
                        ChatColor.GREEN + "▶ 클릭하여 입장")));
        inv.setItem(15, makeItem(Material.HOGLIN_SPAWN_EGG,
                ChatColor.RED + "상혁이 레이드 이벤트",
                List.of(ChatColor.GRAY + "멧돼지 김상혁을 처치하고 보상을!",
                        ChatColor.RED + "2회마다 마일스톤 보상 지급",
                        ChatColor.GREEN + "▶ 클릭하여 입장")));
        fillBorder(inv);
        player.openInventory(inv);
    }

    // ── 출석 이벤트 창 ────────────────────────────────────────────
    public void openAttendance(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_ATTEND);
        UUID id = player.getUniqueId();

        // 헤더
        inv.setItem(4, makeItem(Material.CLOCK,
                ChatColor.GREEN + "✦ 캐시 출석 이벤트 ✦",
                List.of(ChatColor.GRAY + "매일 1회 캐시를 수령하세요!",
                        ChatColor.AQUA + "보유 캐시: " + cm.get(id) + "C")));

        // 7일 슬롯: slots 10-16
        int lastDay = em.getLastClaimedDay(id);     // 0-7
        boolean canClaim = em.canClaimAttendance(id);
        int nextDay = canClaim ? (lastDay % 7) + 1 : -1;

        long[] rewards = EventManager.ATTENDANCE_REWARDS;
        for (int d = 1; d <= 7; d++) {
            int slot = 9 + d; // slots 10-16
            if (!canClaim && d <= lastDay) {
                // 오늘 포함 이미 수령한 날
                inv.setItem(slot, makeItem(Material.GRAY_DYE,
                        ChatColor.GRAY + "Day " + d + " ✓",
                        List.of(ChatColor.DARK_GRAY + String.format("%,d", rewards[d-1]) + "C",
                                ChatColor.DARK_GRAY + "수령 완료")));
            } else if (d == nextDay) {
                // 오늘 수령 가능
                ItemStack item = makeItem(Material.GOLD_INGOT,
                        ChatColor.GOLD + "Day " + d + " ★ 오늘!",
                        List.of(ChatColor.YELLOW + "+" + String.format("%,d", rewards[d-1]) + "C",
                                ChatColor.GREEN + "▶ 클릭하여 수령!"));
                addGlint(item);
                inv.setItem(slot, item);
            } else if (canClaim && d < nextDay) {
                // 이전 사이클에서 수령된 날 (새 사이클 시작)
                inv.setItem(slot, makeItem(Material.GRAY_DYE,
                        ChatColor.GRAY + "Day " + d + " ✓",
                        List.of(ChatColor.DARK_GRAY + String.format("%,d", rewards[d-1]) + "C",
                                ChatColor.DARK_GRAY + "수령 완료 (이전 사이클)")));
            } else {
                // 아직 잠김
                inv.setItem(slot, makeItem(Material.RED_STAINED_GLASS_PANE,
                        ChatColor.RED + "Day " + d + " 🔒",
                        List.of(ChatColor.GRAY + String.format("%,d", rewards[d-1]) + "C",
                                ChatColor.DARK_GRAY + "아직 잠김")));
            }
        }

        fillBorder(inv);
        player.openInventory(inv);
    }

    // ── 레이드 이벤트 창 ──────────────────────────────────────────
    public void openRaid(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_RAID);
        UUID id = player.getUniqueId();
        int kills = em.getBossKills(id);

        // 헤더
        inv.setItem(4, makeItem(Material.HOGLIN_SPAWN_EGG,
                ChatColor.RED + "✦ 상혁이 레이드 이벤트 ✦",
                List.of(ChatColor.GRAY + "멧돼지 김상혁 처치 보상",
                        ChatColor.YELLOW + "내 처치 횟수: " + ChatColor.WHITE + kills + "회",
                        ChatColor.GRAY + "2회마다 마일스톤 보상!")));

        // 마일스톤 슬롯: 10, 11, 12, 13, 14
        String[][] raidDesc = {
            {"황금사과 ×3", "골드 50,000G"},
            {"엔더 진주 ×5", "골드 100,000G"},
            {"힘의 물약 II ×2", "골드 200,000G"},
            {"마법의 황금사과 ×2", "골드 500,000G"},
            {"캐시 3,000C", "골드 1,000,000G"}
        };
        Material[] raidMats = {
            Material.GOLDEN_APPLE,
            Material.ENDER_PEARL,
            Material.POTION,
            Material.ENCHANTED_GOLDEN_APPLE,
            Material.EMERALD
        };

        int[] milestones = EventManager.RAID_MILESTONES;
        for (int i = 0; i < milestones.length; i++) {
            int milestone = milestones[i];
            int slot = 10 + i;
            boolean achieved = kills >= milestone;
            boolean claimed  = em.isRaidClaimed(id, milestone);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "필요 처치: " + ChatColor.WHITE + milestone + "회");
            for (String d : raidDesc[i]) lore.add(ChatColor.YELLOW + d);

            String ms = String.valueOf(milestone);
            if (claimed) {
                lore.add(ChatColor.DARK_GRAY + "✓ 수령 완료");
                inv.setItem(slot, makeItem(Material.GRAY_DYE,
                        ChatColor.GRAY + ms + "회 처치 보상 ✓", lore));
            } else if (achieved) {
                lore.add(ChatColor.GREEN + "▶ 클릭하여 수령!");
                ItemStack it = makeItem(raidMats[i],
                        ChatColor.GREEN + ms + "회 처치 보상!", lore);
                addGlint(it);
                inv.setItem(slot, it);
            } else {
                lore.add(ChatColor.RED + "🔒 " + (milestone - kills) + "회 더 필요");
                inv.setItem(slot, makeItem(raidMats[i],
                        ChatColor.RED + ms + "회 처치 보상 🔒", lore));
            }
        }

        fillBorder(inv);
        player.openInventory(inv);
    }

    // ── 클릭 핸들러 ───────────────────────────────────────────────
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!TITLE_SELECT.equals(title) && !TITLE_ATTEND.equals(title) && !TITLE_RAID.equals(title)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();

        if (TITLE_SELECT.equals(title)) {
            if (name.equals(ChatColor.GREEN + "캐시 출석 이벤트")) {
                player.closeInventory(); openAttendance(player);
            } else if (name.equals(ChatColor.RED + "상혁이 레이드 이벤트")) {
                player.closeInventory(); openRaid(player);
            }
            return;
        }

        if (TITLE_ATTEND.equals(title)) {
            if (!name.contains("오늘")) return;
            // "Day X ★ 오늘!" 클릭
            if (!em.canClaimAttendance(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "[출석] 오늘 이미 수령했습니다.");
                return;
            }
            long reward = em.claimAttendance(player.getUniqueId());
            cm.add(player.getUniqueId(), reward);
            int day = em.getLastClaimedDay(player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "[출석 이벤트] Day " + day + " 보상: +"
                    + String.format("%,d", reward) + "C 지급! (보유: " + cm.get(player.getUniqueId()) + "C)");
            player.sendActionBar(Component.text("+" + String.format("%,d", reward) + "C 출석 보상!")
                    .color(NamedTextColor.GREEN));
            player.closeInventory();
            openAttendance(player);
            return;
        }

        if (TITLE_RAID.equals(title)) {
            if (!name.contains("보상!")) return; // only claimable items have "!"
            handleRaidClaim(player, event.getSlot());
        }
    }

    private void handleRaidClaim(Player player, int slot) {
        int idx = slot - 10;
        if (idx < 0 || idx >= EventManager.RAID_MILESTONES.length) return;
        int milestone = EventManager.RAID_MILESTONES[idx];
        UUID id = player.getUniqueId();

        if (em.isRaidClaimed(id, milestone)) {
            player.sendMessage(ChatColor.RED + "[레이드] 이미 수령한 보상입니다.");
            return;
        }
        if (em.getBossKills(id) < milestone) {
            player.sendMessage(ChatColor.RED + "[레이드] 처치 횟수가 부족합니다. (" + em.getBossKills(id) + "/" + milestone + ")");
            return;
        }

        em.claimRaid(id, milestone);
        giveRaidReward(player, milestone);
        player.closeInventory();
        openRaid(player);
    }

    private void giveRaidReward(Player player, int milestone) {
        switch (milestone) {
            case 2 -> {
                giveOrDrop(player, new ItemStack(Material.GOLDEN_APPLE, 3));
                gm.add(player.getUniqueId(), 50_000L); sb.update(player);
                msg(player, "[레이드 " + milestone + "회] 황금사과 ×3 + 50,000G 지급!");
            }
            case 4 -> {
                giveOrDrop(player, new ItemStack(Material.ENDER_PEARL, 5));
                gm.add(player.getUniqueId(), 100_000L); sb.update(player);
                msg(player, "[레이드 " + milestone + "회] 엔더 진주 ×5 + 100,000G 지급!");
            }
            case 6 -> {
                for (int i = 0; i < 2; i++) giveOrDrop(player, makeStrengthPotion());
                gm.add(player.getUniqueId(), 200_000L); sb.update(player);
                msg(player, "[레이드 " + milestone + "회] 힘의 물약 II ×2 + 200,000G 지급!");
            }
            case 8 -> {
                giveOrDrop(player, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2));
                gm.add(player.getUniqueId(), 500_000L); sb.update(player);
                msg(player, "[레이드 " + milestone + "회] 마법의 황금사과 ×2 + 500,000G 지급!");
            }
            case 10 -> {
                cm.add(player.getUniqueId(), 3_000L);
                gm.add(player.getUniqueId(), 1_000_000L); sb.update(player);
                msg(player, "[레이드 " + milestone + "회] 3,000C + 1,000,000G 지급!");
            }
        }
        player.sendActionBar(Component.text("레이드 보상 수령!").color(NamedTextColor.RED));
    }

    // ── 유틸 ──────────────────────────────────────────────────────
    public static ItemStack makeStrengthPotion() {
        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 9600, 1), true);
        meta.setDisplayName(ChatColor.RED + "힘의 물약 II (8분)");
        item.setItemMeta(meta);
        return item;
    }

    private static void giveOrDrop(Player p, ItemStack item) {
        p.getInventory().addItem(item).values()
                .forEach(l -> p.getWorld().dropItemNaturally(p.getLocation(), l));
    }

    private static void msg(Player p, String text) {
        p.sendMessage(ChatColor.YELLOW + text);
    }

    public static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void addGlint(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private static void fillBorder(Inventory inv) {
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }
    }
}
