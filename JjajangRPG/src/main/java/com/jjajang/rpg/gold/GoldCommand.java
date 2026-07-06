package com.jjajang.rpg.gold;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GoldCommand implements CommandExecutor, Listener {

    private static final String TITLE = ChatColor.GOLD + "✦ 골드 지갑 ✦";
    private final GoldManager gm;
    private final GoldScoreboard sb;

    public GoldCommand(GoldManager gm, GoldScoreboard sb) {
        this.gm = gm;
        this.sb = sb;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // /골드 초기화 [플레이어]
        if (args.length >= 1 && args[0].equals("초기화")) {
            if (!sender.hasPermission("jjajang.gold.admin") && !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
                return true;
            }
            Player target;
            if (args.length >= 2) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "플레이어를 찾을 수 없습니다."); return true; }
            } else {
                if (!(sender instanceof Player p)) { sender.sendMessage("플레이어 전용 명령어입니다."); return true; }
                target = p;
            }
            gm.set(target.getUniqueId(), 0L);
            sb.update(target);
            sender.sendMessage(ChatColor.YELLOW + target.getName() + "의 골드를 0G로 초기화했습니다.");
            if (!target.equals(sender)) target.sendMessage(ChatColor.YELLOW + "골드가 0G로 초기화되었습니다.");
            return true;
        }

        // /골드 지급 [플레이어] <금액>
        if (args.length >= 1 && args[0].equals("지급")) {
            if (!sender.hasPermission("jjajang.gold.admin") && !sender.isOp()) {
                sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
                return true;
            }
            if (args.length == 2) {
                if (!(sender instanceof Player p)) { sender.sendMessage("플레이어 전용 명령어입니다."); return true; }
                try {
                    long amount = Long.parseLong(args[1]);
                    gm.add(p.getUniqueId(), amount);
                    sb.update(p);
                    sender.sendMessage(ChatColor.YELLOW + "" + amount + "G 지급되었습니다. 현재: " + gm.get(p.getUniqueId()) + "G");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "올바른 숫자를 입력하세요.");
                }
            } else if (args.length == 3) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(ChatColor.RED + "플레이어를 찾을 수 없습니다."); return true; }
                try {
                    long amount = Long.parseLong(args[2]);
                    gm.add(target.getUniqueId(), amount);
                    sb.update(target);
                    sender.sendMessage(ChatColor.YELLOW + target.getName() + "에게 " + amount + "G 지급.");
                    target.sendMessage(ChatColor.YELLOW + "" + amount + "G 지급받았습니다! 현재: " + gm.get(target.getUniqueId()) + "G");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "올바른 숫자를 입력하세요.");
                }
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어 전용 명령어입니다.");
            return true;
        }
        openGUI(player);
        return true;
    }

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        long g = gm.get(player.getUniqueId());

        // 중앙: 잔액 표시
        ItemStack balance = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bm = balance.getItemMeta();
        bm.setDisplayName(ChatColor.YELLOW + "현재 골드: " + ChatColor.WHITE + g + "G");
        bm.setLore(List.of(ChatColor.GRAY + "획득한 골드 잔액입니다."));
        balance.setItemMeta(bm);
        inv.setItem(13, balance);

        // OP 전용 테스트 버튼
        if (player.isOp()) {
            inv.setItem(18, makeBtn(Material.GOLD_NUGGET, ChatColor.YELLOW + "+100G",   "테스트용 골드 지급", 100));
            inv.setItem(19, makeBtn(Material.GOLD_INGOT,  ChatColor.YELLOW + "+1,000G", "테스트용 골드 지급", 1000));
            inv.setItem(20, makeBtn(Material.GOLD_BLOCK,  ChatColor.YELLOW + "+10,000G","테스트용 골드 지급", 10000));
            inv.setItem(21, makeBtn(Material.NETHER_STAR, ChatColor.YELLOW + "+100,000G","테스트용 골드 지급", 100000));
        }

        // 테두리
        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        sb.update(event.getPlayer());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        String name = item.getItemMeta().getDisplayName();

        long amount = 0;
        if (name.contains("+100,000G")) amount = 100000;
        else if (name.contains("+10,000G")) amount = 10000;
        else if (name.contains("+1,000G")) amount = 1000;
        else if (name.contains("+100G")) amount = 100;

        if (amount > 0 && player.isOp()) {
            gm.add(player.getUniqueId(), amount);
            sb.update(player);
            player.sendMessage(ChatColor.YELLOW + "" + amount + "G 지급! 현재: " + gm.get(player.getUniqueId()) + "G");
            openGUI(player); // refresh
        }
    }

    private ItemStack makeBtn(Material mat, String name, String lore, long amount) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(ChatColor.GRAY + lore, ChatColor.DARK_GRAY + "+" + amount + "G"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeBorder() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}
