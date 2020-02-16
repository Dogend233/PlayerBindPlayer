package com.tripleying.dogend.PlayerBindPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    
    private String joinKickMsg;
    private String quitKickMsg;
    private String bindListMsg;
    private String bindAddMsg;
    private String bindAddErrorMsg;
    private String bindRemoveMsg;
    private String bindRemoveErrorMsg;
    private String reloadMsg;
    private FileConfiguration config;
    private final HashMap<String,String> bindList = new HashMap();
    private final HashMap<String,List<String>> inverseBindList = new HashMap();
    
    @Override
    public void onEnable(){
        Bukkit.getPluginManager().registerEvents(this, this);
        reload();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(label.equalsIgnoreCase("ppbind") && sender.hasPermission("playerbindplayer.admin")){
            if(args.length>0 && args[0].equalsIgnoreCase("reload")){
                reload();
                sender.sendMessage(reloadMsg);
            }else if(args.length>0 && args[0].equalsIgnoreCase("list")){
                bindList.forEach((k,v) -> sender.sendMessage(bindListMsg.replace("%playerA%", k).replace("%playerB%", v)));
            }else if(args.length>1 && args[0].equalsIgnoreCase("remove")){
                if(bindList.containsKey(args[1])){
                    config.set("bindlist."+args[1], null);
                    saveConfig();
                    reload();
                    sender.sendMessage(bindRemoveMsg.replace("%player%", args[1]));
                }else{
                    sender.sendMessage(bindRemoveErrorMsg.replace("%player%", args[1]));
                }
            }else if(args.length>2 && args[0].equalsIgnoreCase("add")){
                if(bindList.containsKey(args[1])){
                    sender.sendMessage(bindAddErrorMsg.replace("%player%", args[1]));
                }else{
                    config.set("bindlist."+args[1], args[2]);
                    saveConfig();
                    reload();
                    sender.sendMessage(bindAddMsg.replace("%playerA%", args[1]).replace("%playerB%", args[2]));
                    Player A = Bukkit.getPlayer(args[1]);
                    if(A!=null && A.isOnline()){
                        Player B = Bukkit.getPlayer(args[2]);
                        if(B==null || !B.isOnline()){
                            A.kickPlayer(joinKickMsg.replace("%player%", args[2]));
                        }
                    }
                }
            }else{
                return false;
            }
        }
        return true;
    }
    
    private void reload(){
        bindList.clear();
        inverseBindList.clear();
        File f = getDataFolder();
        if(!f.exists()) f.mkdir();
        f = new File(f, "config.yml");
        if(!f.exists()) saveDefaultConfig();
        reloadConfig();
        config = getConfig();
        joinKickMsg = config.getString("message.join_kick","§e%player% §a不在，你不可以进来哟~~");
        quitKickMsg = config.getString("message.quit_kick","§e%player% §a走啦，把你也带走咯~~");
        bindListMsg = config.getString("message.bind_list","§b%playerA% §r-> §a%playerB%");
        bindAddMsg = config.getString("message.bind_add","§a已将%playerA%绑定至%playerB%");
        bindAddErrorMsg = config.getString("message.bind_add_error","§c%player%的绑定已存在");
        bindRemoveMsg = config.getString("message.bind_remove","§a已移除%player%的绑定");
        bindRemoveErrorMsg = config.getString("message.bind_remove_error","§c%player%的绑定不存在");
        reloadMsg = config.getString("message.reload","§a[PlayerBindPlayer] 已重载");
        ConfigurationSection temp = config.getConfigurationSection("bindlist");
        if(temp!=null){
            Set<String> set = temp.getKeys(false);
            if(!set.isEmpty()){
                set.forEach(A -> {
                    String B = temp.getString(A);
                    if(B!=null && !B.trim().equals("")) addBind(A,B);
                });
            }
        }
    }
    
    public void addBind(String A, String B){
        bindList.put(A, B);
        if(inverseBindList.containsKey(B)){
            List<String> temp = inverseBindList.get(B);
            temp.add(A);
            inverseBindList.replace(B, temp);
        }else{
            List<String> temp = new ArrayList();
            temp.add(A);
            inverseBindList.put(B, temp);
        }
    }
    
    public void removeBind(String A){
        if(bindList.containsKey(A)){
            String B = bindList.get(A);
            bindList.remove(A);
            if(inverseBindList.containsKey(B)){
                List<String> temp = inverseBindList.get(B);
                if(temp.contains(A)) temp.remove(A);
                if(temp.isEmpty()){
                    inverseBindList.remove(B);
                }else{
                    inverseBindList.replace(B, temp);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt){
        Player joinP = evt.getPlayer();
        String joinN = joinP.getName();
        if(bindList.containsKey(joinN)){
            String targetN = bindList.get(joinN);
            Player targetP = Bukkit.getPlayer(targetN);
            if(targetP==null || !targetP.isOnline()){
                joinP.kickPlayer(joinKickMsg.replace("%player%", targetN));
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent evt){
        Player quitP = evt.getPlayer();
        String quitN = quitP.getName();
        if(inverseBindList.containsKey(quitN)){
            List<String> targetList = inverseBindList.get(quitN);
            if(!targetList.isEmpty()) targetList.forEach(targetN -> {
                Player targetP = Bukkit.getPlayer(targetN);
                if(targetP!=null && targetP.isOnline()){
                    targetP.kickPlayer(quitKickMsg.replace("%player%", quitN));
                }
            });
        }
    }
    
}
