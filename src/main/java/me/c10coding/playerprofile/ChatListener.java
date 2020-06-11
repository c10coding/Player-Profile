package me.c10coding.playerprofile;

import me.c10coding.coreapi.CoreAPI;
import me.c10coding.coreapi.chat.Chat;
import me.clip.placeholderapi.PlaceholderAPI;
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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;


public class ChatListener implements Listener {

    PlayerProfile plugin;
    CoreAPI api = new CoreAPI();
    Chat chatFactory = api.getChatFactory();

    public ChatListener(PlayerProfile plugin){
        this.plugin = plugin;
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e){

        e.setCancelled(true);
        Player p = e.getPlayer();

        LuckPerms lp = LuckPermsProvider.get();
        ContextManager contextManager = lp.getContextManager();
        User user = lp.getUserManager().getUser(p.getUniqueId());
        ImmutableContextSet contextSet = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);
        CachedMetaData metaData = user.getCachedData().getMetaData(QueryOptions.contextual(contextSet));

        String prefix = "";
        String playerName = p.getName();

        if(metaData.getPrefix() != null){
            prefix = chatFactory.chat(metaData.getPrefix() + " ");
        }

        if(!p.getDisplayName().equalsIgnoreCase(p.getName())){
            playerName = p.getDisplayName();
        }

        TextComponent profile = new TextComponent("<" + chatFactory.chat(playerName));
        TextComponent msg = new TextComponent("> " + e.getMessage());

        profile.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, getComponent(p, prefix)));
        profile.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + p.getName()));
        profile.addExtra(msg);

        for(Player pl : Bukkit.getOnlinePlayers()){
            pl.spigot().sendMessage(profile);
        }

    }

    public BaseComponent[] getComponent(Player p, String prefix){

        File f = new File("plugins/Essentials", "userdata/" + p.getUniqueId() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
        String playerName = p.getName();
        String nickname =  config.getString("nickname");

        if(nickname == null){
            nickname = playerName;
        }

        List<String> lines = getLines();

        ComponentBuilder cb = new ComponentBuilder("");
        for(int x = 0; x < getLines().size(); x++){
            String line = lines.get(x);
            line = PlaceholderAPI.setPlaceholders(p, line);

            if(line.contains("%nickName%")){
                line = line.replace("%nickName%", nickname);
            }

            if(line.contains("%balance%")){
                line = line.replace("%balance%", format((long)PlayerProfile.getEconomy().getBalance(p)));
            }

            cb.append(line);
            if(x != lines.size() - 1){
                cb.append("\n");
            }
        }

        return cb.create();
    }

    public List<String> getLines(){
        return plugin.getConfig().getStringList("Lines");
    }

    private static final NavigableMap<Long, String> suffixes = new TreeMap<>();
    static {
        suffixes.put(1_000L, "k");
        suffixes.put(1_000_000L, "M");
        suffixes.put(1_000_000_000L, "G");
        suffixes.put(1_000_000_000_000L, "T");
        suffixes.put(1_000_000_000_000_000L, "P");
        suffixes.put(1_000_000_000_000_000_000L, "E");
    }

    public static String format(long value) {
        //Long.MIN_VALUE == -Long.MIN_VALUE so we need an adjustment here
        if (value == Long.MIN_VALUE) return format(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + format(-value);
        if (value < 1000) return Long.toString(value); //deal with easy case

        Map.Entry<Long, String> e = suffixes.floorEntry(value);
        Long divideBy = e.getKey();
        String suffix = e.getValue();

        long truncated = value / (divideBy / 10); //the number part of the output times 10
        boolean hasDecimal = truncated < 100 && (truncated / 10d) != (truncated / 10);
        return hasDecimal ? (truncated / 10d) + suffix : (truncated / 10) + suffix;
    }


}
