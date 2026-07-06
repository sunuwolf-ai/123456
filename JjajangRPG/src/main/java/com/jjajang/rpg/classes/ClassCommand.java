package com.jjajang.rpg.classes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ClassCommand implements CommandExecutor {

    public static final String MENU_TITLE = ChatColor.DARK_RED + "✦ 클래스 선택 ✦";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
        openClassMenu(player);
        return true;
    }

    public static void openClassMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, MENU_TITLE);

        menu.setItem(11, makeItem(Material.IRON_SWORD, ChatColor.RED + "⚔ 전사",
                List.of(
                        ChatColor.GRAY + "묵직한 검으로 적을 제압하는 근거리 전사.",
                        "",
                        ChatColor.YELLOW + "[패시브] " + ChatColor.WHITE + "강인한 체력 +20",
                        ChatColor.YELLOW + "[1차] " + ChatColor.WHITE + "전방 베기",
                        ChatColor.YELLOW + "[2차] " + ChatColor.WHITE + "검찌르기 → 회전베기",
                        ChatColor.YELLOW + "[3차] " + ChatColor.WHITE + "불굴의 의지 (풀피+기절)",
                        "",
                        ChatColor.GREEN + "▶ 클릭하여 선택"
                )));

        menu.setItem(13, makeItem(Material.BOW, ChatColor.GREEN + "🏹 궁수",
                List.of(
                        ChatColor.GRAY + "원거리에서 정밀하게 적을 포착한다.",
                        "",
                        ChatColor.YELLOW + "[1차] " + ChatColor.WHITE + "도약 속사",
                        ChatColor.YELLOW + "[2차] " + ChatColor.WHITE + "속성 화살 순환 (빙결/화염/독/번개)",
                        ChatColor.YELLOW + "[3차] " + ChatColor.WHITE + "화살 비",
                        "",
                        ChatColor.GREEN + "▶ 클릭하여 선택"
                )));

        menu.setItem(15, makeItem(Material.WOODEN_SWORD, ChatColor.DARK_PURPLE + "🗡 도적",
                List.of(
                        ChatColor.GRAY + "어둠 속에서 적을 기습하는 암살자.",
                        "",
                        ChatColor.YELLOW + "[패시브] " + ChatColor.WHITE + "기습 (뒤에서 공격 시 2배)",
                        ChatColor.YELLOW + "[1차] " + ChatColor.WHITE + "표창 투척",
                        ChatColor.YELLOW + "[2차] " + ChatColor.WHITE + "출혈 기습 + 은신",
                        ChatColor.YELLOW + "[3차] " + ChatColor.WHITE + "연막 + 이속↑ + 공속↑",
                        "",
                        ChatColor.GREEN + "▶ 클릭하여 선택"
                )));

        menu.setItem(17, makeItem(Material.BOOK, ChatColor.AQUA + "📖 마법사",
                List.of(
                        ChatColor.GRAY + "마법서로 원거리 마법 피해를 가하는 술사.",
                        "",
                        ChatColor.YELLOW + "[패시브] " + ChatColor.WHITE + "처치 시 쿨타임 1.5초 감소",
                        ChatColor.YELLOW + "[좌클릭] " + ChatColor.WHITE + "에너지볼 (기본공격)",
                        ChatColor.YELLOW + "[1차] " + ChatColor.WHITE + "파이어볼",
                        ChatColor.YELLOW + "[2차] " + ChatColor.WHITE + "연쇄 에너지볼 (최대 5회 전이)",
                        ChatColor.YELLOW + "[3차] " + ChatColor.WHITE + "멸절의 광선",
                        "",
                        ChatColor.GREEN + "▶ 클릭하여 선택"
                )));

        menu.setItem(19, makeItem(Material.BOWL, ChatColor.GOLD + "🥣 짜짱",
                List.of(
                        ChatColor.GRAY + "짜장 그릇으로 적을 요리하는 괴짜 술사.",
                        "",
                        ChatColor.YELLOW + "[패시브] " + ChatColor.WHITE + "스킬 명중 시 그릇 드랍, 회수하면 회복",
                        ChatColor.YELLOW + "[좌클릭] " + ChatColor.WHITE + "에너지볼 (기본공격)",
                        ChatColor.YELLOW + "[1차] " + ChatColor.WHITE + "짜장소환",
                        ChatColor.YELLOW + "[2차] " + ChatColor.WHITE + "짜장면 투척 (춘장 지속피해)",
                        ChatColor.YELLOW + "[3차] " + ChatColor.WHITE + "준우준우화 (광역 독+이속감소, 갑옷 탈락)",
                        "",
                        ChatColor.GREEN + "▶ 클릭하여 선택"
                )));

        menu.setItem(22, makeItem(Material.BARRIER, ChatColor.GRAY + "✖ 클래스 초기화",
                List.of(ChatColor.GRAY + "현재 클래스를 해제합니다.")));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) {
            if (menu.getItem(i) == null) menu.setItem(i, border);
        }
        player.openInventory(menu);
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
