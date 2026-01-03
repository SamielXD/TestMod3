package studio;

import arc.*;
import arc.files.*;
import arc.scene.ui.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

public class StudioMod extends Mod {
    public static Seq<Script> loadedScripts = new Seq<>();
    public static Fi scriptsFolder;
    
    public StudioMod() {
        scriptsFolder = Core.files.local("mods/studio-scripts/");
        scriptsFolder.mkdirs();
        
        Events.on(ClientLoadEvent.class, e -> {
            Time.runTask(10f, () -> {
                Vars.ui.menufrag.addButton("Studio", Icon.edit, () -> {
                    new NodeEditor().show();
                });
                
                loadAllScripts();
            });
        });
        
        Events.on(WorldLoadEvent.class, e -> {
            executeEventScripts("worldload");
        });
        
        Events.on(WaveEvent.class, e -> {
            executeEventScripts("wave");
        });
        
        Events.on(UnitCreateEvent.class, e -> {
            executeEventScripts("unitspawn");
        });
    }
    
    public static void loadAllScripts() {
        loadedScripts.clear();
        
        for(Fi file : scriptsFolder.list()) {
            if(file.extension().equals("json")) {
                try {
                    String json = file.readString();
                    Script script = new Script();
                    script.fileName = file.name();
                    loadedScripts.add(script);
                    Log.info("Loaded script: " + file.name());
                } catch(Exception ex) {
                    Log.err("Failed to load script: " + file.name(), ex);
                }
            }
        }
    }
    
    public static void executeEventScripts(String eventType) {
        for(Script script : loadedScripts) {
            if(!script.enabled) continue;
            
            for(Node node : script.nodes) {
                if(node.type.equals("event")) {
                    String trigger = node.value.toLowerCase().replace(" ", "");
                    if(trigger.equals(eventType) || 
                       (trigger.equals("onstart") && eventType.equals("worldload")) ||
                       (trigger.equals("onwave") && eventType.equals("wave")) ||
                       (trigger.equals("onunitspawn") && eventType.equals("unitspawn"))) {
                        executeNode(node, script);
                    }
                }
            }
        }
    }
    
    public static void executeNode(Node node, Script script) {
        switch(node.type) {
            case "action":
                executeAction(node);
                break;
            case "condition":
                if(evaluateCondition(node)) {
                    for(Node conn : node.connections) {
                        executeNode(conn, script);
                    }
                }
                return;
        }
        
        for(Node conn : node.connections) {
            executeNode(conn, script);
        }
    }
    
    public static void executeAction(Node node) {
        String label = node.label.toLowerCase();
        
        if(label.contains("message")) {
            String message = node.value.isEmpty() ? "Hello from Studio!" : node.value;
            Vars.ui.showInfoFade(message);
        }
        else if(label.contains("spawn unit")) {
            try {
                String unitName = node.value.isEmpty() ? "dagger" : node.value.toLowerCase();
                UnitType type = Vars.content.units().find(u -> u.name.equals(unitName));
                if(type == null) type = UnitTypes.dagger;
                
                if(Vars.player != null && Vars.player.unit() != null) {
                    Unit unit = type.spawn(Vars.player.team(), Vars.player.x, Vars.player.y);
                }
            } catch(Exception e) {
                Log.err("Failed to spawn unit", e);
            }
        }
        else if(label.contains("set block")) {
            try {
                String[] parts = node.value.split(",");
                if(parts.length >= 3) {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    String blockName = parts[2].trim().toLowerCase();
                    
                    Block block = Vars.content.blocks().find(b -> b.name.equals(blockName));
                    if(block != null && Vars.world != null) {
                        Vars.world.tile(x, y).setNet(block, Vars.player.team(), 0);
                    }
                }
            } catch(Exception e) {
                Log.err("Failed to set block", e);
            }
        }
    }
    
    public static boolean evaluateCondition(Node node) {
        String label = node.label.toLowerCase();
        
        if(label.contains("wait")) {
            try {
                float seconds = node.value.isEmpty() ? 1f : Float.parseFloat(node.value);
                Timer.schedule(() -> {
                    for(Node conn : node.connections) {
                        executeNode(conn, null);
                    }
                }, seconds);
                return false;
            } catch(Exception e) {
                return true;
            }
        }
        
        if(label.contains("if")) {
            return !node.value.isEmpty();
        }
        
        return true;
    }
    
    public static class Script {
        public String name = "Untitled";
        public String fileName = "";
        public boolean enabled = true;
        public Seq<Node> nodes = new Seq<>();
    }
}