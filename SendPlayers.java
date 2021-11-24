package ua.lorens.vongex.bungee.commands;

import net.md_5.bungee.api.plugin.*;
import net.md_5.bungee.api.connection.*;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.*;
import net.md_5.bungee.api.config.*;
import com.google.common.collect.*;
import java.util.*;
import net.md_5.bungee.api.chat.*;
import com.google.common.base.*;

public class sendplayers extends Command implements TabExecutor
{
    public sendplayers() {
        super("send", "elements.send", new String[0]);
    }
    
    public void execute(final CommandSender sender, final String[] args) {
        if (args.length != 2) {
            sender.sendMessage(ProxyServer.getInstance().getTranslation("send_cmd_usage", new Object[0]));
            return;
        }
        final ServerInfo server = ProxyServer.getInstance().getServerInfo(args[1]);
        if (server == null) {
            sender.sendMessage(ProxyServer.getInstance().getTranslation("no_server", new Object[0]));
            return;
        }
        List<ProxiedPlayer> targets;
        if (args[0].equalsIgnoreCase("all")) {
            targets = new ArrayList<ProxiedPlayer>(ProxyServer.getInstance().getPlayers());
        }
        else if (args[0].equalsIgnoreCase("current")) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ProxyServer.getInstance().getTranslation("player_only", new Object[0]));
                return;
            }
            final ProxiedPlayer player = (ProxiedPlayer)sender;
            targets = new ArrayList<ProxiedPlayer>(player.getServer().getInfo().getPlayers());
        }
        else {
            final ServerInfo serverTarget = ProxyServer.getInstance().getServerInfo(args[0]);
            if (serverTarget != null) {
                targets = new ArrayList<ProxiedPlayer>(serverTarget.getPlayers());
            }
            else {
                final ProxiedPlayer player2 = ProxyServer.getInstance().getPlayer(args[0]);
                if (player2 == null) {
                    sender.sendMessage(ProxyServer.getInstance().getTranslation("user_not_online", new Object[0]));
                    return;
                }
                targets = Collections.singletonList(player2);
            }
        }
        final SendCallback callback = new SendCallback(sender);
        for (final ProxiedPlayer player3 : targets) {
            final ServerConnectRequest request = ServerConnectRequest.builder().target(server).reason(ServerConnectEvent.Reason.COMMAND).callback((Callback)new SendCallback.Entry(callback, player3, server)).build();
            player3.connect(request);
        }
        sender.sendMessage(ChatColor.DARK_GREEN + "Attempting to send " + targets.size() + " players to " + server.getName());
    }
    
    public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
        if (args.length > 2 || args.length == 0) {
        }
        final Set<String> matches = new HashSet<String>();
        if (args.length == 1) {
            final String search = args[0].toLowerCase(Locale.ROOT);
            for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                if (player.getName().toLowerCase(Locale.ROOT).startsWith(search)) {
                    matches.add(player.getName());
                }
            }
            if ("all".startsWith(search)) {
                matches.add("all");
            }
            if ("current".startsWith(search)) {
                matches.add("current");
            }
        }
        else {
            final String search = args[1].toLowerCase(Locale.ROOT);
            for (final String server : ProxyServer.getInstance().getServers().keySet()) {
                if (server.toLowerCase(Locale.ROOT).startsWith(search)) {
                    matches.add(server);
                }
            }
        }
        return matches;
    }
    
    protected static class SendCallback
    {
        private final Map<ServerConnectRequest.Result, List<String>> results;
        private final CommandSender sender;
        private int count;
        
        public SendCallback(final CommandSender sender) {
            this.results = new HashMap<ServerConnectRequest.Result, List<String>>();
            this.count = 0;
            this.sender = sender;
            for (final ServerConnectRequest.Result result : ServerConnectRequest.Result.values()) {
                this.results.put(result, new ArrayList<String>());
            }
        }
        
        public void lastEntryDone() {
            this.sender.sendMessage(ChatColor.GREEN.toString() + ChatColor.BOLD + "Send Results:");
            for (final Map.Entry<ServerConnectRequest.Result, List<String>> entry : this.results.entrySet()) {
                final ComponentBuilder builder = new ComponentBuilder("");
                if (!entry.getValue().isEmpty()) {
                    builder.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Joiner.on(", ").join((Iterable)entry.getValue())).color(ChatColor.YELLOW).create()));
                }
                builder.append(entry.getKey().name() + ": ").color(ChatColor.GREEN);
                builder.append("" + entry.getValue().size()).bold(true);
                this.sender.sendMessage(builder.create());
            }
        }
        
        public static class Entry implements Callback<ServerConnectRequest.Result>
        {
            private final SendCallback callback;
            private final ProxiedPlayer player;
            private final ServerInfo target;
            
            public Entry(final SendCallback callback, final ProxiedPlayer player, final ServerInfo target) {
                this.callback = callback;
                this.player = player;
                this.target = target;
                this.callback.count++;
            }
            
            public void done(final ServerConnectRequest.Result result, final Throwable error) {
                this.callback.results.get(result).add(this.player.getName());
                if (result == ServerConnectRequest.Result.SUCCESS) {
                    this.player.sendMessage(ProxyServer.getInstance().getTranslation("you_got_summoned", new Object[] { this.target.getName(), this.callback.sender.getName() }));
                }
                if (--this.callback.count == 0) {
                    this.callback.lastEntryDone();
                }
            }
        }
    }
}
