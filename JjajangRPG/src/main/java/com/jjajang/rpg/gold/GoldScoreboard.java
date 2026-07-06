package com.jjajang.rpg.gold;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashSet;

public class GoldScoreboard {
    private static final String OBJ = "jrpg_gold";
    private final GoldManager gm;

    public GoldScoreboard(GoldManager gm) { this.gm = gm; }

    @SuppressWarnings("deprecation")
    public void update(Player player) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        Scoreboard board = player.getScoreboard();
        if (board == sm.getMainScoreboard()) {
            board = sm.getNewScoreboard();
        }

        Objective obj = board.getObjective(OBJ);
        if (obj == null) {
            obj = board.registerNewObjective(OBJ, "dummy", ChatColor.GOLD + "✦ 짜장RPG ✦");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        for (String e : new HashSet<>(board.getEntries())) board.resetScores(e);

        long g = gm.get(player.getUniqueId());
        obj.getScore(ChatColor.YELLOW + "골드" + ChatColor.WHITE + ": " + g + "G").setScore(1);
        obj.getScore(ChatColor.GRAY + "─────────────").setScore(2);
        player.setScoreboard(board);
    }

    public void updateAll() {
        Bukkit.getOnlinePlayers().forEach(this::update);
    }
}
