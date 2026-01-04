package studio;

import arc.*;
import arc.files.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

public class NodeEditor extends BaseDialog {
    public NodeCanvas canvas;
    private String currentScriptName = "Untitled";
    private Label statusLabel;
    public String editorMode = "game";

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
        statusLabel.setFontScale(1.2f);

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

    public void updateStatusLabel() {
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
            for(int i = 0; i < node.inputs.size; i++) {
                Node.NodeInput input = node.inputs.get(i);
                
                if(node.label.equals("Spawn Unit") && input.label.equals("Spawn Location")) {
                    Label label = new Label(input.label + ":");
                    label.setFontScale(1.2f);
                    dialog.cont.add(label).left().row();
                    
                    ButtonGroup<TextButton> group = new ButtonGroup<>();
                    Table locTable = new Table();
                    
                    TextButton playerBtn = new TextButton("At Player", Styles.togglet);
                    TextButton coreBtn = new TextButton("At Core", Styles.togglet);
                    TextButton coordBtn = new TextButton("At Coordinates", Styles.togglet);
                    
                    group.add(playerBtn, coreBtn, coordBtn);
                    
                    if(input.value.equals("At Player")) playerBtn.setChecked(true);
                    else if(input.value.equals("At Core")) coreBtn.setChecked(true);
                    else coordBtn.setChecked(true);
                    
                    playerBtn.clicked(() -> input.value = "At Player");
                    coreBtn.clicked(() -> input.value = "At Core");
                    coordBtn.clicked(() -> input.value = "At Coordinates");
                    
                    locTable.add(playerBtn).width(180f).height(60f);
                    locTable.add(coreBtn).width(180f).height(60f);
                    locTable.add(coordBtn).width(220f).height(60f);
                    
                    dialog.cont.add(locTable).row();
                }
                else if(node.label.equals("Spawn Unit") && (input.label.equals("X Coordinate") || input.label.equals("Y Coordinate"))) {
                    continue;
                }
                else {
                    Label label = new Label(input.label + ":");
                    label.setFontScale(1.2f);
                    dialog.cont.add(label).left().row();
                    TextField field = new TextField(input.value);
                    field.setStyle(new TextField.TextFieldStyle(field.getStyle()));
                    field.getStyle().font.getData().setScale(1.2f);
                    dialog.cont.add(field).fillX().height(80f).row();
                    field.changed(() -> {
                        input.value = field.getText();
                        node.value = buildNodeValue(node);
                    });
                }
            }
        }
        dialog.buttons.button("DONE", dialog::hide).size(300f, 80f);
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
        label.setFontScale(1.3f);
        dialog.cont.add(label).row();
        TextField nameField = new TextField(currentScriptName);
        nameField.setStyle(new TextField.TextFieldStyle(nameField.getStyle()));
        nameField.getStyle().font.getData().setScale(1.3f);
        dialog.cont.add(nameField).size(500f, 80f).pad(15f).row();
        dialog.buttons.button("SAVE", () -> {
            currentScriptName = nameField.getText();
            try {
                StringBuilder json = new StringBuilder();
                json.append("[\n");
                
                for(int i = 0; i < canvas.nodes.size; i++) {
                    Node node = canvas.nodes.get(i);
                    json.append("  {\n");
                    json.append("    \"id\": \"").append(node.id).append("\",\n");
                    json.append("    \"type\": \"").append(node.type).append("\",\n");
                    json.append("    \"label\": \"").append(node.label).append("\",\n");
                    json.append("    \"x\": ").append(node.x).append(",\n");
                    json.append("    \"y\": ").append(node.y).append(",\n");
                    json.append("    \"value\": \"").append(node.value.replace("\"", "\\\"")).append("\",\n");
                    json.append("    \"color\": \"").append(node.color.toString()).append("\",\n");
                    
                    json.append("    \"inputValues\": [");
                    for(int j = 0; j < node.inputs.size; j++) {
                        json.append("\"").append(node.inputs.get(j).value.replace("\"", "\\\"")).append("\"");
                        if(j < node.inputs.size - 1) json.append(", ");
                    }
                    json.append("],\n");
                    
                    json.append("    \"connectionIds\": [");
                    for(int j = 0; j < node.connections.size; j++) {
                        json.append("\"").append(node.connections.get(j).id).append("\"");
                        if(j < node.connections.size - 1) json.append(", ");
                    }
                    json.append("]\n");
                    
                    json.append("  }");
                    if(i < canvas.nodes.size - 1) json.append(",");
                    json.append("\n");
                }
                
                json.append("]");
                
                String savePath = editorMode.equals("game") ? "mods/studio-scripts/" : "mods/studio-mods/";
                Fi saveFolder = Core.files.local(savePath);
                saveFolder.mkdirs();
                
                Fi saveFile = saveFolder.child(currentScriptName + ".json");
                saveFile.writeString(json.toString());
                
                statusLabel.setText("Saved: " + currentScriptName);
                Vars.ui.showInfoFade("Saved to: " + saveFile.path());
                Log.info("Saved to: " + saveFile.path());
                dialog.hide();
            } catch(Exception e) {
                Log.err("Save failed", e);
                Vars.ui.showInfoFade("Save failed: " + e.getMessage());
            }
        }).size(250f, 80f);
        dialog.buttons.button("CANCEL", dialog::hide).size(250f, 80f);
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
            label.setFontScale(1.3f);
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
                throw new Exception("File not found");
            }

            String json = file.readString();
            if(json == null || json.trim().isEmpty()) {
                throw new Exception("File is empty");
            }

            canvas.nodes.clear();
            Seq<Node> loadedNodes = new Seq<>();
            ObjectMap<String, String> idMap = new ObjectMap<>();

            json = json.trim();
            if(!json.startsWith("[")) {
                throw new Exception("Invalid JSON format");
            }

            json = json.substring(1, json.length() - 1);
            String[] nodeStrings = json.split("\\},\\s*\\{");

            for(String nodeStr : nodeStrings) {
                nodeStr = nodeStr.trim();
                if(!nodeStr.startsWith("{")) nodeStr = "{" + nodeStr;
                if(!nodeStr.endsWith("}")) nodeStr = nodeStr + "}";

                Node node = new Node();
                node.id = java.util.UUID.randomUUID().toString();
                
                String oldId = extractValue(nodeStr, "id");
                node.type = extractValue(nodeStr, "type");
                node.label = extractValue(nodeStr, "label");
                
                try {
                    node.x = Float.parseFloat(extractValue(nodeStr, "x"));
                    node.y = Float.parseFloat(extractValue(nodeStr, "y"));
                } catch(Exception e) {
                    node.x = 0;
                    node.y = 0;
                }
                
                node.value = extractValue(nodeStr, "value");
                
                try {
                    node.color = Color.valueOf(extractValue(nodeStr, "color"));
                } catch(Exception e) {
                    node.color = Color.gray;
                }

                node.setupInputs();

                String inputValuesStr = extractArray(nodeStr, "inputValues");
                if(inputValuesStr != null && !inputValuesStr.isEmpty()) {
                    String[] values = inputValuesStr.split("\",\\s*\"");
                    for(int i = 0; i < Math.min(node.inputs.size, values.length); i++) {
                        String val = values[i].replace("\"", "").replace("\\\"", "\"").trim();
                        node.inputs.get(i).value = val;
                    }
                }

                idMap.put(oldId, node.id);
                loadedNodes.add(node);
            }

            for(int i = 0; i < nodeStrings.length; i++) {
                String nodeStr = nodeStrings[i];
                Node node = loadedNodes.get(i);

                String connIdsStr = extractArray(nodeStr, "connectionIds");
                if(connIdsStr != null && !connIdsStr.isEmpty()) {
                    String[] connIds = connIdsStr.split("\",\\s*\"");
                    for(String oldConnId : connIds) {
                        oldConnId = oldConnId.replace("\"", "").trim();
                        String newConnId = idMap.get(oldConnId);
                        if(newConnId != null) {
                            Node target = loadedNodes.find(n -> n.id.equals(newConnId));
                            if(target != null) {
                                node.connections.add(target);
                            }
                        }
                    }
                }
            }

            canvas.nodes = loadedNodes;
            currentScriptName = name;
            statusLabel.setText("Loaded: " + name + " (" + canvas.nodes.size + " nodes)");
            Vars.ui.showInfoFade("Loaded " + canvas.nodes.size + " nodes!");
            Log.info("Successfully loaded: " + name);

        } catch(Exception e) {
            Log.err("Load failed: " + name, e);
            Vars.ui.showInfoFade("Load failed: " + e.getMessage());
            statusLabel.setText("Load FAILED!");
        }
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if(start == -1) return "";
        
        start += search.length();
        while(start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\n')) start++;
        
        if(json.charAt(start) == '"') {
            start++;
            int end = start;
            while(end < json.length()) {
                if(json.charAt(end) == '"' && (end == 0 || json.charAt(end - 1) != '\\')) {
                    return json.substring(start, end);
                }
                end++;
            }
        } else {
            int end = start;
            while(end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '\n' && json.charAt(end) != '}') {
                end++;
            }
            return json.substring(start, end).trim();
        }
        
        return "";
    }

    private String extractArray(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if(start == -1) return "";
        
        start += search.length();
        while(start < json.length() && json.charAt(start) != '[') start++;
        if(start >= json.length()) return "";
        
        start++;
        int end = json.indexOf(']', start);
        if(end == -1) return "";
        
        return json.substring(start, end).trim();
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
        boolean hasModFolder = false;
        for(Node node : canvas.nodes) {
            if(node.label.equals("Create Mod Folder")) {
                hasModFolder = true;
                String folderName = node.inputs.get(0).value;
                Fi modFolder = Core.files.local("mods/" + folderName);
                
                if(!modFolder.exists()) {
                    modFolder.mkdirs();
                    Log.info("Created mod folder: " + modFolder.path());
                    Vars.ui.showInfoFade("Created: " + modFolder.path());
                }

                for(Node child : node.connections) {
                    executeModNode(child, modFolder);
                }
            }
        }
        
        if(!hasModFolder) {
            Vars.ui.showInfoFade("Add 'Create Mod Folder' node first!");
        } else {
            Vars.ui.showInfoFade("Mod structure created!");
            statusLabel.setText("Mod created!");
        }
    }

    private void executeModNode(Node node, Fi currentFolder) {
    try {
        Log.info("Executing mod node: " + node.label + " in folder: " + currentFolder.path());

        if(node.label.equals("Create Folder")) {
            String folderName = node.inputs.get(0).value;
            Fi newFolder = currentFolder.child(folderName);
            
            // Make sure it exists as a directory
            if(!newFolder.exists()) {
                newFolder.mkdirs();
                Log.info("Created folder: " + newFolder.path());
            }
            
            // Verify it's actually a directory
            if(newFolder.isDirectory()) {
                Log.info("Verified folder exists: " + newFolder.path());
            } else {
                Log.err("ERROR: " + newFolder.path() + " is not a directory!");
            }

            for(Node child : node.connections) {
                executeModNode(child, newFolder);
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

            Fi hjsonFile = currentFolder.child("mod.hjson");
            
            // Ensure parent directory exists
            if(!currentFolder.exists()) {
                currentFolder.mkdirs();
                Log.info("Created parent directory: " + currentFolder.path());
            }
            
            hjsonFile.writeString(hjson);
            
            // Verify file was created
            if(hjsonFile.exists()) {
                Log.info("✓ VERIFIED: Created mod.hjson at: " + hjsonFile.path());
                Log.info("  File size: " + hjsonFile.length() + " bytes");
            } else {
                Log.err("✗ FAILED: mod.hjson was NOT created at: " + hjsonFile.path());
            }
        }
        else if(node.label.equals("Create Block File")) {
            String blockName = node.inputs.get(0).value;
            String type = node.inputs.get(1).value;
            String health = node.inputs.get(2).value;
            String size = node.inputs.get(3).value;

            String hjson = "type: " + type + "\n" +
                          "health: " + health + "\n" +
                          "size: " + size + "\n";

            Fi blockFile = currentFolder.child(blockName + ".hjson");
            
            // Ensure parent directory exists
            if(!currentFolder.exists()) {
                currentFolder.mkdirs();
                Log.info("Created parent directory: " + currentFolder.path());
            }
            
            blockFile.writeString(hjson);
            
            // Verify file was created
            if(blockFile.exists()) {
                Log.info("✓ VERIFIED: Created block file at: " + blockFile.path());
                Log.info("  File size: " + blockFile.length() + " bytes");
                Log.info("  Full path: " + blockFile.absolutePath());
            } else {
                Log.err("✗ FAILED: Block file was NOT created at: " + blockFile.path());
            }
        }
        else if(node.label.equals("Create Unit File")) {
            String unitName = node.inputs.get(0).value;
            String type = node.inputs.get(1).value;
            String health = node.inputs.get(2).value;
            String speed = node.inputs.get(3).value;

            String hjson = "type: " + type + "\n" +
                          "health: " + health + "\n" +
                          "speed: " + speed + "\n";

            Fi unitFile = currentFolder.child(unitName + ".hjson");
            
            // Ensure parent directory exists
            if(!currentFolder.exists()) {
                currentFolder.mkdirs();
                Log.info("Created parent directory: " + currentFolder.path());
            }
            
            unitFile.writeString(hjson);
            
            // Verify file was created
            if(unitFile.exists()) {
                Log.info("✓ VERIFIED: Created unit file at: " + unitFile.path());
                Log.info("  File size: " + unitFile.length() + " bytes");
            } else {
                Log.err("✗ FAILED: Unit file was NOT created at: " + unitFile.path());
            }
        }
        else if(node.label.equals("Create Item File")) {
            String itemName = node.inputs.get(0).value;
            String color = node.inputs.get(1).value;
            String cost = node.inputs.get(2).value;

            String hjson = "color: " + color + "\n" +
                          "cost: " + cost + "\n";

            Fi itemFile = currentFolder.child(itemName + ".hjson");
            
            // Ensure parent directory exists
            if(!currentFolder.exists()) {
                currentFolder.mkdirs();
                Log.info("Created parent directory: " + currentFolder.path());
            }
            
            itemFile.writeString(hjson);
            
            // Verify file was created
            if(itemFile.exists()) {
                Log.info("✓ VERIFIED: Created item file at: " + itemFile.path());
                Log.info("  File size: " + itemFile.length() + " bytes");
            } else {
                Log.err("✗ FAILED: Item file was NOT created at: " + itemFile.path());
            }
        }
    } catch(Exception e) {
        Log.err("Error in executeModNode: " + node.label, e);
        Vars.ui.showInfoFade("Error creating " + node.label + ": " + e.getMessage());
    }
  }
}