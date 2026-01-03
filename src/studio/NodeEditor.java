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

    private void showModSelector() {
        BaseDialog dialog = new BaseDialog("Select Mod to Edit");
        dialog.cont.defaults().size(500f, 100f).pad(10f);

        Seq<Fi> mods = new Seq<>();
        Fi modsFolder = StudioMod.modsRootFolder;
        for(Fi folder : modsFolder.list()) {
            if(folder.isDirectory() && (folder.child("mod.hjson").exists() || folder.child("mod.json").exists())) {
                mods.add(folder);
            }
        }

        Label info = new Label("[lightgray]Current: " + StudioMod.currentModName);
        info.setFontScale(1.3f);
        dialog.cont.add(info).padBottom(20f).row();

        if(mods.size == 0) {
            Label label = new Label("[lightgray]No mods found\nCreate one first!");
            label.setFontScale(1.3f);
            dialog.cont.add(label).row();
        } else {
            for(Fi modFolder : mods) {
                String modName = modFolder.name();
                dialog.cont.button("[cyan]" + modName, () -> {
                    StudioMod.currentModName = modName;
                    Vars.ui.showInfoFade("Selected: " + modName);
                    updateStatusLabel();
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
            content.add("[lime]═══ GAME SCRIPT NODES ═══").row();
            content.add("[green]═══ EVENTS ═══").padTop(10f).row();
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

            content.add("[yellow]═══ LOGIC ═══").padTop(20f).row();
            content.button("SET VARIABLE", () -> {
                canvas.addNode("logic", "Set Variable", Color.yellow);
                dialog.hide();
            }).row();
            content.button("GET VARIABLE", () -> {
                canvas.addNode("logic", "Get Variable", Color.yellow);
                dialog.hide();
            }).row();
            content.button("MATH OPERATION", () -> {
                canvas.addNode("logic", "Math Operation", Color.gold);
                dialog.hide();
            }).row();
            content.button("COMPARE", () -> {
                canvas.addNode("logic", "Compare", Color.gold);
                dialog.hide();
            }).row();
            content.button("LOOP", () -> {
                canvas.addNode("logic", "Loop", Color.orange);
                dialog.hide();
            }).row();
            content.button("RANDOM NUMBER", () -> {
                canvas.addNode("logic", "Random Number", Color.yellow);
                dialog.hide();
            }).row();
            content.button("LOG MESSAGE", () -> {
                canvas.addNode("logic", "Log Message", Color.lightGray);
                dialog.hide();
            }).row();
        } else {
            content.add("[cyan]═══ MOD CREATOR NODES ═══").row();
            content.add("[sky]═══ MOD STRUCTURE ═══").padTop(10f).row();
            content.button("CREATE MOD", () -> {
                canvas.addNode("mod", "Create Mod", Color.cyan);
                dialog.hide();
            }).row();

            content.add("[royal]═══ CONTENT CREATION ═══").padTop(20f).row();
            content.button("CREATE BLOCK", () -> {
                canvas.addNode("mod", "Create Block", Color.royal);
                dialog.hide();
            }).row();
            content.button("CREATE UNIT", () -> {
                canvas.addNode("mod", "Create Unit", Color.royal);
                dialog.hide();
            }).row();
            content.button("CREATE ITEM", () -> {
                canvas.addNode("mod", "Create Item", Color.royal);
                dialog.hide();
            }).row();

            content.add("[pink]═══ ASSETS ═══").padTop(20f).row();
            content.button("ADD SPRITE", () -> {
                canvas.addNode("mod", "Add Sprite", Color.pink);
                dialog.hide();
            }).row();
            content.button("CREATE SCRIPT", () -> {
                canvas.addNode("mod", "Create Script", Color.pink);
                dialog.hide();
            }).row();
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
            if(node.label.equals("Spawn Unit")) {
                showSpawnUnitDialog(dialog, node);
            }
            else if(node.label.equals("Create Mod")) {
                showCreateModDialog(dialog, node);
            }
            else if(node.label.equals("Create Block")) {
                showCreateBlockDialog(dialog, node);
            }
            else if(node.label.equals("Create Unit")) {
                showCreateUnitDialog(dialog, node);
            }
            else if(node.label.equals("Create Item")) {
                showCreateItemDialog(dialog, node);
            }
            else if(node.label.equals("Add Sprite")) {
                showAddSpriteDialog(dialog, node);
            }
            else if(node.label.equals("Create Script")) {
                showCreateScriptDialog(dialog, node);
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
    }package studio;

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

    private void showModSelector() {
        BaseDialog dialog = new BaseDialog("Select Mod to Edit");
        dialog.cont.defaults().size(500f, 100f).pad(10f);

        Seq<Fi> mods = new Seq<>();
        Fi modsFolder = StudioMod.modsRootFolder;
        for(Fi folder : modsFolder.list()) {
            if(folder.isDirectory() && (folder.child("mod.hjson").exists() || folder.child("mod.json").exists())) {
                mods.add(folder);
            }
        }

        Label info = new Label("[lightgray]Current: " + StudioMod.currentModName);
        info.setFontScale(1.3f);
        dialog.cont.add(info).padBottom(20f).row();

        if(mods.size == 0) {
            Label label = new Label("[lightgray]No mods found\nCreate one first!");
            label.setFontScale(1.3f);
            dialog.cont.add(label).row();
        } else {
            for(Fi modFolder : mods) {
                String modName = modFolder.name();
                dialog.cont.button("[cyan]" + modName, () -> {
                    StudioMod.currentModName = modName;
                    Vars.ui.showInfoFade("Selected: " + modName);
                    updateStatusLabel();
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
            content.add("[lime]═══ GAME SCRIPT NODES ═══").row();
            content.add("[green]═══ EVENTS ═══").padTop(10f).row();
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

            content.add("[yellow]═══ LOGIC ═══").padTop(20f).row();
            content.button("SET VARIABLE", () -> {
                canvas.addNode("logic", "Set Variable", Color.yellow);
                dialog.hide();
            }).row();
            content.button("GET VARIABLE", () -> {
                canvas.addNode("logic", "Get Variable", Color.yellow);
                dialog.hide();
            }).row();
            content.button("MATH OPERATION", () -> {
                canvas.addNode("logic", "Math Operation", Color.gold);
                dialog.hide();
            }).row();
            content.button("COMPARE", () -> {
                canvas.addNode("logic", "Compare", Color.gold);
                dialog.hide();
            }).row();
            content.button("LOOP", () -> {
                canvas.addNode("logic", "Loop", Color.orange);
                dialog.hide();
            }).row();
            content.button("RANDOM NUMBER", () -> {
                canvas.addNode("logic", "Random Number", Color.yellow);
                dialog.hide();
            }).row();
            content.button("LOG MESSAGE", () -> {
                canvas.addNode("logic", "Log Message", Color.lightGray);
                dialog.hide();
            }).row();
        } else {
            content.add("[cyan]═══ MOD CREATOR NODES ═══").row();
            content.add("[sky]═══ MOD STRUCTURE ═══").padTop(10f).row();
            content.button("CREATE MOD", () -> {
                canvas.addNode("mod", "Create Mod", Color.cyan);
                dialog.hide();
            }).row();

            content.add("[royal]═══ CONTENT CREATION ═══").padTop(20f).row();
            content.button("CREATE BLOCK", () -> {
                canvas.addNode("mod", "Create Block", Color.royal);
                dialog.hide();
            }).row();
            content.button("CREATE UNIT", () -> {
                canvas.addNode("mod", "Create Unit", Color.royal);
                dialog.hide();
            }).row();
            content.button("CREATE ITEM", () -> {
                canvas.addNode("mod", "Create Item", Color.royal);
                dialog.hide();
            }).row();

            content.add("[pink]═══ ASSETS ═══").padTop(20f).row();
            content.button("ADD SPRITE", () -> {
                canvas.addNode("mod", "Add Sprite", Color.pink);
                dialog.hide();
            }).row();
            content.button("CREATE SCRIPT", () -> {
                canvas.addNode("mod", "Create Script", Color.pink);
                dialog.hide();
            }).row();
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
            if(node.label.equals("Spawn Unit")) {
                showSpawnUnitDialog(dialog, node);
            }
            else if(node.label.equals("Create Mod")) {
                showCreateModDialog(dialog, node);
            }
            else if(node.label.equals("Create Block")) {
                showCreateBlockDialog(dialog, node);
            }
            else if(node.label.equals("Create Unit")) {
                showCreateUnitDialog(dialog, node);
            }
            else if(node.label.equals("Create Item")) {
                showCreateItemDialog(dialog, node);
            }
            else if(node.label.equals("Add Sprite")) {
                showAddSpriteDialog(dialog, node);
            }
            else if(node.label.equals("Create Script")) {
                showCreateScriptDialog(dialog, node);
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