package com.thespuff.plugins.totem;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by nsmith on 2016-12-23.
 */
public class Totem extends JavaPlugin implements Listener{

  public static String pluginName;
  public static String pluginVersion;
  public static Server server;
  public static Totem plugin;
  public static BukkitTask asyncTask;
  private static HashSet<Player> playerOwnerList = new HashSet<Player>();

  public enum Interaction {
    ANVIL,
    BED,
    BREAK,
    CONTAINER,
    DAMAGEBLOCK,
    EXPLODE,
    EXPLODECREEPER,
    EXPLODEFIREBALL,
    EXPLODETNT,
    FIRESPREAD,
    FLY,
    GRASSGROW,
    HEARTHSTONE,
    IGNITE,
    MAGIC,
    MOBHEALTHBOOST,
    MYCELGROW,
    PLACE,
    POISON,
    POTION,
    PVE,
    PVP,
    SHEAR,
    SLAUGHTER,
    SPAWNFRIENDLY,
    SPAWNUNFRIENDLY,
    STONEMACHINE,
    TELEPAD,
    TELEPORT,
    WOODMACHINE;

    public String getName(){
      return this.toString().toLowerCase();
    }
  }

  public void onDisable() {
    this.getServer().getScheduler().cancelTask(asyncTask.getTaskId());//cancelAllTasks();

    this.saveConfig();
    setTotemMetadata(false);

    log("Disabled");
  }

  public void onEnable() {
    pluginName = this.getDescription().getName();
    pluginVersion = this.getDescription().getVersion();
    server = this.getServer();
    plugin = this;
    int secondsPerPulse = 8;

    getServer().getPluginManager().registerEvents(this, this);

    //Let's ensure that everything is lower case in the config file.
    fixConfigCase();

    //Check that all totems are valid totems.
    checkTotemExists();

    setTotemMetadata(true);

//		asyncTaskID = this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() { public void run() { applyFlagEffects(); } }, 60, secondsPerPulse*20);
    asyncTask = this.getServer().getScheduler().runTaskTimerAsynchronously(this, new Runnable() { public void run() { applyFlagEffects(); } }, 60, secondsPerPulse*20);

    log("Enabled.");
  }

  private void setTotemMetadata(boolean set) {
    log("Setting Totem Metadata...");

    HashMap<String, Block> totems = Utils.getTotems();
    Set<String> keys = totems.keySet();
    for(String key : keys){
      Block block = totems.get(key);
      if(set){
        if(block.getMetadata("Totem").size()!=0) { log("Totem is double-marked: "+key); }
        block.setMetadata("Totem", new FixedMetadataValue(this, key));
      } else {
        if(block.getMetadata("Totem").size()==0) { log("Totem not marked: "+key); }
        block.removeMetadata("Totem", this);
      }
    }
  }

  protected void applyFlagEffects() {
    Collection<? extends Player> players = server.getOnlinePlayers();
    //for(int i=0; i<players.length; i++){
    for(Iterator<? extends Player> i=players.iterator(); i.hasNext(); ) {
      Player player = i.next();
      //Player player = players[i];
      if(!player.hasPermission("totems.special.mayFly")) { //Flight
        player.setFlySpeed(.1f);
        if(player.getGameMode()==GameMode.SURVIVAL) {
          player.setAllowFlight(canEdit(player, player.getLocation().getBlock(), Interaction.FLY));
        }
      }
    }
  }

  //Removes totems not marked with a diamond- or gold-block.
  private void checkTotemExists() {
    log("Checking to ensure that all Totems exist...");
    String path="";
    Set<String> worlds = this.getConfig().getConfigurationSection("totems").getKeys(false);
    worlds.remove("defaults");
    for(String world : worlds){
      if(server.getWorld(world)==null) { continue; }
      Set<String> totems = this.getConfig().getConfigurationSection("totems."+world).getKeys(false);
      totems.remove("defaults");
      for(String totem:totems){
        path = "totems."+world+"."+totem;
        int x = getConfig().getInt(path+".x");
        int z = getConfig().getInt(path+".z");
        int y = getConfig().getInt(path+".y");
        Block block = server.getWorld(world).getBlockAt(x, y, z);
        if(!block.getType().equals(Material.GOLD_BLOCK) && !block.getType().equals(Material.DIAMOND_BLOCK)) {
//        if(block.getTypeId()!=57 && block.getTypeId()!=41) {
//					log(getConfig().getString(path+".owner"));
//					block.setTypeIdAndData(35, (byte) 2, false);
          this.getConfig().set(path, null);
          log("Deleted totem "+totem+": Not a legitimate block.");
        }
      }
    }
  }

