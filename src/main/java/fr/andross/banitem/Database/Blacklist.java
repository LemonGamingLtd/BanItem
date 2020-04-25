package fr.andross.banitem.Database;

import fr.andross.banitem.BanItem;
import fr.andross.banitem.BanUtils;
import fr.andross.banitem.Events.PlayerBanItemEvent;
import fr.andross.banitem.Options.BanData;
import fr.andross.banitem.Options.BanDataType;
import fr.andross.banitem.Options.BanOption;
import fr.andross.banitem.Options.BanOptionData;
import fr.andross.banitem.Utils.Ban.BannedItem;
import fr.andross.banitem.Utils.Debug.Debug;
import fr.andross.banitem.Utils.Debug.DebugMessage;
import fr.andross.banitem.Utils.General.Listable;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Map that contains the blacklisted items and their options
 * @version 2.0
 * @author Andross
 */
public final class Blacklist extends HashMap<World, Map<BannedItem, Map<BanOption, BanOptionData>>> {
    private final BanItem pl;

    /**
     * Constructor for a blacklist map
     * @param pl the main instance
     * @param section {@link ConfigurationSection} which contains the blacklist node
     * @param sender {@link CommandSender} who to send the debug messages
     */
    public Blacklist(@NotNull final BanItem pl, @NotNull final CommandSender sender, @Nullable final ConfigurationSection section) {
        this.pl = pl;

        // Loading blacklist
        if (section == null) return;

        for (final String worldKey : section.getKeys(false)) { // Looping through worlds
            // Getting world(s)
            final List<World> worlds = pl.getUtils().getList(Listable.Type.WORLD, worldKey, new Debug(pl, sender, new DebugMessage(null, "config.yml"), new DebugMessage(null, "blacklist"), new DebugMessage(Listable.Type.WORLD, worldKey)));
            if (worlds.isEmpty()) continue;

            // Checking the banned item
            final ConfigurationSection materialsCs = section.getConfigurationSection(worldKey);
            if (materialsCs == null) continue;
            for (final String materialKey : materialsCs.getKeys(false)) {
                // Preparing debugger
                final Debug d = new Debug(pl, sender, new DebugMessage(null, "config.yml"), new DebugMessage(null, "blacklist"), new DebugMessage(Listable.Type.WORLD, worldKey), new DebugMessage(Listable.Type.ITEM, materialKey));

                // Getting Items
                final List<BannedItem> items = pl.getUtils().getList(Listable.Type.ITEM, materialKey, d);
                if (items.isEmpty()) continue;

                // Getting Options
                final ConfigurationSection optionsCs = materialsCs.getConfigurationSection(materialKey);
                final Map<BanOption, BanOptionData> options = pl.getUtils().getBanOptionsFromItemSection(optionsCs, d);
                if (options.isEmpty()) continue;

                // Adding into the map
                for (final World w : worlds)
                    for (final BannedItem item : items)
                        addNewBan(w, item, options);
            }
        }
    }

    /**
     * This will add a new entry to the blacklist
     * @param world bukkit world <i>({@link World})</i>
     * @param item banned item <i>({@link BannedItem})</i>
     * @param map map containing {@link BanOption} and their respective {@link BanOptionData}
     */
    public void addNewBan(@NotNull final World world, @NotNull final BannedItem item, @Nullable final Map<BanOption, BanOptionData> map) {
        final Map<BannedItem, Map<BanOption, BanOptionData>> newmap = getOrDefault(world, new HashMap<>());
        newmap.put(item, map);
        put(world, newmap);
    }

    /**
     * @param world bukkit world <i>({@link World})</i>
     * @param item banned item <i>({@link BannedItem})</i>
     * @param option the ban option type asked <i>({@link BanOption})</i>
     * @return the {@link BanOptionData} object if the item is banned, or null if there is no banned option for this item with this option in this world
     */
    @Nullable
    public BanOptionData getBanOption(@NotNull final World world, @NotNull final BannedItem item, @NotNull final BanOption option) {
        final Map<BanOption, BanOptionData> map = getBanOptions(world, item);
        return map == null ? null : map.get(option);
    }

    /**
     * Trying to get the ban options with their respective ban options data for this item in the said world
     * @param world bukkit world <i>({@link World})</i>
     * @param item banned item <i>({@link BannedItem})</i>
     * @return a map containing the ban option types and their respective ban options, or null if this item is not banned in this world
     */
    @Nullable
    public Map<BanOption, BanOptionData> getBanOptions(@NotNull final World world, @NotNull final BannedItem item) {
        // World does not contains any banned item?
        if (!containsKey(world)) return null;
        // Getting map, from a Banned Item with ItemMeta, else without
        final Map<BannedItem, Map<BanOption, BanOptionData>> map = get(world);
        return map.get(item);
    }

