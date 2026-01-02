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
        
        if(Core.graphics.isPortrait()) {
            buildMobile();
        } else {
            buildDesktop();
        }
        
        addCloseButton();
        
        buttons.button("Save", Icon.save, this::saveScript).size(120f, 64f);
        buttons.button("Load", Icon.download, this::showLoadDialog).size(120f, 64f);
        buttons.button("Run", Icon.play, this::runScript).size(120f, 64f);
    }
    
    private void buildDesktop() {
        Table main = new Table();
        main.setFillParent(true);
        
        Table toolbox = new Table(Styles.black6);
        toolbox.defaults().size(120f, 50f).pad(5f);
        
        toolbox.button("On Start", () -> canvas.addNode("event", "On Start", Color.green)).row();
        toolbox.button("On Wave", () -> canvas.addNode("event", "On Wave", Color.green)).row();
        toolbox.button("On Unit Spawn", () -> canvas.addNode("event", "On Unit Spawn", Color.green)).row();
        toolbox.row();
        
        toolbox.button("Spawn Unit", () -> canvas.addNode("action", "Spawn Unit", Color.blue)).row();
        toolbox.button("Message", () -> canvas.addNode("action", "Message", Color.blue)).row();
        toolbox.button("Set Block", () -> canvas.addNode("action", "Set Block", Color.blue)).row();
        toolbox.row();
        
        toolbox.button("If", () -> canvas.addNode("condition", "If", Color.orange)).row();
        toolbox.button("Wait", () -> canvas.addNode("condition", "Wait", Color.orange)).row();
        toolbox.row();
        
        toolbox.button("Number", () -> canvas.addNode("value", "Number", Color.purple)).row();
        toolbox.button("Text", () -> canvas.addNode("value", "Text", Color.purple)).row();
        toolbox.button("Unit Type", () -> canvas.addNode("value", "Unit Type", Color.purple)).row();
        
        main.add(toolbox).fillY().width(150f);
        main.add(canvas).grow();
        
        Table modes = new Table();
        modes.defaults().size(80f, 50f).pad(5f);
        modes.button("Move", () -> canvas.mode = "move");
        modes.button("Edit", () -> canvas.mode = "edit");
        modes.button("Connect", () -> canvas.mode = "connect");
        modes.button("Delete", () -> canvas.mode = "delete");
        
        statusLabel = new Label("");
        modes.add(statusLabel).growX().padLeft(20f);
        
        main.row();
        main.add(modes).colspan(2).fillX();
        
        cont.add(main).grow();
    }
    
    private void buildMobile() {
        Table main = new Table();
        main.setFillParent(true);
        
        Table topBar = new Table(Styles.black6);
        topBar.defaults().size(70f, 50f).pad(3f);
        topBar.button("Move", () -> canvas.mode = "move");
        topBar.button("Edit", () -> canvas.mode = "edit");
        topBar.button("Connect", () -> canvas.mode = "connect");
        topBar.button("Delete", () -> canvas.mode = "delete");
        topBar.button("Add", () -> showAddNodeDialog());
        
        main.add(topBar).fillX().row();
        main.add(canvas).grow().row();
        
        statusLabel = new Label("");
        main.add(statusLabel).fillX().pad(5f);
        
        cont.add(main).grow();
    }
    
    private void showAddNodeDialog() {
        BaseDialog dialog = new BaseDialog("Add Node");
        dialog.cont.defaults().size(200f, 50f).pad(5f);
        
        dialog.cont.button("On Start", () -> {
            canvas.addNode("event", "On Start", Color.green);
            dialog.hide();
        }).row();
        
        dialog.cont.button("On Wave", () -> {
            canvas.addNode("event", "On Wave", Color.green);
            dialog.hide();
        }).row();
        
        dialog.cont.button("On Unit Spawn", () -> {
            canvas.addNode("event", "On Unit Spawn", Color.green);
            dialog.hide();
        }).row();
        
        dialog.cont.button("Spawn Unit", () -> {
            canvas.addNode("action", "Spawn Unit", Color.blue);
            dialog.hide();
        }).row();
        
        dialog.cont.button("Message", () -> {
            canvas.addNode("action", "Message", Color.blue);
            dialog.hide();
        }).row();
        
        dialog.cont.button("Set Block", () -> {
            canvas.addNode("action", "Set Block", Color.blue);
            dialog.hide();
        }).row();
        
        dialog.cont.button("If", () -> {
            canvas.addNode("condition", "If", Color.orange);
            dialog.hide();
        }).row();
        
        dialog.cont.button("Wait", () -> {
            canvas.addNode("condition", "Wait", Color.orange);
            dialog.hide();
        }).row();
        
        dialog.cont.button("Number", () -> {
            canvas.addNode("value", "Number", Color.purple);
            dialog.hide();
        }).row();
        
        dialog.cont.button("Text", () -> {
            canvas.addNode("value", "Text", Color.purple);
            dialog.hide();
        }).row();
        
        dialog.cont.button("Unit Type", () -> {
            canvas.addNode("value", "Unit Type", Color.purple);
            dialog.hide();
        }).row();
        
        dialog.addCloseButton();
        dialog.show();
    }
    
    private void showEditDialog(Node node) {
        if(node == null) return;
        
        BaseDialog dialog = new BaseDialog("Edit Node: " + node.label);
        dialog.cont.defaults().size(300f, 50f).pad(5f);
        
        if(node.inputs.size > 0) {
            for(Node.NodeInput input : node.inputs) {
                dialog.cont.add(input.label + ":").left().row();
                
                TextField field = new TextField(input.value);
                dialog.cont.add(field).fillX().row();
                
                field.changed(() -> {
                    input.value = field.getText();
                    node.value = field.getText();
                });
            }
        } else {
            dialog.cont.add("This node has no editable properties").row();
        }
        
        dialog.buttons.button("Done", dialog::hide).size(150f, 50f);
        dialog.show();
    }
    
    private void saveScript() {
        BaseDialog dialog = new BaseDialog("Save Script");
        dialog.cont.add("Script Name:").row();
        
        TextField nameField = new TextField(currentScriptName);
        dialog.cont.add(nameField).size(300f, 50f).pad(10f).row();
        
        dialog.buttons.button("Save", () -> {
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
        }).size(150f, 50f);
        
        dialog.buttons.button("Cancel", dialog::hide).size(150f, 50f);
        dialog.show();
    }
    
    private void showLoadDialog() {
        BaseDialog dialog = new BaseDialog("Load Script");
        dialog.cont.defaults().size(300f, 50f).pad(5f);
        
        Seq<String> scripts = new Seq<>();
        for(var file : Core.files.local("mods/studio-scripts/").list()) {
            if(file.extension().equals("json")) {
                scripts.add(file.nameWithoutExtension());
            }
        }
        
        if(scripts.size == 0) {
            dialog.cont.add("No saved scripts found").row();
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