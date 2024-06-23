package quangdev05.beaddons24;

import net.ess3.api.IEssentials;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class BEAddons24 extends JavaPlugin {
    private Map<UUID, Integer> backUsageMap = new HashMap<>();
    private LuckPerms luckPerms;
    private IEssentials essentials;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    private FileConfiguration config;
    private String licenseKey;
    private boolean isLicenseValid = false;
    private String versionCheckUrl = "";
    private String licenseCheckUrl = "";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadConfig();

        luckPerms = LuckPermsProvider.get();
        essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");

        // Schedule the reset task at 23:59 every day
        new BukkitRunnable() {
            @Override
            public void run() {
                resetBackUsage();
            }
        }.runTaskTimer(this, getInitialDelay(), 24 * 60 * 60 * 20);

        // Check for updates if enabled
        if (config.getBoolean("update-checker", true)) {
            checkForUpdates();
        }

        getLogger().info("BEAddons24 đã được bật.");
        getLogger().info("██████╗ ███████╗ █████╗ ██████╗ ██████╗  ██████╗ ███╗   ██╗███████╗");
        getLogger().info("██╔══██╗██╔════╝██╔══██╗██╔══██╗██╔══██╗██╔═══██╗████╗  ██║██╔════╝");
        getLogger().info("██████╔╝█████╗  ███████║██║  ██║██║  ██║██║   ██║██╔██╗ ██║███████╗");
        getLogger().info("██╔══██╗██╔══╝  ██╔══██║██║  ██║██║  ██║██║   ██║██║╚██╗██║╚════██║");
        getLogger().info("██████╔╝███████╗██║  ██║██████╔╝██████╔╝╚██████╔╝██║ ╚████║███████║");
        getLogger().info("╚═════╝ ╚══════╝╚═╝  ╚═╝╚═════╝ ╚═════╝  ╚═════╝ ╚═╝  ╚═══╝╚══════╝");
        getLogger().info(" ");
        getLogger().info("  Plugin by: QuangDev05");
        getLogger().info("  Premium plugin for PlayST");
        getLogger().info("  Version: " + getDescription().getVersion());
        getLogger().info(" ");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("back")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                UUID playerId = player.getUniqueId();
                if (!isLicenseValid) {
                    sender.sendMessage("Plugin chưa được kích hoạt với license key. Vui lòng nhập license key trong file config.yml.");
                    return true;
                }
                int maxBackUsage = getMaxBackUsage(player);

                int currentUsage = backUsageMap.getOrDefault(playerId, 0);
                if (currentUsage < maxBackUsage) {
                    backUsageMap.put(playerId, currentUsage + 1);
                    // Execute EssentialsX /back command
                    Bukkit.dispatchCommand(sender, "essentials:back");
                } else {
                    player.sendMessage(config.getString("messages.reached_limit"));
                }
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("bea") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            loadConfig();
            sender.sendMessage("Config của BEAddons24 đã được reload.");
            return true;
        }
        return false;
    }

    private void loadConfig() {
        licenseKey = config.getString("license_key", "");
        if (!licenseKey.isEmpty()) {
            validateLicense();
        }
    }

    private void validateLicense() {
        try {
            URL url = new URL(licenseCheckUrl + "?key=" + licenseKey);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = in.readLine();
                if (response.equalsIgnoreCase("valid")) {
                    isLicenseValid = true;
                    getLogger().info("License key đã được xác thực.");
                } else {
                    isLicenseValid = false;
                    getLogger().warning("License key không hợp lệ. Plugin sẽ không hoạt động.");
                }
                in.close();
            } else {
                getLogger().warning("Không thể kết nối đến server kiểm tra license key.");
                isLicenseValid = false;
            }
            connection.disconnect();
        } catch (Exception e) {
            getLogger().warning("Lỗi khi kiểm tra license key: " + e.getMessage());
            isLicenseValid = false;
        }
    }

    private void checkForUpdates() {
        try {
            URL url = new URL(versionCheckUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String latestVersion = in.readLine().trim();
                if (!getDescription().getVersion().equalsIgnoreCase(latestVersion)) {
                    getLogger().info("BEAddons đã có phiên bản mới, tải ngay tại: https://github.com/QuangDev05/BEAddons");
                } else {
                    getLogger().info("Plugin đang ở phiên bản mới nhất.");
                }
                in.close();
            } else {
                getLogger().warning("Không thể kết nối đến server kiểm tra phiên bản mới.");
            }
            connection.disconnect();
        } catch (Exception e) {
            getLogger().warning("Lỗi khi kiểm tra phiên bản mới: " + e.getMessage());
        }
    }

    private int getMaxBackUsage(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            for (Node node : user.getNodes()) {
                if (node.getKey().startsWith("backlimit.")) {
                    String rank = node.getKey().split("\\.")[1];
                    return config.getInt("back-limits." + rank, 1);
                }
            }
        }
        return config.getInt("back-limits.default", 1); // default limit
    }

    private void resetBackUsage() {
        backUsageMap.clear();
        Bukkit.broadcastMessage(config.getString("messages.reset_message"));
        getLogger().info("Giới hạn sử dụng lệnh /back trong một ngày đã được reset.");
    }

    private long getInitialDelay() {
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        LocalDateTime nextMidnight = now.withHour(23).withMinute(59).withSecond(0);
        if (now.isAfter(nextMidnight)) {
            nextMidnight = nextMidnight.plusDays(1);
        }
        return java.time.Duration.between(now, nextMidnight).getSeconds() * 20;
    }
}
