package com.jjajang.rpg.item;

import com.jjajang.rpg.cash.CashManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class CashVoucherListener implements Listener {

    private final CashManager cashManager;

    public CashVoucherListener(CashManager cashManager) {
        this.cashManager = cashManager;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!CashVoucherItem.is(held)) return;

        event.setCancelled(true);

        long amount = CashVoucherItem.getCashAmount(held);
        cashManager.add(player.getUniqueId(), amount);

        // 교환권 소비
        if (held.getAmount() > 1) {
            held.setAmount(held.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(ChatColor.AQUA + "[교환권] " + String.format("%,d", amount) + "C가 지급되었습니다! "
                + ChatColor.DARK_AQUA + "(보유: " + String.format("%,d", cashManager.get(player.getUniqueId())) + "C)");
        player.sendActionBar(Component.text("+" + String.format("%,d", amount) + "C 지급!")
                .color(NamedTextColor.AQUA));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
    }
}
