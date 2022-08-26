package me.halfquark.activesigns;

import org.bukkit.plugin.java.JavaPlugin;

public class ActiveSigns extends JavaPlugin {
	
	public static ActiveSigns instance;
	
	@Override
	public void onEnable() {
		instance = this;
		this.saveDefaultConfig();
		getConfig().options().copyDefaults(true);
		getServer().getPluginManager().registerEvents(new RedstoneListener(), this);
	}
	
}
