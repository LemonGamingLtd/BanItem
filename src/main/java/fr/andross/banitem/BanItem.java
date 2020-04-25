package fr.andross.banitem;

import fr.andross.banitem.Commands.BanCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Main plugin class
 * @version 2.0
 * @author Andross
 */
public class BanItem extends JavaPlugin {
    private static BanItem instance;
    private BanItemAPI api;
    private BanConfig banConfig;
    private BanDatabase banDatabase;
    private final BanUtils utils = new BanUtils(this);

    @Override
    public void onEnable() {
        instance = this;
        api = new BanItemAPI(this);
        // Loading plugin on next tick after worlds
        Bukkit.getScheduler().runTaskLater(this, () -> { if(isEnabled()) load(Bukkit.getConsoleSender(), null); }, 20L);
    }

    /**
     * (re)Loading the plugin with this configuration file
     * @param sender command sender <i>(send the message debug to)</i>
     * @param config the file configuration to load. If null, using (and reloading) the default config
     */
    protected void load(@NotNull final CommandSender sender, @Nullable FileConfiguration config) {
        // (re)Loading config
        if (config == null) {
            saveDefaultConfig();
            reloadConfig();
            config = getConfig();
        }
        banConfig = new BanConfig(utils, sender, config);

        // (re)Loading database
        banDatabase = new BanDatabase(this, sender, config);

        // (re)Loading listeners
        BanListener.loadListeners();

        // Result
        sender.sendMessage(utils.getPrefix() + utils.color("&2Successfully loaded &e" + banDatabase.getBlacklist().getTotal() + "&2 blacklisted & &e" + banDatabase.getWhitelist().getTotal() + "&2 whitelisted item(s)."));
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender sender, @NotNull final Command command, final @NotNull String label, @NotNull final String[] args) {
        try {
            BanCommand.runCommand(this, args[0], sender, args);
        } catch (final Exception e) {
            // Permission?
            if (!sender.hasPermission("banitem.command.help")) {
                final String message = getConfig().getString("no-permission");
                if (message != null) sender.sendMessage(utils.color(message));
                return true;
            }

            // Help messages
            sender.sendMessage(utils.getPrefix() + utils.color(("&7&m     &r &l[&7&lUsage - &e&lv" + getDescription().getVersion() + "&r&l] &7&m     ")));
            sender.sendMessage(utils.getPrefix() + utils.color(" &7- /bi &3add&7: add an item in blacklist."));
            sender.sendMessage(utils.getPrefix() + utils.color(" &7- /bi &3check&7: check if any player has a blacklisted item."));
            sender.sendMessage(utils.getPrefix() + utils.color(" &7- /bi &3customitem&7: add/remove/list custom items."));
            sender.sendMessage(utils.getPrefix() + utils.color(" &7- /bi &3help&7: gives additional informations."));
            sender.sendMessage(utils.getPrefix() + utils.color(" &7- /bi &3info&7: get info about your item in hand."));
            sender.sendMessage(utils.getPrefix() + utils.color(" &7- /bi &3log&7: activate the log mode."));
            sender.sendMessage(utils.getPrefix() + utils.color(" &7- /bi &3reload&7: reload the config."));
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final Command command, @NotNull final String alias, @NotNull final String[] args) {
        // Has permission?
        if (!sender.hasPermission("banitem.command.help")) return new ArrayList<>();

        // Sub command
        if (args.length == 1) return StringUtil.copyPartialMatches(args[args.length - 1], utils.getCommands(), new ArrayList<>());

        // Running subcommand
        try {
            return BanCommand.runTab(this, args[0], sender, args);
        } catch (final Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Gives the current instance of the plugin.
     * The plugin should not be accessed this way, but rather with {@link org.bukkit.plugin.PluginManager#getPlugin(String)}
     * @return the current instance of the plugin
     */
    @NotNull
    public static BanItem getInstance() {
        return instance;
    }

    /**
     * Get the ban api
     * @return the ban item api
     */
    @NotNull
    public BanItemAPI getApi() {
        return api;
    }

    /**
     * Get a the ban config
     * @return the ban config
     */
    @NotNull
    public BanConfig getBanConfig() {
        return banConfig;
    }

    /**
     * Get the ban database, containing blacklist, whitelist and customitems
     * @return the ban database
     */
    @NotNull
    public BanDatabase getBanDatabase() {
        return banDatabase;
    }

    /**
     * An utility class for the plugin
     * @return an utility class
     */
    @NotNull
    public BanUtils getUtils() {
        return utils;
    }
}
