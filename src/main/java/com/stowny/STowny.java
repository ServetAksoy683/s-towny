package com.stowny;

import com.stowny.commands.SCommand;
import com.stowny.listeners.BorderListener;
import com.stowny.listeners.JoinListener;
import com.stowny.listeners.ProtectionListener;
import com.stowny.managers.DataManager;
import com.stowny.managers.DynmapManager;
import com.stowny.managers.EconomyManager;
import com.stowny.managers.InviteManager;
import com.stowny.managers.LangManager;
import com.stowny.managers.ScoreboardManager;
import com.stowny.managers.SoundManager;
import com.stowny.managers.TaxManager;
import com.stowny.managers.WorldManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class STowny extends JavaPlugin {

    private LangManager langManager;
    private SoundManager soundManager;
    private EconomyManager economyManager;
    private DataManager dataManager;
    private InviteManager inviteManager;
    private DynmapManager dynmapManager;
    private ScoreboardManager scoreboardManager;
    private TaxManager taxManager;
    private WorldManager worldManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.langManager = new LangManager(this);
        this.soundManager = new SoundManager(this);
        this.economyManager = new EconomyManager(this);
        this.dataManager = new DataManager(this);
        this.inviteManager = new InviteManager();
        this.dynmapManager = new DynmapManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.taxManager = new TaxManager(this);
        this.worldManager = new WorldManager(this);

        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new BorderListener(this), this);

        getCommand("stowny").setExecutor(new SCommand(this));

        for (var town : dataManager.allTowns().values()) {
            dynmapManager.refreshTown(town);
        }

        scoreboardManager.startTask();
        taxManager.startTask();

        getLogger().info("S-Towny aktif edildi.");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.save();
        }
        getLogger().info("S-Towny devre disi birakildi.");
    }

    public LangManager lang() { return langManager; }
    public SoundManager sounds() { return soundManager; }
    public EconomyManager economy() { return economyManager; }
    public DataManager data() { return dataManager; }
    public InviteManager invites() { return inviteManager; }
    public DynmapManager dynmap() { return dynmapManager; }
    public ScoreboardManager scoreboard() { return scoreboardManager; }
    public WorldManager worlds() { return worldManager; }
}
