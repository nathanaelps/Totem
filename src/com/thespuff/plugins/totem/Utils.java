package com.thespuff.plugins.totem;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.thespuff.plugins.totem.Totem.Interaction;

public class Utils {

  //	private static String pluginName = Totems.pacluginName;
//	private static String pluginVersion = Totems.pluginVersion;
  private static Server server = Totem.server;
  private static Totem plugin = Totem.plugin;

  public static HashMap<String,Block> getTotems(){
    HashMap<String,Block> out = new HashMap<String,Block>();
    List<World> worldList = server.getWorlds();
    for(World world : worldList){
      try{
        out.putAll(getTotems(world.getName()));
      }catch(NullPointerException e) {/*Do Nothing*/}
    }
    return out;
  }

  public static boolean setWorldDefault(World world, String flag, boolean wDefault) {
    if(Interaction.valueOf(flag.toUpperCase()) == null) { return false; }

    plugin.getConfig().set("totems."+world.getName()+".defaults."+flag.toLowerCase(), wDefault);

    return true;
  }

  public static HashMap<String,Block> getTotems(Object world){
    World inWorld = null;
    if(world instanceof String) {
      inWorld = server.getWorld((String) world);
      if(inWorld==null) { return null; }
    } else if(world instanceof World) {
      inWorld = (World) world;
    } else {
      plugin.log("Bad Object type in Utils.getTotems()");
      return null;
    }

    HashMap<String,Block> out = new HashMap<String,Block>();

    String path="";
    try{
      Set<String> totems = plugin.getConfig().getConfigurationSection("totems."+inWorld.getName()).getKeys(false);
      for(String totem:totems){
        if(totem.equalsIgnoreCase("defaults")) { continue; }
        path = "totems."+inWorld.getName()+"."+totem;
        int x = plugin.getConfig().getInt(path+".x");
        int z = plugin.getConfig().getInt(path+".z");
        int y = plugin.getConfig().getInt(path+".y");
        Block block = inWorld.getBlockAt(x, y, z);
        out.put(totem, block);
      }
    } catch(NullPointerException e) {
      plugin.log("No totems recorded for world "+ inWorld.getName());
      return null;
    }

    return out;
  }

  public static HashMap<String,Double> getTotemsAffectingLocation(Location location) {
    return getTotemsAffectingLocation(location.getBlock());
  }

  public static HashMap<String,Double> getTotemsAffectingLocation(Block block) {
    String path = "";
    String world = block.getWorld().getName();
    HashMap<String,Double> out = new HashMap<String,Double>();
    double radius = 0.0;
    int tX,tY,tZ;
    int x = block.getX();
    int y = block.getY();
    int z = block.getZ();

    Set<String> totems = plugin.getConfig().getConfigurationSection("totems."+world).getKeys(false);
    for(String totem:totems){
      if(totem.equals("defaults")) { continue; }
      path = "totems."+world+"."+totem;
      radius = plugin.getConfig().getDouble(path+".radius");
      tX = Math.abs(plugin.getConfig().getInt(path+".x")-x);
      if(tX>radius) { continue; }
      tZ = Math.abs(plugin.getConfig().getInt(path+".z")-z);
      if(tZ>radius) { continue; }
      tY = Math.abs(plugin.getConfig().getInt(path+".y")-y);
      if(tY>radius) { continue; }
      out.put(totem, 1-((tX+tZ+tY)/(3*radius)));
    }
    return out;
  }

  public static void getTotem(Block totemBlock) {
    // TODO Auto-generated method stub

  }
}
