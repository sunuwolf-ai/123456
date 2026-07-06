package com.jjajang.rpg.classes;

import com.jjajang.rpg.archer.ArcherItems;
import com.jjajang.rpg.jjajjang.JjajjangItems;
import com.jjajang.rpg.mage.MageItems;
import com.jjajang.rpg.rogue.RogueItems;
import com.jjajang.rpg.util.WeaponUtils;
import com.jjajang.rpg.warrior.WarriorItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ClassMenuListener implements Listener {

    private final JavaPlugin plugin;

    public ClassMenuListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!ClassCommand.MENU_TITLE.equals(event.getView().getTitle())) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();

        if (name.equals(ChatColor.RED + "⚔ 전사")) {
            player.closeInventory();
            selectClass(player, PlayerClass.WARRIOR_1,
                    ChatColor.RED + "⚔ 전사 1차 클래스를 선택했습니다!",
                    WarriorItems.createT1(plugin), "전사의 석검");
        } else if (name.equals(ChatColor.GREEN + "🏹 궁수")) {
            player.closeInventory();
            selectClass(player, PlayerClass.ARCHER_1,
                    ChatColor.GREEN + "🏹 궁수 1차 클래스를 선택했습니다!",
                    ArcherItems.createT1(plugin), "사냥꾼의 활");
            // 일반 화살 2세트(64×2 = 128개) 지급
            player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
            player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
            player.sendMessage(ChatColor.YELLOW + "  [아이템] 화살 128개 지급됨");
        } else if (name.equals(ChatColor.DARK_PURPLE + "🗡 도적")) {
            player.closeInventory();
            selectClass(player, PlayerClass.ROGUE_1,
                    ChatColor.DARK_PURPLE + "🗡 도적 1차 클래스를 선택했습니다!",
                    RogueItems.createT1(plugin), "암살자의 단검");
        } else if (name.equals(ChatColor.AQUA + "📖 마법사")) {
            player.closeInventory();
            selectClass(player, PlayerClass.MAGE_1,
                    ChatColor.AQUA + "📖 마법사 1차 클래스를 선택했습니다!",
                    MageItems.createT1(plugin), "견습 마법서");
        } else if (name.equals(ChatColor.GOLD + "🥣 짜짱")) {
            player.closeInventory();
            selectClass(player, PlayerClass.JJAJJANG_1,
                    ChatColor.GOLD + "🥣 짜짱 1차 클래스를 선택했습니다!",
                    JjajjangItems.createT1(plugin), "짜장 그릇");
        } else if (name.equals(ChatColor.GRAY + "✖ 클래스 초기화")) {
            player.closeInventory();
            resetClass(player);
        }
    }

    private void selectClass(Player player, PlayerClass cls, String msg, ItemStack weapon, String weaponName) {
        ClassManager.setClass(player, cls);
        player.sendMessage("");
        player.sendMessage("  " + msg);
        player.sendMessage(ChatColor.YELLOW + "  2차 전직: /전직 입력");
        player.sendMessage("");
        player.getInventory().addItem(weapon);
        player.sendMessage(ChatColor.GOLD + "  [아이템] " + weaponName + " 지급됨");
        player.sendMessage("");
    }

    private void resetClass(Player player) {
        PlayerClass current = ClassManager.getClass(player);
        if (current == PlayerClass.NONE) {
            player.sendMessage(ChatColor.GRAY + "[짜장RPG] 선택된 클래스가 없습니다.");
            return;
        }
        WeaponUtils.removeWeaponOfClass(player, current.getWeaponClass(), plugin);
        ClassManager.setClass(player, PlayerClass.NONE);
        player.sendMessage(ChatColor.GRAY + "[짜장RPG] 클래스가 초기화되었습니다. (직업 무기 삭제됨)");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        String weaponClass = ClassManager.getClass(player).getWeaponClass();
        if (weaponClass == null) return;

        // 드랍 목록에서 제거 (keepInventory 꺼져있는 경우 바닥에 떨어지지 않게)
        event.getDrops().removeIf(item -> weaponClass.equals(WeaponUtils.getWeaponClass(item, plugin)));
        // 인벤토리에서도 제거 (keepInventory 켜져있는 경우 대비)
        WeaponUtils.removeWeaponOfClass(player, weaponClass, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // 5틱 후 PDC에서 클래스 복원 + 패시브 적용 (HP 버그 수정)
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            ClassManager.loadAndApply(event.getPlayer()), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ClassManager.remove(event.getPlayer().getUniqueId());
    }
}
