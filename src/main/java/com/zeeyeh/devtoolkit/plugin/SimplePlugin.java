package com.zeeyeh.devtoolkit.plugin;

import com.zeeyeh.devtoolkit.annotation.AutoRegistration;
import com.zeeyeh.devtoolkit.annotation.Commander;
import com.zeeyeh.devtoolkit.command.CommandEntity;
import com.zeeyeh.devtoolkit.command.CommandManager;
import com.zeeyeh.devtoolkit.config.ConfigManager;
import com.zeeyeh.devtoolkit.config.locales.LanguageManager;
import com.zeeyeh.devtoolkit.message.Messenger;
import com.zeeyeh.devtoolkit.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;

public abstract class SimplePlugin extends JavaPlugin {
    private static String basePackage;
    private static String name;
    private static String version;
    private static volatile SimplePlugin instance;
    private static volatile CommandManager commandManager;
    private static volatile ConfigManager configManager;
    private static volatile LanguageManager languageManager;

    public void setBasePackage(String pluginBasePackage) {
        basePackage = pluginBasePackage;
    }

    public static void setName(String name) {
        SimplePlugin.name = name;
    }

    public static void setVersion(String version) {
        SimplePlugin.version = version;
    }

    public static ConfigManager getConfigManager() {
        if (configManager == null) {
            synchronized (ConfigManager.class) {
                if (configManager == null) {
                    configManager = new ConfigManager(getInstance());
                }
            }
        }
        return configManager;
    }

    public static void setConfigManager(ConfigManager configManager) {
        SimplePlugin.configManager = configManager;
    }

    public static LanguageManager getLanguageManager() {
        if (languageManager == null) {
            synchronized (ConfigManager.class) {
                if (languageManager == null) {
                    languageManager = new LanguageManager(getInstance());
                }
            }
        }
        return languageManager;
    }

    public static void setLanguageManager(LanguageManager languageManager) {
        SimplePlugin.languageManager = languageManager;
    }

    public static CommandManager getCommandManager() {
        if (commandManager == null) {
            synchronized (CommandManager.class) {
                if (commandManager == null) {
                    commandManager = new CommandManager(getInstance(), getBasePackage());
                }
            }
        }
        return commandManager;
    }

    public static void setCommandManager(CommandManager commandManager) {
        SimplePlugin.commandManager = commandManager;
    }

    public static SimplePlugin getInstance() {
        synchronized (SimplePlugin.class) {
            if (instance == null) {
                try {
                    instance = JavaPlugin.getPlugin(SimplePlugin.class);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().severe(e.getMessage());
                }
            }
        }
        return instance;
    }

    public abstract void enable();

    public abstract void disable();

    public abstract String getCommandsModelName();

