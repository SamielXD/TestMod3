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
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class NodeEditor extends BaseDialog {
    public NodeCanvas canvas;
    private String currentScriptName = "Untitled";
    private Label statusLabel;
    public String editorMode = "game"; // FIXED: Changed from private to public

    public NodeEditor() {
        super("Studio - Node Editor");
        canvas = new NodeCanvas();
        canvas.onNodeEdit = () -> showEditDialog(canvas.selectedNode);
        buildUI();
    }

    private void buildUI() {
        Table main = new Table();
        main.setFillParent(true);
        main.add(canvas).grow().row();

        statusLabel = new Label("MODE: MOVE | EDITOR: GAME SCRIPTS");
        statusLabel.setFontScale(1.5f);

        Table buttonTable = new Table();
        buttonTable.defaults().size(150f, 80f).pad(4f);

        buttonTable.button("Close", Icon.left, this::hide);
        buttonTable.button("Mode", Icon.menu, this::showModeSelector);
        buttonTable.button("Clear", Icon.trash, () -> {
            canvas.nodes.clear();
            Vars.ui.showInfoFade("Canvas cleared!");
        });
        buttonTable.button("Save", Icon.save, this::saveScript);
        buttonTable.button("Load", Icon.download, this::showLoadDialog);
        buttonTable.button("Run", Icon.play, this::runScript);
        buttonTable.button("Move", Icon.move, () -> {
            canvas.mode = "move";
            updateStatusLabel();
        });
        buttonTable.button("Edit", Icon.edit, () -> {
            canvas.mode = "edit";
            updateStatusLabel();
        });
        buttonTable.button("Link", Icon.link, () -> {
            canvas.mode = "connect";
            updateStatusLabel();
        });
        buttonTable.button("Delete", Icon.trash, () -> {
            canvas.mode = "delete";
            updateStatusLabel();
        });
        buttonTable.button("Add", Icon.add, this::showNodeBrowser);
        buttonTable.button("Z-", Icon.zoom, () -> canvas.zoom = arc.math.Mathf.clamp(canvas.zoom - 0.2f, 0.2f, 3f));
        buttonTable.button("Z+", Icon.zoom, () -> canvas.zoom = arc.math.Mathf.clamp(canvas.zoom + 0.2f, 0.2f, 3f));

        ScrollPane scrollPane = new ScrollPane(buttonTable);
        scrollPane.setScrollingDisabled(false, true);

        main.add(scrollPane).growX().height(90f).row();
        main.add(statusLabel).fillX().pad(10f);
        cont.add(main).grow();
    }

    private void updateStatusLabel() {
        String modeText = canvas.mode.toUpperCase();
        String editorText = editorMode.equals("game") ? "GAME SCRIPTS" : "MOD CREATOR";
        statusLabel.setText("MODE: " + modeText + " | EDITOR: " + editorText);
    }

    private void showModeSelector() {
        BaseDialog dialog = new BaseDialog("Select Editor Mode");
        dialog.cont.defaults().size(500f, 120f).pad(10f);
        Label info = new Label("[lightgray]Choose editor mode:");
        info.setFontScale(1.3f);
        dialog.cont.add(info).padBottom(20f).row();
        dialog.cont.button("[lime]GAME SCRIPTS\n[lightgray]Create gameplay features", Icon.edit, () -> {
            editorMode = "game";
            canvas.nodes.clear();
            updateStatusLabel();
            Vars.ui.showInfoFade("Switched to Game Scripts mode");
            dialog.hide();
        }).row();
        dialog.cont.button("[cyan]MOD CREATOR\n[lightgray]Build custom mods", Icon.box, () -> {
            editorMode = "mod";
            canvas.nodes.clear();
            updateStatusLabel();
            Vars.ui.showInfoFade("Switched to Mod Creator mode");
            dialog.hide();
        }).row();
        dialog.addCloseButton();
        dialog.show();
    }

    private void showNodeBrowser() {
        BaseDialog dialog = new BaseDialog("Add Node");
        Table content = new Table();
        content.defaults().size(450f, 100f).pad(8f);
        if(editorMode.equals("game")) {
            content.add("[green]═══ EVENTS ═══").row();
            content.button("ON START", () -> { canvas.addNode("event", "On Start", Color.green); dialog.hide(); }).row();
            content.button("ON WAVE", () -> { canvas.addNode("event", "On Wave", Color.green); dialog.hide(); }).row();
            content.button("ON BUILD", () -> { canvas.addNode("event", "On Build", Color.green); dialog.hide(); }).row();
            content.add("[blue]═══ ACTIONS ═══").padTop(20f).row();
            content.button("MESSAGE", () -> { canvas.addNode("action", "Message", Color.blue); dialog.hide(); }).row();
            content.button("SPAWN UNIT", () -> { canvas.addNode("action", "Spawn Unit", Color.blue); dialog.hide(); }).row();
            content.button("SET BLOCK", () -> { canvas.addNode("action", "Set Block", Color.blue); dialog.hide(); }).row();
            content.add("[orange]═══ LOGIC ═══").padTop(20f).row();
            content.button("WAIT", () -> { canvas.addNode("logic", "Wait", Color.orange); dialog.hide(); }).row();
            content.button("IF", () -> { canvas.addNode("logic", "If", Color.orange); dialog.hide(); }).row();
            content.button("LOOP", () -> { canvas.addNode("logic", "Loop", Color.orange); dialog.hide(); }).row();
            content.button("SET VARIABLE", () -> { canvas.addNode("logic", "Set Variable", Color.yellow); dialog.hide(); }).row();
            content.button("GET VARIABLE", () -> { canvas.addNode("logic", "Get Variable", Color.yellow); dialog.hide(); }).row();
        } else {
            content.add("[cyan]═══ MOD STRUCTURE ═══").row();
            content.button("CREATE MOD FOLDER", () -> { canvas.addNode("mod", "Create Mod Folder", Color.cyan); dialog.hide(); }).row();
            content.button("CREATE FOLDER", () -> { canvas.addNode("mod", "Create Folder", Color.sky); dialog.hide(); }).row();
            content.button("CREATE mod.hjson", () -> { canvas.addNode("mod", "Create mod.hjson", Color.royal); dialog.hide(); }).row();
            content.add("[green]═══ EVENTS ═══").padTop(20f).row();
            content.button("ON START", () -> { canvas.addNode("event", "On Start", Color.green); dialog.hide(); }).row();
            content.button("ON BUILD", () -> { canvas.addNode("event", "On Build", Color.green); dialog.hide(); }).row();
            content.add("[pink]═══ CONTENT FILES ═══").padTop(20f).row();
            content.button("CREATE BLOCK FILE", () -> { canvas.addNode("mod", "Create Block File", Color.pink); dialog.hide(); }).row();
            content.button("CREATE UNIT FILE", () -> { canvas.addNode("mod", "Create Unit File", Color.pink); dialog.hide(); }).row();
            content.button("CREATE ITEM FILE", () -> { canvas.addNode("mod", "Create Item File", Color.pink); dialog.hide(); }).row();
            content.add("[gold]═══ ASSETS ═══").padTop(20f).row();
            content.button("ADD SPRITE", () -> { canvas.addNode("mod", "Add Sprite", Color.gold); dialog.hide(); }).row();
        }
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setScrollingDisabled(true, false);
        dialog.cont.add(scrollPane).size(500f, 600f);
        dialog.addCloseButton();
        dialog.show();
    }

    private void showEditDialog(Node node) {
        if(node == null) return;
        BaseDialog dialog = new BaseDialog("Edit: " + node.label);
        dialog.cont.defaults().size(600f, 80f).pad(10f);
        if(node.inputs.size > 0) {
            for(Node.NodeInput input : node.inputs) {
                Label label = new Label(input.label + ":");
                label.setFontScale(1.5f);
                dialog.cont.add(label).left().row();
                TextField field = new TextField(input.value);
                field.setStyle(new TextField.TextFieldStyle(field.getStyle()));
                field.getStyle().font.getData().setScale(1.5f);
                dialog.cont.add(field).fillX().height(100f).row();
                field.changed(() -> {
                    input.value = field.getText();
                    node.value = buildNodeValue(node);
                });
            }
        }
        dialog.buttons.button("DONE", dialog::hide).size(300f, 100f);
        dialog.show();
    }

    private String buildNodeValue(Node node) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < node.inputs.size; i++) {
            sb.append(node.inputs.get(i).value);
            if(i < node.inputs.size - 1) sb.append("|");
        }
        return sb.toString();
    }

    private void saveScript() {
        BaseDialog dialog = new BaseDialog("Save Script");
        Label label = new Label("Script Name:");
        label.setFontScale(1.5f);
        dialog.cont.add(label).row();
        TextField nameField = new TextField(currentScriptName);
        nameField.setStyle(new TextField.TextFieldStyle(nameField.getStyle()));
        nameField.getStyle().font.getData().setScale(1.5f);
        dialog.cont.add(nameField).size(500f, 100f).pad(15f).row();
        dialog.buttons.button("SAVE", () -> {
            currentScriptName = nameField.getText();
            try {
                Seq<NodeData> nodeDataList = new Seq<>();
                for(Node node : canvas.nodes) {
                    NodeData data = new NodeData();
                    data.id = node.id;
                    data.type = node.type;
                    data.label = node.label;
                    data.x = node.x;
                    data.y = node.y;
                    data.value = node.value;
                    data.color = node.color.toString();
                    data.inputValues = new Seq<>();
                    for(Node.NodeInput input : node.inputs) {
                        data.inputValues.add(input.value);
                    }
                    data.connectionIds = new Seq<>();
                    for(Node conn : node.connections) {
                        data.connectionIds.add(conn.id);
                    }
                    nodeDataList.add(data);
                }
                String json = new Json().toJson(nodeDataList);
                String savePath = editorMode.equals("game") ? "mods/studio-scripts/" : "mods/studio-mods/";
                Core.files.local(savePath).mkdirs();
                Core.files.local(savePath + currentScriptName + ".json").writeString(json);
                statusLabel.setText("Saved: " + currentScriptName);
                Vars.ui.showInfoFade("Saved successfully!");
                dialog.hide();
            } catch(Exception e) {
                Log.err("Save failed", e);
                Vars.ui.showInfoFade("Save failed: " + e.getMessage());
            }
        }).size(250f, 100f);
        dialog.buttons.button("CANCEL", dialog::hide).size(250f, 100f);
        dialog.show();
    }

    private void showLoadDialog() {
        BaseDialog dialog = new BaseDialog("Load Script");
        dialog.cont.defaults().size(500f, 100f).pad(10f);

        String loadPath = editorMode.equals("game") ? "mods/studio-scripts/" : "mods/studio-mods/";
        Fi folder = Core.files.local(loadPath);

        if(!folder.exists()) {
            folder.mkdirs();
        }

        Seq<String> scripts = new Seq<>();
        for(Fi file : folder.list()) {
            if(file.extension().equals("json")) {
                scripts.add(file.nameWithoutExtension());
            }
        }

        if(scripts.size == 0) {
            Label label = new Label("No saved scripts found");
            label.setFontScale(1.5f);
            dialog.cont.add(label).row();
        } else {
            for(String scriptName : scripts) {
                Table row = new Table();
                row.button(scriptName, () -> {
                    loadScript(scriptName);
                    dialog.hide();
                }).growX();
                row.button("X", Icon.trash, () -> {
                    deleteScript(scriptName);
                    dialog.hide();
                    showLoadDialog();
                }).size(80f, 100f);
                dialog.cont.add(row).fillX().row();
            }
        }
        dialog.addCloseButton();
        dialog.show();
    }

    private void deleteScript(String name) {
        String loadPath = editorMode.equals("game") ? "mods/studio-scripts/" : "mods/studio-mods/";
        Fi file = Core.files.local(loadPath + name + ".json");
        if(file.exists()) {
            file.delete();
            Vars.ui.showInfoFade("Deleted: " + name);
        }
    }

    private void loadScript(String name) {
        try {
            String loadPath = editorMode.equals("game") ? "mods/studio-scripts/" : "mods/studio-mods/";
            Fi file = Core.files.local(loadPath + name + ".json");

            if(!file.exists()) {
                throw new Exception("File not found: " + name);
            }

            String json = file.readString();
            Json jsonParser = new Json();
            jsonParser.setIgnoreUnknownFields(true);

            // FIXED: Use fromJson with proper class type
            Seq<NodeData> nodeDataList = jsonParser.fromJson(Seq.class, NodeData.class, json);

            if(nodeDataList == null || nodeDataList.size == 0) {
                throw new Exception("No nodes in file");
            }

            canvas.nodes.clear();
            Seq<Node> loadedNodes = new Seq<>();

            for(NodeData data : nodeDataList) {
                Node node = new Node();
                node.id = data.id != null ? data.id : java.util.UUID.randomUUID().toString();
                node.type = data.type != null ? data.type : "action";
                node.label = data.label != null ? data.label : "Unknown";
                node.x = data.x;
                node.y = data.y;
                node.value = data.value != null ? data.value : "";
                node.color = data.color != null ? Color.valueOf(data.color) : Color.gray;
                node.setupInputs();

                if(data.inputValues != null && data.inputValues.size > 0) {
                    for(int i = 0; i < Math.min(node.inputs.size, data.inputValues.size); i++) {
                        node.inputs.get(i).value = data.inputValues.get(i);
                    }
                }

                loadedNodes.add(node);
            }

            // Restore connections
            for(int i = 0; i < nodeDataList.size; i++) {
                NodeData data = nodeDataList.get(i);
                Node node = loadedNodes.get(i);

                if(data.connectionIds != null && data.connectionIds.size > 0) {
                    for(String connId : data.connectionIds) {
                        Node target = loadedNodes.find(n -> n.id.equals(connId));
                        if(target != null) {
                            node.connections.add(target);
                        }
                    }
                }
            }

            canvas.nodes = loadedNodes;
            currentScriptName = name;
            statusLabel.setText("Loaded: " + name + " (" + canvas.nodes.size + " nodes)");
            Vars.ui.showInfoFade("Loaded " + canvas.nodes.size + " nodes!");

        } catch(Exception e) {
            Log.err("Load failed: " + name, e);
            Vars.ui.showInfoFade("Load failed: " + e.getMessage());
            statusLabel.setText("Load FAILED!");
        }
    }

    private void runScript() {
        try {
            if(editorMode.equals("mod")) {
                executeModCreation();
            } else {
                executeGameScript();
            }
        } catch(Exception e) {
            Log.err("Run failed", e);
            Vars.ui.showInfoFade("Error: " + e.getMessage());
        }
    }

    private void executeGameScript() {
        boolean hasEventNode = false;
        for(Node node : canvas.nodes) {
            if(node.type.equals("event")) {
                hasEventNode = true;
                StudioMod.executeNodeChain(node, null);
            }
        }
        if(!hasEventNode) {
            Vars.ui.showInfoFade("No event nodes! Add 'On Start' or 'On Wave'.");
        } else {
            statusLabel.setText("Script executed!");
        }
    }

    private void executeModCreation() {
        for(Node node : canvas.nodes) {
            if(node.label.equals("Create Mod Folder")) {
                executeSingleNode(node);
            }
        }
        Vars.ui.showInfoFade("Mod structure created!");
        statusLabel.setText("Mod created!");
    }

    private void executeSingleNode(Node node) {
        if(node.label.equals("Create Mod Folder")) {
            String modName = node.inputs.get(0).value;
            Fi modFolder = Core.files.local("mods/" + modName);
            modFolder.mkdirs();

            for(Node child : node.connections) {
                executeModNode(child, modFolder);
            }
        }
    }

    private void executeModNode(Node node, Fi parentFolder) {
        if(node.label.equals("Create Folder")) {
            String folderName = node.inputs.get(0).value;
            Fi folder = parentFolder.child(folderName);
            folder.mkdirs();
            for(Node child : node.connections) {
                executeModNode(child, folder);
            }
        }
        else if(node.label.equals("Create mod.hjson")) {
            String modName = node.inputs.get(0).value;
            String displayName = node.inputs.get(1).value;
            String author = node.inputs.get(2).value;

            String hjson = "name: " + modName + "\n" +
                          "displayName: " + displayName + "\n" +
                          "author: " + author + "\n" +
                          "version: 1.0\n" +
                          "minGameVersion: 154\n";

            parentFolder.child("mod.hjson").writeString(hjson);
        }
        else if(node.label.equals("Create Block File")) {
            String blockName = node.inputs.get(0).value;
            String type = node.inputs.get(1).value;
            String health = node.inputs.get(2).value;
            String size = node.inputs.get(3).value;

            String hjson = "type: " + type + "\n" +
                          "health: " + health + "\n" +
                          "size: " + size + "\n";

            parentFolder.child(blockName + ".hjson").writeString(hjson);
        }
        else if(node.label.equals("Create Unit File")) {
            String unitName = node.inputs.get(0).value;
            String type = node.inputs.get(1).value;
            String health = node.inputs.get(2).value;
            String speed = node.inputs.get(3).value;

            String hjson = "type: " + type + "\n" +
                          "health: " + health + "\n" +
                          "speed: " + speed + "\n";

            parentFolder.child(unitName + ".hjson").writeString(hjson);
        }
        else if(node.label.equals("Create Item File")) {
            String itemName = node.inputs.get(0).value;
            String color = node.inputs.get(1).value;
            String cost = node.inputs.get(2).value;

            String hjson = "color: " + color + "\n" +
                          "cost: " + cost + "\n";

            parentFolder.child(itemName + ".hjson").writeString(hjson);
        }
    }

    public static class NodeData {
        public String id;
        public String type;
        public String label;
        public float x, y;
        public String value;
        public String color;
        public Seq<String> connectionIds = new Seq<>();
        public Seq<String> inputValues = new Seq<>();
    }
}