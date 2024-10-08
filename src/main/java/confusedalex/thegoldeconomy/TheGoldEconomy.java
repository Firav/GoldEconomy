package confusedalex.thegoldeconomy;

import org.apache.commons.lang.LocaleUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.redlib.commandmanager.ArgType;
import redempt.redlib.commandmanager.CommandParser;

import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

public final class TheGoldEconomy extends JavaPlugin {
  EconomyImplementer eco;
  private VaultHook vaultHook;
  Util util;

  @Override
  public void onEnable() {
    // Config
    saveDefaultConfig();

    // Language
    ResourceBundle bundle;
    String language = getConfig().getString("language");
    HashMap<String, Locale> localeMap = new HashMap<>();
    localeMap.put("de_DE", Locale.GERMANY);
    localeMap.put("en_US", Locale.US);
    localeMap.put("zh_CN", Locale.SIMPLIFIED_CHINESE);
    localeMap.put("es_ES", LocaleUtils.toLocale("es_ES"));
    localeMap.put("tr_TR", LocaleUtils.toLocale("tr_TR"));
    localeMap.put("pt_BR", LocaleUtils.toLocale("pt_BR"));
    localeMap.put("nb_NO", LocaleUtils.toLocale("nb_NO"));

    if (localeMap.containsKey(language)) {
      bundle = ResourceBundle.getBundle("messages", localeMap.get(language));
    } else {
      bundle = ResourceBundle.getBundle("messages", Locale.US);
      getLogger().warning("Invalid language in config. Defaulting to English.");
    }

    String base = getConfig().getString("base");
    if (!base.equals("nuggets") && !base.equals("ingots") && !base.equals("raw")) {
      getLogger().severe(bundle.getString("error.invalidbase"));
      getServer().shutdown();
    }

    // bStats
    int pluginId = 15402;
    new Metrics(this, pluginId);

    // Vault shit
    util = new Util(this);
    eco = new EconomyImplementer(this, bundle, util);
    vaultHook = new VaultHook(this, eco);
    vaultHook.hook();

    // Commands from RedLib
    ArgType<OfflinePlayer> offlinePlayer = new ArgType<>("offlinePlayer", Bukkit::getOfflinePlayer)
        .tabStream(c -> Bukkit.getOnlinePlayers().stream().map(Player::getName));
    new CommandParser(this.getResource("commands.rdcml"))
        .setArgTypes(offlinePlayer)
        .parse()
        .register("TheGoldEconomy",
            new Commands(bundle, eco, util, getConfig()));

    // Event class registering
    Bukkit.getPluginManager().registerEvents(new Events(eco.bank), this);
    // If removeGoldDrop is true, register Listener
    if (getConfig().getBoolean("removeGoldDrop"))
      Bukkit.getPluginManager().registerEvents(new RemoveGoldDrops(), this);

    // Update Checker
    if (getConfig().getBoolean("updateCheck")) {
      new UpdateChecker(this, 102242).getVersion(version -> {
        if (!this.getDescription().getVersion().equals(version)) {
          getLogger().info(bundle.getString("warning.update"));
        }
      });
    }

    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new Placeholders(this).register();
    }
  }

  @Override
  public void onDisable() {
    FileUtilsKt.writeToFiles(eco.bank.getPlayerAccounts(), eco.bank.getFakeAccounts());

    vaultHook.unhook();

    getLogger().info("TheGoldEconomy disabled.");
  }
}
