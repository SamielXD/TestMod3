package studio;

import arc.*;
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
    private String uiLayout = "bottom";
    private String editorMode = "game";

    public NodeEditor() {
        super("Studio - Node Editor");
        canvas = new NodeCanvas();
        canvas.onNodeEdit = () -> showEditDialog(canvas.selectedNode);
        uiLayout = Core.settings.getString("studio-ui-layout", "bottom");
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
        buttonTable.button("Mod", Icon.box, this::showModSelector);
        buttonTable.button("Mode", Icon.menu, this::showModeSelector);
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

        if(uiLayout.equals("top")) {
            main.getChildren().reverse();
        }

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
        Label info = new Label("[lightgray]Choose what you want to create:");
        info.setFontScale(1.3f);
        dialog.cont.add(info).padBottom(20f).row();
        dialog.cont.button("[lime]GAME SCRIPTS\n[lightgray]Add features to gameplay", Icon.edit, () -> {
            editorMode = "game";
            updateStatusLabel();
            Vars.ui.showInfoFade("Switched to Game Scripts mode");
            dialog.hide();
        }).row();
        dialog.cont.button("[cyan]MOD CREATOR\n[lightgray]Create custom mods", Icon.box, () -> {
            editorMode = "mod";
            updateStatusLabel();
            Vars.ui.showInfoFade("Switched to Mod Creator mode");
            dialog.hide();
        }).row();
        dialog.addCloseButton();
        dialog.show();
    }

    private void showModSelector() {
        BaseDialog dialog = new BaseDialog("Select Mod");
        dialog.cont.defaults().size(500f, 100f).pad(10f);
        Seq<Fi> mods = new Seq<>();
        Fi modsFolder = Core.files.local("mods/");
        for(Fi folder : modsFolder.list()) {
            if(folder.isDirectory() && folder.child("mod.hjson").exists()) {
                mods.add(folder);
            }
        }
        if(mods.size == 0) {
            Label label = new Label("[lightgray]No mods found");
            label.setFontScale(1.3f);
            dialog.cont.add(label).row();
        } else {
            for(Fi modFolder : mods) {
                dialog.cont.button("[cyan]" + modFolder.name(), () -> {
                    Vars.ui.showInfoFade("Selected: " + modFolder.name());
                    dialog.hide();
                }).row();
            }
        }
        dialog.addCloseButton();
        dialog.show();
    }

    private void showNodeBrowser() {
        BaseDialog dialog = new BaseDialog("Node Browser");
        Table content = new Table();
        content.defaults().size(450f, 100f).pad(8f);
        if(editorMode.equals("game")) {
            content.add("[green]═══ EVENTS ═══").row();
            content.button("ON START", () -> { canvas.addNode("event", "On Start", Color.green); dialog.hide(); }).row();
            content.button("ON WAVE", () -> { canvas.addNode("event", "On Wave", Color.green); dialog.hide(); }).row();
            content.add("[blue]═══ ACTIONS ═══").padTop(20f).row();
            content.button("MESSAGE", () -> { canvas.addNode("action", "Message", Color.blue); dialog.hide(); }).row();
            content.button("SPAWN UNIT", () -> { canvas.addNode("action", "Spawn Unit", Color.blue); dialog.hide(); }).row();
            content.add("[orange]═══ CONDITIONS ═══").padTop(20f).row();
            content.button("WAIT", () -> { canvas.addNode("condition", "Wait", Color.orange); dialog.hide(); }).row();
        } else {
            content.add("[cyan]═══ MOD NODES ═══").row();
            content.button("CREATE MOD", () -> { canvas.addNode("mod", "Create Mod", Color.cyan); dialog.hide(); }).row();
            content.button("CREATE BLOCK", () -> { canvas.addNode("mod", "Create Block", Color.royal); dialog.hide(); }).row();
            content.button("CREATE UNIT", () -> { canvas.addNode("mod", "Create Unit", Color.royal); dialog.hide(); }).row();
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
                    node.value = field.getText();
                });
            }
        }
        dialog.buttons.button("DONE", dialog::hide).size(300f, 100f);
        dialog.show();
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
                String savePath = editorMode.equals("game") ? "mods/studio-scripts/" : "mods/";
                Core.files.local(savePath + currentScriptName + ".json").writeString(json);
                statusLabel.setText("Saved: " + currentScriptName);
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
        Seq<String> scripts = new Seq<>();
        String loadPath = editorMode.equals("game") ? "mods/studio-scripts/" : "mods/";
        for(var file : Core.files.local(loadPath).list()) {
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
                dialog.cont.button(scriptName, () -> {
                    loadScript(scriptName);
                    dialog.hide();
                }).row();
            }
        }
        dialog.addCloseButton();
        dialog.show();
    }

    private void loadScript(String name) {
        try {
            String loadPath = editorMode.equals("game") ? "mods/studio-scripts/" : "mods/";
            String json = Core.files.local(loadPath + name + ".json").readString();
            Json jsonParser = new Json();
            jsonParser.setIgnoreUnknownFields(true);
            Seq<NodeData> nodeDataList = jsonParser.fromJson(Seq.class, NodeData.class, json);
            if(nodeDataList == null || nodeDataList.size == 0) {
                throw new Exception("No nodes in file");
            }
            canvas.nodes.clear();
            Seq<Node> loadedNodes = new Seq<>();
            for(NodeData data : nodeDataList) {
                Node node = new Node();
                node.id = data.id;
                node.type = data.type;
                node.label = data.label;
                node.x = data.x;
                node.y = data.y;
                node.value = data.value != null ? data.value : "";
                node.color = Color.valueOf(data.color);
                node.setupInputs();
                if(data.inputValues != null && data.inputValues.size > 0) {
                    for(int i = 0; i < Math.min(node.inputs.size, data.inputValues.size); i++) {
                        node.inputs.get(i).value = data.inputValues.get(i);
                    }
                }
                loadedNodes.add(node);
            }
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
    private void runScript() {
        try {
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
        } catch(Exception e) {
            Log.err("Run failed", e);
            Vars.ui.showInfoFade("Error: " + e.getMessage());
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