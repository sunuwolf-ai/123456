package com.jjajang.rpg.item;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PoopListener implements Listener {

    @EventHandler
    public void onEat(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (!PoopItem.is(held)) return;

        event.setCancelled(true);

        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 0));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 0.6f);
        player.sendMessage("§6[응가] §7...왜 먹은 거야?");

        held.setAmount(held.getAmount() - 1);
    }
}
