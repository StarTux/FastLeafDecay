package com.cavetale.fastleafdecay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Description;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.Website;
import org.bukkit.plugin.java.annotation.plugin.author.Author;

@Plugin(name = "FastLeafDecay", version = "1.0")
@Description("Speed up Leaf Decay")
@ApiVersion(ApiVersion.Target.v1_13)
@Author("StarTux")
@Website("https://cavetale.com")
public final class FastLeafDecayPlugin extends JavaPlugin implements Listener {
    private final Set<String> onlyInWorlds = new HashSet<>();
    private final Set<String> excludeWorlds = new HashSet<>();
    private long breakDelay, decayDelay;
    private boolean spawnParticles, playSound;
    private final Set<Block> scheduledBlocks = new HashSet<>();
    private static final BlockFace[] NEIGHBORS = {BlockFace.UP, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.DOWN};
    private static final int PLAYER_PLACED_BIT = 4;

    @Override
    public void onEnable() {
        // Load config
        reloadConfig();
        saveDefaultConfig();
        onlyInWorlds.addAll(getConfig().getStringList("OnlyInWorlds"));
        excludeWorlds.addAll(getConfig().getStringList("ExcludeWorlds"));
        breakDelay = getConfig().getLong("BreakDelay");
        decayDelay = getConfig().getLong("DecayDelay");
        spawnParticles = getConfig().getBoolean("SpawnParticles");
        playSound = getConfig().getBoolean("PlaySound");
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Clean up
        scheduledBlocks.clear();
    }

    /**
     * Whenever a player breaks a log or leaves block, there is a chance
     * that its surrounding blocks should also decay.  We could just
     * wait for the first leaves to decay naturally, but this way, the
     * instant feedback will avoid confusion for players.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        onBlockRemove(event.getBlock(), breakDelay);
    }

    /**
     * Leaves decay has a tendency to cascade.  Whenever leaves decay,
     * we want to check its neighbors to find out if they will also
     * decay.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onLeavesDecay(LeavesDecayEvent event) {
        onBlockRemove(event.getBlock(), decayDelay);
    }

    private boolean isLog(Material mat) {
        switch (mat) {
        case LOG: case LOG_2: return true;
        default: return false;
        }
    }

    private boolean isLeaf(Material mat) {
        switch (mat) {
        case LEAVES: case LEAVES_2: return true;
        default: return false;
        }
    }

    private boolean isPersistent(Block block) {
        return ((int)block.getData() & PLAYER_PLACED_BIT) != 0;
    }

    private int getDistance(Block block) {
        List<Block> todo = new ArrayList<>();
        todo.add(block);
        Set<Block> done = new HashSet<>();
        Map<Block, Integer> distances = new HashMap<>();
        distances.put(block, 0);
        while (!todo.isEmpty()) {
            Block current = todo.remove(0);
            done.add(current);
            int distance = distances.get(current);
            if (distance >= 5) return distance;
            for (BlockFace face: new BlockFace[] {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
                Block nbor = current.getRelative(face);
                if (done.contains(nbor)) continue;
                if (isLog(nbor.getType())) {
                    return distance + 1;
                } else if (isLeaf(nbor.getType())) {
                    todo.add(nbor);
                    distances.put(nbor, distance + 1);
                }
            }
        }
        return 5;
    }

    /**
     * Check if block is either leaves or a log and whether any of the
     * blocks surrounding it are non-persistent leaves blocks.  If so,
     * schedule their respective removal via
     * {@link #decay(Block) block()}.  The latter will perform all
     * necessary checks, including distance.
     *
     * @param oldBlock the block
     * @param delay the delay of the scheduled check, in ticks
     */
    private void onBlockRemove(final Block oldBlock, long delay) {
        if (!isLog(oldBlock.getType()) && !isLeaf(oldBlock.getType())) return;
        final String worldName = oldBlock.getWorld().getName();
        if (!onlyInWorlds.isEmpty() && !onlyInWorlds.contains(worldName)) return;
        if (excludeWorlds.contains(worldName)) return;
        for (BlockFace neighborFace: NEIGHBORS) {
            final Block block = oldBlock.getRelative(neighborFace);
            if (!isLeaf(block.getType())) continue;
            if (isPersistent(block)) continue;
            if (scheduledBlocks.contains(block)) continue;
            scheduledBlocks.add(block);
            getServer().getScheduler().runTaskLater(this, () -> decay(block), delay);
        }
    }

    /**
     * Decay if it is a leaves block and its distance the nearest log
     * block is 7 or greater.
     *
     * This method may only be called by a scheduler if the given
     * block has previously been added to the scheduledBlocks set,
     * from which it will be removed.
     *
     * This method calls {@link LeavesDecayEvent} and will not act if
     * the event is cancelled.
     * @param block The block
     */
    private void decay(Block block) {
        if (!scheduledBlocks.remove(block)) return;
        if (!isLeaf(block.getType())) return;
        if (isPersistent(block)) return;
        if (getDistance(block) <= 4) return;
        LeavesDecayEvent event = new LeavesDecayEvent(block);
        getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        if (spawnParticles) {
            block.getWorld().playEffect(block.getLocation(), Effect.TILE_BREAK, Material.GRASS.getId());
        }
        if (playSound) {
            block.getWorld().playSound(block.getLocation(), Sound.DIG_GRASS, 0.05f, 1.2f);
        }
        block.breakNaturally();
    }
}
