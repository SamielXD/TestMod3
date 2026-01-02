package studio;

import arc.graphics.*;
import arc.math.geom.*;
import arc.struct.*;

public class Node {
    public String type;
    public String label;
    public float x, y;
    public float width = 150f, height = 80f;
    public Color color;
    public Seq<Node> connections = new Seq<>();
    public String value = "";
    public String id;
    public Seq<NodeInput> inputs = new Seq<>();
    
    public Node() {}
    
    public Node(String type, String label, float x, float y, Color color) {
        this.type = type;
        this.label = label;
        this.x = x;
        this.y = y;
        this.color = color;
        this.id = java.util.UUID.randomUUID().toString();
        
        setupInputs();
    }
    
    public void setupInputs() {
        inputs.clear();
        
        if(label.equals("Message")) {
            inputs.add(new NodeInput("text", "Message", "Hello!"));
        }
        else if(label.equals("Spawn Unit")) {
            inputs.add(new NodeInput("text", "Unit Type", "dagger"));
        }
        else if(label.equals("Set Block")) {
            inputs.add(new NodeInput("text", "X,Y,Block", "10,10,router"));
        }
        else if(label.equals("Wait")) {
            inputs.add(new NodeInput("number", "Seconds", "1"));
        }
        else if(label.equals("Number")) {
            inputs.add(new NodeInput("number", "Value", "0"));
        }
        else if(label.equals("Text")) {
            inputs.add(new NodeInput("text", "Value", ""));
        }
        else if(label.equals("Unit Type")) {
            inputs.add(new NodeInput("text", "Type", "dagger"));
        }
    }
    
    public boolean contains(float px, float py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }
    
    public Vec2 getCenter() {
        return new Vec2(x + width/2f, y + height/2f);
    }
    
    public Vec2 getInputPoint() {
        return new Vec2(x, y + height/2f);
    }
    
    public Vec2 getOutputPoint() {
        return new Vec2(x + width, y + height/2f);
    }
    
    public static class NodeInput {
        public String type;
        public String label;
        public String value;
        
        public NodeInput() {}
        
        public NodeInput(String type, String label, String defaultValue) {
            this.type = type;
            this.label = label;
            this.value = defaultValue;
        }
    }
}