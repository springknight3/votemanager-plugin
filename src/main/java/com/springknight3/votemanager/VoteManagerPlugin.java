package com.springknight3.votemanager;

import org.bukkit.plugin.java.JavaPlugin;

public final class VoteManagerPlugin extends JavaPlugin {
	@Override
	public void onEnable() {
		getCommand("redeem").setExecutor((sender, command, label, args) -> true);
	}
}