    @Override
    public void onEnable() {
        configManager = new ConfigManager(getInstance());
        languageManager = new LanguageManager(getInstance());
        Messenger.send(Bukkit.getConsoleSender(), "&aLoading plugin...");
//        String username = getConfig().getString("username");
//        String email = getConfig().getString("email");
//        String password = getConfig().getString("password");
//        UpdateUtil.getToken(username, email, password);
//        if (!UpdateUtil.checkDevices()) {
//            // 未知的使用ip
//            JsonObject serverInfo = UpdateUtil.getServerInfo();
//            String latestVersionName = UpdateUtil.getLatestVersionName(name);
//            String latestVersionUrl = UpdateUtil.getLatestVersionUrl(name);
//            if (latestVersionName == null) {
//                latestVersionName = "";
//            }
//            if (latestVersionUrl == null) {
//                latestVersionUrl = "";
//            }
//            List<String> disallowedDeviceTips = UpdateUtil.getDisallowedDeviceTips(serverInfo, latestVersionName, latestVersionUrl);
//            showTips(disallowedDeviceTips);
//            return;
//        }

//        if (UpdateUtil.checkLatest(name, version)) {
//            // 有新版本
//            JsonObject serverInfo = UpdateUtil.getServerInfo();
//            String latestVersionName = UpdateUtil.getLatestVersionName(name);
//            String latestVersionUrl = UpdateUtil.getLatestVersionUrl(name);
//            List<String> updateTips = UpdateUtil.getUpdateTips(serverInfo, latestVersionName, latestVersionUrl);
//            showTips(updateTips);
//        }
//        if (!UpdateUtil.isAllow(name)) {
//            // 版本被禁止使用
//            JsonObject serverInfo = UpdateUtil.getServerInfo();
//            String latestVersionName = UpdateUtil.getLatestVersionName(name);
//            String latestVersionUrl = UpdateUtil.getLatestVersionUrl(name);
//            if (latestVersionName == null) {
//                latestVersionName = "";
//            }
//            if (latestVersionUrl == null) {
//                latestVersionUrl = "";
//            }
//            List<String> disabledVersionTips = UpdateUtil.getDisabledVersionTips(serverInfo, latestVersionName, latestVersionUrl);
//            showTips(disabledVersionTips);
//            return;
//        }
        commandManager = new CommandManager(
                getInstance(),
                getBasePackage());
        getCommandManager().initializeCommands(getCommandsModelName());
        Class<?> pluginClass = ReflectionUtil.findPluginClass(getInstance(), getBasePackage());
        if (pluginClass == null) {
            Messenger.send(Bukkit.getConsoleSender(), "The plugin lacks necessary configuration. Please contact the plug-in author to fix it");
            Bukkit.getPluginManager().disablePlugin(this);
            System.exit(-1);
            return;
        }
        AutoRegistration autoRegistration = null;
        if (pluginClass.isAnnotationPresent(AutoRegistration.class)) {
            autoRegistration = pluginClass.getAnnotation(AutoRegistration.class);
        }
        if (autoRegistration != null) {
            if (autoRegistration.command()) {
                try {
                    registerCommands();
                } catch (Exception e) {
                    Messenger.send(Bukkit.getConsoleSender(), "Command registration failed: " + e.getMessage());
                }
            }
            if (autoRegistration.listener()) {
                Set<Class<?>> listeners = ReflectionUtil.findListeners(getInstance(), getBasePackage());
                try {
                    for (Class<?> listener : listeners) {
                        Constructor<?> constructor = listener.getDeclaredConstructor();
                        Listener listenInstance = (Listener) constructor.newInstance();
                        Bukkit.getPluginManager().registerEvents(listenInstance, getInstance());
                    }
                } catch (Exception e) {
                    Messenger.send(Bukkit.getConsoleSender(), "Event registration failed: " + e.getMessage());
                }
            }
        }
        enable();
        Messenger.send(Bukkit.getConsoleSender(), "&aPlugin enabled");
    }

    protected void registerCommands() throws Exception {
        Set<CommandEntity> commands = getCommandManager().getCommands();
        for (CommandEntity command : commands) {
            registerCommand(command.getName(), command.getInstance(), command.getDescription());
        }
    }

    protected void registerCommand(String command, Class<?> commandClass, Commander entity) throws Exception {
        Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
        constructor.setAccessible(true);
        PluginCommand pluginCommand = constructor.newInstance(command, getInstance());
        pluginCommand.setDescription(entity.description());
        pluginCommand.setUsage(entity.usage());
        pluginCommand.setAliases(Arrays.asList(entity.aliases()));
        pluginCommand.setPermission(entity.permission());
        Constructor<?> entityInstanceConstructor = commandClass.getDeclaredConstructor();
        CommandExecutor entityCommandExecutorInstance = (CommandExecutor) entityInstanceConstructor.newInstance();
        pluginCommand.setExecutor(entityCommandExecutorInstance);
        TabCompleter entityTabCompleterInstance = (TabCompleter) entityInstanceConstructor.newInstance();
        pluginCommand.setTabCompleter(entityTabCompleterInstance);
        getServer().getCommandMap().register(command, pluginCommand);
    }

    @Override
    public void onDisable() {
        disable();
        Messenger.send(Bukkit.getConsoleSender(), "&aPlugin disabled");
    }

    public static String getBasePackage() {
        return basePackage;
    }
}
