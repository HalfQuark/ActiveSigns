package me.halfquark.activesigns;

import java.util.*;

import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.SubCraft;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.RedstoneWire;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
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
import net.md_5.bungee.api.chat.ClickEvent;

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
			if(!Tag.SIGNS.isTagged(e.getBlock().getRelative(bf).getType()))
				continue;
			if(isFacePowered(e.getBlock().getRelative(bf), bf.getOppositeFace()))
				powerSign(e.getBlock().getRelative(bf), bf);
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOW)
	public void signEvent(SignTranslateEvent e) {
		World world = e.getCraft().getWorld();
		if(e.getLine(0).equalsIgnoreCase("Speed:") && ActiveSigns.instance.getConfig().getBoolean("SpeedOutput")){
			int speed = (int) (e.getCraft().getSpeed() / 0.25d);
			if(speed > 15)
				speed = 15;
			for(MovecraftLocation ml : e.getLocations()) {
				Block signBlock = ml.toBukkit(world).getBlock();
				for(BlockFace bf : BlockFaceList) {
					if(signBlock.getRelative(bf).getType().equals(Material.REDSTONE_WIRE)) {
						staticSignals.put(signBlock.getRelative(bf), speed);
						RedstoneWire blockData = (RedstoneWire) signBlock.getRelative(bf).getBlockData();
						blockData.setPower((byte) speed);
						signBlock.getRelative(bf).setBlockData(blockData, true);
					}
				}
			}
			return;
		}
		if(e.getLine(0).equalsIgnoreCase("Status:") && ActiveSigns.instance.getConfig().getBoolean("StatusOutput")) {
			for(MovecraftLocation ml : e.getLocations()) {
				Block signBlock = ml.toBukkit(world).getBlock();
				int strength;
				Sign sign = (Sign) signBlock.getState();
				try {
					String[] stats = ChatColor.stripColor(sign.getLine(1)).replace("/", " ").split("\\s+");
					if(sign.getBlockData() instanceof WallSign) {
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
						RedstoneWire blockData = (RedstoneWire) signBlock.getRelative(bf).getBlockData();
						blockData.setPower((byte) strength);
						signBlock.getRelative(bf).setBlockData(blockData, true);
					}
				}
			}
			return;
		}
		if(e.getLine(0).equalsIgnoreCase("Contacts:") && ActiveSigns.instance.getConfig().getBoolean("ContactsOutput")) {
			MovecraftLocation shipMid = e.getCraft().getHitBox().getMidPoint();
			for(MovecraftLocation ml : e.getLocations()) {
				Block signBlock = ml.toBukkit(world).getBlock();
				Sign sign = (Sign) signBlock.getState();
				if(sign.getBlockData() instanceof WallSign) {
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
							RedstoneWire blockData = (RedstoneWire) signBlock.getRelative(bf).getBlockData();
							blockData.setPower((byte) strength);
							signBlock.getRelative(bf).setBlockData(blockData, true);
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
							RedstoneWire blockData = (RedstoneWire) signBlock.getRelative(bf).getBlockData();
							blockData.setPower((byte) strength);
							signBlock.getRelative(bf).setBlockData(blockData, true);
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
		if(!(sign.getBlockData() instanceof WallSign)) {
			int signDir = (byte) (signBlock.getData() / 4);
			if((signDir - faceToInt(powerFace) + 4) % 4 >= 2) {
				action = Action.LEFT_CLICK_BLOCK;
			}
		}
		for(Craft craft : CraftManager.getInstance().getCraftsInWorld(sign.getWorld())) {
			if(craft.getHitBox().contains(sign.getX(), sign.getY(), sign.getZ())) {
				Player p;
				if(craft instanceof SubCraft)
					craft = (((SubCraft) craft).getParent());
				if(craft instanceof PlayerCraft){
					p = ((PlayerCraft) craft).getPilot();
				}else{
					continue;
				}
				PlayerInteractEvent clickEvent = new PlayerInteractEvent(p, action, new ItemStack(Material.AIR, 1), signBlock, powerFace);
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
		if(powMat.equals(Material.REDSTONE_TORCH))
			return true;
		if(powMat.equals(Material.REPEATER) && intToFace(power.getData() % 4).equals(bf.getOppositeFace()))
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
