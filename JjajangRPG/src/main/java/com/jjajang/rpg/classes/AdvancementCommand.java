package com.jjajang.rpg.classes;

import com.jjajang.rpg.archer.ArcherItems;
import com.jjajang.rpg.jjajjang.JjajjangItems;
import com.jjajang.rpg.mage.MageItems;
import com.jjajang.rpg.rogue.RogueItems;
import com.jjajang.rpg.warrior.WarriorItems;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AdvancementCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public AdvancementCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }
        advance(player);
        return true;
    }

    public void advance(Player player) {
        switch (ClassManager.getClass(player)) {
            case WARRIOR_1 -> advanceTo(player, PlayerClass.WARRIOR_2,
                    "⚔ 전사 2차 전직 완료!", "전사의 검 지급 (검찌르기 + 회전베기)",
                    WarriorItems.createT2(plugin));
            case WARRIOR_2 -> advanceTo(player, PlayerClass.WARRIOR_3,
                    "⚔ 전사 3차 전직 완료!", "전사의 대검 지급 (불굴의 의지)",
                    WarriorItems.createT3(plugin));
            case ARCHER_1 -> advanceTo(player, PlayerClass.ARCHER_2,
                    "🏹 궁수 2차 전직 완료!", "마법사의 활 지급 (속성 화살 순환)",
                    ArcherItems.createT2(plugin));
            case ARCHER_2 -> advanceTo(player, PlayerClass.ARCHER_3,
                    "🏹 궁수 3차 전직 완료!", "신궁의 활 지급 (화살 비)",
                    ArcherItems.createT3(plugin));
            case ROGUE_1 -> advanceTo(player, PlayerClass.ROGUE_2,
                    "🗡 도적 2차 전직 완료!", "혈독 단검 지급 (출혈 기습)",
                    RogueItems.createT2(plugin));
            case ROGUE_2 -> advanceTo(player, PlayerClass.ROGUE_3,
                    "🗡 도적 3차 전직 완료!", "망령의 단검 지급 (연막)",
                    RogueItems.createT3(plugin));
            case MAGE_1 -> advanceTo(player, PlayerClass.MAGE_2,
                    "📖 마법사 2차 전직 완료!", "비전 마법서 지급 (연쇄 에너지볼)",
                    MageItems.createT2(plugin));
            case MAGE_2 -> advanceTo(player, PlayerClass.MAGE_3,
                    "📖 마법사 3차 전직 완료!", "대마법사의 마도서 지급 (멸절의 광선)",
                    MageItems.createT3(plugin));
            case JJAJJANG_1 -> advanceTo(player, PlayerClass.JJAJJANG_2,
                    "🥣 짜짱 2차 전직 완료!", "명품 레스토랑 지급 (짜장면 투척)",
                    JjajjangItems.createT2(plugin));
            case JJAJJANG_2 -> advanceTo(player, PlayerClass.JJAJJANG_3,
                    "🥣 짜짱 3차 전직 완료!", "중국집 코스요리 지급 (준우준우화)",
                    JjajjangItems.createT3(plugin));
            case WARRIOR_3, ARCHER_3, ROGUE_3, MAGE_3, JJAJJANG_3 ->
                    player.sendMessage(ChatColor.YELLOW + "[짜장RPG] 이미 최고 전직 단계입니다.");
            case NONE ->
                    player.sendMessage(ChatColor.RED + "[짜장RPG] 먼저 /클래스 에서 직업을 선택하세요.");
        }
    }

    private void advanceTo(Player player, PlayerClass next, String title, String desc,
                           org.bukkit.inventory.ItemStack weapon) {
        ClassManager.setClass(player, next);
        player.sendMessage("");
        player.sendMessage(next.getColor() + "  " + title);
        player.sendMessage(ChatColor.GRAY + "  " + desc);
        player.sendMessage("");
        player.getInventory().addItem(weapon);
    }
}
