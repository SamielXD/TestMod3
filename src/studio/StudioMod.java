package studio;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.type.*;
import mindustry.world.*;

public class StudioMod extends Mod {
    public static Seq<Script> loadedScripts = new Seq<>();
    public static Fi scriptsFolder;

    private NodeEditor nodeEditor;

    public StudioMod() {
        Log.info("Studio - Visual Scripting System loading...");
    }

    @Override
    public void init() {
        Log.info("Studio initializing...");

        scriptsFolder = Core.files.local("mods/studio-scripts/");
        scriptsFolder.mkdirs();

        nodeEditor = new NodeEditor();

        Events.on(ClientLoadEvent.class, e -> {
            setupUI();
            loadAllScripts();
        });

        Events.on(WorldLoadEvent.class, e -> {
            Log.info("WorldLoadEvent triggered - executing 'On Start' scripts");
            executeEventScripts("On Start");
        });

        Events.on(WaveEvent.class, e -> {
            Log.info("WaveEvent triggered - executing 'On Wave' scripts");
            executeEventScripts("On Wave");
        });

        Events.on(UnitCreateEvent.class, e -> {
            Log.info("UnitCreateEvent triggered - executing 'On Unit Spawn' scripts");
            executeEventScripts("On Unit Spawn");
        });

        Log.info("Studio loaded successfully!");
    }

    void setupUI() {
        Vars.ui.menufrag.addButton("Studio", Icon.edit, () -> {
            nodeEditor.show();
        });
    }

    public static void loadAllScripts() {
        loadedScripts.clear();

        for(Fi file : scriptsFolder.list()) {
            if(file.extension().equals("json")) {
                try {
                    String json = file.readString();
                    Seq<NodeEditor.NodeData> nodeDataList = new Json().fromJson(Seq.class, NodeEditor.NodeData.class, json);

                    Script script = new Script();
                    script.fileName = file.name();
                    script.name = file.nameWithoutExtension();
                    script.enabled = true;
                    script.nodes = new Seq<>();

                    Seq<Node> loadedNodes = new Seq<>();

                    // Load all nodes first
                    for(NodeEditor.NodeData data : nodeDataList) {
                        Node node = new Node();
                        node.id = data.id;
                        node.type = data.type;
                        node.label = data.label;
                        node.x = data.x;
                        node.y = data.y;
                        node.value = data.value;
                        node.setupInputs();
                        loadedNodes.add(node);
                    }

                    // Restore connections
                    for(int i = 0; i < nodeDataList.size; i++) {
                        NodeEditor.NodeData data = nodeDataList.get(i);
                        Node node = loadedNodes.get(i);

                        for(String connId : data.connectionIds) {
                            Node target = loadedNodes.find(n -> n.id.equals(connId));
                            if(target != null) {
                                node.connections.add(target);
                            }
                        }
                    }

                    script.nodes = loadedNodes;
                    loadedScripts.add(script);
                    Log.info("Loaded script: " + file.name() + " with " + script.nodes.size + " nodes");
                } catch(Exception ex) {
                    Log.err("Failed to load script: " + file.name(), ex);
                }
            }
        }
    }

    public static void executeEventScripts(String eventLabel) {
        for(Script script : loadedScripts) {
            if(!script.enabled) continue;

            for(Node node : script.nodes) {
                if(node.type.equals("event") && node.label.equalsIgnoreCase(eventLabel)) {
                    Log.info("Executing event node: " + node.label + " in script: " + script.name);
                    executeNodeChain(node, script);
                }
            }
        }
    }

    public static void executeNodeChain(Node node, Script script) {
        executeNode(node, script);

        // Execute all connected nodes
        for(Node conn : node.connections) {
            executeNode(conn, script);
        }
    }

    public static void executeNode(Node node, Script script) {
        Log.info("Executing node: " + node.label + " (type: " + node.type + ")");

        switch(node.type) {
            case "action":
                executeAction(node);
                // Continue to connected nodes after action
                for(Node conn : node.connections) {
                    executeNode(conn, script);
                }
                break;
                
            case "condition":
                if(evaluateCondition(node)) {
                    for(Node conn : node.connections) {
                        executeNode(conn, script);
                    }
                }
                break;
                
            case "event":
                // Event nodes trigger their connections
                for(Node conn : node.connections) {
                    executeNode(conn, script);
                }
                break;
        }
    }

    public static void executeAction(Node node) {
        String label = node.label.toLowerCase();
        Log.info("Executing action: " + label + " with value: " + node.value);

        if(label.contains("message")) {
            String message = node.value.isEmpty() ? "Hello from Studio!" : node.value;
            Vars.ui.showInfoFade(message);
            Log.info("Displayed message: " + message);
        }
        else if(label.contains("spawn unit")) {
            try {
                String unitName = node.value.isEmpty() ? "dagger" : node.value.toLowerCase().trim();
                
                // Find unit type
                UnitType type = Vars.content.units().find(u -> u.name.equals(unitName));
                if(type == null) {
                    Log.warn("Unit type not found: " + unitName + ", using dagger");
                    type = UnitTypes.dagger;
                }

                // Spawn unit at player position or world center
                float spawnX = 0f, spawnY = 0f;
                if(Vars.player != null && Vars.player.unit() != null) {
                    spawnX = Vars.player.x;
                    spawnY = Vars.player.y;
                } else if(Vars.state != null && Vars.state.rules != null && Vars.state.rules.defaultTeam != null) {
                    spawnX = Vars.world.width() * 4f; // World center
                    spawnY = Vars.world.height() * 4f;
                }

                Unit unit = type.spawn(Vars.player.team(), spawnX, spawnY);
                Log.info("Spawned unit: " + type.name + " at (" + spawnX + ", " + spawnY + ")");
                Vars.ui.showInfoFade("Spawned " + type.name + "!");
            } catch(Exception e) {
                Log.err("Failed to spawn unit", e);
                Vars.ui.showInfoFade("Failed to spawn unit: " + e.getMessage());
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
                        Log.info("Set block at (" + x + ", " + y + ") to " + blockName);
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
                return false; // Don't execute connections immediately
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