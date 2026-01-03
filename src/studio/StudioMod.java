package studio;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
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
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

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
            setupSettings();
            loadAllScripts();
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

        Log.info("Studio loaded successfully!");
    }

    void setupUI() {
        Vars.ui.menufrag.addButton("Studio", Icon.edit, () -> {
            showStudioMenu();
        });
    }

    void setupSettings() {
        Vars.ui.settings.addCategory("Studio", Icon.edit, table -> {
            table.add("[accent]═══ Studio Settings ═══").padTop(10).row();
        });
    }

    void showStudioMenu() {
        BaseDialog menuDialog = new BaseDialog("Studio");
        menuDialog.cont.defaults().size(500f, 120f).pad(15f);

        Label titleLabel = new Label("[cyan]Choose Mode:");
        titleLabel.setFontScale(2f);
        menuDialog.cont.add(titleLabel).padBottom(30f).row();

        menuDialog.cont.button("[lime]Script Editor\n[lightgray]Create visual scripts", Icon.edit, () -> {
            nodeEditor.show();
            menuDialog.hide();
        }).row();

        menuDialog.cont.button("[sky]Example Scripts\n[lightgray]Browse pre-made scripts", Icon.book, () -> {
            showExampleScripts();
            menuDialog.hide();
        }).row();

        menuDialog.cont.button("[orange]Mod Manager\n[lightgray]Manage created mods", Icon.box, () -> {
            showModManager();
            menuDialog.hide();
        }).row();

        menuDialog.cont.button("[royal]Settings\n[lightgray]Configure Studio", Icon.settings, () -> {
            Vars.ui.settings.show();
            menuDialog.hide();
        }).row();

        menuDialog.addCloseButton();
        menuDialog.show();
    }

    void showExampleScripts() {
        BaseDialog dialog = new BaseDialog("Example Scripts");
        dialog.cont.defaults().size(550f, 100f).pad(10f);

        dialog.cont.button("[green]Hello World\n[lightgray]Shows a message", () -> {
            createExampleHelloWorld();
            dialog.hide();
            nodeEditor.show();
        }).row();

        dialog.cont.button("[blue]Auto Spawn Units\n[lightgray]Spawns units every wave", () -> {
            createExampleAutoSpawn();
            dialog.hide();
            nodeEditor.show();
        }).row();

        dialog.cont.button("[cyan]Welcome Message\n[lightgray]Greet player on start", () -> {
            createExampleWelcome();
            dialog.hide();
            nodeEditor.show();
        }).row();

        dialog.addCloseButton();
        dialog.show();
    }

    void showModManager() {
        BaseDialog dialog = new BaseDialog("Mod Manager");
        dialog.cont.defaults().size(500f, 100f).pad(10f);

        Seq<Fi> mods = new Seq<>();
        Fi modsFolder = Core.files.local("mods/");
        for(Fi folder : modsFolder.list()) {
            if(folder.isDirectory() && folder.child("mod.hjson").exists()) {
                mods.add(folder);
            }
        }

        if(mods.size == 0) {
            Label label = new Label("[lightgray]No mods created yet\nUse 'Create Mod Folder' node");
            label.setFontScale(1.3f);
            dialog.cont.add(label).row();
        } else {
            for(Fi modFolder : mods) {
                String modName = modFolder.name();
                dialog.cont.button("[cyan]" + modName + "\n[lightgray]" + modFolder.path(), () -> {
                    Vars.ui.showInfoText("Mod: " + modName, 
                        "Location: " + modFolder.path() + "\n\n" +
                        "To edit this mod:\n" +
                        "1. Open Studio Script Editor\n" +
                        "2. Add mod editing nodes\n" +
                        "3. Save and run your script");
                }).row();
            }
        }

        dialog.addCloseButton();
        dialog.show();
    }

    void createExampleHelloWorld() {
        nodeEditor.canvas.nodes.clear();

        Node event = new Node("event", "On Start", 0f, 0f, Color.green);
        Node action = new Node("action", "Message", 500f, 0f, Color.blue);
        action.value = "Hello from Studio!";
        action.inputs.get(0).value = "Hello from Studio!";

        event.connections.add(action);

        nodeEditor.canvas.nodes.add(event);
        nodeEditor.canvas.nodes.add(action);

        Vars.ui.showInfoFade("Example loaded! Click Run to test");
    }

    void createExampleAutoSpawn() {
        nodeEditor.canvas.nodes.clear();

        Node event = new Node("event", "On Wave", 0f, 0f, Color.green);
        Node action = new Node("action", "Spawn Unit", 500f, 0f, Color.blue);
        action.value = "dagger|core|0|0|3";
        action.inputs.get(0).value = "dagger";
        action.inputs.get(1).value = "core";
        action.inputs.get(4).value = "3";

        event.connections.add(action);

        nodeEditor.canvas.nodes.add(event);
        nodeEditor.canvas.nodes.add(action);

        Vars.ui.showInfoFade("Example loaded! Click Run to test");
    }

    void createExampleWelcome() {
        nodeEditor.canvas.nodes.clear();

        Node event = new Node("event", "On Start", 0f, 0f, Color.green);
        Node wait = new Node("condition", "Wait", 500f, 0f, Color.orange);
        wait.value = "2";
        wait.inputs.get(0).value = "2";
        Node action = new Node("action", "Message", 1000f, 0f, Color.blue);
        action.value = "Welcome to this map!";
        action.inputs.get(0).value = "Welcome to this map!";

        event.connections.add(wait);
        wait.connections.add(action);

        nodeEditor.canvas.nodes.add(event);
        nodeEditor.canvas.nodes.add(wait);
        nodeEditor.canvas.nodes.add(action);

        Vars.ui.showInfoFade("Example loaded! Click Run to test");
    }

    public static void loadAllScripts() {
        loadedScripts.clear();

        for(Fi file : scriptsFolder.list()) {
            if(file.extension().equals("json")) {
                try {
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

    public static void executeNodeChain(Node node, Script script) {
        executeNode(node, script);

        for(Node conn : node.connections) {
            executeNode(conn, script);
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