package com.jjajang.rpg.cash;

import com.jjajang.rpg.item.PlatinumJjajangItem;
import com.jjajang.rpg.item.PoopItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class CashShopMenuListener implements Listener {

    public static final String TITLE = ChatColor.LIGHT_PURPLE + "✦ 캐시상점 ✦";
    private final CashManager cm;

    // 상품 정의: [이름, 재료, 가격, 수량, 설명]
    private record ShopItem(String displayName, Material mat, long price, int amount, String desc) {}

    private static final ShopItem[] ITEMS = {
        new ShopItem("무거운 코어",   Material.HEAVY_CORE,       200_000, 1,  "제련할 수 없는 갑옷을 제조합니다."),
        new ShopItem("겉날개",        Material.ELYTRA,           150_000, 1,  "하늘을 날 수 있습니다."),
        new ShopItem("브리즈 막대기", Material.BREEZE_ROD,       100_000, 1,  "브리즈가 떨어뜨립니다."),
        new ShopItem("불사의 토템",   Material.TOTEM_OF_UNDYING,  70_000, 1,  "죽음을 한 번 막아줍니다."),
        new ShopItem("폭죽 ×64",      Material.FIREWORK_ROCKET,   30_000, 64, "겉날개 비행에 사용합니다."),
        new ShopItem("플래티넘 짜장", Material.PAPER,             50_000, 1,  "랜덤박스! 경험치 쿠폰 또는 골드 획득."),
        new ShopItem("응가",          Material.BROWN_DYE,            500, 1,  "...왜 사는 거지? 먹으면 독+멀미."),
    };

    // 슬롯 배치 (27칸)
    private static final int[] ITEM_SLOTS = {10, 12, 14, 16, 20, 22, 24};

    public CashShopMenuListener(CashManager cm) {
        this.cm = cm;
    }

    public static void open(Player player, CashManager cm) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // 잔액 표시
        long cash = cm.get(player.getUniqueId());
        ItemStack balance = makeItem(Material.DIAMOND,
                ChatColor.LIGHT_PURPLE + "보유 캐시: " + ChatColor.WHITE + String.format("%,d", cash) + "C",
                List.of(ChatColor.GRAY + "캐시는 교환권 또는 이벤트로 획득"));
        inv.setItem(4, balance);

        for (int i = 0; i < ITEMS.length; i++) {
            ShopItem si = ITEMS[i];
            inv.setItem(ITEM_SLOTS[i], makeItem(si.mat(),
                    ChatColor.AQUA + si.displayName(),
                    List.of(ChatColor.GRAY + si.desc(),
                            ChatColor.LIGHT_PURPLE + "가격: " + String.format("%,d", si.price()) + "C",
                            ChatColor.YELLOW + "수량: ×" + si.amount(),
                            ChatColor.GREEN + "▶ 클릭하여 구매")));
        }

        fillBorder(inv);
        player.openInventory(inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();

        for (ShopItem si : ITEMS) {
            if (!name.equals(ChatColor.AQUA + si.displayName())) continue;
            if (!cm.spend(player.getUniqueId(), si.price())) {
                player.sendMessage(ChatColor.RED + "[캐시상점] 캐시가 부족합니다. (필요: "
                        + String.format("%,d", si.price()) + "C, 보유: " + cm.get(player.getUniqueId()) + "C)");
                return;
            }
            ItemStack product = switch (si.displayName()) {
                case "플래티넘 짜장" -> PlatinumJjajangItem.create(si.amount());
                case "응가" -> PoopItem.create(si.amount());
                default -> new ItemStack(si.mat(), si.amount());
            };
            player.getInventory().addItem(product).values()
                    .forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "[캐시상점] " + si.displayName() + " ×" + si.amount()
                    + " 구매! (-" + String.format("%,d", si.price()) + "C)");
            player.sendActionBar(Component.text(si.displayName() + " 구매 완료!")
                    .color(NamedTextColor.LIGHT_PURPLE));
            // 잔액만 갱신 (인벤토리 닫지 않음 → 커서 유지)
            long newCash = cm.get(player.getUniqueId());
            event.getView().getTopInventory().setItem(4, makeItem(Material.DIAMOND,
                    ChatColor.LIGHT_PURPLE + "보유 캐시: " + ChatColor.WHITE + String.format("%,d", newCash) + "C",
                    List.of(ChatColor.GRAY + "캐시는 교환권 또는 이벤트로 획득")));
            return;
        }
    }

    private static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta(); meta.setDisplayName(" "); border.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }
    }
}
