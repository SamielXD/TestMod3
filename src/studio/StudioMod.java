package studio;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class StudioMod extends Mod {
    public static NodeEditor nodeEditor;
    private Table floatingButton;
    private static float buttonSize = 80f;
    private static float buttonOpacity = 0.8f;
    public static boolean showLabels = true;
    public static float labelScale = 1.0f;
    public static float infoScale = 1.0f;
    public static float typeScale = 1.0f;

    public StudioMod() {
        Log.info("Studio - Visual Scripting System loading...");

        Events.on(ClientLoadEvent.class, e -> {
            Log.info("Studio initializing...");

            nodeEditor = new NodeEditor();

            addStudioMenuButton();

            createFloatingButton();

            addSettingsMenu();

            loadSettings();

            Log.info("Studio loaded successfully!");
        });
    }

    private void addStudioMenuButton() {
        try {
            Core.app.post(() -> {
                try {
                    if(Vars.ui != null && Vars.ui.menufrag != null) {
                        Vars.ui.menufrag.addButton("Studio", () -> {
                            showStudioMenu();
                        });
                    }
                } catch(Exception ex) {
                    Log.err("Failed to add Studio menu button", ex);
                }
            });
        } catch(Exception ex) {
            Log.err("Error in addStudioMenuButton", ex);
        }
    }

    private void showStudioMenu() {
        BaseDialog dialog = new BaseDialog("Studio");
        dialog.cont.defaults().size(400f, 100f).pad(10f);

        dialog.cont.button("Script Editor", Icon.edit, () -> {
            dialog.hide();
            nodeEditor.show();
        }).row();

        dialog.cont.button("Mod Manager", Icon.box, () -> {
            dialog.hide();
            showModManager();
        }).row();

        dialog.cont.button("Load Example", Icon.download, () -> {
            dialog.hide();
            showExampleScripts();
        }).row();

        dialog.addCloseButton();
        dialog.show();
    }

    private void showModManager() {
        BaseDialog dialog = new BaseDialog("Mod Manager");
        dialog.cont.defaults().size(500f, 100f).pad(10f);

        Label info = new Label("[lightgray]Manage your mods");
        info.setFontScale(1.2f);
        dialog.cont.add(info).padBottom(20f).row();

        Fi modDir = Vars.modDirectory;
        if(modDir.exists()) {
            for(Fi folder : modDir.list()) {
                if(folder.isDirectory() && !folder.name().equals("studio-scripts") && !folder.name().equals("studio-mods")) {
                    Fi modHjson = folder.child("mod.hjson");
                    if(modHjson.exists()) {
                        Table modRow = new Table();
                        modRow.button(folder.name(), () -> {
                            Vars.ui.showInfoFade("Mod: " + folder.name());
                        }).growX();

                        modRow.button("Delete", Icon.trash, () -> {
                            showDeleteConfirmation(folder);
                            dialog.hide();
                        }).size(100f, 100f);

                        dialog.cont.add(modRow).fillX().row();
                    }
                }
            }
        }

        dialog.addCloseButton();
        dialog.show();
    }

    private void showDeleteConfirmation(Fi folder) {
        BaseDialog confirmDialog = new BaseDialog("Confirm Delete");
        confirmDialog.cont.add("[red]Delete mod: " + folder.name() + "?").pad(20f).row();
        confirmDialog.cont.add("[lightgray]This cannot be undone!").pad(10f).row();

        confirmDialog.buttons.button("Cancel", confirmDialog::hide).size(150f, 60f);
        confirmDialog.buttons.button("[red]Delete", () -> {
            try {
                folder.deleteDirectory();
                Vars.ui.showInfoFade("[red]Deleted: " + folder.name());
                Log.info("Deleted mod: " + folder.name());
                confirmDialog.hide();
            } catch(Exception e) {
                Log.err("Failed to delete mod", e);
                Vars.ui.showInfoFade("[red]Delete failed!");
            }
        }).size(150f, 60f);

        confirmDialog.show();
    }

    private void showExampleScripts() {
        BaseDialog dialog = new BaseDialog("Example Scripts");
        dialog.cont.defaults().size(500f, 120f).pad(10f);

        dialog.cont.button("[lime]Hello World\n[lightgray]Simple message script", () -> {
            loadExampleHelloWorld();
            dialog.hide();
            nodeEditor.show();
        }).row();

        dialog.cont.button("[cyan]Auto Spawn Units\n[lightgray]Spawn units on wave", () -> {
            loadExampleAutoSpawn();
            dialog.hide();
            nodeEditor.show();
        }).row();

        dialog.cont.button("[gold]Simple Mod\n[lightgray]Create a basic mod", () -> {
            loadExampleSimpleMod();
            dialog.hide();
            nodeEditor.show();
        }).row();

        dialog.addCloseButton();
        dialog.show();
    }

    private void loadExampleHelloWorld() {
        nodeEditor.canvas.nodes.clear();
        nodeEditor.editorMode = "game";
        nodeEditor.updateStatusLabel();

        Node startNode = new Node();
        startNode.type = "event";
        startNode.label = "On Start";
        startNode.color = Color.green;
        startNode.x = 100;
        startNode.y = 300;
        startNode.setupInputs();

        Node msgNode = new Node();
        msgNode.type = "action";
        msgNode.label = "Message";
        msgNode.color = Color.blue;
        msgNode.x = 400;
        msgNode.y = 300;
        msgNode.setupInputs();
        msgNode.inputs.get(0).value = "Hello from Studio!";
        msgNode.value = "Hello from Studio!";

        startNode.connections.add(msgNode);

        nodeEditor.canvas.nodes.add(startNode);
        nodeEditor.canvas.nodes.add(msgNode);

        Vars.ui.showInfoFade("[lime]Loaded: Hello World");
    }

    private void loadExampleAutoSpawn() {
        nodeEditor.canvas.nodes.clear();
        nodeEditor.editorMode = "game";
        nodeEditor.updateStatusLabel();

        Node waveNode = new Node();
        waveNode.type = "event";
        waveNode.label = "On Wave";
        waveNode.color = Color.green;
        waveNode.x = 100;
        waveNode.y = 300;
        waveNode.setupInputs();

        Node spawnNode = new Node();
        spawnNode.type = "action";
        spawnNode.label = "Spawn Unit";
        spawnNode.color = Color.blue;
        spawnNode.x = 400;
        spawnNode.y = 300;
        spawnNode.setupInputs();
        spawnNode.inputs.get(0).value = "dagger";
        spawnNode.inputs.get(1).value = "5";
        spawnNode.inputs.get(2).value = "At Player";
        spawnNode.value = "dagger|5|At Player";

        waveNode.connections.add(spawnNode);

        nodeEditor.canvas.nodes.add(waveNode);
        nodeEditor.canvas.nodes.add(spawnNode);

        Vars.ui.showInfoFade("[lime]Loaded: Auto Spawn");
    }

    private void loadExampleSimpleMod() {
        nodeEditor.canvas.nodes.clear();
        nodeEditor.editorMode = "mod";
        nodeEditor.updateStatusLabel();

        Node modFolderNode = new Node();
        modFolderNode.type = "mod";
        modFolderNode.label = "Create Mod Folder";
        modFolderNode.color = Color.cyan;
        modFolderNode.x = 100;
        modFolderNode.y = 500;
        modFolderNode.setupInputs();
        modFolderNode.inputs.get(0).value = "mymod";
        modFolderNode.value = "mymod";

        Node hjsonNode = new Node();
        hjsonNode.type = "mod";
        hjsonNode.label = "Create mod.hjson";
        hjsonNode.color = Color.royal;
        hjsonNode.x = 400;
        hjsonNode.y = 500;
        hjsonNode.setupInputs();
        hjsonNode.inputs.get(0).value = "mymod";
        hjsonNode.inputs.get(1).value = "My Mod";
        hjsonNode.inputs.get(2).value = "Studio";
        hjsonNode.value = "mymod|My Mod|Studio";

        Node contentFolderNode = new Node();
        contentFolderNode.type = "mod";
        contentFolderNode.label = "Create Folder";
        contentFolderNode.color = Color.sky;
        contentFolderNode.x = 400;
        contentFolderNode.y = 350;
        contentFolderNode.setupInputs();
        contentFolderNode.inputs.get(0).value = "content";
        contentFolderNode.value = "content";

        Node blocksFolderNode = new Node();
        blocksFolderNode.type = "mod";
        blocksFolderNode.label = "Create Folder";
        blocksFolderNode.color = Color.sky;
        blocksFolderNode.x = 700;
        blocksFolderNode.y = 350;
        blocksFolderNode.setupInputs();
        blocksFolderNode.inputs.get(0).value = "blocks";
        blocksFolderNode.value = "blocks";

        Node blockFileNode = new Node();
        blockFileNode.type = "mod";
        blockFileNode.label = "Create Block File";
        blockFileNode.color = Color.pink;
        blockFileNode.x = 1000;
        blockFileNode.y = 350;
        blockFileNode.setupInputs();
        blockFileNode.inputs.get(0).value = "my-wall";
        blockFileNode.inputs.get(1).value = "Wall";
        blockFileNode.inputs.get(2).value = "1000";
        blockFileNode.inputs.get(3).value = "2";
        blockFileNode.value = "my-wall|Wall|1000|2";

        modFolderNode.connections.add(hjsonNode);
        modFolderNode.connections.add(contentFolderNode);
        contentFolderNode.connections.add(blocksFolderNode);
        blocksFolderNode.connections.add(blockFileNode);

        nodeEditor.canvas.nodes.add(modFolderNode);
        nodeEditor.canvas.nodes.add(hjsonNode);
        nodeEditor.canvas.nodes.add(contentFolderNode);
        nodeEditor.canvas.nodes.add(blocksFolderNode);
        nodeEditor.canvas.nodes.add(blockFileNode);

        Vars.ui.showInfoFade("[lime]Loaded: Simple Mod");
    }

    private void createFloatingButton() {
        floatingButton = new Table();
        floatingButton.background(Styles.black6);

        floatingButton.button("STUDIO", Icon.edit, () -> {
            nodeEditor.show();
        }).size(buttonSize, buttonSize).get();

        float savedX = Core.settings.getFloat("studio-button-x", Core.graphics.getWidth() - 100);
        float savedY = Core.settings.getFloat("studio-button-y", Core.graphics.getHeight() / 2);

        floatingButton.setPosition(savedX, savedY);

        floatingButton.touchable = arc.scene.event.Touchable.enabled;
        floatingButton.addListener(new arc.scene.event.InputListener() {
            float lastX, lastY;

            public boolean touchDown(arc.scene.event.InputEvent event, float x, float y, int pointer, arc.input.KeyCode button) {
                lastX = x;
                lastY = y;
                floatingButton.toFront();
                return true;
            }

            public void touchDragged(arc.scene.event.InputEvent event, float x, float y, int pointer) {
                floatingButton.x += x - lastX;
                floatingButton.y += y - lastY;

                Core.settings.put("studio-button-x", floatingButton.x);
                Core.settings.put("studio-button-y", floatingButton.y);
            }
        });

        floatingButton.update(() -> {
            floatingButton.color.a = buttonOpacity;
            floatingButton.visible = Vars.ui.hudfrag.shown;
        });

        Vars.ui.hudGroup.addChild(floatingButton);
    }

    private void addSettingsMenu() {
        Vars.ui.settings.addCategory("Studio", icon -> {
            icon.row();

            icon.add("[cyan]FLOATING BUTTON").padTop(10f).row();

            icon.table(t -> {
                t.add("Button Size: ").left();
                t.slider(40f, 200f, 5f, buttonSize, val -> {
                    buttonSize = val;
                    updateFloatingButton();
                }).width(300f).get();
                t.add(new Label(() -> (int)buttonSize + "px")).padLeft(10f);
            }).fillX().padTop(10f).row();

            icon.table(t -> {
                t.add("Button Opacity: ").left();
                t.slider(0.1f, 1.0f, 0.1f, buttonOpacity, val -> {
                    buttonOpacity = val;
                    updateFloatingButton();
                }).width(300f).get();
                t.add(new Label(() -> (int)(buttonOpacity * 100) + "%")).padLeft(10f);
            }).fillX().padTop(10f).row();

            icon.button("Reset Button Position", () -> {
                Core.settings.put("studio-button-x", Core.graphics.getWidth() - 100f);
                Core.settings.put("studio-button-y", Core.graphics.getHeight() / 2f);
                if(floatingButton != null) {
                    floatingButton.setPosition(Core.graphics.getWidth() - 100f, Core.graphics.getHeight() / 2f);
                }
                Vars.ui.showInfoFade("Button position reset");
            }).size(300f, 50f).padTop(10f).row();

            icon.add("").padTop(20f).row();

            icon.add("[cyan]TEXT SIZES").padTop(10f).row();

            icon.table(t -> {
                t.add("Node Label Size: ").left();
                t.slider(0.5f, 2.0f, 0.1f, labelScale, val -> {
                    labelScale = val;
                }).width(300f).get();
                t.add(new Label(() -> String.format("%.1fx", labelScale))).padLeft(10f);
            }).fillX().padTop(10f).row();

            icon.table(t -> {
                t.add("Node Info Size: ").left();
                t.slider(0.5f, 2.0f, 0.1f, infoScale, val -> {
                    infoScale = val;
                }).width(300f).get();
                t.add(new Label(() -> String.format("%.1fx", infoScale))).padLeft(10f);
            }).fillX().padTop(10f).row();

            icon.table(t -> {
                t.add("Node Type Size: ").left();
                t.slider(0.5f, 2.0f, 0.1f, typeScale, val -> {
                    typeScale = val;
                }).width(300f).get();
                t.add(new Label(() -> String.format("%.1fx", typeScale))).padLeft(10f);
            }).fillX().padTop(10f).row();

            icon.add("").padTop(20f).row();

            icon.add("[cyan]DISPLAY").padTop(10f).row();

            icon.check("Node Labels", showLabels, val -> {
                showLabels = val;
            }).padTop(10f).row();

            icon.add("").padTop(20f).row();

            icon.add("[cyan]QUICK ACCESS").padTop(10f).row();

            icon.button("Open Studio Editor", () -> {
                Vars.ui.settings.hide();
                nodeEditor.show();
            }).size(300f, 50f).padTop(10f).row();
        });
    }

    private void updateFloatingButton() {
        if(floatingButton != null) {
            floatingButton.clearChildren();
            floatingButton.button("STUDIO", Icon.edit, () -> {
                nodeEditor.show();
            }).size(buttonSize, buttonSize).get();
        }
    }

    public static float getButtonSize() {
        return buttonSize;
    }

    public static float getButtonOpacity() {
        return buttonOpacity;
    }

    public static boolean getShowLabels() {
        return showLabels;
    }

    public static float getLabelScale() {
        return labelScale;
    }

    public static float getInfoScale() {
        return infoScale;
    }

    public static float getTypeScale() {
        return typeScale;
    }

    private void saveSettings() {
        try {
            Fi settingsFile = Core.files.local("studio-settings.json");

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"buttonSize\": ").append(buttonSize).append(",\n");
            json.append("  \"buttonOpacity\": ").append(buttonOpacity).append(",\n");
            json.append("  \"showLabels\": ").append(showLabels).append(",\n");
            json.append("  \"labelScale\": ").append(labelScale).append(",\n");
            json.append("  \"infoScale\": ").append(infoScale).append(",\n");
            json.append("  \"typeScale\": ").append(typeScale).append("\n");
            json.append("}");

            settingsFile.writeString(json.toString());
            Log.info("Settings saved!");
        } catch(Exception e) {
            Log.err("Failed to save settings", e);
        }
    }

    private void loadSettings() {
        try {
            Fi settingsFile = Core.files.local("studio-settings.json");
            if(settingsFile.exists()) {
                String json = settingsFile.readString();

                buttonSize = parseFloat(json, "buttonSize", 80f);
                buttonOpacity = parseFloat(json, "buttonOpacity", 0.8f);
                showLabels = parseBoolean(json, "showLabels", true);
                labelScale = parseFloat(json, "labelScale", 1.0f);
                infoScale = parseFloat(json, "infoScale", 1.0f);
                typeScale = parseFloat(json, "typeScale", 1.0f);

                updateFloatingButton();
                Log.info("Settings loaded!");
            }
        } catch(Exception e) {
            Log.err("Failed to load settings", e);
        }
    }

    private float parseFloat(String json, String key, float defaultValue) {
        try {
            String search = "\"" + key + "\":";
            int start = json.indexOf(search);
            if(start == -1) return defaultValue;

            start += search.length();
            while(start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\n')) start++;

            int end = start;
            while(end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '\n' && json.charAt(end) != '}') {
                end++;
            }

            String value = json.substring(start, end).trim();
            return Float.parseFloat(value);
        } catch(Exception e) {
            return defaultValue;
        }
    }

    private boolean parseBoolean(String json, String key, boolean defaultValue) {
        try {
            String search = "\"" + key + "\":";
            int start = json.indexOf(search);
            if(start == -1) return defaultValue;

            start += search.length();
            while(start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\n')) start++;

            int end = start;
            while(end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '\n' && json.charAt(end) != '}') {
                end++;
            }

            String value = json.substring(start, end).trim();
            return Boolean.parseBoolean(value);
        } catch(Exception e) {
            return defaultValue;
        }
    }public static void executeNodeChain(Node startNode, Node previousNode) {
        if(startNode == null) return;

        Log.info("Executing node: " + startNode.label);

        switch(startNode.type) {
            case "event":
                break;

            case "action":
                executeAction(startNode);
                break;

            case "logic":
                executeLogic(startNode);
                break;
        }

        for(Node connected : startNode.connections) {
            executeNodeChain(connected, startNode);
        }
    }

    private static void executeAction(Node node) {
        switch(node.label) {
            case "Message":
                String message = node.inputs.size > 0 ? node.inputs.get(0).value : "Hello!";
                Vars.ui.showInfoToast(message, 3);
                break;

            case "Spawn Unit":
                spawnUnits(node);
                break;

            case "Set Block":
                setBlock(node);
                break;
        }
    }

    private static void spawnUnits(Node node) {
        try {
            if(node.inputs.size < 3) return;

            String unitName = node.inputs.get(0).value;
            int amount = Integer.parseInt(node.inputs.get(1).value);
            String spawnLocation = node.inputs.get(2).value;

            mindustry.type.UnitType unitType = mindustry.content.UnitTypes.dagger;

            for(mindustry.type.UnitType type : mindustry.content.UnitTypes.copy()) {
                if(type.name.equals(unitName)) {
                    unitType = type;
                    break;
                }
            }

            float spawnX = Vars.player.x;
            float spawnY = Vars.player.y;

            if(spawnLocation.equals("At Core")) {
                mindustry.world.blocks.storage.CoreBlock.CoreBuild core = Vars.player.team().core();
                if(core != null) {
                    float angle = arc.math.Mathf.random(360f);
                    spawnX = core.x + arc.math.Angles.trnsx(angle, 800f * 8f);
                    spawnY = core.y + arc.math.Angles.trnsy(angle, 800f * 8f);
                }
            } else if(spawnLocation.equals("At Player")) {
                float angle = arc.math.Mathf.random(360f);
                float distance = arc.math.Mathf.random(64f, 128f);
                spawnX = Vars.player.x + arc.math.Angles.trnsx(angle, distance);
                spawnY = Vars.player.y + arc.math.Angles.trnsy(angle, distance);
            }

            for(int i = 0; i < amount; i++) {
                float angle = arc.math.Mathf.random(360f);
                float distance = arc.math.Mathf.random(32f);
                float finalX = spawnX + arc.math.Angles.trnsx(angle, distance);
                float finalY = spawnY + arc.math.Angles.trnsy(angle, distance);

                unitType.spawn(Vars.player.team(), finalX, finalY);
            }

            Log.info("Spawned " + amount + " " + unitName + " at " + spawnLocation);
            Vars.ui.showInfoToast("Spawned " + amount + " " + unitName, 2);

        } catch(Exception e) {
            Log.err("Failed to spawn unit", e);
        }
    }

    private static void setBlock(Node node) {
        try {
            if(node.inputs.size < 3) return;

            String blockName = node.inputs.get(0).value;
            int x = Integer.parseInt(node.inputs.get(1).value);
            int y = Integer.parseInt(node.inputs.get(2).value);

            mindustry.world.Block block = mindustry.content.Blocks.copperWall;

            for(mindustry.world.Block b : mindustry.content.Blocks.copy()) {
                if(b.name.equals(blockName)) {
                    block = b;
                    break;
                }
            }

            mindustry.world.Tile tile = Vars.world.tile(x, y);
            if(tile != null) {
                tile.setNet(block, Vars.player.team(), 0);
                Vars.ui.showInfoToast("Placed " + blockName, 2);
            }

        } catch(Exception e) {
            Log.err("Failed to set block", e);
        }
    }

    private static void executeLogic(Node node) {
        switch(node.label) {
            case "Wait":
                break;

            case "If":
                break;

            case "Loop":
                break;

            case "Set Variable":
                break;

            case "Get Variable":
                break;
        }
    }

    @Override
    public void init() {
        Events.on(WorldLoadEvent.class, e -> {
            saveSettings();
        });
    }
}