  private void fixConfigCase() {
    log("Checking config.yml. Lowering cases, raising cases.");
    if(!this.getConfig().contains("totems")) this.saveDefaultConfig();

    Set<String> worlds = this.getConfig().getConfigurationSection("totems").getKeys(false);
    if(worlds.equals(null)) return;

    for(String world : worlds){
      Set<String> totems = this.getConfig().getConfigurationSection("totems."+world).getKeys(false);
      for(String totem : totems){
        List<String> friends = this.getConfig().getStringList("totems."+world+"."+totem+".friends");
        List<String> outfriends = new ArrayList<String>();
        for(String friend:friends) {
          outfriends.add(friend.toLowerCase());
        }
        this.getConfig().set("totems."+world+"."+totem+".friends", outfriends);
      }
    }
  }

  public void log(Object in) {
    System.out.println("[" + pluginName + "] " + String.valueOf(in));
  }

	/* Special totem-related functions ======================================================================*/

  //Breaking a totem
  @EventHandler(priority = EventPriority.HIGHEST)
  public void onTotemBreak(BlockBreakEvent event) {
    try {
      if(event.getBlock().hasMetadata("Totem")) {
        String world = event.getBlock().getWorld().getName();
        String serial = event.getBlock().getMetadata("Totem").get(0).asString();
        String player = event.getPlayer().getName();
        String owner = this.getConfig().getString("totems."+world+"."+serial+".owner");
        if(owner.equalsIgnoreCase(player)) {
          this.getConfig().set("totems."+world+"."+serial, null);
          event.getBlock().removeMetadata("Totem", this);
          event.getPlayer().sendMessage("You broke a totem!");
          saveConfig();
        } else {
          event.setCancelled(true);
          event.getPlayer().sendMessage("You may not break "+owner+"'s totem.");
        }
      }
    } catch (NullPointerException e) {
      return;
    }
  }

  //Building a totem
  @EventHandler (priority = EventPriority.HIGHEST)
  public void onTotemCreate(PlayerInteractEvent event) {
    try {
      if(event.isCancelled()) { return; } //Not allowed to interact. Does this ever get called?
      if(!event.hasBlock()) { return; }
      if(!canEdit(event.getPlayer(),event.getClickedBlock(),Interaction.PLACE)) { return; }
//      this.log(event.getItem().getType().toString());
      if(!event.getItem().getType().equals(Material.GOLD_SPADE)) { return; }
//      if(event.getItem().getTypeId()!=284) { return; } //Gold Spade
      if(event.getClickedBlock().hasMetadata("Totem")) { return; }

      HashMap<String,Boolean> flags = new HashMap<String,Boolean>();
      float radius = 5;

      switch(event.getClickedBlock().getType()){
        case DIAMOND_BLOCK: //Diamond block defaults to the following permissions:
          flags.put("anvil", false);
          flags.put("bed", false);
          flags.put("break", false);
          flags.put("container", false);
          flags.put("explode", false);
          flags.put("firespread", false);
          flags.put("hearthstone", true);
          flags.put("ignite", false);
          flags.put("mobHealthBoost", false);
          flags.put("place", false);
          flags.put("slaughter", false);
          flags.put("stonemachine", false);
          radius = 20f;
          break;
        case GOLD_BLOCK: //Gold block
          flags.put("break", false);
          flags.put("explode", false);
          flags.put("firespread", false);
          flags.put("hearthstone", true);
          flags.put("place", false);
          radius = 3f;
          break;
        default:
          return;
      }
      Set<String> keys = flags.keySet();

      Player player = event.getPlayer();
      Block block = event.getClickedBlock();

      List<String> friends = new ArrayList<String>();

      String serial = (new SimpleDateFormat("yyMMddHHmmss").format(new Date()));


      String path = "totems."+player.getWorld().getName()+"."+serial;
      this.getConfig().set(path+".x", block.getX());
      this.getConfig().set(path+".y", block.getY());
      this.getConfig().set(path+".z", block.getZ());
      this.getConfig().set(path+".radius", radius);
      for(String key:keys){
        this.getConfig().set(path+".flags."+key, flags.get(key));
      }
      this.getConfig().set(path+".friends", friends);
      this.getConfig().set(path+".owner", player.getName());

      block.setMetadata("Totem", new FixedMetadataValue(this, serial));

      player.sendMessage("Totem created!");

    } catch (NullPointerException e) {
      return;
    }
  }

