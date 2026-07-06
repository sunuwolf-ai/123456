package com.jjajang.rpg.enhance;

import com.jjajang.rpg.gold.GoldManager;
import com.jjajang.rpg.gold.GoldScoreboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class EnhanceListener implements Listener {

    private final JavaPlugin plugin;
    private final GoldManager gm;
    private final GoldScoreboard sb;

    public EnhanceListener(JavaPlugin plugin, GoldManager gm, GoldScoreboard sb) {
        this.plugin = plugin;
        this.gm = gm;
        this.sb = sb;
    }

    public static void open(Player player) {
        player.openInventory(EnhanceGUI.build(player, null));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!EnhanceGUI.TITLE.equals(event.getView().getTitle())) return;

        int slot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        // 아이템 슬롯 (11): 배치 허용, 1틱 후 UI 갱신
        if (slot == EnhanceGUI.ITEM_SLOT) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Inventory top = player.getOpenInventory().getTopInventory();
                ItemStack placed = top.getItem(EnhanceGUI.ITEM_SLOT);
                // 슬롯 플레이스홀더 제거
                if (placed != null && placed.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                    top.setItem(EnhanceGUI.ITEM_SLOT, null);
                    placed = null;
                }
                EnhanceGUI.updateContents(top, placed, player);
            }, 1L);
            return; // 취소하지 않음 (아이템 이동 허용)
        }

        // 강화/복원 버튼 클릭
        if (slot == EnhanceGUI.ENHANCE_BTN && slot < topSize) {
            event.setCancelled(true);
            handleEnhanceClick(player, event.getView().getTopInventory());
            return;
        }

        // 플레이어 인벤토리에서 shift-click → 아이템 슬롯으로
        if (slot >= topSize) {
            if (event.isShiftClick()) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && EnhanceManager.isEnhanceable(clicked)) {
                    event.setCancelled(true);
                    Inventory top = event.getView().getTopInventory();
                    ItemStack existing = top.getItem(EnhanceGUI.ITEM_SLOT);
                    // 기존 아이템이 있으면 교환
                    if (existing != null && existing.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                        player.getInventory().setItem(event.getSlot(), existing);
                    } else {
                        player.getInventory().setItem(event.getSlot(), null);
                    }
                    top.setItem(EnhanceGUI.ITEM_SLOT, clicked.clone());
                    plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        EnhanceGUI.updateContents(top, top.getItem(EnhanceGUI.ITEM_SLOT), player), 1L);
                    return;
                }
            }
            return; // 플레이어 인벤토리 일반 클릭 허용
        }

        // 나머지 GUI 슬롯: 취소
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!EnhanceGUI.TITLE.equals(event.getView().getTitle())) return;
        // 아이템 슬롯 외 GUI 영역에 드래그 방지
        int topSize = event.getView().getTopInventory().getSize();
        for (int slot : event.getRawSlots()) {
            if (slot < topSize && slot != EnhanceGUI.ITEM_SLOT) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!EnhanceGUI.TITLE.equals(event.getView().getTitle())) return;

        // 아이템 슬롯에 남은 아이템 반환
        ItemStack left = event.getInventory().getItem(EnhanceGUI.ITEM_SLOT);
        if (left != null && left.getType() != Material.GRAY_STAINED_GLASS_PANE && !left.getType().isAir()) {
            player.getInventory().addItem(left);
        }
    }

    private void handleEnhanceClick(Player player, Inventory top) {
        ItemStack item = top.getItem(EnhanceGUI.ITEM_SLOT);
        if (item == null || item.getType().isAir() || item.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // 복원 모드
        if (EnhanceManager.isDestroyed(item)) {
            handleRestore(player, top, item);
            return;
        }

        if (!EnhanceManager.isEnhanceable(item)) return;

        int stars = EnhanceManager.getStars(item);
        if (stars >= 20) return;

        long cost = EnhanceManager.getCost(stars);
        if (!gm.spend(player.getUniqueId(), cost)) {
            player.sendMessage(ChatColor.RED + "골드가 부족합니다! 필요: " + cost + "G, 보유: " + gm.get(player.getUniqueId()) + "G");
            return;
        }
        sb.update(player);

        EnhanceManager.Result result = EnhanceManager.roll(stars);
        switch (result) {
            case SUCCESS -> {
                EnhanceManager.setStars(plugin, item, stars + 1);
                top.setItem(EnhanceGUI.ITEM_SLOT, item);
                player.sendMessage(ChatColor.GREEN + "[강화] " + (stars + 1) + "성 강화 성공! ✦");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
            }
            case FAIL -> {
                if (stars >= 10) {
                    EnhanceManager.setStars(plugin, item, stars - 1);
                    top.setItem(EnhanceGUI.ITEM_SLOT, item);
                    player.sendMessage(ChatColor.YELLOW + "[강화] 실패! " + (stars - 1) + "성으로 하락.");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "[강화] 실패! 유지됨.");
                }
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.7f);
            }
            case DESTROY -> {
                player.sendMessage(ChatColor.RED + "[강화] ☠ 장비가 파괴되었습니다!");
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.5f);
                player.spawnParticle(Particle.EXPLOSION, player.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0);
                EnhanceManager.destroy(plugin, item);
                top.setItem(EnhanceGUI.ITEM_SLOT, item);
            }
        }

        EnhanceGUI.updateContents(top, top.getItem(EnhanceGUI.ITEM_SLOT), player);
    }

    private void handleRestore(Player player, Inventory top, ItemStack item) {
        String matName = item.getItemMeta().getPersistentDataContainer()
                .get(EnhanceManager.KEY_SOURCE_MAT, PersistentDataType.STRING);
        if (matName == null) return;

        Material mat;
        try { mat = Material.valueOf(matName); } catch (IllegalArgumentException e) { return; }

        // 인벤토리에서 같은 종류 비파괴 아이템 제거
        boolean found = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s != null && s.getType() == mat && !EnhanceManager.isDestroyed(s)) {
                s.setAmount(s.getAmount() - 1);
                if (s.getAmount() <= 0) player.getInventory().setItem(i, null);
                found = true;
                break;
            }
        }
        if (!found) {
            player.sendMessage(ChatColor.RED + "재료 아이템이 없습니다!");
            return;
        }

        // 파괴 플래그 제거, 10성으로 복원
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(EnhanceManager.KEY_DESTROYED);
        meta.getPersistentDataContainer().set(EnhanceManager.KEY_STARS, PersistentDataType.INTEGER, 10);
        // 원래 이름 복원
        String displayName = meta.getDisplayName()
                .replace(ChatColor.DARK_GRAY + "☠ [흔적] " + ChatColor.RESET, "");
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        EnhanceManager.refreshLore(plugin, item);
        top.setItem(EnhanceGUI.ITEM_SLOT, item);

        player.sendMessage(ChatColor.GREEN + "[복원] 장비가 10성으로 복원되었습니다!");
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        EnhanceGUI.updateContents(top, item, player);
    }
}
