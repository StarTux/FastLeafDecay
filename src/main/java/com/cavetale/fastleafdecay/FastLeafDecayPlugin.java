package com.cavetale.fastleafdecay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class FastLeafDecayPlugin extends JavaPlugin implements Listener {
    private final Set<String> onlyInWorlds = new HashSet<>();
    private final Set<String> excludeWorlds = new HashSet<>();
    private long breakDelay;
    private long decayDelay;
    private boolean spawnParticles;
    private boolean playSound;
    private boolean oneByOne;
    private final List<Block> scheduledBlocks = new ArrayList<>();
    private static final List<BlockFace> NEIGHBORS = Arrays
        .asList(BlockFace.UP,
                BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
                BlockFace.DOWN);

    @Override
    public void onEnable() {
        // Load config
        reloadConfig();
        saveDefaultConfig();
        onlyInWorlds.addAll(getConfig().getStringList("OnlyInWorlds"));
        excludeWorlds.addAll(getConfig().getStringList("ExcludeWorlds"));
        breakDelay = getConfig().getLong("BreakDelay");
        decayDelay = getConfig().getLong("DecayDelay");
        oneByOne = getConfig().getBoolean("OneByOne");
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
        if (!Tag.LOGS.isTagged(oldBlock.getType())
            && !Tag.LEAVES.isTagged(oldBlock.getType())) {
            return;
        }
        final String worldName = oldBlock.getWorld().getName();
        if (!onlyInWorlds.isEmpty() && !onlyInWorlds.contains(worldName)) return;
        if (excludeWorlds.contains(worldName)) return;
        // No return
        Collections.shuffle(NEIGHBORS);
        for (BlockFace neighborFace: NEIGHBORS) {
            final Block block = oldBlock.getRelative(neighborFace);
            if (!Tag.LEAVES.isTagged(block.getType())) continue;
            Leaves leaves = (Leaves) block.getBlockData();
            if (leaves.isPersistent()) continue;
            if (scheduledBlocks.contains(block)) continue;
            if (oneByOne) {
                if (scheduledBlocks.isEmpty()) {
                    getServer().getScheduler().runTaskLater(this, this::decayOne, delay);
                }
                scheduledBlocks.add(block);
            } else {
                getServer().getScheduler().runTaskLater(this, () -> decay(block), delay);
            }
            scheduledBlocks.add(block);
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
     *
     * @return true if the block was decayed, false otherwise.
     */
    private boolean decay(Block block) {
        if (!scheduledBlocks.remove(block)) return false;
        if (!block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) return false;
        if (!Tag.LEAVES.isTagged(block.getType())) return false;
        Leaves leaves = (Leaves) block.getBlockData();
        if (leaves.isPersistent()) return false;
        if (leaves.getDistance() < 7) return false;
        LeavesDecayEvent event = new LeavesDecayEvent(block);
        getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;
        if (spawnParticles) {
            block.getWorld()
                .spawnParticle(Particle.BLOCK,
                               block.getLocation().add(0.5, 0.5, 0.5),
                               8, 0.2, 0.2, 0.2, 0,
                               block.getType().createBlockData());
        }
        if (playSound) {
            block.getWorld().playSound(block.getLocation(),
                                       Sound.BLOCK_GRASS_BREAK,
                                       SoundCategory.BLOCKS, 0.05f, 1.2f);
        }
        block.breakNaturally();
        return true;
    }

    /**
     * Decay one block from the list of scheduled blocks. Schedule the
     * same function again if the list is not empty.
     * This gets called if OneByOne is activated in the
     * config. Therefore, we wait at least one tick.
     *
     * This could undermine the BlockDelay if the DecayDelay
     * significantly smaller and the list devoid of valid leaf blocks.
     */
    private void decayOne() {
        boolean decayed = false;
        do {
            if (scheduledBlocks.isEmpty()) return;
            Block block = scheduledBlocks.get(0);
            decayed = decay(block); // Will remove block from list.
        } while (!decayed);
        if (!scheduledBlocks.isEmpty()) {
            long delay = decayDelay;
            if (delay <= 0) delay = 1L;
            getServer().getScheduler().runTaskLater(this, this::decayOne, delay);
        }
    }
}
