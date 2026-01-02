package studio;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.struct.*;
import arc.util.*;
import mindustry.graphics.*;
import mindustry.ui.*;

public class NodeCanvas extends Element {
    public Seq<Node> nodes = new Seq<>();
    public Vec2 offset = new Vec2(0, 0);
    public float zoom = 1f;
    
    public String mode = "move";
    
    private Node dragNode = null;
    private Vec2 dragStart = new Vec2();
    private Vec2 panStart = new Vec2();
    private boolean panning = false;
    
    private Node connectStart = null;
    
    public Runnable onNodeEdit;
    public Node selectedNode = null;
    
    public NodeCanvas() {
        setFillParent(true);
        
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                Vec2 worldPos = screenToWorld(x, y);
                
                if(Core.input.keyDown(KeyCode.mouseRight) || pointer == 1) {
                    panning = true;
                    panStart.set(x, y);
                    return true;
                }
                
                if(mode.equals("move")) {
                    for(Node node : nodes) {
                        if(node.contains(worldPos.x, worldPos.y)) {
                            dragNode = node;
                            dragStart.set(worldPos.x - node.x, worldPos.y - node.y);
                            return true;
                        }
                    }
                }
                
                if(mode.equals("edit")) {
                    for(Node node : nodes) {
                        if(node.contains(worldPos.x, worldPos.y)) {
                            selectedNode = node;
                            if(onNodeEdit != null) {
                                onNodeEdit.run();
                            }
                            return true;
                        }
                    }
                }
                
                if(mode.equals("connect")) {
                    for(Node node : nodes) {
                        if(node.contains(worldPos.x, worldPos.y)) {
                            if(connectStart == null) {
                                connectStart = node;
                            } else {
                                if(connectStart != node && !connectStart.connections.contains(node)) {
                                    connectStart.connections.add(node);
                                }
                                connectStart = null;
                            }
                            return true;
                        }
                    }
                    connectStart = null;
                }
                
                if(mode.equals("delete")) {
                    for(Node node : nodes) {
                        if(node.contains(worldPos.x, worldPos.y)) {
                            nodes.remove(node);
                            for(Node n : nodes) {
                                n.connections.remove(node);
                            }
                            return true;
                        }
                    }
                }
                
                return true;
            }
            
            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if(panning) {
                    offset.x += (x - panStart.x) / zoom;
                    offset.y += (y - panStart.y) / zoom;
                    panStart.set(x, y);
                    return;
                }
                
                if(dragNode != null && mode.equals("move")) {
                    Vec2 worldPos = screenToWorld(x, y);
                    dragNode.x = worldPos.x - dragStart.x;
                    dragNode.y = worldPos.y - dragStart.y;
                }
            }
            
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                dragNode = null;
                panning = false;
            }
            
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                float prevZoom = zoom;
                zoom = arc.math.Mathf.clamp(zoom - amountY * 0.1f, 0.3f, 2f);
                
                Vec2 mouseWorld = screenToWorld(x, y);
                Vec2 mouseWorldAfter = screenToWorld(x, y);
                offset.add(mouseWorld).sub(mouseWorldAfter);
                
                return true;
            }
        });
    }
    
    public Vec2 screenToWorld(float x, float y) {
        return new Vec2(
            (x - width/2f) / zoom - offset.x,
            (y - height/2f) / zoom - offset.y
        );
    }
    
    public Vec2 worldToScreen(float x, float y) {
        return new Vec2(
            (x + offset.x) * zoom + width/2f,
            (y + offset.y) * zoom + height/2f
        );
    }
    
    public void addNode(String type, String label, Color color) {
        Node node = new Node(type, label, -offset.x, -offset.y, color);
        nodes.add(node);
    }
    
    @Override
    public void draw() {
        validate();
        
        Draw.color(Color.darkGray);
        Fill.crect(x, y, width, height);
        
        Lines.stroke(1f);
        for(float gx = -1000f; gx < 1000f; gx += 50f) {
            Vec2 start = worldToScreen(gx, -1000f);
            Vec2 end = worldToScreen(gx, 1000f);
            Draw.color(0.3f, 0.3f, 0.3f, 0.5f);
            Lines.line(start.x, start.y, end.x, end.y);
        }
        for(float gy = -1000f; gy < 1000f; gy += 50f) {
            Vec2 start = worldToScreen(-1000f, gy);
            Vec2 end = worldToScreen(1000f, gy);
            Draw.color(0.3f, 0.3f, 0.3f, 0.5f);
            Lines.line(start.x, start.y, end.x, end.y);
        }
        
        for(Node node : nodes) {
            for(Node target : node.connections) {
                Vec2 start = worldToScreen(node.getOutputPoint().x, node.getOutputPoint().y);
                Vec2 end = worldToScreen(target.getInputPoint().x, target.getInputPoint().y);
                
                Draw.color(Color.white);
                Lines.stroke(3f);
                Lines.line(start.x, start.y, end.x, end.y);
            }
        }
        
        for(Node node : nodes) {
            Vec2 screenPos = worldToScreen(node.x, node.y);
            float screenWidth = node.width * zoom;
            float screenHeight = node.height * zoom;
            
            Draw.color(node.color);
            Fill.crect(screenPos.x, screenPos.y, screenWidth, screenHeight);
            
            Draw.color(Color.white);
            Lines.stroke(2f);
            Lines.rect(screenPos.x, screenPos.y, screenWidth, screenHeight);
            
            Fonts.outline.getData().setScale(0.5f * zoom);
            Fonts.outline.draw(node.label, screenPos.x + 10f * zoom, screenPos.y + screenHeight - 15f * zoom);
            
            if(!node.value.isEmpty()) {
                Fonts.outline.getData().setScale(0.35f * zoom);
                String displayValue = node.value.length() > 15 ? node.value.substring(0, 15) + "..." : node.value;
                Fonts.outline.draw(displayValue, screenPos.x + 10f * zoom, screenPos.y + 20f * zoom);
            }
            
            Fonts.outline.getData().setScale(1f);
            
            Vec2 inputScreen = worldToScreen(node.getInputPoint().x, node.getInputPoint().y);
            Draw.color(Color.green);
            Fill.circle(inputScreen.x, inputScreen.y, 6f * zoom);
            
            Vec2 outputScreen = worldToScreen(node.getOutputPoint().x, node.getOutputPoint().y);
            Draw.color(Color.red);
            Fill.circle(outputScreen.x, outputScreen.y, 6f * zoom);
        }
        
        if(connectStart != null) {
            Vec2 start = worldToScreen(connectStart.getOutputPoint().x, connectStart.getOutputPoint().y);
            Draw.color(Color.yellow);
            Lines.stroke(3f);
            Lines.circle(start.x, start.y, 15f);
        }
        
        Draw.reset();
    }
}