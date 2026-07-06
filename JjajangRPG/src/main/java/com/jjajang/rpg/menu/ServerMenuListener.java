package com.jjajang.rpg.menu;

import com.jjajang.rpg.cash.CashManager;
import com.jjajang.rpg.cash.CashShopMenuListener;
import com.jjajang.rpg.event.EventManager;
import com.jjajang.rpg.event.EventMenuListener;
import com.jjajang.rpg.pass.PassMenuListener;
import com.jjajang.rpg.classes.AdvancementCommand;
import com.jjajang.rpg.classes.ClassCommand;
import com.jjajang.rpg.enhance.EnhanceListener;
import com.jjajang.rpg.gold.GoldCommand;
import com.jjajang.rpg.quest.QuestManager;
import com.jjajang.rpg.quest.QuestMenuListener;
import com.jjajang.rpg.refine.DanmujiItem;
import com.jjajang.rpg.refine.RefineListener;
import com.jjajang.rpg.reward.RewardManager;
import com.jjajang.rpg.reward.RewardMenuListener;
import com.jjajang.rpg.shop.ShopMenuListener;
import com.jjajang.rpg.warp.WarpMenuListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ServerMenuListener implements Listener, CommandExecutor {

    private static final String MENU_TITLE = ChatColor.DARK_AQUA + "✦ 짜장RPG 메뉴 ✦";

    private final JavaPlugin plugin;
    private final AdvancementCommand advancementCommand;
    private final GoldCommand goldCommand;
    private final QuestManager questManager;
    private final RewardManager rewardManager;
    private final CashManager cashManager;
    private EventMenuListener eventMenuListener;
    private PassMenuListener passMenuListener;

    private final Set<UUID> sneaking = new HashSet<>();

    public ServerMenuListener(JavaPlugin plugin, AdvancementCommand advancementCommand,
                               GoldCommand goldCommand, QuestManager questManager,
                               RewardManager rewardManager, CashManager cashManager) {
        this.plugin = plugin;
        this.advancementCommand = advancementCommand;
        this.goldCommand = goldCommand;
        this.questManager = questManager;
        this.rewardManager = rewardManager;
        this.cashManager = cashManager;
    }

    public void setEventMenuListener(EventMenuListener l) { this.eventMenuListener = l; }
    public void setPassMenuListener(PassMenuListener l)   { this.passMenuListener = l; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSneak(PlayerToggleSneakEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (event.isSneaking()) sneaking.add(id);
        else sneaking.remove(id);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!sneaking.contains(player.getUniqueId())) return;
        event.setCancelled(true);
        openServerMenu(player);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용 가능합니다.");
            return true;
        }
        openServerMenu(player);
        return true;
    }

    public void openServerMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, MENU_TITLE);

        // 12 items: slots 0-11 (left-to-right, top row then second row)
        menu.setItem(0,  makeItem(Material.BEACON,     ChatColor.AQUA        + "워프",       "어디로 워프할까요?"));
        menu.setItem(1,  makeItem(Material.IRON_SWORD, ChatColor.RED         + "클래스",     "클래스 선택 창을 엽니다."));
        menu.setItem(2,  makeSteve(                    ChatColor.GOLD        + "전직",        "다음 단계로 전직합니다."));
        menu.setItem(3,  makeItem(Material.PAPER,      ChatColor.YELLOW      + "퀘스트",     "퀘스트 목록을 확인합니다."));
        menu.setItem(4,  makeItem(Material.EMERALD,    ChatColor.GREEN       + "상점",        "판매/구매 상점을 이용합니다."));
        menu.setItem(5,  makeItem(Material.ANVIL,      ChatColor.DARK_RED    + "강화",        "장비를 강화합니다."));
        menu.setItem(6,  makeItem(Material.GRINDSTONE, ChatColor.LIGHT_PURPLE+ "세공",        "잠재능력을 세공합니다."));
        menu.setItem(7,  makeItem(Material.BUNDLE,     ChatColor.GOLD        + "보상",        "받을 수 있는 보상을 확인합니다."));
        menu.setItem(8,  makeItem(Material.GOLD_INGOT, ChatColor.YELLOW      + "골드지급",   "골드 지갑을 엽니다."));
        menu.setItem(9,  makeItem(Material.YELLOW_WOOL,ChatColor.YELLOW      + "단무지지급", "단무지를 지급받습니다."));
        menu.setItem(10, makeItem(Material.DIAMOND,    ChatColor.LIGHT_PURPLE+ "캐시상점",   "캐시로 아이템을 구매합니다."));
        menu.setItem(11, makeItem(Material.BROWN_WOOL, ChatColor.DARK_RED    + "짜장면처먹기","짜장면을 처먹어 보상을 받습니다."));
        menu.setItem(12, makeItem(Material.CLOCK,      ChatColor.GREEN       + "이벤트",      "진행 중인 이벤트를 확인합니다."));
        menu.setItem(13, makeItem(Material.NETHER_STAR,ChatColor.GOLD        + "짜장패스",    "짜장패스: 프리시즌 보상을 확인합니다."));
        menu.setItem(14, makeItem(Material.COOKED_BEEF,ChatColor.DARK_AQUA   + "명품 레스토랑","채팅으로 링크를 보내드립니다."));

        ItemStack border = makeBorder();
        for (int i = 15; i < 27; i++) menu.setItem(i, border);

        player.openInventory(menu);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!MENU_TITLE.equals(event.getView().getTitle())) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();

        if (name.equals(ChatColor.AQUA + "워프")) {
            player.closeInventory(); WarpMenuListener.open(player);
        } else if (name.equals(ChatColor.RED + "클래스")) {
            player.closeInventory(); ClassCommand.openClassMenu(player);
        } else if (name.equals(ChatColor.GOLD + "전직")) {
            player.closeInventory(); advancementCommand.advance(player);
        } else if (name.equals(ChatColor.YELLOW + "퀘스트")) {
            player.closeInventory(); QuestMenuListener.open(player, questManager);
        } else if (name.equals(ChatColor.GREEN + "상점")) {
            player.closeInventory(); ShopMenuListener.open(player);
        } else if (name.equals(ChatColor.DARK_RED + "강화")) {
            player.closeInventory(); EnhanceListener.open(player);
        } else if (name.equals(ChatColor.LIGHT_PURPLE + "세공")) {
            player.closeInventory(); RefineListener.open(player);
        } else if (name.equals(ChatColor.GOLD + "보상")) {
            player.closeInventory(); RewardMenuListener.open(player, rewardManager);
        } else if (name.equals(ChatColor.YELLOW + "골드지급")) {
            player.closeInventory(); goldCommand.openGUI(player);
        } else if (name.equals(ChatColor.YELLOW + "단무지지급")) {
            player.closeInventory(); openDanmujiMenu(player);
        } else if (name.equals(ChatColor.LIGHT_PURPLE + "캐시상점")) {
            player.closeInventory(); CashShopMenuListener.open(player, cashManager);
        } else if (name.equals(ChatColor.DARK_RED + "짜장면처먹기")) {
            player.closeInventory(); QuestMenuListener.open(player, questManager);
        } else if (name.equals(ChatColor.GREEN + "이벤트")) {
            player.closeInventory(); EventMenuListener.openSelect(player);
        } else if (name.equals(ChatColor.GOLD + "짜장패스")) {
            player.closeInventory(); if (passMenuListener != null) passMenuListener.open(player);
        } else if (name.equals(ChatColor.DARK_AQUA + "명품 레스토랑")) {
            player.closeInventory();
            player.sendMessage(net.kyori.adventure.text.Component.text("🔗 명품 레스토랑 영상 보기")
                    .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                    .decorate(net.kyori.adventure.text.format.TextDecoration.UNDERLINED)
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(
                            "https://youtu.be/HaE1CSlFtXY?si=pBEgF9hYub45kkdI")));
        }
    }

    private void openDanmujiMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, ChatColor.YELLOW + "✦ 단무지 지급 ✦");

        inv.setItem(13, makeItem(Material.YELLOW_WOOL,
                ChatColor.YELLOW + "단무지 1개 받기",
                "클릭하여 단무지를 받습니다."));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onDanmujiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(ChatColor.YELLOW + "✦ 단무지 지급 ✦").equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        if ((ChatColor.YELLOW + "단무지 1개 받기").equals(clicked.getItemMeta().getDisplayName())) {
            player.getInventory().addItem(DanmujiItem.create(1));
            player.sendMessage(ChatColor.YELLOW + "단무지 1개를 지급받았습니다.");
        }
    }

    private ItemStack makeItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack makeSteve(String name, String lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer("MHF_Steve"));
        meta.setDisplayName(name);
        meta.setLore(List.of(lore));
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
