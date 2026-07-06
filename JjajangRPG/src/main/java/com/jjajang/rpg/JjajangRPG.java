package com.jjajang.rpg;

import com.jjajang.rpg.archer.ArcherSkillListener;
import com.jjajang.rpg.event.EventManager;
import com.jjajang.rpg.event.EventMenuListener;
import com.jjajang.rpg.item.CashVoucherItem;
import com.jjajang.rpg.item.CashVoucherListener;
import com.jjajang.rpg.item.ExpCouponItem;
import com.jjajang.rpg.item.PlatinumJjajangItem;
import com.jjajang.rpg.item.PlatinumJjajangListener;
import com.jjajang.rpg.item.PoopItem;
import com.jjajang.rpg.item.PoopListener;
import com.jjajang.rpg.mage.MageSkillListener;
import com.jjajang.rpg.jjajjang.JjajjangBowlItem;
import com.jjajang.rpg.jjajjang.JjajjangSkillListener;
import com.jjajang.rpg.pass.PassMenuListener;
import com.jjajang.rpg.util.ResourcePackHost;
import com.jjajang.rpg.cash.CashManager;
import com.jjajang.rpg.cash.CashShopMenuListener;
import com.jjajang.rpg.classes.AdvancementCommand;
import com.jjajang.rpg.classes.ClassCommand;
import com.jjajang.rpg.classes.ClassManager;
import com.jjajang.rpg.classes.ClassMenuListener;
import com.jjajang.rpg.enhance.EnhanceListener;
import com.jjajang.rpg.enhance.EnhanceManager;
import com.jjajang.rpg.gold.GoldCommand;
import com.jjajang.rpg.gold.GoldManager;
import com.jjajang.rpg.gold.GoldScoreboard;
import com.jjajang.rpg.menu.ServerMenuListener;
import com.jjajang.rpg.quest.QuestManager;
import com.jjajang.rpg.quest.QuestMenuListener;
import com.jjajang.rpg.refine.DanmujiItem;
import com.jjajang.rpg.refine.RefineListener;
import com.jjajang.rpg.refine.RefineManager;
import com.jjajang.rpg.reward.RewardManager;
import com.jjajang.rpg.reward.RewardMenuListener;
import com.jjajang.rpg.rogue.RogueSkillListener;
import com.jjajang.rpg.shop.ShopMenuListener;
import com.jjajang.rpg.stats.StatListener;
import com.jjajang.rpg.util.WeaponPenaltyListener;
import com.jjajang.rpg.warp.WarpMenuListener;
import com.jjajang.rpg.warrior.WarriorSkillListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class JjajangRPG extends JavaPlugin {

    private ResourcePackHost resourcePackHost;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        saveDefaultConfig();

        ClassManager.init(this);
        EnhanceManager.init(this);
        RefineManager.init(this);
        DanmujiItem.init(this);
        com.jjajang.rpg.item.BossFragmentItem.init(this);
        com.jjajang.rpg.item.BoarHornItem.init(this);
        com.jjajang.rpg.item.BoarLeatherItem.init(this);
        com.jjajang.rpg.item.BoarMeatItem.init(this);
        CashVoucherItem.init(this);
        PlatinumJjajangItem.init(this);
        ExpCouponItem.init(this);
        PoopItem.init(this);
        JjajjangBowlItem.init(this);

        GoldManager goldManager = new GoldManager(this);
        GoldScoreboard goldSb   = new GoldScoreboard(goldManager);
        GoldCommand goldCmd     = new GoldCommand(goldManager, goldSb);
        CashManager cashManager = new CashManager(this);
        QuestManager questManager = new QuestManager(this);
        RewardManager rewardManager = new RewardManager(this);
        StatListener statListener = new StatListener(this, goldManager, goldSb);

        AdvancementCommand advCmd = new AdvancementCommand(this);
        ServerMenuListener serverMenu = new ServerMenuListener(this, advCmd, goldCmd, questManager, rewardManager, cashManager);

        getServer().getPluginManager().registerEvents(serverMenu,    this);
        getServer().getPluginManager().registerEvents(new ClassMenuListener(this),     this);
        getServer().getPluginManager().registerEvents(new WeaponPenaltyListener(this), this);
        getServer().getPluginManager().registerEvents(new WarriorSkillListener(this),  this);
        getServer().getPluginManager().registerEvents(new ArcherSkillListener(this),   this);
        getServer().getPluginManager().registerEvents(new RogueSkillListener(this),    this);
        getServer().getPluginManager().registerEvents(new MageSkillListener(this),     this);
        getServer().getPluginManager().registerEvents(new EnhanceListener(this, goldManager, goldSb), this);
        RefineListener refineListener = new RefineListener(this);
        getServer().getPluginManager().registerEvents(refineListener, this);
        getServer().getPluginManager().registerEvents(statListener,  this);
        refineListener.setStatListener(statListener);
        getServer().getPluginManager().registerEvents(goldCmd,       this);
        getServer().getPluginManager().registerEvents(new WarpMenuListener(this),      this);
        getServer().getPluginManager().registerEvents(new QuestMenuListener(this, questManager, goldManager, goldSb), this);
        getServer().getPluginManager().registerEvents(new ShopMenuListener(this, goldManager, goldSb), this);
        getServer().getPluginManager().registerEvents(new RewardMenuListener(rewardManager, cashManager), this);
        getServer().getPluginManager().registerEvents(new CashShopMenuListener(cashManager), this);
        getServer().getPluginManager().registerEvents(new CashVoucherListener(cashManager), this);
        getServer().getPluginManager().registerEvents(new PlatinumJjajangListener(this, goldManager, goldSb, cashManager), this);
        getServer().getPluginManager().registerEvents(new PoopListener(), this);
        getServer().getPluginManager().registerEvents(new JjajjangSkillListener(this), this);

        // 이벤트 & 짜장패스
        EventManager eventManager = new EventManager(this);
        getServer().getPluginManager().registerEvents(eventManager, this);
        EventMenuListener eventMenu = new EventMenuListener(eventManager, goldManager, goldSb, cashManager);
        getServer().getPluginManager().registerEvents(eventMenu, this);
        PassMenuListener passMenu = new PassMenuListener(eventManager, cashManager, goldManager, goldSb);
        getServer().getPluginManager().registerEvents(passMenu, this);
        serverMenu.setEventMenuListener(eventMenu);
        serverMenu.setPassMenuListener(passMenu);

        com.jjajang.rpg.boss.BossKimSanghyuk bossKimSH =
                new com.jjajang.rpg.boss.BossKimSanghyuk(this, goldManager, goldSb);
        getServer().getPluginManager().registerEvents(bossKimSH, this);
        getCommand("상혁이").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) { sender.sendMessage("플레이어 전용입니다."); return true; }
            bossKimSH.spawn(p.getLocation());
            return true;
        });

        getCommand("클래스").setExecutor(new ClassCommand());
        getCommand("전직").setExecutor(advCmd);
        getCommand("메뉴").setExecutor(serverMenu);
        getCommand("골드").setExecutor(goldCmd);
        getCommand("캐시").setExecutor((sender, cmd, label, args) -> {
            if (!sender.isOp()) { sender.sendMessage("§c권한이 없습니다."); return true; }
            if (args.length < 2) { sender.sendMessage("§e사용법: /캐시 <플레이어> <금액>"); return true; }
            org.bukkit.entity.Player target = getServer().getPlayer(args[0]);
            if (target == null) { sender.sendMessage("§c플레이어를 찾을 수 없습니다."); return true; }
            try {
                long amount = Long.parseLong(args[1]);
                cashManager.add(target.getUniqueId(), amount);
                sender.sendMessage("§d" + target.getName() + "에게 " + amount + "C 지급.");
                target.sendMessage("§d" + amount + "C를 지급받았습니다! 현재: " + cashManager.get(target.getUniqueId()) + "C");
            } catch (NumberFormatException e) {
                sender.sendMessage("§c올바른 숫자를 입력하세요.");
            }
            return true;
        });
        getCommand("단무지").setExecutor((sender, cmd, label, args) -> {
            if (!(sender instanceof Player p)) { sender.sendMessage("플레이어 전용입니다."); return true; }
            int amount = 1;
            if (args.length > 0) { try { amount = Math.max(1, Integer.parseInt(args[0])); } catch (NumberFormatException ignored) {} }
            p.getInventory().addItem(DanmujiItem.create(amount));
            p.sendMessage("§e단무지 " + amount + "개를 지급받았습니다.");
            return true;
        });

        // 테러방지: 엔더 크리스탈 / 리스폰 앵커 조합법 삭제
        getServer().removeRecipe(org.bukkit.NamespacedKey.minecraft("end_crystal"));
        getServer().removeRecipe(org.bukkit.NamespacedKey.minecraft("respawn_anchor"));

        resourcePackHost = new ResourcePackHost(this);
        if (resourcePackHost.start()) {
            getServer().getPluginManager().registerEvents(resourcePackHost, this);
        }

        // 리로드 시 이미 접속 중인 플레이어 HP 버그 즉시 수정
        final StatListener finalStat = statListener;
        getServer().getScheduler().runTaskLater(this, () ->
            getServer().getOnlinePlayers().forEach(finalStat::applyAttributes), 10L);

        getLogger().info("짜장RPG 플러그인이 활성화되었습니다!");
    }

    @Override
    public void onDisable() {
        if (resourcePackHost != null) resourcePackHost.stop();
        getLogger().info("짜장RPG 플러그인이 비활성화되었습니다.");
    }
}
