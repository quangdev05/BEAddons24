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

public class BEAddons24 extends JavaPlugin {
    private Map<UUID, Integer> backUsageMap = new HashMap<>();
    private LuckPerms luckPerms;
    private IEssentials essentials;
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    private FileConfiguration config;
    private String licenseKey;
    private boolean isLicenseValid = false;
    private static final String NEW_VERSION_MESSAGE = "&aBEAddons đã có phiên bản mới, tải ngay tại: https://github.com/QuangDev05/BEAddons";
    private static final String NO_UPDATE_MESSAGE = "&ePlugin đang ở phiên bản mới nhất.";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadConfig();

        luckPerms = LuckPermsProvider.get();
        essentials = (IEssentials) Bukkit.getPluginManager().getPlugin("Essentials");

        new BukkitRunnable() {
            @Override
            public void run() {
                resetBackUsage();
            }
        }.runTaskTimer(this, getInitialDelay(), 24 * 60 * 60 * 20);

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
                    Bukkit.dispatchCommand(sender, "essentials:back");
                } else {
                    player.sendMessage(config.getString("messages.reached_limit"));
                }
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("bea")) {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                sender.sendMessage("§a--- BEAddons24 Help ---");
                sender.sendMessage("§b/bea reload §7- Reload cấu hình plugin");
                sender.sendMessage("§b/bea addback <player> <amount> §7- Thêm số lần sử dụng lệnh /back không hết hạn cho người chơi");
                sender.sendMessage("§b/back §7- Sử dụng lệnh /back với giới hạn");
                sender.sendMessage("§ePlugin by: QuangDev05");
                sender.sendMessage("§eVersion: " + getDescription().getVersion());
                sender.sendMessage("§eLicense Key: " + (isLicenseValid ? "§aHợp lệ" : "§cKhông hợp lệ"));
                return true;
            } else if (args.length > 0) {
                if (args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    loadConfig();
                    sender.sendMessage("Config của BEAddons24 đã được reload.");
                    return true;
                } else if (args[0].equalsIgnoreCase("addback")) {
                    if (args.length == 3) {
                        String playerName = args[1];
                        int amount;
                        try {
                            amount = Integer.parseInt(args[2]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage("Số lần sử dụng phải là một số nguyên dương.");
                            return true;
                        }
                        Player target = Bukkit.getPlayer(playerName);
                        if (target == null || !target.isOnline()) {
                            sender.sendMessage("Người chơi không tồn tại hoặc không online.");
                            return true;
                        }
                        addBackUsage(target, amount);
                        sender.sendMessage("Đã thêm " + amount + " lần sử dụng /back không hết hạn cho người chơi " + target.getName());
                        return true;
                    } else {
                        sender.sendMessage("Sử dụng: /bea addback <player> <amount>");
                        return true;
                    }
                }
            }
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
            URL url = new URL(LICENSE_CHECK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                isLicenseValid = false;
                while ((line = in.readLine()) != null) {
                    if (line.trim().equalsIgnoreCase(licenseKey)) {
                        isLicenseValid = true;
                        getLogger().info("License key đã được xác thực.");
                        break;
                    }
                }
                if (!isLicenseValid) {
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
            URL url = new URL(VERSION_CHECK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String latestVersion = in.readLine().trim();
                if (!getDescription().getVersion().equalsIgnoreCase(latestVersion)) {
                    getLogger().info(NEW_VERSION_MESSAGE);
                    notifyAdmins(NEW_VERSION_MESSAGE);
                } else {
                    getLogger().info(NO_UPDATE_MESSAGE);
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

    private void notifyAdmins(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(message);
            }
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
        return config.getInt("back-limits.default", 1); 
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

    private void addBackUsage(Player player, int amount) {
        UUID playerId = player.getUniqueId();
        backUsageMap.put(playerId, backUsageMap.getOrDefault(playerId, 0) + amount);
    }
}
