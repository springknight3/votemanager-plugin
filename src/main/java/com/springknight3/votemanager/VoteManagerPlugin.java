package com.springknight3.votemanager;

import java.util.Objects;
import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class VoteManagerPlugin extends JavaPlugin {
	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadConfig();
		File configFile = new File(getDataFolder(), "config.yml");
		getLogger().info("[DEBUG] Config path: " + configFile.getAbsolutePath());
		getLogger().info("[DEBUG] Config readable: " + configFile.canRead()
				+ ", writable: " + configFile.canWrite());
		getLogger().info("[DEBUG] Loaded messages: redeemable_item_message='"
				+ getConfig().getString("redeemable_item_message")
				+ "', unredeemable_item_message='"
				+ getConfig().getString("unredeemable_item_message")
				+ "', redeemable_item_error='"
				+ getConfig().getString("redeemable_item_error") + "'");
		var redeemCommand = getCommand("redeem");
		if (redeemCommand == null) {
			getLogger().severe("[DEBUG] /redeem is missing from plugin.yml");
			return;
		}

		getLogger().info("[DEBUG] VoteManager enabled; registering /redeem");
		redeemCommand.setExecutor((sender, command, label, args) -> {
			getLogger().info("[DEBUG] /redeem issued by " + sender.getName());
			if (!(sender instanceof Player player)) {
				getLogger().info("[DEBUG] Sender is not a player; no item check performed");
				return true;
			}

			var item = player.getInventory().getItemInMainHand();
			boolean redeemable = hasRedeemableNbt(item);
			getLogger().info("[DEBUG] " + player.getName() + " holds " + item.getType()
					+ " and vote:redeemable is " + (redeemable ? "present" : "absent"));

			if (!redeemable) {
				sendConfiguredMessage(player, "unredeemable_item_message");
				return true;
			}

			String onRedeemCommand = getOnRedeemCommand(item);
			if (onRedeemCommand == null || onRedeemCommand.isBlank()) {
				getLogger().info("[DEBUG] No on_redeem command found");
				sendConfiguredMessage(player, "redeemable_item_error");
				return true;
			}

			String commandToExecute = onRedeemCommand.replace("<user>", player.getName());
			getLogger().info("[DEBUG] Executing on_redeem command: " + commandToExecute);
			boolean commandSucceeded = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
			if (!commandSucceeded) {
				getLogger().warning("[DEBUG] on_redeem command failed");
				sendConfiguredMessage(player, "redeemable_item_error");
				return true;
			}

			item.setAmount(item.getAmount() - 1);
			sendConfiguredMessage(player, "redeemable_item_message");
			getLogger().info("[DEBUG] Redeemed item and removed one item from " + player.getName());
			return true;
		});
	}

	private void sendConfiguredMessage(Player player, String key) {
		String message = Objects.requireNonNullElse(getConfig().getString(key), "");
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
		getLogger().info("[DEBUG] Sent " + key + " to " + player.getName());
	}

	private boolean hasRedeemableNbt(ItemStack item) {
		return getVoteNbt(item, "redeemable") != null;
	}

	private String getOnRedeemCommand(ItemStack item) {
		Object onRedeemTag = getVoteNbt(item, "on_redeem");
		if (onRedeemTag == null) {
			return null;
		}

		String tagText = onRedeemTag.toString();
		if (tagText.length() < 2 || tagText.charAt(0) != '"' || tagText.charAt(tagText.length() - 1) != '"') {
			getLogger().warning("[DEBUG] vote:on_redeem is not a string NBT value: " + tagText);
			return null;
		}

		return tagText.substring(1, tagText.length() - 1)
				.replace("\\\"", "\"")
				.replace("\\\\", "\\");
	}

	private Object getVoteNbt(ItemStack item, String key) {
		try {
			Class<?> craftItemStack = Class.forName("org.bukkit.craftbukkit.inventory.CraftItemStack");
			Object nmsItem = craftItemStack.getMethod("asNMSCopy", ItemStack.class).invoke(null, item);
			Class<?> dataComponentType = Class.forName("net.minecraft.core.component.DataComponentType");
			Class<?> dataComponents = Class.forName("net.minecraft.core.component.DataComponents");
			Object customDataType = dataComponents.getField("CUSTOM_DATA").get(null);
			Object customData = nmsItem.getClass().getMethod("get", dataComponentType)
					.invoke(nmsItem, customDataType);
			if (customData == null) {
				return null;
			}

			Object rootTag = customData.getClass().getMethod("copyTag").invoke(customData);
			Object voteTag = rootTag.getClass().getMethod("get", String.class).invoke(rootTag, "vote");
			if (voteTag == null) {
				return null;
			}

			return voteTag.getClass().getMethod("get", String.class).invoke(voteTag, key);
		} catch (ReflectiveOperationException exception) {
			getLogger().warning("[DEBUG] Could not inspect raw custom_data NBT: " + exception.getMessage());
			return null;
		}
	}
}