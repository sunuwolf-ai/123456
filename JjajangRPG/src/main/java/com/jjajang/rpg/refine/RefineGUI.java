package com.jjajang.rpg.refine;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RefineGUI {

    public static final String TITLE     = ChatColor.DARK_PURPLE + "✦ 세공";
    public static final int ITEM_SLOT    = 11;
    public static final int GRADE_SLOT   = 13;
    public static final int CEIL_SLOT    = 15;
    public static final int LINE0_SLOT   = 20;
    public static final int LINE1_SLOT   = 21;
    public static final int LINE2_SLOT   = 22;
    public static final int DAN_SLOT     = 24;
    public static final int ROLL_BTN     = 31;

    public static Inventory build(Player player, ItemStack equipped) {
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        ItemStack border = border();
        for (int i = 0; i < 45; i++) inv.setItem(i, border);

        inv.setItem(ITEM_SLOT, makePlaceholder());

        if (equipped != null && RefineManager.isRefineable(equipped)) {
            updateContents(inv, equipped, player);
        } else {
            inv.setItem(GRADE_SLOT, makeInfo(ChatColor.GRAY + "장비 없음", List.of(ChatColor.GRAY + "세공할 장비를 슬롯에 넣으세요.")));
            inv.setItem(ROLL_BTN, makeBtn(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "장비 없음", List.of()));
        }

        return inv;
    }

    public static void updateContents(Inventory inv, ItemStack item, Player player) {
        PotentialGrade grade = RefineManager.getGrade(item);
        int rolls = RefineManager.getRolls(item);
        int danCount = DanmujiItem.count(player);
        String[] lines = RefineManager.getLines(item);

        // 등급 표시
        String gradeName = grade == null ? "없음" : grade.color + "[" + grade.displayName + "]";
        List<String> gradeLore = new ArrayList<>();
        gradeLore.add(ChatColor.GRAY + "현재 등급: " + gradeName);
        if (grade != null && !grade.isMax()) gradeLore.add(ChatColor.GRAY + "다음 등급: " + grade.next().color + grade.next().displayName);
        inv.setItem(GRADE_SLOT, makeInfo(ChatColor.LIGHT_PURPLE + "잠재능력 등급", gradeLore));

        // 천장 표시
        int remaining = 50 - rolls;
        List<String> ceilLore = new ArrayList<>();
        ceilLore.add(ChatColor.GRAY + "현재 세공 횟수: " + ChatColor.WHITE + rolls + "/50");
        if (grade != null && !grade.isMax()) {
            ceilLore.add(ChatColor.GRAY + "등급 업까지: " + ChatColor.YELLOW + remaining + "회");
        } else if (grade != null) {
            ceilLore.add(ChatColor.LIGHT_PURPLE + "최고 등급입니다!");
        }
        inv.setItem(CEIL_SLOT, makeInfo(ChatColor.YELLOW + "천장 게이지", ceilLore));

        // 잠재능력 라인들
        PotentialOption[] opts = {null, null, null};
        int[] vals = {0, 0, 0};
        for (int i = 0; i < 3; i++) {
            if (lines[i] != null) {
                opts[i] = PotentialOption.fromKey(lines[i]);
                vals[i] = PotentialOption.valueFromKey(lines[i]);
            }
        }

        int[] lineSlots = {LINE0_SLOT, LINE1_SLOT, LINE2_SLOT};
        for (int i = 0; i < 3; i++) {
            if (opts[i] != null) {
                String valStr = opts[i].percent ? "+" + vals[i] + "%" : (opts[i] == PotentialOption.LEVEL_REDUCE ? "-" + vals[i] : "+" + vals[i]);
                inv.setItem(lineSlots[i], makeInfo(
                    ChatColor.YELLOW + opts[i].displayName + " " + ChatColor.GREEN + valStr,
                    List.of(ChatColor.GRAY + "잠재능력 라인 " + (i + 1))
                ));
            } else {
                inv.setItem(lineSlots[i], makeInfo(ChatColor.GRAY + "─ 미공개 ─", List.of(ChatColor.GRAY + "세공을 통해 해방하세요.")));
            }
        }

        // 단무지 정보
        inv.setItem(DAN_SLOT, makeInfo(ChatColor.YELLOW + "단무지",
            List.of(ChatColor.GRAY + "보유: " + ChatColor.WHITE + danCount + "개",
                    ChatColor.GRAY + "세공 1회당 1개 소모")));

        // 세공 버튼
        boolean canRoll = danCount >= 1;
        inv.setItem(ROLL_BTN, makeBtn(
            canRoll ? Material.GOLDEN_CARROT : Material.BARRIER,
            canRoll ? ChatColor.LIGHT_PURPLE + "▶ 세공 (단무지 ×1)" : ChatColor.RED + "✗ 단무지 부족",
            canRoll
                ? List.of(ChatColor.GRAY + "클릭하여 잠재능력을 재롤링합니다.",
                          (grade == null ? ChatColor.YELLOW + "첫 세공: 에픽 등급 시작" : ChatColor.GRAY + "현재: " + gradeName))
                : List.of(ChatColor.GRAY + "단무지가 필요합니다.")
        ));
    }

    private static ItemStack border() {
        ItemStack i = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta m = i.getItemMeta(); m.setDisplayName(" "); i.setItemMeta(m); return i;
    }

    public static void showEmpty(Inventory inv) {
        inv.setItem(ITEM_SLOT, makePlaceholder());
        inv.setItem(GRADE_SLOT, makeInfo(ChatColor.GRAY + "장비 없음", List.of(ChatColor.GRAY + "세공할 장비를 슬롯에 넣으세요.")));
        inv.setItem(CEIL_SLOT,  makeInfo(ChatColor.GRAY + "─", List.of()));
        inv.setItem(LINE0_SLOT, makeInfo(ChatColor.GRAY + "─ 미공개 ─", List.of(ChatColor.GRAY + "세공을 통해 해방하세요.")));
        inv.setItem(LINE1_SLOT, makeInfo(ChatColor.GRAY + "─ 미공개 ─", List.of(ChatColor.GRAY + "세공을 통해 해방하세요.")));
        inv.setItem(LINE2_SLOT, makeInfo(ChatColor.GRAY + "─ 미공개 ─", List.of(ChatColor.GRAY + "세공을 통해 해방하세요.")));
        inv.setItem(DAN_SLOT,   makeInfo(ChatColor.GRAY + "─", List.of()));
        inv.setItem(ROLL_BTN,   makeBtn(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "장비 없음", List.of()));
    }

    public static ItemStack makePlaceholder() {
        ItemStack i = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(ChatColor.GRAY + "[ 장비 슬롯 ]");
        m.setLore(List.of(ChatColor.DARK_GRAY + "세공할 장비를 여기에 넣으세요"));
        i.setItemMeta(m); return i;
    }

    private static ItemStack makeInfo(String name, List<String> lore) {
        ItemStack i = new ItemStack(Material.PAPER);
        ItemMeta m = i.getItemMeta(); m.setDisplayName(name); m.setLore(lore); i.setItemMeta(m); return i;
    }

    private static ItemStack makeBtn(Material mat, String name, List<String> lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta(); m.setDisplayName(name); m.setLore(lore); i.setItemMeta(m); return i;
    }
}
