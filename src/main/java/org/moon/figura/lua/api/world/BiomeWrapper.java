package org.moon.figura.lua.api.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import org.moon.figura.lua.LuaWhitelist;
import org.moon.figura.lua.docs.LuaFieldDoc;
import org.moon.figura.lua.docs.LuaTypeDoc;

import java.lang.ref.WeakReference;

@LuaWhitelist
@LuaTypeDoc(
        name = "Biome",
        description = "A proxy for a Minecraft biome. Instances are obtained through the WorldAPI. " +
                "(Not implemented yet, currently only has a \"name\" field)"
)
public class BiomeWrapper {

    private final WeakReference<Biome> wrappedBiome;

    public BiomeWrapper(Biome biome) {
        this.wrappedBiome = new WeakReference<>(biome);
        name = Minecraft.getInstance().level.registryAccess().registry(Registry.BIOME_REGISTRY).get().getKey(biome).toString();
    }

    @LuaWhitelist
    @LuaFieldDoc(
            canEdit = false,
            description = "The name of the biome, according to the registry."
    )
    public final String name;

}