  @EventHandler public void onPlayerChangeState(PlayerMoveEvent event) {
    if(event.getFrom().equals(event.getTo())) { return; }
    if(event.getPlayer().getItemOnCursor().getType().equals(Material.MAP)) {
//    if(event.getPlayer().getItemInHand().getType().equals(Material.MAP)) {
      Player player = event.getPlayer();
      if(isOwner(player.getName(), player.getLocation().getBlock())) {
        if(!playerOwnerList.contains(player)) {
          playerOwnerList.add(player);
          player.sendMessage("You own this area.");
        }
      } else {
        if(playerOwnerList.contains(player)) {
          playerOwnerList.remove(player);
          player.sendMessage("You don't own this area.");
        }
      }
    }

  }

	/* Events to watch for =================================================================================*/

  @EventHandler (priority = EventPriority.LOW) public void onBlockBreak(BlockBreakEvent event) {
//		if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
    if(event.getPlayer().isOp()) { return; }
    event.setCancelled(!canEdit(event.getPlayer(),event.getBlock(),Interaction.BREAK));
  }

  @EventHandler (priority = EventPriority.LOW) public void onBlockSpread(BlockSpreadEvent event) {
    //if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
    if(event.getSource().getType().equals(Material.MYCEL)) { // Id()==110) {
      event.setCancelled(!canEdit("NoPlayerEvent",event.getBlock(),Interaction.MYCELGROW));
//			log(event.isCancelled());
//    } else if(event.getSource().getTypeId()==2) {
    } else if(event.getSource().getType().equals(Material.GRASS)) {
      event.setCancelled(!canEdit("NoPlayerEvent",event.getBlock(),Interaction.GRASSGROW));
    }
  }

  @EventHandler public void onBlockPlace(BlockPlaceEvent event) {
//		if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
    if(event.getPlayer().isOp()) { return; }
    event.setCancelled(!canEdit(event.getPlayer(),event.getBlock(),Interaction.PLACE));
  }

  @EventHandler public void onHangingBreak(HangingBreakEvent event) {
    switch(event.getCause()){
      case EXPLOSION:
        event.setCancelled(!canEdit("NoPlayerEvent",event.getEntity().getLocation().getBlock(),Interaction.EXPLODE));
        break;
      case ENTITY:
        //Deal with this in onHangingBreakByEntity
        break;
      case OBSTRUCTION:
      case PHYSICS:
        event.setCancelled(!canEdit("NoPlayerEvent",event.getEntity().getLocation().getBlock(),Interaction.BREAK));
        break;
      default:
        event.setCancelled(!canEdit("NoPlayerEvent",event.getEntity().getLocation().getBlock(),Interaction.BREAK));
        break;
    }
/*		if(event.getCause().equals(RemoveCause.EXPLOSION)) {
		event.setCancelled(!canEdit("NoPlayerEvent",event.getEntity().getLocation(),Interaction.EXPLODE));
		}
*/	}

