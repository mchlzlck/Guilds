package com.blizzardfyre.guilds.objects;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.YamlConfiguration;

import com.blizzardfyre.guilds.GuildMain;
import com.blizzardfyre.guilds.utils.FileUtils;
import com.blizzardfyre.guilds.utils.LocationUtils;

public class Guild {

	private File file;
	private YamlConfiguration yml;
	private String name;
	private UUID leader;
	private String prefix;
	private double balance;
	private HashMap<String, ArrayList<UUID>> rankMembers = new HashMap<String, ArrayList<UUID>>();
	private HashMap<String, ArrayList<String>> rankPerms = new HashMap<String, ArrayList<String>>();
	private ArrayList<Chunk> chunks = new ArrayList<Chunk>();

	/**
	 * @param name
	 *            The name of the guild that you want to load from the file.
	 */
	public Guild(String name) {
		file = new File(GuildMain.getInstance().getDataFolder() + File.separator + "guilds", name + ".yml");
		yml = YamlConfiguration.loadConfiguration(file);
		this.name = name;
		leader = UUID.fromString(yml.getString("leader"));
		prefix = ChatColor.translateAlternateColorCodes('&', yml.getString("prefix")) + " ";
		balance = yml.getDouble("balance");
		if (yml.getConfigurationSection("ranks") != null) {
			for (String string : yml.getConfigurationSection("ranks").getKeys(false)) {
				ArrayList<UUID> players = new ArrayList<UUID>();
				for (String player : yml.getStringList("ranks." + string + ".members")) {
					players.add(UUID.fromString(player));
				}
				rankMembers.put(string, players);
				ArrayList<String> perms = new ArrayList<String>();
				for (String perm : yml.getStringList("ranks." + string + ".permissions")) {
					perms.add(perm);
				}
				rankPerms.put(string, perms);
			}
		}
		if (yml.getStringList("claims") != null)
			for (String string : yml.getStringList("claims")) {
				chunks.add(LocationUtils.stringToChunk(string));
			}
	}

	/**
	 * <p>
	 * Gets the name of the guild defined at the creation of the guild. This
	 * string is liable to change, so storing it by the name is not recommended
	 * as it can lead to issues later on.
	 * 
	 * @return Returns the name of the guild.
	 */
	public String getName() {
		return name;
	}

	/**
	 * <p>
	 * This changes the name of the guild in both the file, and in the main class.
	 * Try to avoid doing this from outside of the Guilds plugin, as it can cause
	 * issues if not done in the proper manner.
	 * @param name The new desired name
	 */
	public void setName(String name) {
		File newFile = new File(file.getAbsolutePath().replace(this.name, name));
		file.delete();
		try {
			newFile.createNewFile();
			yml.save(newFile);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		file = newFile;
	}

	public UUID getLeader() {
		return leader;
	}

	public HashMap<String, ArrayList<UUID>> getRanks() {
		return rankMembers;
	}

	public String getRank(User player) {
		for (String string : rankMembers.keySet()) {
			if (rankMembers.get(string).contains(player.getUniqueId())) {
				return string;
			}
		}
		return null;
	}

	public void addMember(User player) {
		List<UUID> members = (List<UUID>) rankMembers.get(getDefaultRank());
		members.add(player.getUniqueId());
		rankMembers.put(getDefaultRank(), (ArrayList<UUID>) members);
		yml.set("ranks." + getDefaultRank() + ".members", members);
		saveYml();
	}

	public void removeMember(User player) {
		ArrayList<UUID> members = rankMembers.get(getRank(player));
		members.remove(player.getUniqueId());
		rankMembers.put(getRank(player), members);
		yml.set("ranks." + getRank(player) + ".members", members);
		saveYml();
	}

	public String getDefaultRank() {
		for (String string : getRanks().keySet()) {
			if (yml.getBoolean("ranks." + string + ".default")) {
				return string;
			}
		}
		return (String) getRanks().keySet().toArray(new String[getRanks().keySet().size()])[getRanks().keySet().size()];
	}

	public ArrayList<UUID> getMembers(String rank) {
		return rankMembers.get(rank);
	}

	public void claimChunk(Chunk chunk) {
		chunks.add(chunk);
		yml.set("claims", LocationUtils.convertChunks(chunks));
		saveYml();
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String newPrefix) {
		prefix = ChatColor.translateAlternateColorCodes('&', newPrefix) + " ";
		yml.set("prefix", newPrefix);
		saveYml();
	}

	public double getBalance() {
		return balance;
	}

	public void deposit(double d) {
		balance += d;
		yml.set("balance", balance);
		saveYml();
	}

	public void withdraw(double d) {
		balance -= d;
		yml.set("balance", balance);
		saveYml();
	}

	private void saveYml() {
		try {
			yml.save(file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void createGuild(String string, User leader) {
		File file = new File(GuildMain.getInstance().getDataFolder() + File.separator + "guilds", string + ".yml");
		YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
		yml.set("leader", leader.getUniqueId().toString());
		yml.set("balance", 0);
		List<String> blank = new ArrayList<String>();
		yml.set("ranks.member.members", blank);
		blank = new ArrayList<String>();
		yml.set("ranks.member.permissions", blank);
		blank = new ArrayList<String>();
		yml.set("ranks.officer.members", blank);
		blank = new ArrayList<String>();
		yml.set("ranks.officer.permissions", blank);
		try {
			file.createNewFile();
			yml.save(file);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		GuildMain.getInstance().loadGuilds();
	}

	public static boolean exists(String name) {
		for (Guild guild : GuildMain.getInstance().getGuilds()) {
			if (guild.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<Guild> loadGuilds() {
		ArrayList<Guild> guilds = new ArrayList<Guild>();
		for (String string : FileUtils.getGuildFolder().list()) {
			guilds.add(new Guild(string.replace(".yml", "")));
		}
		return guilds;
	}
}