package com.gmail.panmpan.BountyPlugin;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

public class Bounty {
	static UUID target;
	static Date start_at;
	static ItemStack price;
	static ArrayList<UUID> bad_killers = new ArrayList<UUID>();
}
