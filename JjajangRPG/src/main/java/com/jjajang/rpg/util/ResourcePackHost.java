package com.jjajang.rpg.util;

import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;

public class ResourcePackHost implements Listener {

    private final JavaPlugin plugin;
    private HttpServer server;
    private String packUrl;
    private String packSha1;

    public ResourcePackHost(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean start() {
        File packFile = new File(plugin.getDataFolder(), "resourcepack.zip");
        if (!packFile.exists()) {
            plugin.getLogger().warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            plugin.getLogger().warning("[짜장RPG] 리소스팩 파일을 찾을 수 없습니다!");
            plugin.getLogger().warning("JjajangRPG_ResourcePack.zip 을 아래 경로로 복사 후 재시작:");
            plugin.getLogger().warning("→ " + packFile.getAbsolutePath());
            plugin.getLogger().warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return false;
        }

        try {
            byte[] packData = Files.readAllBytes(packFile.toPath());

            // SHA-1 계산
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = sha1.digest(packData);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            packSha1 = sb.toString();

            // HTTP 서버 시작 (포트 8765)
            int port = plugin.getConfig().getInt("resource-pack.port", 8765);
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/resourcepack.zip", exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, packData.length);
                try (var os = exchange.getResponseBody()) {
                    os.write(packData);
                }
            });
            server.setExecutor(null);
            server.start();

            // URL 구성
            String host = plugin.getConfig().getString("resource-pack.host", "127.0.0.1");
            packUrl = "http://" + host + ":" + port + "/resourcepack.zip";
            plugin.getLogger().info("[짜장RPG] 리소스팩 서버 시작: " + packUrl);
            plugin.getLogger().info("[짜장RPG] SHA1: " + packSha1);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("[짜장RPG] 리소스팩 서버 시작 실패: " + e.getMessage());
            return false;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public boolean isReady() {
        return server != null && packUrl != null;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!isReady()) return;
        Player player = event.getPlayer();
        // 1틱 후 전송 (로그인 처리 완료 대기)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            player.setResourcePack(packUrl, packSha1, false,
                    Component.text("짜장RPG 리소스팩을 설치해주세요!").color(NamedTextColor.YELLOW));
        }, 20L);
    }
}
