package com.jjajang.rpg.shop;

import com.jjajang.rpg.gold.GoldManager;
import com.jjajang.rpg.gold.GoldScoreboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public class ShopMenuListener implements Listener {

    public static final String MAIN_TITLE = ChatColor.GREEN + "✦ 상점 ✦";
    private static final String SELL_TITLE = ChatColor.YELLOW + "✦ 판매상점 ✦";
    private static final String BUY_TITLE  = ChatColor.AQUA  + "✦ 구매상점 ✦";
    private static final String NETHER_TP_NAME = ChatColor.RED + "네더TP권";

    private final JavaPlugin plugin;
    private final GoldManager gm;
    private final GoldScoreboard sb;

    // 판매 가격표 (아이템 → 골드 per 1개)
    private static final Map<Material, Long> SELL_PRICES = Map.ofEntries(
        Map.entry(Material.COAL,           5L),
        Map.entry(Material.IRON_INGOT,    15L),
        Map.entry(Material.GOLD_INGOT,    30L),
        Map.entry(Material.DIAMOND,      200L),
        Map.entry(Material.EMERALD,      150L),
        Map.entry(Material.NETHERITE_INGOT, 2000L),
        Map.entry(Material.LAPIS_LAZULI,   8L),
        Map.entry(Material.REDSTONE,       5L),
        Map.entry(Material.WHEAT,          3L),
        Map.entry(Material.CARROT,         3L),
        Map.entry(Material.POTATO,         3L),
        Map.entry(Material.BEETROOT,       4L),
        Map.entry(Material.SUGAR_CANE,     2L),
        Map.entry(Material.MELON_SLICE,    2L),
        Map.entry(Material.PUMPKIN,        5L),
        Map.entry(Material.COCOA_BEANS,    4L)
    );

    public ShopMenuListener(JavaPlugin plugin, GoldManager gm, GoldScoreboard sb) {
        this.plugin = plugin;
        this.gm = gm;
        this.sb = sb;
    }

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MAIN_TITLE);

        inv.setItem(11, makeItem(Material.GOLD_NUGGET, ChatColor.YELLOW + "판매상점",
                List.of(ChatColor.GRAY + "광물, 농작물을 팔아 골드를 얻습니다.",
                        ChatColor.GREEN + "클릭하여 입장")));

        inv.setItem(15, makeItem(Material.EMERALD, ChatColor.AQUA + "구매상점",
                List.of(ChatColor.GRAY + "골드로 아이템을 구매합니다.",
                        ChatColor.GREEN + "클릭하여 입장")));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }
        player.openInventory(inv);
    }

    private void openSellShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, SELL_TITLE);

        int slot = 0;
        for (Map.Entry<Material, Long> entry : SELL_PRICES.entrySet()) {
            if (slot >= 45) break;
            Material mat = entry.getKey();
            long price = entry.getValue();
            ItemStack item = makeItem(mat, ChatColor.WHITE + prettyName(mat),
                    List.of(ChatColor.GRAY + "판매가: " + ChatColor.GOLD + price + "G / 1개",
                            ChatColor.YELLOW + "클릭: 인벤 전량 판매"));
            inv.setItem(slot++, item);
        }

        // 하단 빈칸 채우기
        ItemStack border = makeBorder();
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        player.openInventory(inv);
    }

    private void openBuyShop(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, BUY_TITLE);

        inv.setItem(10, makeItem(Material.NETHER_PORTAL,
                NETHER_TP_NAME,
                List.of(ChatColor.GRAY + "가장 가까운 네더로 이동합니다.",
                        ChatColor.GOLD + "가격: 2,009G",
                        ChatColor.YELLOW + "클릭하여 구매")));

        inv.setItem(11, makeItem(Material.IRON_BLOCK,
                ChatColor.GRAY + "무거운 코어",
                List.of(ChatColor.GRAY + "강력한 강화 재료입니다.",
                        ChatColor.GOLD + "가격: 52,300G",
                        ChatColor.YELLOW + "클릭하여 구매")));

        inv.setItem(12, makeItem(Material.ARROW,
                ChatColor.WHITE + "일반 화살",
                List.of(ChatColor.GRAY + "1개당 구매합니다.",
                        ChatColor.GOLD + "가격: 300G / 1개",
                        ChatColor.YELLOW + "클릭하여 구매")));

        inv.setItem(13, makeItem(Material.GOLDEN_APPLE,
                ChatColor.GOLD + "황금사과",
                List.of(ChatColor.GRAY + "회복에 도움이 됩니다.",
                        ChatColor.GOLD + "가격: 5,230G",
                        ChatColor.YELLOW + "클릭하여 구매")));

        inv.setItem(14, makeItem(Material.BREAD,
                ChatColor.YELLOW + "빵 64개",
                List.of(ChatColor.GRAY + "64개 묶음으로 판매합니다.",
                        ChatColor.GOLD + "가격: 518G",
                        ChatColor.YELLOW + "클릭하여 구매")));

        ItemStack border = makeBorder();
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!MAIN_TITLE.equals(title) && !SELL_TITLE.equals(title) && !BUY_TITLE.equals(title)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();

        if (MAIN_TITLE.equals(title)) {
            if (name.equals(ChatColor.YELLOW + "판매상점")) { player.closeInventory(); openSellShop(player); }
            else if (name.equals(ChatColor.AQUA + "구매상점")) { player.closeInventory(); openBuyShop(player); }

        } else if (SELL_TITLE.equals(title)) {
            Material mat = clicked.getType();
            Long price = SELL_PRICES.get(mat);
            if (price == null) return;

            // 인벤에서 해당 아이템 전량 판매
            int count = 0;
            for (ItemStack inv : player.getInventory().getContents()) {
                if (inv != null && inv.getType() == mat) count += inv.getAmount();
            }
            if (count == 0) { player.sendMessage(ChatColor.RED + "판매할 아이템이 없습니다."); return; }

            player.getInventory().remove(mat);
            long earned = price * count;
            gm.add(player.getUniqueId(), earned);
            sb.update(player);
            player.sendMessage(ChatColor.YELLOW + prettyName(mat) + " " + count + "개 판매 → +" + earned + "G");
            player.sendActionBar(Component.text("+" + earned + "G").color(NamedTextColor.GOLD));
            player.closeInventory();
            openSellShop(player);

        } else if (BUY_TITLE.equals(title)) {
            handleBuy(player, name);
        }
    }

    private void handleBuy(Player player, String name) {
        long balance = gm.get(player.getUniqueId());

        if (name.equals(NETHER_TP_NAME)) {
            if (!gm.spend(player.getUniqueId(), 2009)) {
                player.sendMessage(ChatColor.RED + "골드가 부족합니다. (필요: 2,009G)");
                return;
            }
            sb.update(player);
            player.getInventory().addItem(makeNetherTpItem());
            player.sendMessage(ChatColor.RED + "[구매] 네더TP권 구매 완료! (-2,009G)");

        } else if (name.equals(ChatColor.GRAY + "무거운 코어")) {
            if (!gm.spend(player.getUniqueId(), 52300)) {
                player.sendMessage(ChatColor.RED + "골드가 부족합니다. (필요: 52,300G)");
                return;
            }
            sb.update(player);
            ItemStack core = new ItemStack(Material.IRON_BLOCK);
            ItemMeta m = core.getItemMeta();
            m.setDisplayName(ChatColor.GRAY + "무거운 코어");
            m.setLore(List.of(ChatColor.DARK_GRAY + "강화 재료"));
            core.setItemMeta(m);
            player.getInventory().addItem(core);
            player.sendMessage(ChatColor.GRAY + "[구매] 무거운 코어 구매 완료! (-52,300G)");

        } else if (name.equals(ChatColor.WHITE + "일반 화살")) {
            if (!gm.spend(player.getUniqueId(), 300)) {
                player.sendMessage(ChatColor.RED + "골드가 부족합니다. (필요: 300G)");
                return;
            }
            sb.update(player);
            player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
            player.sendMessage(ChatColor.WHITE + "[구매] 일반 화살 1개 구매 완료! (-300G)");

        } else if (name.equals(ChatColor.GOLD + "황금사과")) {
            if (!gm.spend(player.getUniqueId(), 5230)) {
                player.sendMessage(ChatColor.RED + "골드가 부족합니다. (필요: 5,230G)");
                return;
            }
            sb.update(player);
            player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
            player.sendMessage(ChatColor.GOLD + "[구매] 황금사과 구매 완료! (-5,230G)");

        } else if (name.equals(ChatColor.YELLOW + "빵 64개")) {
            if (!gm.spend(player.getUniqueId(), 518)) {
                player.sendMessage(ChatColor.RED + "골드가 부족합니다. (필요: 518G)");
                return;
            }
            sb.update(player);
            player.getInventory().addItem(new ItemStack(Material.BREAD, 64));
            player.sendMessage(ChatColor.YELLOW + "[구매] 빵 64개 구매 완료! (-518G)");
        }
    }

    // 네더TP권 아이템
    public static ItemStack makeNetherTpItem() {
        ItemStack item = new ItemStack(Material.MAGMA_CREAM);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "네더TP권");
        meta.setLore(List.of(ChatColor.GRAY + "우클릭 시 네더로 이동합니다."));
        item.setItemMeta(meta);
        return item;
    }

    // 네더TP권 사용 처리
    @EventHandler
    public void onUseNetherTp(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;
        if (!(ChatColor.RED + "네더TP권").equals(item.getItemMeta().getDisplayName())) return;

        event.setCancelled(true);
        World nether = Bukkit.getWorld("world_nether");
        if (nether == null) { player.sendMessage(ChatColor.RED + "네더 월드를 찾을 수 없습니다."); return; }

        // Overworld → Nether 좌표 변환 (÷8)
        double nx = player.getLocation().getX() / 8.0;
        double ny = Math.max(10, Math.min(player.getLocation().getY(), 115));
        double nz = player.getLocation().getZ() / 8.0;

        player.teleport(new org.bukkit.Location(nether, nx, ny, nz,
                player.getLocation().getYaw(), player.getLocation().getPitch()));
        player.sendMessage(ChatColor.RED + "[이동] 네더로 이동했습니다!");

        // 아이템 1개 소모
        if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
        else player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
    }

    private static String prettyName(Material mat) {
        return mat.name().replace('_', ' ');
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
