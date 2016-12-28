package com.thespuff.plugins.totem;

import java.util.HashMap;
import java.util.Iterator;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Commands {

  //	private static String pluginName = Totems.pluginName;
//	private static String pluginVersion = Totems.pluginVersion;
//	private static Server server = Totems.server;
  private static Totem plugin = Totem.plugin;

  public static boolean whose(Player player) {
    if(!player.hasPermission("totems.command.whose")) {
      player.sendMessage("You don't have permissions to ask such questions!");
      return true;
    }
    Location location = player.getLocation();

    try{
      HashMap<String, Double> affectiveTotems = Utils.getTotemsAffectingLocation(location);

      if(affectiveTotems.isEmpty()){
        player.sendMessage("This area is not protected.");
        return true;
      } else {
        String out = "(World: "+location.getWorld().getName()+")";
        Iterator<String> outIt = affectiveTotems.keySet().iterator();
        while(outIt.hasNext()){
          out = out+", "+outIt.next(); //TODO: Currently tells us totem names, not owner names. FIX!
        }
        player.sendMessage(out);
        return true;
      }
    } catch (NullPointerException e) {
      plugin.log("NPE in Commands.whose()");
    }
    return false;
  }

  public static boolean setTotemDefault(Player player, String[] args) {
    if(!player.hasPermission("totems.admin.setDefault")) {
      player.sendMessage("You don't have permissions to set world default flags!");
      return true;
    }
    if(args.length<2) { return false; }
    String wDefaultS = args[1];
    if(Utils.setWorldDefault(player.getWorld(), args[0], wDefaultS.equalsIgnoreCase("true"))){
      plugin.log("Flag "+ args[0] + " now defaults to " + wDefaultS + " for world " + player.getWorld().getName() +".");
    } else {
      plugin.log("I don't know that flag!");
    }
    return true;
  }

  public static boolean reloadConfig(CommandSender sender) {
    if(sender instanceof Player) {
      if(!(sender.hasPermission("totems.admin.reload") || sender.isOp())) {
        sender.sendMessage("No can do, poopsie! Insufficient permissions!");
        return true;
      }
      plugin.reloadConfig();
      sender.sendMessage("Config reloaded!");
      plugin.log(sender.getName()+" reloaded config.");
    } else {
      plugin.reloadConfig();
      plugin.log("Config reloaded!");
    }
    return true;
  }

}
