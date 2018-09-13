package com.gmail.panmpan.BountyPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.ChatColor;

public class BountyPlugin extends JavaPlugin implements Listener {
	// player skull
	private ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
	private SkullMeta meta_skull = (SkullMeta) skull.getItemMeta();
	
	//	change by config yaml
	int RANDOM_NEXT = 8;
	long TIMER_INTERVAL = 3600 * 20;
	float HEADDROP_CHANCE = (float) 2.0;
	List<ItemStack> price_list = new ArrayList<ItemStack>();
	
	// repeat work
	BukkitScheduler scheduler = getServer().getScheduler();
	BukkitTask repeat_task = null;
	BukkitTask bounty_task = null;
	int bounty_cooldown = ThreadLocalRandom.current().nextInt(1, RANDOM_NEXT + 1);
	final long ONE_MINUTE_IN_MILLIS = 60000;

	Bounty bounty = null;
	List<UUID> fugitives = new ArrayList<UUID>();
	
	@Override
	public void onEnable() {
		getLogger().info("BountyPlugin 開始");
		
		parseConfigYml();
		bounty_cooldown = ThreadLocalRandom.current().nextInt(1, RANDOM_NEXT + 1);
		
		getServer().getPluginManager().registerEvents(this, this);

		/*repeat_task = scheduler.runTaskTimer(this, new Runnable () {
			public void run() {
				if (bounty_cooldown == 0) {
					Collection<Player> player_list = (Collection<Player>) getServer().getOnlinePlayers();
					
					if ((player_list.size() >= 2) && (bounty == null)) {
						newBounty(player_list);
					}
					bounty_cooldown = ThreadLocalRandom.current().nextInt(1, RANDOM_NEXT + 1);
				}
				else {
					bounty_cooldown--;
				}
			}
		}, 0, TIMER_INTERVAL);*/
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (sender instanceof Player) {
			if (cmd.getName().equalsIgnoreCase("startbounty")) {
				Player player = (Player) sender;
				if (player.hasPermission("op")) {
					Collection<Player> player_list = (Collection<Player>) getServer().getOnlinePlayers();
					
					if ((player_list.size() >= 2) && (bounty == null)) {
						newBounty(player_list);
					}
				}
				else {
					player.sendMessage(ChatColor.RED + "你沒有權限");
				}
			}
		}
		return true;
	}
	
	public void newBounty(Collection<Player> player_list) {
		if (bounty != null) {
			return;
		}
//		Pick random player as a target
		Player target = null;
		int random_index = new Random().nextInt(player_list.size());
		int index = 0;
		for (Player player: player_list) {
			if (index == random_index) {
				target = player;
			}
			index++;
		}
		
		bounty = new Bounty();
		bounty.target = target.getUniqueId();
		bounty.price = pickPrice();
		bounty.start_at = new Date();;
		
		Bukkit.broadcastMessage(ChatColor.GOLD + "[通契令]");
		Bukkit.broadcastMessage(ChatColor.GOLD + "捉拿 " + target.getDisplayName());
		
		if (bounty_task != null) {
			bounty_task.cancel();
		}
		bounty_task = scheduler.runTaskLater(this, new Runnable () {
			public void run() {
				if (bounty != null) {
					Player target = Bukkit.getPlayer(bounty.target);
					target.setHealth(target.getMaxHealth());
					double bad_killer_size = bounty.bad_killers.size();
					if (bad_killer_size > 0) {
						bad_killer_size++;
						Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[通契令結束] " + target.getDisplayName() +
							" 逃走了 ! 他拿到了 " + String.valueOf(bad_killer_size) + " 倍的賞金");
						bounty.price.setAmount((int) (bounty.price.getAmount() * bad_killer_size)); 
						target.getInventory().addItem(bounty.price);
					}
					else {
						Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[通契令結束] " + target.getDisplayName() + " 逃走了 !");
					}
					bounty = null;
				}
			}
			
		}, 3000L);
	}
	
	public ItemStack pickPrice() {
		
		return price_list.get(0);
	}
	
	public void parseConfigYml() {
		this.saveDefaultConfig();
		FileConfiguration config = this.getConfig();
		TIMER_INTERVAL = config.getLong("interval") * 20;
		RANDOM_NEXT = config.getInt("random_next");
		HEADDROP_CHANCE = (float) config.getInt("head_drop_change");
		
		List<LinkedHashMap> price_config = (List<LinkedHashMap>) config.getList("bounty_price");
		//getLogger().info(price_config.get(0).getClass().getName());
		
		for (LinkedHashMap price: price_config) {
			if (price.containsKey("type") && price.containsKey("amount")) {
				String type = (String) price.get("type");
				int amount = Integer.valueOf((String) price.get("amount"));
				
				price_list.add(new ItemStack(Material.getMaterial(type), amount));
			}
		}
		
//		price_list.add(new ItemStack(Material.GOLD_INGOT, amount));
		
//		getLogger().info("I: " + String.valueOf(TIMER_INTERVAL) + " R:" + String.valueOf(RANDOM_NEXT) +
//			" H:" + String.valueOf(HEADDROP_CHANCE));
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (event.getEntity().getType() == EntityType.PLAYER) {
			Player target = (Player) event.getEntity();
			Player killer = (Player) event.getEntity().getKiller();
			DamageCause death_cause = target.getLastDamageCause().getCause();
			
			if (bounty != null) {
				if (killer != null) {
					if (bounty.target == target.getUniqueId()) {
						if (bounty.target == killer.getUniqueId()) {
							Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[通緝令結束] " + target.getDisplayName() + " 畏罪自殺了 !");
							bounty_task.cancel();
							bounty_task = null;
							bounty = null;
						}
						else {
							killer.getInventory().addItem(bounty.price);
							Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[通緝令結束] " + target.getDisplayName() +
								" 被 " + killer.getDisplayName() + " 處刑了 ! 賞金被拿走了 !");
							bounty_task.cancel();
							bounty_task = null;
							bounty = null;
						}
					}
					else if (bounty.target == killer.getUniqueId()) {
						Bukkit.broadcastMessage(ChatColor.RED + target.getDisplayName() + " 慘遭通緝犯 " + killer.getDisplayName() + " 殘殺 !");
						bounty.bad_killers.add(target.getUniqueId());
					}
				}
				else {
					Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[通緝令結束] " + target.getDisplayName() + " 在逃跑中死亡了 !");
					bounty_task.cancel();
					bounty_task = null;
					bounty = null;
				}
			}
			if (killer != null) {
				float chance = ThreadLocalRandom.current().nextInt(1, (int) HEADDROP_CHANCE + 1);

				getLogger().info(String.valueOf(chance));
				getLogger().info(String.valueOf(HEADDROP_CHANCE));
				if (chance == HEADDROP_CHANCE) {
					meta_skull.setOwner(target.getName());
					skull.setItemMeta(meta_skull);
					
					killer.getInventory().addItem(skull);
					
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (fugitives.contains(player.getUniqueId())) {
			Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "逃犯 " + player.getDisplayName() + " 被抓到了 ! 等級重置處罰 !");
			player.giveExpLevels(-1000);
			fugitives.remove(player.getUniqueId());
		}
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (bounty != null) {
			if (bounty.target == player.getUniqueId()) {
				Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "[通緝令結束] " + player.getDisplayName() + " 逃到國外了 !");
				fugitives.add(player.getUniqueId());
				bounty_task.cancel();
				bounty_task = null;
				bounty = null;
			}
		}
	}
}