  @EventHandler public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
//		if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
    if(!(event.getRemover() instanceof Player)) {
      event.setCancelled(!canEdit("NoPlayerEvent",event.getEntity().getLocation().getBlock(),Interaction.BREAK));
    } else {
      if(((Player) event.getRemover()).isOp()) { return; }
      event.setCancelled(!canEdit((Player) event.getRemover(),event.getEntity().getLocation().getBlock(),Interaction.BREAK));
    }
  }

  @EventHandler public void onHangingPlace(HangingPlaceEvent event) {
//		if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
    if(event.getPlayer().isOp()) { return; }
    event.setCancelled(!canEdit(event.getPlayer(),event.getEntity().getLocation().getBlock(),Interaction.PLACE));
  }

  @EventHandler public void onEntityExplode(EntityExplodeEvent event) {
//		if(event.getEntity() instanceof LivingEntity) { return; }
    event.setCancelled(!canEdit("NoPlayerEvent",event.getLocation().getBlock(),Interaction.EXPLODE));
    if(event.isCancelled()) { return; }
    try{
      if(event.getEntityType().equals(EntityType.CREEPER)) {
        event.setCancelled(!canEdit("NoPlayerEvent",event.getLocation().getBlock(),Interaction.EXPLODECREEPER));
      } else if(event.getEntityType().equals(EntityType.FIREBALL)) {
        event.setCancelled(!canEdit("NoPlayerEvent",event.getLocation().getBlock(),Interaction.EXPLODEFIREBALL));
      } else if(event.getEntityType().equals(EntityType.PRIMED_TNT)) {
        event.setCancelled(!canEdit("NoPlayerEvent",event.getLocation().getBlock(),Interaction.EXPLODETNT));
      }
    } catch(NullPointerException e) { return; }
  }

  @EventHandler public void onPlayerBedEnter(PlayerBedEnterEvent event) {
//		if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
    if(event.getPlayer().isOp()) { return; }
    event.setCancelled(!canEdit(event.getPlayer(),event.getBed(),Interaction.BED));
  }

/*	@EventHandler public void onBlockDamage(BlockDamageEvent event) {
//		if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
		if(event.getPlayer().isOp()) { return; }
		event.setCancelled(!canEdit(event.getPlayer(),event.getBlock().getLocation(),Interaction.DAMAGEBLOCK));
	}
*/

  @EventHandler public void onPlayerInteract(PlayerInteractEvent event) {
//		if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
    if(event.getPlayer().isOp()) { return; }
//    int mat;
    Material mat;
    Block block;
    try{
      block = event.getClickedBlock();
//      mat = block.getTypeId();
      mat = block.getType();
    } catch (NullPointerException e) { //Air will give 'null'.
      return;
    }
    switch (mat) {
      //Wood machines!
      case WOODEN_DOOR: // wood door
      case LEVER: // lever
      case WOOD_PLATE: // wood plate
      case TRAP_DOOR: // trapdoor
      case FENCE_GATE: // gate
      case TRIPWIRE_HOOK: // tripwire hook
      case TRIPWIRE: // tripwire
      case WOOD_BUTTON: // wood button
        event.setCancelled(!canEdit(event.getPlayer(),block,Interaction.WOODMACHINE));
        break;

      //Stone machines!
      case STONE_PLATE: // stone plate
      case STONE_BUTTON: // stone button
        event.setCancelled(!canEdit(event.getPlayer(),block,Interaction.STONEMACHINE));
        break;

      //Containers!
      case DISPENSER: // dispenser
      case CHEST: // chest
      case FURNACE: // furnace
      case BURNING_FURNACE: // burning furnace
      case JUKEBOX: // jukebox
      case BREWING_STAND: // brewing stand
        event.setCancelled(!canEdit(event.getPlayer(),block,Interaction.CONTAINER));
        break;

      //Anvil
      case ANVIL:
        event.setCancelled(!canEdit(event.getPlayer(),block,Interaction.ANVIL));
        break;

      //TelePad
      case OBSIDIAN:
        event.setCancelled(!canEdit(event.getPlayer(),block,Interaction.TELEPAD));
        break;

      default: break;
    }
  }

  @EventHandler public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