    /**
     * Check if the item is banned.
     * <b>Does not consider permission!</b> <i>(You'll have to use {@link BanUtils#hasPermission(HumanEntity, String, String, BanOption, BanData...)})</i>
     * @param player player involved
     * @param item the banned item
     * @param sendMessage send a message to the player if banned
     * @param option ban option
     * @param data optional ban data
     * @return true if the item is blacklisted for the player world, otherwise false
     */
    public boolean isBlacklisted(@NotNull final Player player, @NotNull final BannedItem item, final boolean sendMessage, @NotNull final BanOption option, @Nullable final BanData... data) {
        /* Checking blacklisted */
        // Checking by item (can include meta)
        Map<BanOption, BanOptionData> map = getBanOptions(player.getWorld(), item);
        // Checking by item without meta
        if (map == null) {
            final BannedItem itemType = new BannedItem(item, false);
            map = getBanOptions(player.getWorld(), itemType);
        }

        if (map != null && map.containsKey(option)) { // Blacklisted?
            final BanOptionData blacklisted = map.get(option);
            // Checking custom data
            if (data == null || Arrays.stream(data).allMatch(blacklisted::contains)) {
                // Checking creative data?
                if (blacklisted.containsKey(BanDataType.GAMEMODE)) {
                    final Set<GameMode> set = blacklisted.getData(BanDataType.GAMEMODE);
                    if (set != null && !set.contains(player.getGameMode())) return false;
                }

                // Checking cooldown?
                if (blacklisted.containsKey(BanDataType.COOLDOWN)) {
                    final long cooldown = (long) blacklisted.get(BanDataType.COOLDOWN);
                    final Map<UUID, Long> cooldowns = blacklisted.getCooldowns();

                    // Not in cooldown? Adding!
                    if (!cooldowns.containsKey(player.getUniqueId())) {
                        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + cooldown);
                        return false;
                    }

                    // Checking cooldown
                    final long playerCooldown = cooldowns.get(player.getUniqueId());

                    // Not in cooldown anymore?
                    if (playerCooldown < System.currentTimeMillis()) {
                        cooldowns.remove(player.getUniqueId()); // not in cooldown anymore, cleaning up'
                        return false;
                    }

                    // Still in cooldown
                    if (sendMessage) {
                        // Calling event?
                        if (pl.getBanConfig().isUseEventApi()) {
                            final PlayerBanItemEvent e = new PlayerBanItemEvent(player, PlayerBanItemEvent.Type.BLACKLIST, item, option, blacklisted, data);
                            Bukkit.getPluginManager().callEvent(e);
                            if (e.isCancelled()) return false;
                        }

                        // Sending message
                        final List<String> message = blacklisted.getData(BanDataType.MESSAGE);
                        if (message != null)
                            message.stream().map(m -> m.replace("{time}", pl.getUtils().getCooldownString(playerCooldown - System.currentTimeMillis()))).forEach(player::sendMessage);
                        return true;
                    }
                }

                // Calling event?
                if (pl.getBanConfig().isUseEventApi()) {
                    final PlayerBanItemEvent e = new PlayerBanItemEvent(player, PlayerBanItemEvent.Type.BLACKLIST, item, option, blacklisted, data);
                    Bukkit.getPluginManager().callEvent(e);
                    if (e.isCancelled()) return false;
                }

                // Checking delete?
                if (map.containsKey(BanOption.DELETE))
                    Bukkit.getScheduler().runTask(BanItem.getInstance(), () -> pl.getUtils().deleteItemFromInventory(player, player.getInventory()));

                if (sendMessage) {
                    String itemName = pl.getBanDatabase().getCustomItems().getName(item);
                    if (itemName == null) itemName = item.getType().name();
                    pl.getUtils().sendMessage(player, itemName, option, blacklisted);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * This method is used to check if the item is banned, not involving a player
     * This method is mainly used to check dispensers <i>dispense</i> and hoppers <i>transfer</i>
     * @param world bukkit world
     * @param item the banned item
     * @param option ban option
     * @param data optional ban data
     * @return true if the item is blacklisted for the player world, otherwise false
     */
    public boolean isBlacklisted(@NotNull final World world, @NotNull final BannedItem item, @NotNull final BanOption option, @Nullable final BanData... data) {
        /* Checking blacklisted */
        // Checking by item (can include meta)
        Map<BanOption, BanOptionData> map = getBanOptions(world, item);
        // Checking by item without meta
        if (map == null) {
            final BannedItem itemType = new BannedItem(item, false);
            map = getBanOptions(world, itemType);
        }

        if (map != null && map.containsKey(option)) { // Blacklisted?
            final BanOptionData blacklisted = map.get(option);
            // Checking custom data
            return data == null || Arrays.stream(data).allMatch(blacklisted::contains);
        }
        return false;
    }

    /**
     * @return the total amount of banned items in all worlds
     */
    public int getTotal() {
        int count = 0;
        for (final Map<BannedItem, Map<BanOption, BanOptionData>> map : values()) count += map.size();
        return count;
    }
}
