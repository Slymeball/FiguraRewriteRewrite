package org.moon.figura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.Entity;
import org.moon.figura.avatar.Avatar;
import org.moon.figura.avatar.AvatarManager;
import org.moon.figura.avatar.local.CacheAvatarLoader;
import org.moon.figura.avatar.local.LocalAvatarFetcher;
import org.moon.figura.avatar.local.LocalAvatarLoader;
import org.moon.figura.backend2.NetworkStuff;
import org.moon.figura.commands.FiguraCommands;
import org.moon.figura.config.ConfigManager;
import org.moon.figura.config.Configs;
import org.moon.figura.entries.EntryPointManager;
import org.moon.figura.gui.Emojis;
import org.moon.figura.lua.FiguraLuaPrinter;
import org.moon.figura.lua.docs.FiguraDocsManager;
import org.moon.figura.mixin.SkullBlockEntityAccessor;
import org.moon.figura.permissions.PermissionManager;
import org.moon.figura.resources.FiguraRuntimeResources;
import org.moon.figura.utils.ColorUtils;
import org.moon.figura.utils.IOUtils;
import org.moon.figura.utils.TextUtils;
import org.moon.figura.utils.Version;
import org.moon.figura.wizards.AvatarWizard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Calendar;
import java.util.UUID;

public class FiguraMod implements ClientModInitializer {

    public static final String MOD_ID = "figura";
    public static final String MOD_NAME = "Figura";
    public static final ModMetadata METADATA = FabricLoader.getInstance().getModContainer(MOD_ID).get().getMetadata();
    public static final Version VERSION = new Version(METADATA.getVersion().getFriendlyString());
    public static final boolean DEBUG_MODE = true;
    public static final Calendar CALENDAR = Calendar.getInstance();
    public static final Path GAME_DIR = FabricLoader.getInstance().getGameDir().normalize();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static int ticks;
    public static Entity extendedPickEntity;
    public static Component splashText;
    public static boolean parseMessages = true;
    public static boolean processingKeybind;

    @Override
    public void onInitializeClient() {
        //init managers
        EntryPointManager.init();
        ConfigManager.init();
        PermissionManager.init();
        LocalAvatarFetcher.init();
        CacheAvatarLoader.init();
        FiguraDocsManager.init();
        FiguraCommands.init();
        FiguraRuntimeResources.init();

        //register reload listener
        ResourceManagerHelper managerHelper = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
        managerHelper.registerReloadListener(LocalAvatarLoader.AVATAR_LISTENER);
        managerHelper.registerReloadListener(Emojis.RESOURCE_LISTENER);
        managerHelper.registerReloadListener(AvatarWizard.RESOURCE_LISTENER);
        managerHelper.registerReloadListener(AvatarManager.RESOURCE_RELOAD_EVENT);
    }

    public static void tick() {
        pushProfiler("network");
        NetworkStuff.tick();
        popPushProfiler("files");
        LocalAvatarLoader.tick();
        LocalAvatarFetcher.tick();
        popPushProfiler("avatars");
        AvatarManager.tickLoadedAvatars();
        popPushProfiler("chatPrint");
        FiguraLuaPrinter.printChatFromQueue();
        popProfiler();
        ticks++;
    }

    // -- Helper Functions -- //

    //debug print
    public static void debug(String str, Object... args) {
        if (DEBUG_MODE) LOGGER.info("[DEBUG] " + str, args);
        else LOGGER.debug(str, args);
    }

    //mod root directory
    public static Path getFiguraDirectory() {
        String config = Configs.MAIN_DIR.value;
        Path p = config.isBlank() ? GAME_DIR.resolve(MOD_ID) : Path.of(config);
        return IOUtils.createDirIfNeeded(p);
    }

    //mod cache directory
    public static Path getCacheDirectory() {
        return IOUtils.getOrCreateDir(getFiguraDirectory(), "cache");
    }

    //get local player uuid
    public static UUID getLocalPlayerUUID() {
        return Minecraft.getInstance().getUser().getGameProfile().getId();
    }

    public static boolean isLocal(UUID other) {
        return getLocalPlayerUUID().equals(other);
    }

    /**
     * Sends a chat message right away. Use when you know your message is safe.
     * If your message is unsafe, (generated by a user), use luaSendChatMessage instead.
     * @param message - text to send
     */
    public static void sendChatMessage(Component message) {
        if (Minecraft.getInstance().gui != null) {
            parseMessages = false;
            Minecraft.getInstance().gui.getChat().addMessage(TextUtils.replaceTabs(message));
            parseMessages = true;
        } else {
            LOGGER.info(message.getString());
        }
    }

    /**
     * Converts a player name to UUID using minecraft internal functions.
     * @param playerName - the player name
     * @return - the player's uuid or null
     */
    public static UUID playerNameToUUID(String playerName) {
        GameProfileCache cache = SkullBlockEntityAccessor.getProfileCache();
        if (cache == null)
            return null;

        var profile = cache.get(playerName);
        return profile.isEmpty() ? null : profile.get().getId();
    }

    public static Style getAccentColor() {
        Avatar avatar = AvatarManager.getAvatarForPlayer(getLocalPlayerUUID());
        int color = avatar != null ? ColorUtils.rgbToInt(ColorUtils.userInputHex(avatar.color, ColorUtils.Colors.FRAN_PINK.vec)) : ColorUtils.Colors.FRAN_PINK.hex;
        return Style.EMPTY.withColor(color);
    }

    // -- profiler -- //

    public static void pushProfiler(String name) {
        Minecraft.getInstance().getProfiler().push(name);
    }

    public static void pushProfiler(Avatar avatar) {
        Minecraft.getInstance().getProfiler().push(avatar.entityName.isBlank() ? avatar.owner.toString() : avatar.entityName);
    }

    public static void popPushProfiler(String name) {
        Minecraft.getInstance().getProfiler().popPush(name);
    }

    public static void popProfiler() {
        Minecraft.getInstance().getProfiler().pop();
    }

    public static <T> T popReturnProfiler(T var) {
        Minecraft.getInstance().getProfiler().pop();
        return var;
    }

    public static void popProfiler(int times) {
        var profiler = Minecraft.getInstance().getProfiler();
        for (int i = 0; i < times; i++)
            profiler.pop();
    }
}
