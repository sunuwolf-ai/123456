package com.jjajang.rpg.pass;

import com.jjajang.rpg.cash.CashManager;
import com.jjajang.rpg.event.EventManager;
import com.jjajang.rpg.event.EventMenuListener;
import com.jjajang.rpg.gold.GoldManager;
import com.jjajang.rpg.gold.GoldScoreboard;
import com.jjajang.rpg.item.BoarHornItem;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PassMenuListener implements Listener {

    public static final String TITLE = ChatColor.GOLD + "✦ 짜장패스: 프리시즌 ✦";

    // 54-슬롯 레이아웃: 티어 1~10
    // 각 티어: [무료 슬롯] [정보 슬롯] [프리미엄 슬롯]
    // 티어 두 개씩 한 행에, 중간 구분선 포함
    private static final int[] FREE_SLOTS = {9,  13, 18, 22, 27, 31, 36, 40, 45, 49};
    private static final int[] INFO_SLOTS = {10, 14, 19, 23, 28, 32, 37, 41, 46, 50};
    private static final int[] PREM_SLOTS = {11, 15, 20, 24, 29, 33, 38, 42, 47, 51};

    // 티어별 무료 보상 설명
    private static final String[][] FREE_DESC = {
        {"단무지 ×3", "빵 ×16"},
        {"황금사과 ×5"},
        {"엔더 진주 ×8"},
        {"다이아몬드 ×5"},
        {"경험치 병 ×5"},
        {"금 블록 ×5"},
        {"힘의 물약 II ×3"},
        {"다이아몬드 블록 ×2"},
        {"황금사과 ×10"},
        {"상혁이 조각 ×3"}
    };
    // 티어별 무료 보상 대표 아이콘
    private static final Material[] FREE_MATS = {
        Material.PAPER, Material.GOLDEN_APPLE, Material.ENDER_PEARL,
        Material.DIAMOND, Material.EXPERIENCE_BOTTLE, Material.GOLD_BLOCK,
        Material.POTION, Material.DIAMOND_BLOCK, Material.GOLDEN_APPLE,
        Material.NETHER_STAR
    };

    // 티어별 프리미엄 보상 설명
    private static final String[][] PREM_DESC = {
        {"캐시 1,000C", "단무지 ×10"},
        {"캐시 2,000C", "멧돼지 뿔 ×20"},
        {"캐시 3,000C", "황금사과 ×30"},
        {"캐시 5,000C", "불사의 토템"},
        {"캐시 7,000C", "힘의 물약 II ×10"},
        {"캐시 5,000C", "겉날개"},
        {"캐시 10,000C", "다이아몬드 블록 ×5"},
        {"캐시 10,000C", "브리즈 막대기"},
        {"캐시 15,000C", "무거운 코어"},
        {"캐시 20,000C", "마법의 황금사과 ×3"}
    };
    private static final Material[] PREM_MATS = {
        Material.EMERALD, Material.BONE, Material.GOLDEN_APPLE,
        Material.TOTEM_OF_UNDYING, Material.POTION, Material.ELYTRA,
        Material.DIAMOND_BLOCK, Material.BREEZE_ROD, Material.HEAVY_CORE,
        Material.ENCHANTED_GOLDEN_APPLE
    };

    private final EventManager em;
    private final CashManager cm;
    private final GoldManager gm;
    private final GoldScoreboard sb;

    public PassMenuListener(EventManager em, CashManager cm, GoldManager gm, GoldScoreboard sb) {
        this.em = em; this.cm = cm; this.gm = gm; this.sb = sb;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        UUID id = player.getUniqueId();
        int kills = em.getBossKills(id);
        boolean premium = em.hasPremiumPass(id);

        // ── 헤더 (row 0) ──────────────────────────────────────────
        inv.setItem(0, makeItem(Material.NETHER_STAR,
                ChatColor.GOLD + "짜장패스: 프리시즌",
                List.of(ChatColor.GRAY + "기간: 정식 오픈 전까지",
                        ChatColor.YELLOW + "내 상혁이 처치: " + kills + "회",
                        ChatColor.AQUA + "진행도로 무료/프리미엄 보상 획득!")));
        inv.setItem(4, makeItem(Material.BARRIER,
                ChatColor.RED + "[ 무료 ] ← 왼쪽  /  오른쪽 → [ 프리미엄 ]",
                List.of(ChatColor.GREEN + "무료 패스: 누구나 수령 가능",
                        ChatColor.GOLD + "프리미엄 패스: " + String.format("%,d", EventManager.PASS_PRICE) + "C",
                        ChatColor.GRAY + "멧돼지 김상혁 처치 횟수로 진행!")));
        if (premium) {
            inv.setItem(8, makeItem(Material.GOLD_BLOCK,
                    ChatColor.GOLD + "✦ 프리미엄 패스 보유 ✦",
                    List.of(ChatColor.YELLOW + "모든 프리미엄 보상을 수령하세요!")));
        } else {
            ItemStack buyBtn = makeItem(Material.EMERALD,
                    ChatColor.AQUA + "프리미엄 패스 구매",
                    List.of(ChatColor.GRAY + "가격: " + String.format("%,d", EventManager.PASS_PRICE) + "C",
                            ChatColor.AQUA + "보유 캐시: " + cm.get(id) + "C",
                            ChatColor.GREEN + "▶ 클릭하여 구매"));
            addGlint(buyBtn);
            inv.setItem(8, buyBtn);
        }

        // ── 티어 슬롯 ─────────────────────────────────────────────
        for (int tier = 1; tier <= 10; tier++) {
            int fi = tier - 1;
            boolean unlocked = em.isTierUnlocked(id, tier);
            int required = EventManager.PASS_KILLS[fi];

            // 무료 보상 슬롯
            boolean freeClaimed = em.isPassFreeClaimed(id, tier);
            List<String> freeLore = new ArrayList<>();
            freeLore.add(ChatColor.GRAY + "필요 처치: " + required + "회");
            for (String d : FREE_DESC[fi]) freeLore.add(ChatColor.GREEN + d);
            if (freeClaimed) {
                freeLore.add(ChatColor.DARK_GRAY + "✓ 수령 완료");
                inv.setItem(FREE_SLOTS[fi], makeItem(Material.GRAY_DYE,
                        ChatColor.GRAY + "[무료] 티어 " + tier + " ✓", freeLore));
            } else if (unlocked) {
                freeLore.add(ChatColor.GREEN + "▶ 클릭하여 수령!");
                ItemStack it = makeItem(FREE_MATS[fi], ChatColor.GREEN + "[무료] 티어 " + tier, freeLore);
                addGlint(it); inv.setItem(FREE_SLOTS[fi], it);
            } else {
                freeLore.add(ChatColor.RED + "🔒 " + (required - kills) + "회 더 필요");
                inv.setItem(FREE_SLOTS[fi], makeItem(FREE_MATS[fi],
                        ChatColor.RED + "[무료] 티어 " + tier + " 🔒", freeLore));
            }

            // 정보 슬롯 (중앙)
            inv.setItem(INFO_SLOTS[fi], makeItem(Material.BLACK_STAINED_GLASS_PANE,
                    ChatColor.YELLOW + "─ 티어 " + tier + " ─",
                    List.of(ChatColor.GRAY + "필요: " + required + "회 처치",
                            unlocked ? ChatColor.GREEN + "✓ 달성!" : ChatColor.RED + "" + kills + "/" + required)));

            // 프리미엄 보상 슬롯
            boolean premClaimed = em.isPassPremClaimed(id, tier);
            List<String> premLore = new ArrayList<>();
            premLore.add(ChatColor.GRAY + "필요 처치: " + required + "회");
            for (String d : PREM_DESC[fi]) premLore.add(ChatColor.GOLD + d);
            if (premClaimed) {
                premLore.add(ChatColor.DARK_GRAY + "✓ 수령 완료");
                inv.setItem(PREM_SLOTS[fi], makeItem(Material.GRAY_DYE,
                        ChatColor.GRAY + "[프리미엄] 티어 " + tier + " ✓", premLore));
            } else if (!premium) {
                premLore.add(ChatColor.DARK_GRAY + "프리미엄 패스 필요");
                inv.setItem(PREM_SLOTS[fi], makeItem(PREM_MATS[fi],
                        ChatColor.DARK_GRAY + "[프리미엄] 티어 " + tier + " 🔒", premLore));
            } else if (unlocked) {
                premLore.add(ChatColor.GOLD + "▶ 클릭하여 수령!");
                ItemStack it = makeItem(PREM_MATS[fi],
                        ChatColor.GOLD + "[프리미엄] 티어 " + tier, premLore);
                addGlint(it); inv.setItem(PREM_SLOTS[fi], it);
            } else {
                premLore.add(ChatColor.RED + "🔒 " + (required - kills) + "회 더 필요");
                inv.setItem(PREM_SLOTS[fi], makeItem(PREM_MATS[fi],
                        ChatColor.RED + "[프리미엄] 티어 " + tier + " 🔒", premLore));
            }
        }

        fillBorder(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onPassClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();
        int slot = event.getSlot();
        UUID id = player.getUniqueId();

        // 프리미엄 구매 버튼
        if (slot == 8 && name.equals(ChatColor.AQUA + "프리미엄 패스 구매")) {
            if (em.hasPremiumPass(id)) { player.sendMessage(ChatColor.RED + "[패스] 이미 프리미엄입니다."); return; }
            if (!cm.spend(id, EventManager.PASS_PRICE)) {
                player.sendMessage(ChatColor.RED + "[패스] 캐시 부족! (필요: "
                        + String.format("%,d", EventManager.PASS_PRICE) + "C, 보유: " + cm.get(id) + "C)");
                return;
            }
            em.purchasePremiumPass(id);
            player.sendMessage(ChatColor.GOLD + "[짜장패스] 프리미엄 패스 구매 완료! 모든 프리미엄 보상을 수령하세요!");
            player.sendActionBar(Component.text("★ 짜장패스 프리미엄 활성화!").color(NamedTextColor.GOLD));
            player.closeInventory(); open(player); return;
        }

        // 무료 보상 클릭
        for (int i = 0; i < FREE_SLOTS.length; i++) {
            if (slot == FREE_SLOTS[i]) {
                int tier = i + 1;
                if (!name.contains("[무료] 티어 " + tier) || name.contains("🔒") || name.contains("✓")) return;
                if (em.isPassFreeClaimed(id, tier)) { player.sendMessage(ChatColor.RED + "[패스] 이미 수령했습니다."); return; }
                if (!em.isTierUnlocked(id, tier)) { player.sendMessage(ChatColor.RED + "[패스] 조건 미달!"); return; }
                em.claimPassFree(id, tier);
                giveFreeReward(player, tier);
                player.closeInventory(); open(player); return;
            }
        }

        // 프리미엄 보상 클릭
        for (int i = 0; i < PREM_SLOTS.length; i++) {
            if (slot == PREM_SLOTS[i]) {
                int tier = i + 1;
                if (!name.contains("[프리미엄] 티어 " + tier) || name.contains("🔒") || name.contains("✓")) return;
                if (!em.hasPremiumPass(id)) { player.sendMessage(ChatColor.RED + "[패스] 프리미엄 패스가 필요합니다."); return; }
                if (em.isPassPremClaimed(id, tier)) { player.sendMessage(ChatColor.RED + "[패스] 이미 수령했습니다."); return; }
                if (!em.isTierUnlocked(id, tier)) { player.sendMessage(ChatColor.RED + "[패스] 조건 미달!"); return; }
                em.claimPassPrem(id, tier);
                givePremReward(player, tier);
                player.closeInventory(); open(player); return;
            }
        }
    }

    // ── 무료 보상 지급 ────────────────────────────────────────────
    private void giveFreeReward(Player p, int tier) {
        switch (tier) {
            case 1  -> { give(p, new ItemStack(Material.PAPER, 3)); give(p, new ItemStack(Material.BREAD, 16)); }
            case 2  -> give(p, new ItemStack(Material.GOLDEN_APPLE, 5));
            case 3  -> give(p, new ItemStack(Material.ENDER_PEARL, 8));
            case 4  -> give(p, new ItemStack(Material.DIAMOND, 5));
            case 5  -> give(p, new ItemStack(Material.EXPERIENCE_BOTTLE, 5));
            case 6  -> give(p, new ItemStack(Material.GOLD_BLOCK, 5));
            case 7  -> { for (int i = 0; i < 3; i++) give(p, EventMenuListener.makeStrengthPotion()); }
            case 8  -> give(p, new ItemStack(Material.DIAMOND_BLOCK, 2));
            case 9  -> give(p, new ItemStack(Material.GOLDEN_APPLE, 10));
            case 10 -> give(p, new ItemStack(Material.NETHER_STAR, 3));
        }
        p.sendMessage(ChatColor.GREEN + "[짜장패스] 티어 " + tier + " 무료 보상 수령!");
        p.sendActionBar(Component.text("패스 티어 " + tier + " 무료 보상!").color(NamedTextColor.GREEN));
    }

    // ── 프리미엄 보상 지급 ────────────────────────────────────────
    private void givePremReward(Player p, int tier) {
        UUID id = p.getUniqueId();
        switch (tier) {
            case 1  -> { cm.add(id, 1_000); give(p, new ItemStack(Material.PAPER, 10)); }
            case 2  -> { cm.add(id, 2_000); give(p, BoarHornItem.create(20)); }
            case 3  -> { cm.add(id, 3_000); give(p, new ItemStack(Material.GOLDEN_APPLE, 30)); }
            case 4  -> { cm.add(id, 5_000); give(p, new ItemStack(Material.TOTEM_OF_UNDYING, 1)); }
            case 5  -> { cm.add(id, 7_000); for (int i = 0; i < 10; i++) give(p, EventMenuListener.makeStrengthPotion()); }
            case 6  -> { cm.add(id, 5_000); give(p, new ItemStack(Material.ELYTRA, 1)); }
            case 7  -> { cm.add(id, 10_000); give(p, new ItemStack(Material.DIAMOND_BLOCK, 5)); }
            case 8  -> { cm.add(id, 10_000); give(p, new ItemStack(Material.BREEZE_ROD, 1)); }
            case 9  -> { cm.add(id, 15_000); give(p, new ItemStack(Material.HEAVY_CORE, 1)); }
            case 10 -> { cm.add(id, 20_000); give(p, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3)); }
        }
        p.sendMessage(ChatColor.GOLD + "[짜장패스] 티어 " + tier + " 프리미엄 보상 수령!");
        p.sendActionBar(Component.text("패스 티어 " + tier + " 프리미엄 보상!").color(NamedTextColor.GOLD));
    }

    // ── 유틸 ──────────────────────────────────────────────────────
    private static void give(Player p, ItemStack item) {
        Map<Integer, ItemStack> overflow = p.getInventory().addItem(item);
        overflow.values().forEach(l -> p.getWorld().dropItemNaturally(p.getLocation(), l));
    }

    private static ItemStack makeItem(Material mat, String name, List<String> lore) {
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
