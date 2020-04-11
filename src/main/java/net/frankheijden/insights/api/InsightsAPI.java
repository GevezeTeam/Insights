package net.frankheijden.insights.api;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.frankheijden.insights.Insights;
import net.frankheijden.insights.config.*;
import net.frankheijden.insights.hooks.HookManager;
import net.frankheijden.insights.tasks.LoadChunksTask;
import net.frankheijden.insights.utils.StringUtils;
import net.frankheijden.insights.utils.WorldGuardUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class InsightsAPI {
    private Insights instance = null;

    /**
     * Initiates a new InsightsAPI instance.
     */
    public InsightsAPI() {}

    /**
     * Gets the instance of Insights.
     *
     * @return Insights Main class
     */
    public Insights getInstance() {
        if (instance == null) {
            instance = Insights.getInstance();
        }
        return instance;
    }

    /**
     * Toggles realtime checking for the UUID specified.
     * NOTE: To use realtime checking, the user still needs the permission 'insights.check.realtime'.
     *
     * @param uuid UUID of player
     */
    public void toggleCheck(UUID uuid) {
        getInstance().getSqLite().toggleRealtimeCheck(uuid);
    }

    /**
     * Enables or disabled realtime checking for the UUID specified.
     * NOTE: To use realtime checking, the user still needs the permission 'insights.check.realtime'.
     *
     * @param uuid UUID of player
     * @param enabled boolean enabled
     */
    public void setToggleCheck(UUID uuid, boolean enabled) {
        getInstance().getSqLite().setRealtimeCheck(uuid, enabled);
    }

    /**
     * Checks if the player specified is scanning for chunks.
     *
     * @param uuid UUID of player
     * @return boolean scanning
     */
    public boolean isScanningChunks(UUID uuid) {
        return getInstance().getPlayerScanTasks().containsKey(uuid);
    }

    /**
     * Gets a percentage between 0 and 1 of the progress of scanning chunks,
     * returns null if the player is not scanning chunks.
     *
     * @param uuid UUID of player
     * @return double progress, or null if no ScanTask.
     */
    public Double getScanProgress(UUID uuid) {
        LoadChunksTask loadChunksTask = getInstance().getPlayerScanTasks().get(uuid);
        if (loadChunksTask != null) {
            double total = loadChunksTask.getTotalChunks();
            double done = loadChunksTask.getScanChunksTask().getChunksDone();
            double progress = done/total;
            if (progress < 0) {
                progress = 0;
            } else if (progress > 1) {
                progress = 1;
            }
            return progress;
        }
        return null;
    }

    /**
     * Gets the time elapsed for the current scan of a player
     *
     * @param uuid UUID of player
     * @return String time elapsed, or null if no ScanTask.
     */
    public String getTimeElapsedOfScan(UUID uuid) {
        LoadChunksTask loadChunksTask = getInstance().getPlayerScanTasks().get(uuid);
        if (loadChunksTask != null) {
            return getInstance().getUtils().getDHMS(loadChunksTask.getStartTime());
        }
        return null;
    }

    /**
     * Retrieves the HookManager instance
     *
     * @return HookManager instance
     */
    public HookManager getHookManager() {
        return getInstance().getHookManager();
    }

    public boolean isLimitingEnabled(World world) {
        String name = world.getName();
        Config config = Insights.getInstance().getConfiguration();
        if (config.GENERAL_WORLDS_WHITELIST) {
            if (!StringUtils.matches(config.GENERAL_WORLDS_LIST, name)) {
                return false;
            }
        } else if (StringUtils.matches(config.GENERAL_WORLDS_LIST, name)) {
            return false;
        }
        return true;
    }

    public boolean isLimitingEnabled(String region) {
        Config config = Insights.getInstance().getConfiguration();
        if (config.GENERAL_REGIONS_WHITELIST) {
            if (!StringUtils.matches(config.GENERAL_REGIONS_LIST, region)) {
                return false;
            }
        } else if (StringUtils.matches(config.GENERAL_REGIONS_LIST, region)) {
            return false;
        }
        return true;
    }

    public boolean isInDisabledRegion(Location location) {
        WorldGuardUtils wgUtils = Insights.getInstance().getWorldGuardUtils();
        if (wgUtils != null) {
            ProtectedRegion region = wgUtils.isInInsightsRegion(location);
            if (region != null) {
                return !isLimitingEnabled(region.getId());
            }
        }
        return false;
    }

    public Limit getLimit(Player player, String str) {
        return getLimit(player.getWorld(), player.getLocation(), str);
    }

    public Limit getLimit(World world, String str) {
        return getLimit(world, null, str);
    }

    public Limit getLimit(World world, Location location, String str) {
        if (!isLimitingEnabled(world)) return null;
        if (location != null && isInDisabledRegion(location)) return null;

        Limits limits = Insights.getInstance().getConfiguration().getLimits();
        return limits.getLimit(str);
    }
}
