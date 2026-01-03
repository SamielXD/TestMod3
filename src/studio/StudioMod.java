package studio;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
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
    public static Fi modsRootFolder;
    public static String currentModName = "none";

    private NodeEditor nodeEditor;

    public StudioMod() {
        Log.info("Studio - Visual Scripting System loading...");
    }

    @Override
    public void init() {
        Log.info("Studio initializing...");

        scriptsFolder = Core.files.local("mods/studio-scripts/");
        scriptsFolder.mkdirs();
        
        modsRootFolder = Core.files.local("mods/");
        modsRootFolder.mkdirs();

        nodeEditor = new NodeEditor();

        Events.on(ClientLoadEvent.class, e -> {
            setupUI();
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

        dialog.cont.button("[cyan]Simple Mod\n[lightgray]Create a basic mod structure", () -> {
            createExampleSimpleMod();
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
            Label label = new Label("[lightgray]No mods created yet\nUse Mod Creator mode");
            label.setFontScale(1.3f);
            dialog.cont.add(label).row();
        } else {
            for(Fi modFolder : mods) {
                String modName = modFolder.name();
                Table row = new Table();
                row.button("[cyan]" + modName, () -> {
                    Vars.ui.showInfoText("Mod: " + modName, "Location: " + modFolder.path());
                }).growX();
                row.button("Delete", Icon.trash, () -> {
                    deleteMod(modFolder);
                    dialog.hide();
                    showModManager();
                }).size(120f, 100f);
                dialog.cont.add(row).fillX().row();
            }
        }

        dialog.addCloseButton();
        dialog.show();
    }

    void deleteMod(Fi modFolder) {
        Vars.ui.showConfirm("Delete Mod?", "Delete " + modFolder.name() + "?\nThis cannot be undone!", () -> {
            modFolder.deleteDirectory();
            Vars.ui.showInfoFade("Deleted: " + modFolder.name());
        });
    }

    void createExampleHelloWorld() {
        nodeEditor.canvas.nodes.clear();
        Node event = new Node("event", "On Start", 0f, 0f, Color.green);
        Node action = new Node("action", "Message", 500f, 0f, Color.blue);
        action.inputs.get(0).value = "Hello from Studio!";
        action.value = "Hello from Studio!";
        event.connections.add(action);
        nodeEditor.canvas.nodes.add(event);
        nodeEditor.canvas.nodes.add(action);
        Vars.ui.showInfoFade("Example loaded!");
    }

    void createExampleAutoSpawn() {
        nodeEditor.canvas.nodes.clear();
        Node event = new Node("event", "On Wave", 0f, 0f, Color.green);
        Node action = new Node("action", "Spawn Unit", 500f, 0f, Color.blue);
        action.inputs.get(0).value = "dagger";
        action.inputs.get(1).value = "3";
        action.value = "dagger|3";
        event.connections.add(action);
        nodeEditor.canvas.nodes.add(event);
        nodeEditor.canvas.nodes.add(action);
        Vars.ui.showInfoFade("Example loaded!");
    }

    void createExampleSimpleMod() {
        nodeEditor.editorMode = "mod";
        nodeEditor.canvas.nodes.clear();
        
        Node modFolder = new Node("mod", "Create Mod Folder", 0f, 0f, Color.cyan);
        modFolder.inputs.get(0).value = "mymod";
        modFolder.inputs.get(1).value = "My First Mod";
        modFolder.inputs.get(2).value = "YourName";
        
        Node modHjson = new Node("mod", "Create mod.hjson", 500f, 0f, Color.royal);
        modHjson.inputs.get(0).value = "mymod";
        modHjson.inputs.get(1).value = "My First Mod";
        modHjson.inputs.get(2).value = "YourName";
        
        Node contentFolder = new Node("mod", "Create Folder", 500f, 300f, Color.sky);
        contentFolder.inputs.get(0).value = "content";
        
        Node blocksFolder = new Node("mod", "Create Folder", 1000f, 300f, Color.sky);
        blocksFolder.inputs.get(0).value = "blocks";
        
        Node blockFile = new Node("mod", "Create Block File", 1500f, 300f, Color.pink);
        blockFile.inputs.get(0).value = "my-wall";
        blockFile.inputs.get(1).value = "Wall";
        blockFile.inputs.get(2).value = "500";
        blockFile.inputs.get(3).value = "1";
        
        modFolder.connections.add(modHjson);
        modFolder.connections.add(contentFolder);
        contentFolder.connections.add(blocksFolder);
        blocksFolder.connections.add(blockFile);
        
        nodeEditor.canvas.nodes.add(modFolder);
        nodeEditor.canvas.nodes.add(modHjson);
        nodeEditor.canvas.nodes.add(contentFolder);
        nodeEditor.canvas.nodes.add(blocksFolder);
        nodeEditor.canvas.nodes.add(blockFile);
        
        Vars.ui.showInfoFade("Example loaded! Click Run to create mod.");
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
                    if(trigger.equals(eventType)) {
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
            case "logic":
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
            String message = node.inputs.size > 0 ? node.inputs.get(0).value : "Hello!";
            Vars.ui.showInfoFade(message);
        }
        else if(label.contains("spawn unit")) {
            try {
                String unitName = node.inputs.get(0).value;
                int amount = Integer.parseInt(node.inputs.get(1).value);
                UnitType type = Vars.content.units().find(u -> u.name.equals(unitName));
                if(type == null) type = UnitTypes.dagger;
                if(Vars.player != null && Vars.player.unit() != null) {
                    for(int i = 0; i < amount; i++) {
                        type.spawn(Vars.player.team(), Vars.player.x, Vars.player.y);
                    }
                }
            } catch(Exception e) {
                Log.err("Failed to spawn unit", e);
            }
        }
        else if(label.contains("set block")) {
            try {
                int x = Integer.parseInt(node.inputs.get(0).value);
                int y = Integer.parseInt(node.inputs.get(1).value);
                String blockName = node.inputs.get(2).value;
                Block block = Vars.content.blocks().find(b -> b.name.equals(blockName));
                if(block != null && Vars.world != null) {
                    Vars.world.tile(x, y).setNet(block, Vars.player.team(), 0);
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
                float seconds = Float.parseFloat(node.inputs.get(0).value);
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
        return true;
    }

    public static class Script {
        public String name = "Untitled";
        public String fileName = "";
        public boolean enabled = true;
        public Seq<Node> nodes = new Seq<>();
    }
}