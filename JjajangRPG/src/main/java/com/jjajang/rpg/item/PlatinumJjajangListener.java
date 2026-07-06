package com.jjajang.rpg.item;

import com.jjajang.rpg.cash.CashShopMenuListener;
import com.jjajang.rpg.cash.CashManager;
import com.jjajang.rpg.gold.GoldManager;
import com.jjajang.rpg.gold.GoldScoreboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PlatinumJjajangListener implements Listener {

    private static final String TITLE = ChatColor.LIGHT_PURPLE + "✦ 플래티넘 짜장 ✦";
    private static final String BTN_OPEN1  = ChatColor.YELLOW + "1회 오픈";
    private static final String BTN_OPEN3  = ChatColor.GOLD   + "3회 오픈";
    private static final String BTN_BUY    = ChatColor.AQUA   + "더 사기";

    // exp coupon active: UUID → {multiplier, expiry_ms}
    private final Map<UUID, long[]> activeExpBoost = new HashMap<>();

    private final JavaPlugin plugin;
    private final GoldManager gm;
    private final GoldScoreboard sb;
    private final CashManager cm;
    private final Random rng = new Random();

    public PlatinumJjajangListener(JavaPlugin plugin, GoldManager gm, GoldScoreboard sb, CashManager cm) {
        this.plugin = plugin;
        this.gm = gm;
        this.sb = sb;
        this.cm = cm;
    }

    // ── 플래티넘 짜장 우클릭 → UI 오픈 ───────────────────────────────
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!PlatinumJjajangItem.is(held)) return;

        event.setCancelled(true);
        openJjajangUI(player);
    }

    // ── XP 쿠폰 우클릭 → 활성화 ────────────────────────────────────
    @EventHandler
    public void onCouponUse(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!ExpCouponItem.is(held)) return;

        event.setCancelled(true);
        int mult = ExpCouponItem.getMultiplier(held);
        long expiry = System.currentTimeMillis() + 30 * 60 * 1000L;
        activeExpBoost.put(player.getUniqueId(), new long[]{mult, expiry});

        held.setAmount(held.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        player.sendMessage(ChatColor.YELLOW + "★ 경험치 " + mult + "배 쿠폰 활성화! (30분)");
        player.sendActionBar(Component.text("경험치 " + mult + "배 쿠폰 적용 중!")
                .color(NamedTextColor.YELLOW));
    }

    // ── XP 획득 시 배율 적용 ────────────────────────────────────────
    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        long[] boost = activeExpBoost.get(event.getPlayer().getUniqueId());
        if (boost == null) return;
        if (System.currentTimeMillis() > boost[1]) {
            activeExpBoost.remove(event.getPlayer().getUniqueId());
            return;
        }
        int mult = (int) boost[0];
        if (mult > 1) event.setAmount(event.getAmount() * mult);
    }

    // ── 사망 시 쿠폰 효과 제거 ──────────────────────────────────────
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        activeExpBoost.remove(event.getEntity().getUniqueId());
    }

    // ── 짜장 오픈 UI ─────────────────────────────────────────────────
    public void openJjajangUI(Player player) {
        int count = countJjajang(player);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // 아이템 미리보기 (중앙)
        inv.setItem(13, makeItem(Material.PAPER,
                ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ 플래티넘 짜장 ✦",
                List.of(ChatColor.GRAY + "보유: " + ChatColor.WHITE + count + "개",
                        ChatColor.DARK_PURPLE + "경험치 쿠폰 / 골드 보상")));

        // 1회 오픈
        inv.setItem(10, makeItem(Material.GOLD_INGOT, BTN_OPEN1,
                List.of(ChatColor.GRAY + "플래티넘 짜장 1개를 오픈합니다",
                        ChatColor.YELLOW + "보유: " + count + "개",
                        count >= 1 ? (ChatColor.GREEN + "▶ 클릭하여 오픈") : (ChatColor.RED + "짜장이 부족합니다"))));

        // 3회 오픈
        inv.setItem(12, makeItem(Material.GOLD_BLOCK, BTN_OPEN3,
                List.of(ChatColor.GRAY + "플래티넘 짜장 3개를 오픈합니다",
                        ChatColor.YELLOW + "보유: " + count + "개",
                        count >= 3 ? (ChatColor.GREEN + "▶ 클릭하여 오픈") : (ChatColor.RED + "짜장이 부족합니다"))));

        // 더 사기
        inv.setItem(16, makeItem(Material.DIAMOND, BTN_BUY,
                List.of(ChatColor.GRAY + "캐시상점으로 이동합니다",
                        ChatColor.AQUA + "▶ 클릭")));

        fillBorder(inv);
        player.openInventory(inv);
    }

    // ── UI 클릭 처리 ─────────────────────────────────────────────────
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String name = clicked.getItemMeta().getDisplayName();

        if (BTN_OPEN1.equals(name)) {
            if (!consumeJjajang(player, 1)) {
                player.sendMessage(ChatColor.RED + "[플래티넘 짜장] 짜장이 부족합니다.");
                return;
            }
            giveRewards(player, 1);
            player.closeInventory();
            return;
        }

        if (BTN_OPEN3.equals(name)) {
            if (!consumeJjajang(player, 3)) {
                player.sendMessage(ChatColor.RED + "[플래티넘 짜장] 짜장이 3개 이상 필요합니다.");
                return;
            }
            giveRewards(player, 3);
            player.closeInventory();
            return;
        }

        if (BTN_BUY.equals(name)) {
            player.closeInventory();
            CashShopMenuListener.open(player, cm);
        }
    }

    // ── 보상 지급 ────────────────────────────────────────────────────
    private void giveRewards(Player player, int times) {
        for (int i = 0; i < times; i++) {
            int roll = rng.nextInt(100);
            if (roll < 15) {
                // 15%: 경험치 3배 쿠폰
                giveOrDrop(player, ExpCouponItem.create(3));
                player.sendMessage(ChatColor.GOLD + "  ★ 경험치 3배 쿠폰!");
            } else if (roll < 45) {
                // 30%: 경험치 2배 쿠폰
                giveOrDrop(player, ExpCouponItem.create(2));
                player.sendMessage(ChatColor.YELLOW + "  ★ 경험치 2배 쿠폰!");
            } else {
                // 55%: 랜덤 골드 (5,000~30,000G)
                long gold = (rng.nextInt(6) + 1) * 5_000L;
                gm.add(player.getUniqueId(), gold);
                sb.update(player);
                player.sendMessage(ChatColor.GOLD + "  ★ +" + String.format("%,d", gold) + "G!");
            }
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        player.sendActionBar(Component.text("플래티넘 짜장 오픈!").color(NamedTextColor.LIGHT_PURPLE));
    }

    // ── 유틸 ─────────────────────────────────────────────────────────
    private int countJjajang(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (PlatinumJjajangItem.is(item)) count += item.getAmount();
        }
        return count;
    }

    private boolean consumeJjajang(Player player, int amount) {
        if (countJjajang(player) < amount) return false;
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (!PlatinumJjajangItem.is(item)) continue;
            if (item.getAmount() <= remaining) {
                remaining -= item.getAmount();
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(item.getAmount() - remaining);
                remaining = 0;
            }
        }
        return true;
    }

    private static void giveOrDrop(Player player, ItemStack item) {
        player.getInventory().addItem(item).values()
                .forEach(l -> player.getWorld().dropItemNaturally(player.getLocation(), l));
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
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) inv.setItem(i, border);
        }
    }
}
