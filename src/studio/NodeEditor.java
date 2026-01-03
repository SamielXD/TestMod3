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
    private NodeCanvas canvas;
    private String currentScriptName = "Untitled";
    private Label statusLabel;

    public NodeEditor() {
        super("Studio - Node Editor");

        canvas = new NodeCanvas();
        canvas.onNodeEdit = () -> showEditDialog(canvas.selectedNode);

        buildUI();

        addCloseButton();

        buttons.button("Save", Icon.save, this::saveScript).size(200f, 100f);
        buttons.button("Load", Icon.download, this::showLoadDialog).size(200f, 100f);
        buttons.button("Run", Icon.play, this::runScript).size(200f, 100f);
    }

    private void buildUI() {
        Table main = new Table();
        main.setFillParent(true);

        main.add(canvas).grow().row();

        statusLabel = new Label("");
        statusLabel.setFontScale(1.5f);
        main.add(statusLabel).fillX().pad(10f);

        cont.add(main).grow();
    }

    private void setupModeButtons(TextButton moveBtn, TextButton editBtn, TextButton linkBtn, TextButton delBtn) {
        moveBtn.clicked(() -> {
            moveBtn.setChecked(true);
            editBtn.setChecked(false);
            linkBtn.setChecked(false);
            delBtn.setChecked(false);
        });

        editBtn.clicked(() -> {
            moveBtn.setChecked(false);
            editBtn.setChecked(true);
            linkBtn.setChecked(false);
            delBtn.setChecked(false);
        });

        linkBtn.clicked(() -> {
            moveBtn.setChecked(false);
            editBtn.setChecked(false);
            linkBtn.setChecked(true);
            delBtn.setChecked(false);
        });

        delBtn.clicked(() -> {
            moveBtn.setChecked(false);
            editBtn.setChecked(false);
            linkBtn.setChecked(false);
            delBtn.setChecked(true);
        });

        moveBtn.setChecked(true);
    }

    private void showAddNodeDialog() {
        BaseDialog dialog = new BaseDialog("Add Node");
        dialog.cont.defaults().size(400f, 100f).pad(8f);

        dialog.cont.button("ON START (Event)", () -> {
            canvas.addNode("event", "On Start", Color.green);
            dialog.hide();
        }).row();

        dialog.cont.button("ON WAVE (Event)", () -> {
            canvas.addNode("event", "On Wave", Color.green);
            dialog.hide();
        }).row();

        dialog.cont.button("ON UNIT SPAWN (Event)", () -> {
            canvas.addNode("event", "On Unit Spawn", Color.green);
            dialog.hide();
        }).row();

        dialog.cont.button("SPAWN UNIT (Action)", () -> {
            canvas.addNode("action", "Spawn Unit", Color.blue);
            dialog.hide();
        }).row();

        dialog.cont.button("MESSAGE (Action)", () -> {
            canvas.addNode("action", "Message", Color.blue);
            dialog.hide();
        }).row();

        dialog.cont.button("SET BLOCK (Action)", () -> {
            canvas.addNode("action", "Set Block", Color.blue);
            dialog.hide();
        }).row();

        dialog.cont.button("IF (Condition)", () -> {
            canvas.addNode("condition", "If", Color.orange);
            dialog.hide();
        }).row();

        dialog.cont.button("WAIT (Condition)", () -> {
            canvas.addNode("condition", "Wait", Color.orange);
            dialog.hide();
        }).row();

        dialog.cont.button("NUMBER (Value)", () -> {
            canvas.addNode("value", "Number", Color.purple);
            dialog.hide();
        }).row();

        dialog.cont.button("TEXT (Value)", () -> {
            canvas.addNode("value", "Text", Color.purple);
            dialog.hide();
        }).row();

        dialog.cont.button("UNIT TYPE (Value)", () -> {
            canvas.addNode("value", "Unit Type", Color.purple);
            dialog.hide();
        }).row();

        dialog.addCloseButton();
        dialog.show();
    }

    private void showEditDialog(Node node) {
        if(node == null) return;

        BaseDialog dialog = new BaseDialog("Edit Node: " + node.label);
        dialog.cont.defaults().size(500f, 80f).pad(10f);

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
        } else {
            Label label = new Label("This node has no editable properties");
            label.setFontScale(1.5f);
            dialog.cont.add(label).row();
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
                StudioMod.Script script = new StudioMod.Script();
                script.name = currentScriptName;
                script.enabled = true;
                script.nodes = canvas.nodes;

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

                    data.connectionIds = new Seq<>();
                    for(Node conn : node.connections) {
                        data.connectionIds.add(conn.id);
                    }

                    nodeDataList.add(data);
                }

                String json = new Json().toJson(nodeDataList);

                Core.files.local("mods/studio-scripts/" + currentScriptName + ".json").writeString(json);

                statusLabel.setText("Saved: " + currentScriptName);
                StudioMod.loadAllScripts();

                dialog.hide();
            } catch(Exception e) {
                Log.err("Save failed", e);
            }
        }).size(250f, 100f);

        dialog.buttons.button("CANCEL", dialog::hide).size(250f, 100f);
        dialog.show();
    }

    private void showLoadDialog() {
        BaseDialog dialog = new BaseDialog("Load Script");
        dialog.cont.defaults().size(500f, 100f).pad(10f);

        Seq<String> scripts = new Seq<>();
        for(var file : Core.files.local("mods/studio-scripts/").list()) {
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
            String json = Core.files.local("mods/studio-scripts/" + name + ".json").readString();
            Seq<NodeData> nodeDataList = new Json().fromJson(Seq.class, json);

            canvas.nodes.clear();
            Seq<Node> loadedNodes = new Seq<>();

            for(NodeData data : nodeDataList) {
                Node node = new Node();
                node.id = data.id;
                node.type = data.type;
                node.label = data.label;
                node.x = data.x;
                node.y = data.y;
                node.value = data.value;
                node.color = Color.valueOf(data.color);
                node.width = 400f;
                node.height = 200f;
                node.setupInputs();

                for(Node.NodeInput input : node.inputs) {
                    input.value = data.value;
                }

                loadedNodes.add(node);
            }

            for(int i = 0; i < nodeDataList.size; i++) {
                NodeData data = nodeDataList.get(i);
                Node node = loadedNodes.get(i);

                for(String connId : data.connectionIds) {
                    Node target = loadedNodes.find(n -> n.id.equals(connId));
                    if(target != null) {
                        node.connections.add(target);
                    }
                }
            }

            canvas.nodes = loadedNodes;
            currentScriptName = name;
            statusLabel.setText("Loaded: " + name);

        } catch(Exception e) {
            Log.err("Load failed", e);
        }
    }

    private void runScript() {
        try {
            StudioMod.Script script = new StudioMod.Script();
            script.name = currentScriptName;
            script.enabled = true;
            script.nodes = canvas.nodes;

            boolean hasEventNode = false;
            for(Node node : canvas.nodes) {
                if(node.type.equals("event")) {
                    hasEventNode = true;
                    StudioMod.executeNode(node, script);
                }
            }

            if(!hasEventNode) {
                Vars.ui.showInfoFade("No event nodes! Add 'On Start', 'On Wave', or 'On Unit Spawn'.");
            } else {
                statusLabel.setText("Script executed!");
            }

        } catch(Exception e) {
            Log.err("Run failed", e);
        }
    }

    @Override
    public void addCloseButton() {
        buttons.button("@close", Icon.left, () -> {
            hide();
        }).size(200f, 100f);

        buttons.button("MODES", Icon.menu, () -> {
            showModesDialog();
        }).size(200f, 100f);

        buttons.button("ADD NODE", Icon.add, () -> {
            showAddNodeDialog();
        }).size(200f, 100f);

        buttons.button("ZOOM -", Icon.zoom, () -> {
            canvas.zoom = arc.math.Mathf.clamp(canvas.zoom - 0.2f, 0.2f, 3f);
        }).size(200f, 100f);

        buttons.button("ZOOM +", Icon.zoom, () -> {
            canvas.zoom = arc.math.Mathf.clamp(canvas.zoom + 0.2f, 0.2f, 3f);
        }).size(200f, 100f);
    }

    private void showModesDialog() {
        BaseDialog dialog = new BaseDialog("SELECT MODE");
        dialog.cont.defaults().size(400f, 120f).pad(10f);

        TextButton moveBtn = dialog.cont.button("MOVE MODE", () -> {
            canvas.mode = "move";
            dialog.hide();
        }).get();
        dialog.cont.row();

        TextButton editBtn = dialog.cont.button("EDIT MODE", () -> {
            canvas.mode = "edit";
            dialog.hide();
        }).get();
        dialog.cont.row();

        TextButton linkBtn = dialog.cont.button("LINK MODE", () -> {
            canvas.mode = "connect";
            dialog.hide();
        }).get();
        dialog.cont.row();

        TextButton delBtn = dialog.cont.button("DELETE MODE", () -> {
            canvas.mode = "delete";
            dialog.hide();
        }).get();

        if(canvas.mode.equals("move")) moveBtn.setChecked(true);
        if(canvas.mode.equals("edit")) editBtn.setChecked(true);
        if(canvas.mode.equals("connect")) linkBtn.setChecked(true);
        if(canvas.mode.equals("delete")) delBtn.setChecked(true);

        dialog.addCloseButton();
        dialog.show();
    }

    public static class NodeData {
        public String id;
        public String type;
        public String label;
        public float x, y;
        public String value;
        public String color;
        public Seq<String> connectionIds = new Seq<>();
    }
}