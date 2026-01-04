package studio;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
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
    public static ObjectMap<String, String> variables = new ObjectMap<>();

    private NodeEditor nodeEditor;
    private Table floatingButton;
    private boolean floatingButtonDragging = false;
    
    public static float buttonSize = 80f;
    public static Color buttonColor = Color.white;
    public static float buttonOpacity = 0.8f;
    public static boolean showLabels = true;

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

        loadSettings();

        nodeEditor = new NodeEditor();

        Events.on(ClientLoadEvent.class, e -> {
            setupUI();
            setupFloatingButton();
            setupSettingsMenu();
            loadAllScripts();
        });

        Events.on(WorldLoadEvent.class, e -> {
            Log.info("WorldLoadEvent - executing 'On Start'");
            executeEventScripts("On Start");
        });

        Events.on(WaveEvent.class, e -> {
            Log.info("WaveEvent - executing 'On Wave'");
            executeEventScripts("On Wave");
        });

        Events.on(BlockBuildEndEvent.class, e -> {
            if(e.breaking) return;
            Log.info("BuildEvent - executing 'On Build'");
            executeEventScripts("On Build");
        });

        Log.info("Studio loaded successfully!");
    }

    void loadSettings() {
        try {
            Fi settingsFile = Core.settings.getDataDirectory().child("studio-settings.json");
            if(settingsFile.exists()) {
                String json = settingsFile.readString();
                ObjectMap<String, String> settings = new ObjectMap<>();
                
                String[] lines = json.split("\n");
                for(String line : lines) {
                    if(line.contains(":")) {
                        String[] parts = line.replace("{", "").replace("}", "").replace("\"", "").split(":");
                        if(parts.length == 2) {
                            settings.put(parts[0].trim(), parts[1].trim().replace(",", ""));
                        }
                    }
                }
                
                if(settings.containsKey("buttonSize")) {
                    buttonSize = Float.parseFloat(settings.get("buttonSize"));
                }
                if(settings.containsKey("buttonOpacity")) {
                    buttonOpacity = Float.parseFloat(settings.get("buttonOpacity"));
                }
                if(settings.containsKey("showLabels")) {
                    showLabels = Boolean.parseBoolean(settings.get("showLabels"));
                }
                
                Log.info("Settings loaded!");
            }
        } catch(Exception e) {
            Log.err("Failed to load settings", e);
        }
    }

    void saveSettings() {
        try {
            Fi settingsFile = Core.settings.getDataDirectory().child("studio-settings.json");
            String json = "{\n" +
                "  \"buttonSize\": " + buttonSize + ",\n" +
                "  \"buttonOpacity\": " + buttonOpacity + ",\n" +
                "  \"showLabels\": " + showLabels + "\n" +
                "}";
            settingsFile.writeString(json);
            Log.info("Settings saved!");
        } catch(Exception e) {
            Log.err("Failed to save settings", e);
        }
    }

    void setupUI() {
        Vars.ui.menufrag.addButton("Studio", Icon.edit, () -> {
            showStudioMenu();
        });
    }

    void setupFloatingButton() {
        if(floatingButton != null) {
            floatingButton.remove();
        }

        floatingButton = new Table();
        floatingButton.setSize(buttonSize, buttonSize);
        
        float savedX = Core.settings.getFloat("studio-button-x", 100f);
        float savedY = Core.settings.getFloat("studio-button-y", 100f);
        floatingButton.setPosition(savedX, savedY);
        
        ImageButton button = new ImageButton(Icon.edit);
        button.getStyle().imageUpColor = buttonColor.cpy().a(buttonOpacity);
        button.setSize(buttonSize, buttonSize);
        
        button.clicked(() -> {
            if(!floatingButtonDragging) {
                showStudioMenu();
            }
        });

        button.addListener(new InputListener() {
            float startX, startY;
            float lastX, lastY;
            
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode btn) {
                startX = x;
                startY = y;
                lastX = floatingButton.x;
                lastY = floatingButton.y;
                floatingButtonDragging = false;
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                float dx = x - startX;
                float dy = y - startY;
                
                if(Math.abs(dx) > 10f || Math.abs(dy) > 10f) {
                    floatingButtonDragging = true;
                    floatingButton.setPosition(
                        Mathf.clamp(lastX + dx, 0, Core.graphics.getWidth() - floatingButton.getWidth()),
                        Mathf.clamp(lastY + dy, 0, Core.graphics.getHeight() - floatingButton.getHeight())
                    );
                    Core.settings.put("studio-button-x", floatingButton.x);
                    Core.settings.put("studio-button-y", floatingButton.y);
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode btn) {
                Timer.schedule(() -> floatingButtonDragging = false, 0.1f);
            }
        });

        floatingButton.add(button).size(buttonSize, buttonSize);
        floatingButton.touchable = Touchable.enabled;
        
        Core.scene.add(floatingButton);
        floatingButton.toFront();
    }

    void setupSettingsMenu() {
        Vars.ui.settings.addCategory("Studio", Icon.edit, table -> {
            table.defaults().left().pad(5f);
            
            table.add("[cyan]═══ FLOATING BUTTON ═══").padTop(10f).row();
            
            table.add("Button Size: " + (int)buttonSize).padTop(10f);
            Slider sizeSlider = new Slider(40f, 200f, 10f, false);
            sizeSlider.setValue(buttonSize);
            sizeSlider.moved(val -> {
                buttonSize = val;
                table.getCells().get(1).get().setText("Button Size: " + (int)buttonSize);
                setupFloatingButton();
                saveSettings();
            });
            table.add(sizeSlider).width(400f).row();
            
            table.add("Button Opacity: " + (int)(buttonOpacity * 100) + "%").padTop(10f);
            Slider opacitySlider = new Slider(0.1f, 1f, 0.1f, false);
            opacitySlider.setValue(buttonOpacity);
            opacitySlider.moved(val -> {
                buttonOpacity = val;
                table.getCells().get(3).get().setText("Button Opacity: " + (int)(buttonOpacity * 100) + "%");
                setupFloatingButton();
                saveSettings();
            });
            table.add(opacitySlider).width(400f).row();
            
            table.add("[cyan]═══ DISPLAY ═══").padTop(20f).row();
            
            table.button(showLabels ? "[green]Node Labels: ON" : "[red]Node Labels: OFF", () -> {
                showLabels = !showLabels;
                saveSettings();
                setupSettingsMenu();
                Vars.ui.settings.show();
            }).width(400f).height(60f).row();
            
            table.add("[cyan]═══ QUICK ACCESS ═══").padTop(20f).row();
            
            table.button("Open Studio Editor", Icon.edit, () -> {
                showStudioMenu();
            }).width(400f).height(60f).row();
            
            table.button("Reset Button Position", Icon.refresh, () -> {
                Core.settings.put("studio-button-x", 100f);
                Core.settings.put("studio-button-y", 100f);
                setupFloatingButton();
                Vars.ui.showInfoFade("Button position reset!");
            }).width(400f).height(60f).row();
            
            table.add("[lightgray]Version: 1.0 | Mindustry 154").padTop(20f).row();
        });
    }

    void showStudioMenu() {
        BaseDialog menuDialog = new BaseDialog("Studio");
        menuDialog.cont.defaults().size(500f, 120f).pad(15f);

        Label titleLabel = new Label("[cyan]Choose Mode:");
        titleLabel.setFontScale(1.5f);
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
            if(folder.isDirectory() && !folder.name().equals("studio-scripts") && !folder.name().equals("studio-mods")) {
                if(folder.child("mod.hjson").exists()) {
                    mods.add(folder);
                }
            }
        }
        
        if(mods.size == 0) {
            Label label = new Label("[lightgray]No mods created yet\nUse Mod Creator mode");
            label.setFontScale(1.2f);
            dialog.cont.add(label).row();
        } else {
            for(Fi modFolder : mods) {
                String modName = modFolder.name();
                Table row = new Table();
                row.button("[cyan]" + modName, () -> {
                    Vars.ui.showInfoText("Mod: " + modName, "Location: " + modFolder.path());
                }).growX();
                row.button("Delete", Icon.trash, () -> {
                    Vars.ui.showConfirm("Delete " + modName + "?", "This cannot be undone!", () -> {
                        modFolder.deleteDirectory();
                        Vars.ui.showInfoFade("Deleted: " + modName);
                        dialog.hide();
                        showModManager();
                    });
                }).size(120f, 100f);
                dialog.cont.add(row).fillX().row();
            }
        }

        dialog.addCloseButton();
        dialog.show();
    }

    void createExampleHelloWorld() {
        nodeEditor.canvas.nodes.clear();
        nodeEditor.editorMode = "game";
        nodeEditor.updateStatusLabel();
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
        nodeEditor.editorMode = "game";
        nodeEditor.updateStatusLabel();
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
        nodeEditor.updateStatusLabel();
        nodeEditor.canvas.nodes.clear();

        Node modFolder = new Node("mod", "Create Mod Folder", 0f, 0f, Color.cyan);
        modFolder.inputs.get(0).value = "mymod";

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

    public static void executeEventScripts(String eventLabel) {
        for(Script script : loadedScripts) {
            if(!script.enabled) continue;
            for(Node node : script.nodes) {
                if(node.type.equals("event") && node.label.equalsIgnoreCase(eventLabel)) {
                    Log.info("Executing: " + node.label);
                    executeNodeChain(node, script);
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
        Log.info("Executing node: " + node.label);

        switch(node.type) {
            case "action":
                executeAction(node);
                for(Node conn : node.connections) {
                    executeNode(conn, script);
                }
                break;

            case "logic":
                executeLogic(node, script);
                break;

            case "event":
                for(Node conn : node.connections) {
                    executeNode(conn, script);
                }
                break;
        }
    }

    public static void executeAction(Node node) {
        String label = node.label.toLowerCase();

        if(label.contains("message")) {
            String message = node.inputs.size > 0 ? node.inputs.get(0).value : "Hello!";
            Vars.ui.showInfoFade(message);
            Log.info("Message: " + message);
        }
        else if(label.contains("spawn unit")) {
            try {
                String unitName = node.inputs.get(0).value.toLowerCase().trim();
                int amount = Integer.parseInt(node.inputs.get(1).value);

                UnitType type = Vars.content.units().find(u -> u.name.equals(unitName));
                if(type == null) {
                    Log.warn("Unit not found: " + unitName);
                    type = UnitTypes.dagger;
                }

                float spawnX = 0f, spawnY = 0f;
                if(Vars.player != null && Vars.player.team() != null && Vars.player.team().core() != null) {
                    spawnX = Vars.player.team().core().x;
                    spawnY = Vars.player.team().core().y;
                } else if(Vars.player != null && Vars.player.unit() != null) {
                    spawnX = Vars.player.x;
                    spawnY = Vars.player.y;
                }

                for(int i = 0; i < amount; i++) {
                    float offsetX = (float)(Math.random() * 32f - 16f);
                    float offsetY = (float)(Math.random() * 32f - 16f);
                    type.spawn(Vars.player.team(), spawnX + offsetX, spawnY + offsetY);
                }

                Vars.ui.showInfoFade("Spawned " + amount + "x " + type.name);
                Log.info("Spawned " + amount + " " + unitName);
            } catch(Exception e) {
                Log.err("Failed to spawn unit", e);
            }
        }
        else if(label.contains("set block")) {
            try {
                int x = Integer.parseInt(node.inputs.get(0).value);
                int y = Integer.parseInt(node.inputs.get(1).value);
                String blockName = node.inputs.get(2).value.toLowerCase().trim();

                Block block = Vars.content.blocks().find(b -> b.name.equals(blockName));
                if(block != null && Vars.world != null) {
                    Vars.world.tile(x, y).setNet(block, Vars.player.team(), 0);
                    Log.info("Set block at (" + x + ", " + y + ") to " + blockName);
                }
            } catch(Exception e) {
                Log.err("Failed to set block", e);
            }
        }
    }

    public static void executeLogic(Node node, Script script) {
        String label = node.label.toLowerCase();

        if(label.contains("wait")) {
            try {
                float seconds = Float.parseFloat(node.inputs.get(0).value);
                Timer.schedule(() -> {
                    for(Node conn : node.connections) {
                        executeNode(conn, script);
                    }
                }, seconds);
                Log.info("Waiting " + seconds + " seconds");
            } catch(Exception e) {
                Log.err("Wait failed", e);
            }
        }
        else if(label.contains("if")) {
            String condition = node.inputs.get(0).value;
            boolean result = evaluateCondition(condition);
            if(result) {
                for(Node conn : node.connections) {
                    executeNode(conn, script);
                }
            }
            Log.info("If condition: " + condition + " = " + result);
        }
        else if(label.contains("loop")) {
            try {
                int count = Integer.parseInt(node.inputs.get(0).value);
                for(int i = 0; i < count; i++) {
                    for(Node conn : node.connections) {
                        executeNode(conn, script);
                    }
                }
                Log.info("Looped " + count + " times");
            } catch(Exception e) {
                Log.err("Loop failed", e);
            }
        }
        else if(label.contains("set variable")) {
            String varName = node.inputs.get(0).value;
            String varValue = node.inputs.get(1).value;
            variables.put(varName, varValue);
            Log.info("Set variable: " + varName + " = " + varValue);

            for(Node conn : node.connections) {
                executeNode(conn, script);
            }
        }
        else if(label.contains("get variable")) {
            String varName = node.inputs.get(0).value;
            String value = variables.get(varName, "undefined");
            Vars.ui.showInfoFade(varName + " = " + value);
            Log.info("Get variable: " + varName + " = " + value);

            for(Node conn : node.connections) {
                executeNode(conn, script);
            }
        }
    }

    public static boolean evaluateCondition(String condition) {
        condition = condition.trim().toLowerCase();

        if(condition.equals("true")) return true;
        if(condition.equals("false")) return false;

        if(condition.contains("==")) {
            String[] parts = condition.split("==");
            if(parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();

                String leftVal = variables.get(left, left);
                String rightVal = variables.get(right, right);

                return leftVal.equals(rightVal);
            }
        }

        if(condition.contains(">")) {
            String[] parts = condition.split(">");
            if(parts.length == 2) {
                try {
                    float left = Float.parseFloat(variables.get(parts[0].trim(), parts[0].trim()));
                    float right = Float.parseFloat(variables.get(parts[1].trim(), parts[1].trim()));
                    return left > right;
                } catch(Exception e) {
                    return false;
                }
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