//		if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
    if(event.getPlayer().isOp()) { return; }
    event.setCancelled(!canEdit(event.getPlayer(),event.getBlockClicked().getRelative(event.getBlockFace()),Interaction.PLACE));
  }

  @EventHandler public void onPlayerShearEntity(PlayerShearEntityEvent event) {
//		if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
    if(event.getPlayer().isOp()) { return; }
    event.setCancelled(!canEdit(event.getPlayer(),event.getEntity().getLocation().getBlock(),Interaction.SHEAR));
  }

  @EventHandler public void onPlayerBucketFill(PlayerBucketFillEvent event) {
//		if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
    if(event.getPlayer().isOp()) { return; }
    event.setCancelled(!canEdit(event.getPlayer(),event.getBlockClicked().getRelative(event.getBlockFace()),Interaction.BREAK));
  }

  /*	@EventHandler public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
      if(!(event.getDamager() instanceof Player)) { return; }
  //		if(!((Player) event.getDamager()).hasPermission("totems.admin.unrestricted")) { return; }
      if(((Player) event.getDamager()).isOp()) { return; }
      if(event.getEntity() instanceof Player) {
        event.setCancelled(!canEdit((Player) event.getDamager(),event.getEntity().getLocation(),Interaction.PVP));
        return;
      }
      if(event.getEntity().getType().getTypeId() >= 90) {
        event.setCancelled(!canEdit((Player) event.getDamager(),event.getEntity().getLocation(),Interaction.SLAUGHTER));
      } else  if(event.getEntity().getType().getTypeId() >= 48) {
        event.setCancelled(!canEdit((Player) event.getDamager(),event.getEntity().getLocation(),Interaction.PVE));
      }
    }
  */
