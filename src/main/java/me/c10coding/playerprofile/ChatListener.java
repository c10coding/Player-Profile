package me.c10coding.playerprofile;

import lpsapi.lpsapi.LPSAPI;
import me.c10coding.coreapi.CoreAPI;
import me.c10coding.coreapi.chat.Chat;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.chat.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ChatListener implements Listener {

    LPSAPI lpapi = (LPSAPI) Bukkit.getPluginManager().getPlugin("LPSAPI");
    Economy e = PlayerProfile.getEconomy();
    PlayerProfile plugin;

    public ChatListener(PlayerProfile plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e){
        e.setCancelled(true);
        Player p = e.getPlayer();
        String format = e.getFormat();

        LuckPerms lp = LuckPermsProvider.get();
        ContextManager contextManager = lp.getContextManager();
        User user = lp.getUserManager().getUser(p.getUniqueId());
        ImmutableContextSet contextSet = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);
        CachedMetaData metaData = user.getCachedData().getMetaData(QueryOptions.contextual(contextSet));

        String prefix = metaData.getPrefix();
        TextComponent profile = new TextComponent("<" + prefix + " "  + p.getName());
        TextComponent msg = new TextComponent("> " + e.getMessage());

        profile.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, getComponent(p)));
        profile.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + p.getName()));
        profile.addExtra(msg);

        for(Player pl : Bukkit.getOnlinePlayers()){
            pl.spigot().sendMessage(profile);
        }


    }

    public BaseComponent[] getComponent(Player p){

        CoreAPI api = new CoreAPI();
        Chat chatFactory = api.getChatFactory();
        String playerName = chatFactory.removeChatColor(p.getName());
        String displayName = chatFactory.removeChatColor(p.getDisplayName());
        int balance = (int) e.getBalance(p);
        int level = lpapi.exp.checkLevel(p);

        if(playerName.equalsIgnoreCase(displayName)){
            displayName = "None";
        }

        List<String> settingKeys = getSettingsKeys();
        List<String> settingsValues = new ArrayList<>();
        settingsValues.add(playerName);
        settingsValues.add(displayName);
        settingsValues.add(String.valueOf(balance));
        settingsValues.add(String.valueOf(level));

        ComponentBuilder cb = new ComponentBuilder("");
        for(int x = 0; x < settingKeys.size(); x++){
            if(plugin.getConfig().getBoolean("Settings." + settingKeys.get(x))){
                cb.append(settingKeys.get(x) + ": " + settingsValues.get(x));
                if(x != settingKeys.size()-1){
                    cb.append("\n");
                }
            }
        }

        return cb.create();
    }

    public List<String> getSettingsKeys(){
        List<String> settingsKeys = new ArrayList<>();
        ConfigurationSection cs = plugin.getConfig().getConfigurationSection("Settings");
        cs.getKeys(false).stream().forEach(key -> settingsKeys.add(key));
        return settingsKeys;
    }

}
