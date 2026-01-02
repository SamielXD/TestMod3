package fuse;

import arc.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.mod.*;

public class FuseMod extends Mod {
    
    public static boolean enabled = false;
    public static FuseCamera camera;
    public static FuseRenderer renderer;
    public static FuseControls controls;
    public static FuseHUD hud;
    
    public FuseMod() {
        Log.info("Fuse mod initializing...");
        
        Events.on(ClientLoadEvent.class, e -> {
            initialize();
        });
    }
    
    private void initialize() {
        Log.info("Fuse systems starting...");
        
        camera = new FuseCamera();
        renderer = new FuseRenderer();
        controls = new FuseControls();
        hud = new FuseHUD();
        
        Events.on(UnitControlEvent.class, event -> {
            if (event.player == Vars.player && event.unit != null) {
                enableFuse();
            }
        });
        
        Events.run(Trigger.update, () -> {
            if (enabled && (Vars.player == null || Vars.player.unit() == null || Vars.player.unit().isPlayer())) {
                disableFuse();
            }
        });
        
        Log.info("Fuse mod loaded successfully!");
    }
    
    public static void enableFuse() {
        enabled = true;
        controls.show();
        hud.show();
        Log.info("Fuse mode: ACTIVE");
    }
    
    public static void disableFuse() {
        enabled = false;
        controls.hide();
        hud.hide();
        Log.info("Fuse mode: INACTIVE");
    }
}