package net.enelson.soptntrun;

import net.enelson.sopparty.api.SopPartyApi;
import net.enelson.sopparty.api.SopPartyServices;
import net.enelson.soptntrun.arena.ArenaManager;
import net.enelson.soptntrun.command.TNTRunCommand;
import net.enelson.soptntrun.config.TNTRunConfig;
import net.enelson.soptntrun.edit.EditorManager;
import net.enelson.soptntrun.listener.ControlItemListener;
import net.enelson.soptntrun.listener.MatchStateListener;
import net.enelson.soptntrun.listener.PowerupListener;
import net.enelson.soptntrun.match.MatchManager;
import net.enelson.soptntrun.message.MessageService;
import net.enelson.soptntrun.party.PartyBridge;
import net.enelson.soptntrun.party.SoloPartyBridge;
import net.enelson.soptntrun.party.SopPartyPartyBridgeAdapter;
import net.enelson.soptntrun.placeholder.SopTNTRunPlaceholderExpansion;
import net.enelson.soptntrun.stats.PlayerStatisticsManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SopTNTRunPlugin extends JavaPlugin implements Listener {

    private MessageService messageService;
    private TNTRunConfig tntrunConfig;
    private ArenaManager arenaManager;
    private EditorManager editorManager;
    private MatchManager matchManager;
    private PlayerStatisticsManager statisticsManager;
    private PartyBridge partyBridge = new SoloPartyBridge();
    private SopTNTRunPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messageService = new MessageService(this);
        this.tntrunConfig = new TNTRunConfig(this);
        this.arenaManager = new ArenaManager(this);
        this.editorManager = new EditorManager();
        this.matchManager = new MatchManager(this);
        this.statisticsManager = new PlayerStatisticsManager(this);

        if (!reloadPlugin()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        TNTRunCommand command = new TNTRunCommand(this);
        getCommand("tntrun").setExecutor(command);
        getCommand("tntrun").setTabCompleter(command);

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new ControlItemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MatchStateListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PowerupListener(this), this);
        attachSopPartyBridgeIfAvailable();
        registerPlaceholderExpansionIfAvailable();
    }

    @Override
    public void onDisable() {
        if (matchManager != null) {
            matchManager.shutdownAndEvacuate();
        }
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
            placeholderExpansion = null;
        }
    }

    public boolean reloadPlugin() {
        reloadConfig();
        try {
            tntrunConfig.reload();
            arenaManager.reload();
            statisticsManager.reload();
            matchManager.reset();
            matchManager.startTicker();
            return true;
        } catch (Exception exception) {
            getLogger().severe("Failed to reload SopTNTRun: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    private void attachSopPartyBridgeIfAvailable() {
        SopPartyApi api = SopPartyServices.get();
        if (api != null) {
            setPartyBridge(new SopPartyPartyBridgeAdapter(api));
            getLogger().info("SopParty detected - using its API for grouped TNTRun joins.");
        }
    }

    @EventHandler
    public void onPluginEnableLate(PluginEnableEvent event) {
        if ("SopParty".equals(event.getPlugin().getName())) {
            attachSopPartyBridgeIfAvailable();
            return;
        }
        if ("PlaceholderAPI".equalsIgnoreCase(event.getPlugin().getName())) {
            registerPlaceholderExpansionIfAvailable();
        }
    }

    @EventHandler
    public void onPluginDisableEarly(PluginDisableEvent event) {
        if ("SopParty".equals(event.getPlugin().getName())) {
            setPartyBridge(null);
            getLogger().info("SopParty disabled - reverting to solo grouping.");
            return;
        }
        if ("PlaceholderAPI".equalsIgnoreCase(event.getPlugin().getName()) && placeholderExpansion != null) {
            placeholderExpansion = null;
        }
    }

    private void registerPlaceholderExpansionIfAvailable() {
        if (placeholderExpansion != null) {
            return;
        }
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi == null || !papi.isEnabled()) {
            return;
        }
        placeholderExpansion = new SopTNTRunPlaceholderExpansion(this);
        placeholderExpansion.register();
        getLogger().info("PlaceholderAPI detected - registered %soptntrun_*% placeholders.");
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public TNTRunConfig getTNTRunConfig() {
        return tntrunConfig;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public EditorManager getEditorManager() {
        return editorManager;
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public PlayerStatisticsManager getStatistics() {
        return statisticsManager;
    }

    public PartyBridge getPartyBridge() {
        return partyBridge;
    }

    public void setPartyBridge(PartyBridge partyBridge) {
        this.partyBridge = partyBridge == null ? new SoloPartyBridge() : partyBridge;
    }
}
