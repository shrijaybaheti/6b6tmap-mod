package net.map6b6t.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.map6b6t.SpawnMapMod;

public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new MapConfigScreen(SpawnMapMod.getInstance()::resetSessionCache, parent);
    }
}