/*	@EventHandler public void onCreatureSpawn(CreatureSpawnEvent event) {
		if(event.getEntity().getType().getTypeId() >= 90) {
			event.setCancelled(!canEdit("NoPlayerEvent",event.getEntity().getLocation(),Interaction.SPAWNFRIENDLY));
		} else {
			event.setCancelled(!canEdit("NoPlayerEvent",event.getEntity().getLocation(),Interaction.SPAWNUNFRIENDLY));
		}
	}
*/
  @EventHandler public void onBlockIgnite(BlockIgniteEvent event) {
    if(event.getPlayer() instanceof Player) {
//			if(!event.getPlayer().hasPermission("totems.admin.unrestricted")) { return; }
      if(event.getPlayer().isOp()) { return; }
      event.setCancelled(!canEdit(event.getPlayer(),event.getBlock(),Interaction.IGNITE));
    } else {
      event.setCancelled(!canEdit("NoPlayerEvent",event.getBlock(),Interaction.FIRESPREAD));
    }
  }


	/* canEdit, the queen of Sea-Cows =======================================================================*/

  public boolean canEdit(Player player, Block block, Interaction flag) {
    if(player.isOp()) { return true; }
    return canEdit(player.getName(), block, flag);
  }

  public boolean canEdit(String player, Block block, String flag) {
    return canEdit(player, block, Interaction.valueOf(flag.toUpperCase()));
  }

  public boolean canEdit(Player player, Block block, String flag) {
    if(player.isOp()) { return true; }
    return canEdit(player.getName(), block, Interaction.valueOf(flag.toUpperCase()));
  }

  public boolean canEdit(String playerName, Block block, Interaction flag) {
    boolean defaultTo = true;
    double permissionScale = 0.0;
    String path;
    String worldName = block.getWorld().getName();
    boolean flagValue, isOwner;
    String flagName = flag.getName().toLowerCase();

    try{
//			Set<String> totems = getConfig().getConfigurationSection("totems."+worldName).getKeys(false);
      if(getConfig().contains("totems.defaults."+flagName)){
        defaultTo = getConfig().getBoolean("totems.defaults."+flagName);
      }
      if(getConfig().contains("totems."+worldName+".defaults."+flagName)){
        defaultTo = getConfig().getBoolean("totems."+worldName+".defaults."+flagName);
      }

      HashMap<String, Double> totems = Utils.getTotemsAffectingLocation(block);
      Set<String> keys = totems.keySet();
      keys.remove("defaults");

      for(String key:keys){
        path = "totems."+worldName+"."+key;
        try{ //if we are in the radius of a totem,
          if(getConfig().contains(path+".flags."+flagName)){
            flagValue = getConfig().getBoolean(path+".flags."+flagName);
          } else {
            flagValue = defaultTo;
          }
          String ownerName = getConfig().getString(path+".owner").toLowerCase();
          if(ownerName.equalsIgnoreCase(playerName) || //if I own this totem, or
                  getConfig().getBoolean("totems.groups."+ownerName+"."+playerName.toLowerCase()) || //you're a friend to me or
                  getConfig().getStringList(path+".friends").contains(playerName.toLowerCase())) {//you're a friend of this totem
            //add distance-from-center weight to true
            isOwner = true;
          } else {//if it's not our totem, or our friend's totem,
            //add distance-from-center weight to false
            isOwner = false;
          }

          if(isOwner != flagValue) {
            if(defaultTo) { permissionScale += totems.get(key); }
            else { permissionScale -= totems.get(key); }
          } else {
            if(isOwner) { permissionScale += totems.get(key); }
            else { permissionScale -= totems.get(key); }
          }



        } catch (NullPointerException f) { }
      }
    } catch (NullPointerException e) { }

    if(permissionScale==0) { return defaultTo; }
    return (permissionScale>0);
  }

  public boolean isOwner(String playerName, Block block) {
    double permissionScale = 0.0;
    String path;
    String worldName = block.getWorld().getName();

    try{
      HashMap<String, Double> totems = Utils.getTotemsAffectingLocation(block);
      Set<String> keys = totems.keySet();
      keys.remove("defaults");

      for(String key:keys){
        path = "totems."+worldName+"."+key;
        try{ //if we are in the radius of a totem,
          String ownerName = getConfig().getString(path+".owner").toLowerCase();
          if(ownerName.equalsIgnoreCase(playerName)) {//you're a friend of this totem
            //add distance-from-center weight to true
            permissionScale += totems.get(key);
          } else {//if it's not our totem, or our friend's totem,
            //add distance-from-center weight to false
            permissionScale -= totems.get(key);
          }

        } catch (NullPointerException f) { }
      }
    } catch (NullPointerException e) { }

    return (permissionScale>0);
  }


	/* Commands ==============================================================================================*/

  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    String command = cmd.getName();

    if(command.equalsIgnoreCase("tr")){
      return Commands.reloadConfig(sender);
    }

    if(sender instanceof Player){
      Player player = (Player) sender;

      if(command.equalsIgnoreCase("whose")) { return Commands.whose(player); }

      if(args.length>0){

      }

      if(args.length>1){
        if(command.equalsIgnoreCase("setTotemDefault")) { return Commands.setTotemDefault(player, args); }

      }
    }


    if(sender instanceof Player) {
      Player player = (Player) sender;

/*			if(cmd.getName().equalsIgnoreCase("totemCreate")){
				if(!player.hasPermission("totems.command.createTotem")) {
					player.sendMessage("Totems are not free!");
					return true;
				}
				HashMap<String,Boolean> flags = new HashMap<String,Boolean>();
				List<String> friends = new ArrayList<String>();

				String serial = (new SimpleDateFormat("yyMMddHHmmss").format(new Date()));

				flags.put("fly", false);
				Set<String> keys = flags.keySet();

				String path = "totems."+player.getWorld().getName()+"."+serial;
				this.getConfig().set(path+".x", player.getLocation().getBlockX());
				this.getConfig().set(path+".y", player.getLocation().getBlockY());
				this.getConfig().set(path+".z", player.getLocation().getBlockZ());
				this.getConfig().set(path+".radius", 20.0);
				for(String key:keys){
					this.getConfig().set(path+".flags."+key, flags.get(key));
				}
				this.getConfig().set(path+".friends", friends);
				this.getConfig().set(path+".owner", player.getName());

				player.getWorld().getBlockAt(player.getLocation()).setType(Material.DIAMOND_BLOCK);
				player.getWorld().getBlockAt(player.getLocation()).setMetadata("Totem", new FixedMetadataValue(this, serial));

				return true;
			}
*/
      if(cmd.getName().equalsIgnoreCase("totemFlag")){
        if(!player.hasPermission("totems.command.addFlag")) {
          player.sendMessage("You don't have permission to add flags for free!");
          return true;
        }

        if(args.length<2) {
          return false;
        }

        Block totemBlock = player.getLastTwoTargetBlocks(new HashSet<Material>(), 20).get(1);
        String path = "";

        try{
          Utils.getTotem(totemBlock);
          Set<String> totems = this.getConfig().getConfigurationSection("totems."+player.getWorld().getName()).getKeys(false);
          for(String totem:totems){
            path = "totems."+player.getWorld().getName()+"."+totem;
            if(getConfig().getInt(path+".x") != totemBlock.getX()) { continue; }
            if(getConfig().getInt(path+".z") != totemBlock.getZ()) { continue; }
            if(getConfig().getInt(path+".y") != totemBlock.getY()) { continue; }
            if((!player.hasPermission("totems.admin.setFlag")) &&
                    (!player.isOp()) &&
                    (!getConfig().getString(path+".owner").equalsIgnoreCase(player.getName()))
                    ) {
              player.sendMessage("You have not selected a totem that belongs to you.");
              return true;
            }
            break;
          }
        } catch (NullPointerException e) {
          log("NPE in onCommand.totemFlag");
        }

        String flag = args[0];
        Boolean value = null;
        if(!args[1].equalsIgnoreCase("remove")) {
          value = (args[1].equalsIgnoreCase("true"));
        }

        getConfig().set(path+".flags."+flag, value);

        return true;
      }
      if(cmd.getName().equalsIgnoreCase("totemgoto")){
        if(!player.isOp()) { player.sendMessage("You don't have permission to do that."); return true; }
        if(args.length<1) { return false; }
        Location loc = getBlock(args[0].toLowerCase()).getLocation();
        if(loc==null){
          player.sendMessage("Totem "+args[0]+" does not exist!");
          return true;
        }
        player.teleport(loc);
        return true;
      }
      if(cmd.getName().equalsIgnoreCase("tfriend")){
        if(args.length<1) { return false; }
        this.getConfig().set("totems.groups."+sender.getName().toLowerCase()+"."+args[0].toLowerCase(), true);
        this.saveConfig();
        sender.sendMessage(args[0]+" is now permitted to do stuff in your areas.");
        return true;
      }
      if(cmd.getName().equalsIgnoreCase("tunfriend")){
        if(args.length<1) { return false; }
        this.getConfig().set("totems.groups."+sender.getName().toLowerCase()+"."+args[0].toLowerCase(), false);
        this.saveConfig();
        sender.sendMessage(args[0]+" is now unpermitted to do stuff in your areas.");
        return true;
      }
    }
    return false;
  }

  private Block getBlock(String totem) {
    String path="";
    Set<String> worlds = this.getConfig().getConfigurationSection("totems").getKeys(false);
    for(String world : worlds){
      if(server.getWorld(world)==null) { continue; }
//			Set<String> totems = this.getConfig().getConfigurationSection("totems."+world).getKeys(false);
//			for(String totem:totems){
      path = "totems."+world+"."+totem;
      if(!getConfig().contains(path)) { return null; }
      int x = getConfig().getInt(path+".x");
      int z = getConfig().getInt(path+".z");
      int y = getConfig().getInt(path+".y");
      return server.getWorld(world).getBlockAt(x, y, z);
    }
//		}
    return null;
  }

  public boolean setTotemFlag(Location loc, Player player, String flag, String sValue) {
    Block totemBlock = loc.getBlock();
    String path = "";

    try{
      Set<String> totems = getConfig().getConfigurationSection("totems."+player.getWorld().getName()).getKeys(false);
      for(String totem:totems){
        path = "totems."+player.getWorld().getName()+"."+totem;
        if(getConfig().getInt(path+".x") != totemBlock.getX()) { path=""; continue; }
        if(getConfig().getInt(path+".z") != totemBlock.getZ()) { path=""; continue; }
        if(getConfig().getInt(path+".y") != totemBlock.getY()) { path=""; continue; }
        if((!player.hasPermission("totems.set.adminFlag")) &&
                (!player.isOp()) &&
                (!getConfig().getString(path+".owner").equalsIgnoreCase(player.getName()))
                ) {
          player.sendMessage("No valid totem selected.");
          return false;
        }
        break;
      }
      if(path.equals("")) {
        player.sendMessage("No valid totem selected!");
        return false;
      }
    } catch (NullPointerException e) {
      log("NPE in Totems.setTotemFlag");
      return false;
    }

    Boolean value = null;
    if(!sValue.equalsIgnoreCase("remove")) {
      value = (sValue.equalsIgnoreCase("true"));
    }

    getConfig().set(path+".flags."+flag, value);
    saveConfig();

    return true;
  }

}
