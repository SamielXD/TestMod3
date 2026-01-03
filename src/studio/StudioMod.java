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
    public static String currentModName = "studio-temp";

    private NodeEditor nodeEditor;
    private ImageButton floatingButton;
    private Table floatingButtonContainer;
    private boolean floatingButtonEnabled = true;

    public StudioMod() {
        Log.info("Studio - Visual Scripting System loading...");
    }

    @Override
    public void init() {
        Log.info("Studio initializing...");

        scriptsFolder = Vars.modDirectory.child("studio-scripts");
        scriptsFolder.mkdirs();
        
        modsRootFolder = Vars.modDirectory;
        Log.info("Mods folder: " + modsRootFolder.absolutePath());

        nodeEditor = new NodeEditor();

        Events.on(ClientLoadEvent.class, e -> {
            setupUI();
            setupSettings();
            setupFloatingButton();
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
        Log.info("Real mods path: " + modsRootFolder.absolutePath());
    }

    void setupFloatingButton() {
        floatingButtonEnabled = Core.settings.getBool("studio-floating-button", true);
        
        if(!floatingButtonEnabled) return;

        floatingButtonContainer = new Table();
        floatingButtonContainer.setFillParent(true);
        floatingButtonContainer.touchable = Touchable.childrenOnly;

        floatingButton = new ImageButton(Icon.edit, Styles.clearTogglei);
        floatingButton.resizeImage(40f);
        
        Table buttonWrapper = new Table();
        buttonWrapper.background(Styles.black6);
        buttonWrapper.add(floatingButton).size(80f, 80f);
        
        buttonWrapper.update(() -> {
            buttonWrapper.setPosition(
                Core.settings.getFloat("studio-button-x", Core.graphics.getWidth() - 100f),
                Core.settings.getFloat("studio-button-y", Core.graphics.getHeight() / 2f)
            );
        });

        floatingButton.clicked(() -> {
            showStudioMenu();
        });

        floatingButton.addListener(new InputListener() {
            float lastX, lastY;
            boolean dragging = false;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                lastX = x;
                lastY = y;
                dragging = false;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                dragging = true;
                float newX = buttonWrapper.x + (x - lastX);
                float newY = buttonWrapper.y + (y - lastY);
                
                newX = arc.math.Mathf.clamp(newX, 0, Core.graphics.getWidth() - 80f);
                newY = arc.math.Mathf.clamp(newY, 0, Core.graphics.getHeight() - 80f);
                
                Core.settings.put("studio-button-x", newX);
                Core.settings.put("studio-button-y", newY);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if(!dragging) {
                    showStudioMenu();
                }
            }
        });

        floatingButtonContainer.add(buttonWrapper);
        Core.scene.add(floatingButtonContainer);
        
        Log.info("Floating Studio button added!");
    }

    void setupUI() {
        Vars.ui.menufrag.addButton("Studio", Icon.edit, () -> {
            showStudioMenu();
        });
    }

    void setupSettings() {
        Vars.ui.settings.addCategory("Studio", Icon.edit, table -> {
            table.add("[accent]═══ Studio Visual Scripting ═══").padTop(10).row();
            
            table.table(t -> {
                t.left();
                t.add("Floating Button:").left().padRight(10);
                t.check("", Core.settings.getBool("studio-floating-button", true), val -> {
                    Core.settings.put("studio-floating-button", val);
                    Vars.ui.showInfoFade("Restart game to apply");
                }).size(50f);
            }).padTop(10).row();
            
            table.table(t -> {
                t.left();
                t.add("Button Layout:").left().padRight(10);
                t.button(Core.settings.getString("studio-ui-layout", "bottom"), () -> {
                    String current = Core.settings.getString("studio-ui-layout", "bottom");
                    String newLayout = current.equals("bottom") ? "top" : "bottom";
                    Core.settings.put("studio-ui-layout", newLayout);
                    Vars.ui.showInfoFade("Restart editor to apply: " + newLayout);
                }).size(200f, 50f);
            }).padTop(10).row();
            
            table.table(t -> {
                t.left();
                t.add("Auto-save:").left().padRight(10);
                t.check("", Core.settings.getBool("studio-autosave", false), val -> {
                    Core.settings.put("studio-autosave", val);
                }).size(50f);
            }).padTop(10).row();
            
            table.table(t -> {
                t.left();
                t.add("Grid Size:").left().padRight(10);
                t.slider(20, 100, 10, Core.settings.getInt("studio-grid-size", 50), val -> {
                    Core.settings.put("studio-grid-size", (int)val);
                }).width(200f);
            }).padTop(10).row();
            
            table.table(t -> {
                t.left();
                t.add("Mods Folder:").left().padRight(10);
                t.add("[lightgray]" + modsRootFolder.absolutePath()).left();
            }).padTop(10).row();
            
            table.button("Open Mods Folder", Icon.folder, () -> {
                Vars.ui.showInfoText("Mods Folder Location", 
                    "Your mods are stored at:\n" + modsRootFolder.absolutePath() + 
                    "\n\nOn Android:\n/data/io.anuke.mindustry/files/mods");
            }).size(300f, 60f).padTop(20).row();
            
            table.button("Reset Button Position", Icon.refresh, () -> {
                Core.settings.put("studio-button-x", Core.graphics.getWidth() - 100f);
                Core.settings.put("studio-button-y", Core.graphics.getHeight() / 2f);
                Vars.ui.showInfoFade("Button position reset!");
            }).size(300f, 60f).padTop(10);
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
        for(Fi folder : modsRootFolder.list()) {
            if(folder.isDirectory() && (folder.child("mod.hjson").exists() || folder.child("mod.json").exists())) {
                mods.add(folder);
            }
        }

        if(mods.size == 0) {
            Label label = new Label("[lightgray]No mods created yet\nUse 'Create Mod' node");
            label.setFontScale(1.3f);
            dialog.cont.add(label).row();
        } else {
            for(Fi modFolder : mods) {
                String modName = modFolder.name();
                dialog.cont.button("[cyan]" + modName + "\n[lightgray]Tap to select", () -> {
                    currentModName = modName;
                    Vars.ui.showInfoFade("Selected mod: " + modName);
                    dialog.hide();
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
        if(action.inputs.size > 0) action.inputs.get(0).value = "Hello from Studio!";

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
        if(action.inputs.size >= 5) {
            action.inputs.get(0).value = "dagger";
            action.inputs.get(1).value = "core";
            action.inputs.get(4).value = "3";
        }

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
        if(wait.inputs.size > 0) wait.inputs.get(0).value = "2";
        Node action = new Node("action", "Message", 1000f, 0f, Color.blue);
        action.value = "Welcome to this map!";
        if(action.inputs.size > 0) action.inputs.get(0).value = "Welcome to this map!";

        event.connections.add(wait);
        wait.connections.add(action);

        nodeEditor.canvas.nodes.add(event);
        nodeEditor.canvas.nodes.add(wait);
        nodeEditor.canvas.nodes.add(action);

        Vars.ui.showInfoFade("Example loaded! Click Run to test");
    }

    public static void loadAllScripts() {
        loadedScripts.clear();

        if(!scriptsFolder.exists()) return;

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
                String[] parts = node.value.split("\\|");
                String unitName = parts.length > 0 ? parts[0] : "dagger";
                
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

    public static void executeModNode(Node node) {
        String label = node.label.toLowerCase();

        try {
            Fi modFolder = modsRootFolder.child(currentModName);
            
            if(label.contains("create mod")) {
                String modName = node.inputs.get(0).value;
                currentModName = modName;
                modFolder = modsRootFolder.child(modName);
                
                modFolder.mkdirs();
                modFolder.child("scripts").mkdirs();
                modFolder.child("content").mkdirs();
                modFolder.child("content/blocks").mkdirs();
                modFolder.child("content/units").mkdirs();
                modFolder.child("content/items").mkdirs();
                modFolder.child("sprites").mkdirs();

                String displayName = node.inputs.get(1).value;
                String author = node.inputs.get(2).value;
                String description = node.inputs.get(3).value;
                String version = node.inputs.get(4).value;

                String modHjson = "name: \"" + modName + "\"\n" +
                                "displayName: \"" + displayName + "\"\n" +
                                "author: \"" + author + "\"\n" +
                                "description: \"" + description + "\"\n" +
                                "version: \"" + version + "\"\n" +
                                "minGameVersion: \"146\"\n";

                modFolder.child("mod.hjson").writeString(modHjson);
                Vars.ui.showInfoFade("Created mod at: " + modFolder.absolutePath());
                Log.info("Mod created: " + modFolder.absolutePath());
            }
            else if(label.contains("create block")) {
                String blockName = node.inputs.get(0).value;
                String displayName = node.inputs.get(1).value;
                String blockType = node.inputs.get(2).value;
                String health = node.inputs.get(3).value;
                String size = node.inputs.get(4).value;

                String blockJson = "{\n" +
                                 "  \"type\": \"" + blockType + "\",\n" +
                                 "  \"name\": \"" + blockName + "\",\n" +
                                 "  \"description\": \"" + displayName + "\",\n" +
                                 "  \"health\": " + health + ",\n" +
                                 "  \"size\": " + size + "\n" +
                                 "}\n";

                modFolder.child("content/blocks/" + blockName + ".json").writeString(blockJson);
                Vars.ui.showInfoFade("Created block: " + blockName);
            }
            else if(label.contains("create unit")) {
                String unitName = node.inputs.get(0).value;
                String displayName = node.inputs.get(1).value;
                String speed = node.inputs.get(2).value;
                String health = node.inputs.get(3).value;
                String flying = node.inputs.get(4).value;

                String unitJson = "{\n" +
                                "  \"type\": \"flying\",\n" +
                                "  \"name\": \"" + unitName + "\",\n" +
                                "  \"description\": \"" + displayName + "\",\n" +
                                "  \"speed\": " + speed + ",\n" +
                                "  \"health\": " + health + ",\n" +
                                "  \"flying\": " + flying + "\n" +
                                "}\n";

                modFolder.child("content/units/" + unitName + ".json").writeString(unitJson);
                Vars.ui.showInfoFade("Created unit: " + unitName);
            }
            else if(label.contains("create item")) {
                String itemName = node.inputs.get(0).value;
                String displayName = node.inputs.get(1).value;
                String color = node.inputs.get(2).value;

                String itemJson = "{\n" +
                                "  \"type\": \"resource\",\n" +
                                "  \"name\": \"" + itemName + "\",\n" +
                                "  \"description\": \"" + displayName + "\",\n" +
                                "  \"color\": \"" + color + "\"\n" +
                                "}\n";

                modFolder.child("content/items/" + itemName + ".json").writeString(itemJson);
                Vars.ui.showInfoFade("Created item: " + itemName);
            }
            else if(label.contains("add sprite")) {
                String spriteName = node.inputs.get(0).value;
                String filePath = node.inputs.get(1).value;
                Vars.ui.showInfoFade("Sprite: " + spriteName + " - Copy " + filePath + " to: " + modFolder.child("sprites").absolutePath());
            }
            else if(label.contains("create script")) {
                String scriptName = node.inputs.get(0).value;
                String scriptContent = node.inputs.get(1).value;

                modFolder.child("scripts/" + scriptName).writeString(scriptContent);
                Vars.ui.showInfoFade("Created script: " + scriptName);
            }
        } catch(Exception e) {
            Log.err("Mod node failed", e);
            Vars.ui.showInfoFade("Error: " + e.getMessage());
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