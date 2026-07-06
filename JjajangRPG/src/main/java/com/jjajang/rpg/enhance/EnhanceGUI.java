package com.jjajang.rpg.enhance;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class EnhanceGUI {

    public static final String TITLE = ChatColor.DARK_RED + "⚒ 장비 강화";
    public static final int ITEM_SLOT    = 11;
    public static final int INFO_SLOT    = 13;
    public static final int RATE_SLOT    = 15;
    public static final int ENHANCE_BTN  = 22;
    public static final int RESTORE_BTN  = 22;

    public static Inventory build(Player player, ItemStack equipped) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // 테두리
        ItemStack border = border();
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // 아이템 슬롯 (빈 칸)
        inv.setItem(ITEM_SLOT, makeSlotPlaceholder());

        // 아이템이 있을 경우
        if (equipped != null && EnhanceManager.isEnhanceable(equipped)) {
            updateContents(inv, equipped, player);
        } else {
            inv.setItem(INFO_SLOT, makeInfo("장비를 슬롯에 넣으세요", List.of(
                ChatColor.GRAY + "강화할 무기/방어구를",
                ChatColor.GRAY + "왼쪽 슬롯에 배치하세요."
            )));
            inv.setItem(ENHANCE_BTN, makeBtn(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "장비 없음", List.of()));
        }

        return inv;
    }

    public static void updateContents(Inventory inv, ItemStack item, Player player) {
        boolean destroyed = EnhanceManager.isDestroyed(item);

        if (destroyed) {
            // 복원 모드
            String sourceMat = item.getItemMeta().getPersistentDataContainer()
                    .get(EnhanceManager.KEY_SOURCE_MAT, org.bukkit.persistence.PersistentDataType.STRING);
            inv.setItem(INFO_SLOT, makeInfo(ChatColor.RED + "☠ 파괴된 장비", List.of(
                ChatColor.GRAY + "재료: " + ChatColor.YELLOW + "같은 종류 장비 1개",
                ChatColor.GRAY + "복원 후: " + ChatColor.GREEN + "10성에서 시작",
                ChatColor.GRAY + "소지 여부: " + (hasSourceItem(player, sourceMat) ? ChatColor.GREEN + "보유 중" : ChatColor.RED + "없음")
            )));
            inv.setItem(RATE_SLOT, border());
            boolean canRestore = hasSourceItem(player, sourceMat);
            inv.setItem(RESTORE_BTN, makeBtn(
                canRestore ? Material.EMERALD : Material.BARRIER,
                canRestore ? ChatColor.GREEN + "▶ 복원" : ChatColor.RED + "✗ 재료 부족",
                canRestore ? List.of(ChatColor.GRAY + "클릭하여 10성으로 복원합니다.") : List.of(ChatColor.GRAY + "같은 종류 장비가 필요합니다.")
            ));
        } else {
            int stars = EnhanceManager.getStars(item);
            long cost  = EnhanceManager.getCost(stars);
            double[] rates = EnhanceManager.getRates(stars);
            boolean maxed = stars >= 20;

            // 정보
            List<String> infoLore = new ArrayList<>();
            infoLore.add(ChatColor.YELLOW + "현재: " + ChatColor.GOLD + stars + "성");
            infoLore.add(ChatColor.GRAY + "비용: " + ChatColor.WHITE + cost + "G");
            if (EnhanceManager.isWeapon(item)) {
                infoLore.add(ChatColor.GRAY + "현재 공격력 보너스: +" + EnhanceManager.getAttackBonus(stars));
                if (!maxed) infoLore.add(ChatColor.GRAY + "강화 시: +" + EnhanceManager.getAttackBonus(stars + 1));
            } else {
                infoLore.add(ChatColor.GRAY + "현재 방어력 보너스: +" + EnhanceManager.getDefenseBonus(stars));
                if (!maxed) infoLore.add(ChatColor.GRAY + "강화 시: +" + EnhanceManager.getDefenseBonus(stars + 1));
            }
            inv.setItem(INFO_SLOT, makeInfo(ChatColor.GOLD + "★ 강화 정보", infoLore));

            // 확률
            List<String> rateLore = new ArrayList<>();
            rateLore.add(ChatColor.GREEN + "성공: " + rates[0] + "%");
            rateLore.add(ChatColor.YELLOW + "실패: " + rates[1] + "%" + (stars >= 10 ? " (1성 하락)" : ""));
            if (rates[2] > 0) rateLore.add(ChatColor.RED + "파괴: " + rates[2] + "%");
            inv.setItem(RATE_SLOT, makeInfo(ChatColor.WHITE + "강화 확률", rateLore));

            // 강화 버튼
            if (maxed) {
                inv.setItem(ENHANCE_BTN, makeBtn(Material.GOLD_BLOCK, ChatColor.GOLD + "✦ 최대 강화!", List.of(ChatColor.GRAY + "이미 20성입니다.")));
            } else {
                inv.setItem(ENHANCE_BTN, makeBtn(Material.ANVIL, ChatColor.GREEN + "▶ 강화 (" + cost + "G)",
                    List.of(ChatColor.GRAY + "클릭하여 강화를 시도합니다.")));
            }
        }
    }

    private static boolean hasSourceItem(Player player, String matName) {
        if (matName == null) return false;
        Material mat;
        try { mat = Material.valueOf(matName); } catch (IllegalArgumentException e) { return false; }
        for (ItemStack s : player.getInventory().getContents()) {
            if (s != null && s.getType() == mat && !EnhanceManager.isDestroyed(s)) return true;
        }
        return false;
    }

    private static ItemStack border() {
        ItemStack i = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = i.getItemMeta(); m.setDisplayName(" "); i.setItemMeta(m); return i;
    }

    private static ItemStack makeSlotPlaceholder() {
        ItemStack i = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.GRAY + "[ 장비 슬롯 ]");
        m.setLore(List.of(ChatColor.DARK_GRAY + "강화할 장비를 여기에 넣으세요"));
        i.setItemMeta(m); return i;
    }

    private static ItemStack makeInfo(String name, List<String> lore) {
        ItemStack i = new ItemStack(Material.BOOK);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name); m.setLore(lore); i.setItemMeta(m); return i;
    }

    private static ItemStack makeBtn(Material mat, String name, List<String> lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name); m.setLore(lore); i.setItemMeta(m); return i;
    }
}
