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

        statusLabel = new Label("MODE: MOVE");
        statusLabel.setFontScale(1.5f);

        Table buttonTable = new Table();
        buttonTable.defaults().size(150f, 80f).pad(4f);

        buttonTable.button("Close", Icon.left, this::hide);
        buttonTable.button("Save", Icon.save, this::saveScript);
        buttonTable.button("Load", Icon.download, this::showLoadDialog);
        buttonTable.button("Run", Icon.play, this::runScript);
        buttonTable.button("Move", Icon.move, () -> {
            canvas.mode = "move";
            statusLabel.setText("MODE: MOVE");
        });
        buttonTable.button("Edit", Icon.edit, () -> {
            canvas.mode = "edit";
            statusLabel.setText("MODE: EDIT");
        });
        buttonTable.button("Link", Icon.link, () -> {
            canvas.mode = "connect";
            statusLabel.setText("MODE: LINK");
        });
        buttonTable.button("Delete", Icon.trash, () -> {
            canvas.mode = "delete";
            statusLabel.setText("MODE: DELETE");
        });
        buttonTable.button("Add", Icon.add, this::showNodeBrowser);
        buttonTable.button("Z-", Icon.zoom, () -> canvas.zoom = arc.math.Mathf.clamp(canvas.zoom - 0.2f, 0.2f, 3f));
        buttonTable.button("Z+", Icon.zoom, () -> canvas.zoom = arc.math.Mathf.clamp(canvas.zoom + 0.2f, 0.2f, 3f));
        buttonTable.button("Settings", Icon.settings, this::showUISettings);

        ScrollPane scrollPane = new ScrollPane(buttonTable);
        scrollPane.setScrollingDisabled(false, true);

        if(uiLayout.equals("top")) {
            main.getChildren().reverse();
        }

        main.add(scrollPane).growX().height(90f).row();
        main.add(statusLabel).fillX().pad(10f);

        cont.add(main).grow();
    }

    private void showUISettings() {
        BaseDialog dialog = new BaseDialog("UI Settings");
        dialog.cont.defaults().size(400f, 80f).pad(10f);

        Label label = new Label("Button Position:");
        label.setFontScale(1.5f);
        dialog.cont.add(label).row();

        dialog.cont.button("Bottom (Current: " + uiLayout + ")", () -> {
            Core.settings.put("studio-ui-layout", "bottom");
            Vars.ui.showInfoFade("Restart editor to apply");
            dialog.hide();
        }).row();

        dialog.cont.button("Top", () -> {
            Core.settings.put("studio-ui-layout", "top");
            Vars.ui.showInfoFade("Restart editor to apply");
            dialog.hide();
        }).row();

        dialog.addCloseButton();
        dialog.show();
    }

    private void showNodeBrowser() {
        BaseDialog dialog = new BaseDialog("Node Browser");

        Table content = new Table();
        content.defaults().size(450f, 100f).pad(8f);

        content.add("[green]═══ EVENTS ═══").row();
        content.button("ON START", () -> {
            canvas.addNode("event", "On Start", Color.green);
            dialog.hide();
        }).row();
        content.button("ON WAVE", () -> {
            canvas.addNode("event", "On Wave", Color.green);
            dialog.hide();
        }).row();
        content.button("ON UNIT SPAWN", () -> {
            canvas.addNode("event", "On Unit Spawn", Color.green);
            dialog.hide();
        }).row();

        content.add("[blue]═══ ACTIONS ═══").padTop(20f).row();
        content.button("MESSAGE", () -> {
            canvas.addNode("action", "Message", Color.blue);
            dialog.hide();
        }).row();
        content.button("SPAWN UNIT", () -> {
            canvas.addNode("action", "Spawn Unit", Color.blue);
            dialog.hide();
        }).row();
        content.button("SET BLOCK", () -> {
            canvas.addNode("action", "Set Block", Color.blue);
            dialog.hide();
        }).row();
        content.button("CREATE MOD FOLDER", () -> {
            canvas.addNode("action", "Create Mod Folder", Color.cyan);
            dialog.hide();
        }).row();

        content.add("[orange]═══ CONDITIONS ═══").padTop(20f).row();
        content.button("IF", () -> {
            canvas.addNode("condition", "If", Color.orange);
            dialog.hide();
        }).row();
        content.button("WAIT", () -> {
            canvas.addNode("condition", "Wait", Color.orange);
            dialog.hide();
        }).row();

        content.add("[purple]═══ VALUES ═══").padTop(20f).row();
        content.button("NUMBER", () -> {
            canvas.addNode("value", "Number", Color.purple);
            dialog.hide();
        }).row();
        content.button("TEXT", () -> {
            canvas.addNode("value", "Text", Color.purple);
            dialog.hide();
        }).row();
        content.button("UNIT TYPE", () -> {
            canvas.addNode("value", "Unit Type", Color.purple);
            dialog.hide();
        }).row();

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
            if(node.label.equals("Spawn Unit")) {
                showSpawnUnitDialog(dialog, node);
            }
            else if(node.label.equals("Create Mod Folder")) {
                showCreateModDialog(dialog, node);
            }
            else {
                showDefaultEditDialog(dialog, node);
            }
        } else {
            Label label = new Label("This node has no editable properties");
            label.setFontScale(1.5f);
            dialog.cont.add(label).row();
            dialog.buttons.button("DONE", dialog::hide).size(300f, 100f);
        }

        dialog.show();
    }

    private void showSpawnUnitDialog(BaseDialog dialog, Node node) {
        Label unitLabel = new Label("Unit Type:");
        unitLabel.setFontScale(1.5f);
        dialog.cont.add(unitLabel).left().row();

        TextField unitField = new TextField(node.inputs.get(0).value);
        unitField.setStyle(new TextField.TextFieldStyle(unitField.getStyle()));
        unitField.getStyle().font.getData().setScale(1.5f);
        dialog.cont.add(unitField).fillX().height(100f).row();

        Label locationLabel = new Label("Spawn Location:");
        locationLabel.setFontScale(1.5f);
        dialog.cont.add(locationLabel).left().row();

        Table locationButtons = new Table();
        locationButtons.defaults().size(180f, 80f).pad(5f);

        final String[] selectedLocation = {node.inputs.get(1).value};

        TextButton coreBtn = new TextButton("At Core", Styles.togglet);
        coreBtn.setChecked(selectedLocation[0].equals("core"));
        coreBtn.clicked(() -> {
            selectedLocation[0] = "core";
        });
        locationButtons.add(coreBtn);

        TextButton coordBtn = new TextButton("At Coordinates", Styles.togglet);
        coordBtn.setChecked(selectedLocation[0].equals("coordinates"));
        coordBtn.clicked(() -> {
            selectedLocation[0] = "coordinates";
        });
        locationButtons.add(coordBtn);

        TextButton playerBtn = new TextButton("At Player", Styles.togglet);
        playerBtn.setChecked(selectedLocation[0].equals("player"));
        playerBtn.clicked(() -> {
            selectedLocation[0] = "player";
        });
        locationButtons.add(playerBtn);

        dialog.cont.add(locationButtons).row();

        Label xLabel = new Label("X Coordinate:");
        xLabel.setFontScale(1.5f);
        dialog.cont.add(xLabel).left().row();

        TextField xField = new TextField(node.inputs.get(2).value);
        xField.setStyle(new TextField.TextFieldStyle(xField.getStyle()));
        xField.getStyle().font.getData().setScale(1.5f);
        dialog.cont.add(xField).fillX().height(100f).row();

        Label yLabel = new Label("Y Coordinate:");
        yLabel.setFontScale(1.5f);
        dialog.cont.add(yLabel).left().row();

        TextField yField = new TextField(node.inputs.get(3).value);
        yField.setStyle(new TextField.TextFieldStyle(yField.getStyle()));
        yField.getStyle().font.getData().setScale(1.5f);
        dialog.cont.add(yField).fillX().height(100f).row();

        Label amountLabel = new Label("Amount:");
        amountLabel.setFontScale(1.5f);
        dialog.cont.add(amountLabel).left().row();

        TextField amountField = new TextField(node.inputs.get(4).value);
        amountField.setStyle(new TextField.TextFieldStyle(amountField.getStyle()));
        amountField.getStyle().font.getData().setScale(1.5f);
        dialog.cont.add(amountField).fillX().height(100f).row();

        dialog.buttons.button("SAVE", () -> {
            node.inputs.get(0).value = unitField.getText();
            node.inputs.get(1).value = selectedLocation[0];
            node.inputs.get(2).value = xField.getText();
            node.inputs.get(3).value = yField.getText();
            node.inputs.get(4).value = amountField.getText();

            node.value = unitField.getText() + "|" + selectedLocation[0] + "|" + 
                         xField.getText() + "|" + yField.getText() + "|" + amountField.getText();

            dialog.hide();
        }).size(300f, 100f);
    }

    private void showCreateModDialog(BaseDialog dialog, Node node) {
        Label nameLabel = new Label("Mod Folder Name:");
        nameLabel.setFontScale(1.5f);
        dialog.cont.add(nameLabel).left().row();

        TextField nameField = new TextField(node.inputs.get(0).value);
        nameField.setStyle(new TextField.TextFieldStyle(nameField.getStyle()));
        nameField.getStyle().font.getData().setScale(1.5f);
        dialog.cont.add(nameField).fillX().height(100f).row();

        Label displayNameLabel = new Label("Display Name:");
        displayNameLabel.setFontScale(1.5f);
        dialog.cont.add(displayNameLabel).left().row();

        TextField displayNameField = new TextField(node.inputs.get(1).value);
        displayNameField.setStyle(new TextField.TextFieldStyle(displayNameField.getStyle()));
        displayNameField.getStyle().font.getData().setScale(1.5f);
        dialog.cont.add(displayNameField).fillX().height(100f).row();

        Label authorLabel = new Label("Author:");
        authorLabel.setFontScale(1.5f);
        dialog.cont.add(authorLabel).left().row();

        TextField authorField = new TextField(node.inputs.get(2).value);
        authorField.setStyle(new TextField.TextFieldStyle(authorField.getStyle()));
        authorField.getStyle().font.getData().setScale(1.5f);
        dialog.cont.add(authorField).fillX().height(100f).row();

        Label descLabel = new Label("Description:");
        descLabel.setFontScale(1.5f);
        dialog.cont.add(descLabel).left().row();

        TextField descField = new TextField(node.inputs.get(3).value);
        descField.setStyle(new TextField.TextFieldStyle(descField.getStyle()));
        descField.getStyle().font.getData().setScale(1.5f);
        dialog.cont.add(descField).fillX().height(100f).row();

        dialog.buttons.button("SAVE", () -> {
            node.inputs.get(0).value = nameField.getText();
            node.inputs.get(1).value = displayNameField.getText();
            node.inputs.get(2).value = authorField.getText();
            node.inputs.get(3).value = descField.getText();

            node.value = nameField.getText() + "|" + displayNameField.getText() + "|" + 
                         authorField.getText() + "|" + descField.getText();

            dialog.hide();
        }).size(300f, 100f);
    }

    private void showDefaultEditDialog(BaseDialog dialog, Node node) {
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

        dialog.buttons.button("DONE", dialog::hide).size(300f, 100f);
    }private void saveScript() {
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
                Core.files.local("mods/studio-scripts/" + currentScriptName + ".json").writeString(json);

                statusLabel.setText("Saved: " + currentScriptName);
                StudioMod.loadAllScripts();

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
                node.width = 400f;
                node.height = 200f;
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
                    StudioMod.executeNodeChain(node, script);
                }
            }

            if(!hasEventNode) {
                Vars.ui.showInfoFade("No event nodes! Add 'On Start', 'On Wave', or 'On Unit Spawn'.");
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