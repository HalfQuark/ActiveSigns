package me.halfquark.activesigns;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;

public class RedstoneListener implements Listener {
	
	private static List<BlockFace> BlockFaceList = Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);
	private HashMap<Block, Long> lastActive = new HashMap<Block, Long>();
	private HashMap<Block, Integer> staticSignals = new HashMap<Block, Integer>();
	
	//Piston extension
	@EventHandler(priority = EventPriority.LOW)
	public void redstoneEvent(BlockRedstoneEvent e) {
		if(staticSignals.containsKey(e.getBlock())) {
			if(e.getNewCurrent() < staticSignals.get(e.getBlock()))
				e.setNewCurrent(staticSignals.get(e.getBlock()));
		}
		if(e.getNewCurrent() == 0 || e.getOldCurrent() > 0)
			return;
		for(BlockFace bf : BlockFaceList) {
			if(!e.getBlock().getRelative(bf).getType().equals(Material.SIGN_POST) &&
			   !e.getBlock().getRelative(bf).getType().equals(Material.WALL_SIGN))
				continue;
			if(isFacePowered(e.getBlock().getRelative(bf), bf.getOppositeFace()))
				powerSign(e.getBlock().getRelative(bf), bf);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOW)
	public void signEvent(SignTranslateEvent e) {
		World world = e.getCraft().getW();
		if(e.getLine(0).equalsIgnoreCase("Speed:")){
			int speed = (int) (e.getCraft().getSpeed() / 0.25d);
			if(speed > 15)
				speed = 15;
			for(MovecraftLocation ml : e.getLocations()) {
				Block signBlock = ml.toBukkit(world).getBlock();
				for(BlockFace bf : BlockFaceList) {
					if(signBlock.getRelative(bf).getType().equals(Material.REDSTONE_WIRE)) {
						staticSignals.put(signBlock.getRelative(bf), speed);
						signBlock.getRelative(bf).setTypeIdAndData(55, (byte) speed, true);
					}
				}
			}
			return;
		}
		if(e.getLine(0).equalsIgnoreCase("Status:")) {
			for(MovecraftLocation ml : e.getLocations()) {
				Block signBlock = ml.toBukkit(world).getBlock();
				int strength;
				Sign sign = (Sign) signBlock.getState();
				try {
					String[] stats = ChatColor.stripColor(sign.getLine(1)).replace("/", " ").split("\\s+");
					if(signBlock.getType().equals(Material.WALL_SIGN)) {
						strength = Math.min(15, Integer.parseInt(stats[1]) - Integer.parseInt(stats[2]) + 1);
					} else {
						strength = Math.min(15, Integer.parseInt(stats[4]) - Integer.parseInt(stats[5]));
					}
				} catch(Exception excp) {
					return;
				}
				for(BlockFace bf : BlockFaceList) {
					if(signBlock.getRelative(bf).getType().equals(Material.REDSTONE_WIRE)) {
						staticSignals.put(signBlock.getRelative(bf), strength);
						signBlock.getRelative(bf).setTypeIdAndData(55, (byte) strength, true);
					}
				}
			}
			return;
		}
		if(e.getLine(0).equalsIgnoreCase("Contacts:")) {
			MovecraftLocation shipMid = e.getCraft().getHitBox().getMidPoint();
			for(MovecraftLocation ml : e.getLocations()) {
				Block signBlock = ml.toBukkit(world).getBlock();
				if(signBlock.getType().equals(Material.WALL_SIGN)) {
					int minRange = -1;
					for(Craft contact : e.getCraft().getContacts()) {
						int dist = contact.getHitBox().getMidPoint().distanceSquared(shipMid);
						if(dist < minRange || minRange == -1) {
							minRange = dist;
						}
					}
					int strength = 0;
					if(minRange >= 0)
						strength = Math.min(15, 300 / (int)Math.sqrt(minRange));
					for(BlockFace bf : BlockFaceList) {
						if(signBlock.getRelative(bf).getType().equals(Material.REDSTONE_WIRE)) {
							staticSignals.put(signBlock.getRelative(bf), strength);
							signBlock.getRelative(bf).setTypeIdAndData(55, (byte) strength, true);
						}
					}
				} else {
					for(BlockFace bf : BlockFaceList) {
						int minRange = 10000;
						for(Craft contact : e.getCraft().getContacts()) {
							int dist = contact.getHitBox().getMidPoint().getX()*bf.getModX() + 
										contact.getHitBox().getMidPoint().getZ()*bf.getModZ() -
										signBlock.getX()*bf.getModX() - signBlock.getZ()*bf.getModZ();
							if(dist < minRange && dist >= 0) {
								minRange = dist;
							}
						}
						int strength = Math.min(15, 300 / minRange);
						if(signBlock.getRelative(bf).getType().equals(Material.REDSTONE_WIRE)) {
							staticSignals.put(signBlock.getRelative(bf), strength);
							signBlock.getRelative(bf).setTypeIdAndData(55, (byte) strength, true);
						}
					}
				}
			}
			return;
		}
	}
	
	@SuppressWarnings("deprecation")
	private void powerSign(Block signBlock, BlockFace powerFace) {
		Long currentTime = System.currentTimeMillis();
		lastActive.entrySet().removeIf(entry->entry.getValue() <= currentTime - ActiveSigns.instance.getConfig().getLong("CooldownMilis"));
		if(lastActive.containsKey(signBlock))
			return;
		lastActive.put(signBlock, currentTime);
		Sign sign = (Sign) signBlock.getState();
		if(ActiveSigns.instance.getConfig().getStringList("Blacklist") != null) {
			for(String blacklistedString : ActiveSigns.instance.getConfig().getStringList("Blacklist")) {
				for(String line : sign.getLines()) {
					if(line.contains(blacklistedString))
						return;
				}
			}
		}
		if(ActiveSigns.instance.getConfig().getStringList("Whitelist") != null) {
			for(String whitelistedString : ActiveSigns.instance.getConfig().getStringList("Whitelist")) {
				boolean present = false;
				for(String line : sign.getLines()) {
					if(line.contains(whitelistedString))
						present = true;
				}
				if(!present)
					return;
			}
		}
		Action action = Action.RIGHT_CLICK_BLOCK;
		if(sign.getType().equals(Material.SIGN_POST)) {
			int signDir = (byte) (signBlock.getData() / 4);
			if((signDir - faceToInt(powerFace) + 4) % 4 >= 2) {
				action = Action.LEFT_CLICK_BLOCK;
			}
		}
		for(Craft craft : CraftManager.getInstance().getCraftsInWorld(sign.getWorld())) {
			if(craft.getHitBox().contains(sign.getX(), sign.getY(), sign.getZ())) {
				PlayerInteractEvent clickEvent = new PlayerInteractEvent(craft.getNotificationPlayer(), action, new ItemStack(0, 1), signBlock, powerFace);
				Bukkit.getPluginManager().callEvent(clickEvent);
				return;
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void translateEvent(CraftTranslateEvent e) {
		Set<Block> kSet = new HashSet<Block>(staticSignals.keySet());
		for(Block block : kSet)
			if(e.getOldHitBox().contains(block.getX(), block.getY(), block.getZ()))
				staticSignals.remove(block);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void rotateEvent(CraftRotateEvent e) {
		Set<Block> kSet = new HashSet<Block>(staticSignals.keySet());
		for(Block block : kSet)
			if(e.getOldHitBox().contains(block.getX(), block.getY(), block.getZ()))
				staticSignals.remove(block);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void releaseEvent(CraftReleaseEvent e) {
		Set<Block> kSet = new HashSet<Block>(staticSignals.keySet());
		for(Block block : kSet)
			if(e.getCraft().getHitBox().contains(block.getX(), block.getY(), block.getZ()))
				staticSignals.remove(block);
		for(Block block : kSet)
			if(block.getType().equals(Material.REDSTONE_WIRE))
				block.setType(Material.REDSTONE_WIRE);
	}
	
	@SuppressWarnings("deprecation")
	private boolean isFacePowered(Block block, BlockFace bf) {
		Block power = block.getRelative(bf);
		Material powMat = power.getType();
		if(powMat.equals(Material.REDSTONE_WIRE))
			return true;
		if(powMat.equals(Material.REDSTONE_TORCH_OFF))
			return true;
		if(powMat.equals(Material.DIODE_BLOCK_OFF) && intToFace(power.getData() % 4).equals(bf.getOppositeFace()))
			return true;
		return false;
	}
	
	private BlockFace intToFace(int bt) {
		return BlockFaceList.get(bt);
	}
	
	private int faceToInt(BlockFace bf) {
		return BlockFaceList.indexOf(bf);
	}
	
}
