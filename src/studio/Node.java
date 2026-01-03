package studio;

import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;

public class Node {
    public String id;
    public String type;
    public String label;
    public float x, y;
    public float width = 400f;
    public float height = 200f;
    public String value = "";
    public Color color = Color.gray;

    public Seq<Node> connections = new Seq<>();
    public Seq<NodeInput> inputs = new Seq<>();

    public Node() {
        this.id = java.util.UUID.randomUUID().toString();
    }

    public Node(String type, String label, float x, float y, Color color) {
        this();
        this.type = type;
        this.label = label;
        this.x = x;
        this.y = y;
        this.color = color;
        setupInputs();
    }

    public void setupInputs() {
        inputs.clear();

        if(label.equals("Message")) {
            inputs.add(new NodeInput("Text", "Hello!"));
        }
        else if(label.equals("Spawn Unit")) {
            inputs.add(new NodeInput("Unit Type", "dagger"));
            inputs.add(new NodeInput("Spawn Location", "core"));
            inputs.add(new NodeInput("X Coordinate", "0"));
            inputs.add(new NodeInput("Y Coordinate", "0"));
            inputs.add(new NodeInput("Amount", "1"));
        }
        else if(label.equals("Set Block")) {
            inputs.add(new NodeInput("Position (x,y,blockname)", "10,10,copper-wall"));
        }
        else if(label.equals("Create Mod")) {
            inputs.add(new NodeInput("Mod Name", "mymod"));
            inputs.add(new NodeInput("Display Name", "My Mod"));
            inputs.add(new NodeInput("Author", "YourName"));
            inputs.add(new NodeInput("Description", "My custom mod"));
            inputs.add(new NodeInput("Version", "1.0"));
        }
        else if(label.equals("Create Block")) {
            inputs.add(new NodeInput("Block Name", "my-block"));
            inputs.add(new NodeInput("Display Name", "My Block"));
            inputs.add(new NodeInput("Block Type", "wall"));
            inputs.add(new NodeInput("Health", "100"));
            inputs.add(new NodeInput("Size", "1"));
        }
        else if(label.equals("Create Unit")) {
            inputs.add(new NodeInput("Unit Name", "my-unit"));
            inputs.add(new NodeInput("Display Name", "My Unit"));
            inputs.add(new NodeInput("Speed", "1.0"));
            inputs.add(new NodeInput("Health", "200"));
            inputs.add(new NodeInput("Flying", "false"));
        }
        else if(label.equals("Create Item")) {
            inputs.add(new NodeInput("Item Name", "my-item"));
            inputs.add(new NodeInput("Display Name", "My Item"));
            inputs.add(new NodeInput("Color (hex)", "ff0000"));
            inputs.add(new NodeInput("Flammability", "0"));
        }
        else if(label.equals("Add Sprite")) {
            inputs.add(new NodeInput("Sprite Name", "my-sprite"));
            inputs.add(new NodeInput("File Path", "sprites/my-sprite.png"));
        }
        else if(label.equals("Create Script")) {
            inputs.add(new NodeInput("Script Name", "main.js"));
            inputs.add(new NodeInput("Script Content", "// Your code here"));
        }
        else if(label.equals("Wait")) {
            inputs.add(new NodeInput("Seconds", "1"));
        }
        else if(label.equals("If")) {
            inputs.add(new NodeInput("Condition", "true"));
        }
        else if(label.equals("Number")) {
            inputs.add(new NodeInput("Value", "0"));
        }
        else if(label.equals("Text")) {
            inputs.add(new NodeInput("Value", ""));
        }
        else if(label.equals("Unit Type")) {
            inputs.add(new NodeInput("Unit Name", "dagger"));
        }
        else if(label.equals("On Start")) {
            value = "On Start";
        }
        else if(label.equals("On Wave")) {
            value = "On Wave";
        }
        else if(label.equals("On Unit Spawn")) {
            value = "On Unit Spawn";
        }
    }

    public Vec2 getInputPoint() {
        return new Vec2(x, y + height / 2);
    }

    public Vec2 getOutputPoint() {
        return new Vec2(x + width, y + height / 2);
    }

    public static class NodeInput {
        public String label;
        public String value;

        public NodeInput(String label, String defaultValue) {
            this.label = label;
            this.value = defaultValue;
        }
    }
}