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
    public float zoom = 0.5f;

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

                Node clickedNode = getNodeAt(worldPos.x, worldPos.y);

                if(mode.equals("move") && clickedNode != null) {
                    dragNode = clickedNode;
                    dragStart.set(worldPos.x - clickedNode.x, worldPos.y - clickedNode.y);
                    return true;
                }

                if(mode.equals("edit") && clickedNode != null) {
                    selectedNode = clickedNode;
                    if(onNodeEdit != null) {
                        onNodeEdit.run();
                    }
                    return true;
                }

                if(mode.equals("connect") && clickedNode != null) {
                    if(connectStart == null) {
                        connectStart = clickedNode;
                    } else {
                        if(connectStart != clickedNode && !connectStart.connections.contains(clickedNode)) {
                            connectStart.connections.add(clickedNode);
                        }
                        connectStart = null;
                    }
                    return true;
                } else if(mode.equals("connect")) {
                    connectStart = null;
                }

                if(mode.equals("delete") && clickedNode != null) {
                    nodes.remove(clickedNode);
                    for(Node n : nodes) {
                        n.connections.remove(clickedNode);
                    }
                    return true;
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
                Vec2 worldPosBefore = screenToWorld(x, y);

                zoom = arc.math.Mathf.clamp(zoom - amountY * 0.15f, 0.2f, 3f);

                Vec2 worldPosAfter = screenToWorld(x, y);
                offset.add(worldPosBefore).sub(worldPosAfter);

                return true;
            }
        });
    }

    private Node getNodeAt(float worldX, float worldY) {
        for(int i = nodes.size - 1; i >= 0; i--) {
            Node node = nodes.get(i);
            float margin = 20f;
            if(worldX >= node.x - margin && 
               worldX <= node.x + node.width + margin &&
               worldY >= node.y - margin && 
               worldY <= node.y + node.height + margin) {
                return node;
            }
        }
        return null;
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

        Draw.color(0.15f, 0.15f, 0.2f, 1f);
        Fill.rect(x + width/2f, y + height/2f, width, height);

        Lines.stroke(2f);
        Draw.color(0.25f, 0.25f, 0.3f, 1f);
        for(float gx = -2000f; gx < 2000f; gx += 50f) {
            Vec2 start = worldToScreen(gx, -2000f);
            if(start.x >= x && start.x <= x + width) {
                Lines.line(start.x, y, start.x, y + height);
            }
        }
        for(float gy = -2000f; gy < 2000f; gy += 50f) {
            Vec2 start = worldToScreen(-2000f, gy);
            if(start.y >= y && start.y <= y + height) {
                Lines.line(x, start.y, x + width, start.y);
            }
        }

        for(Node node : nodes) {
            for(Node target : node.connections) {
                Vec2 start = worldToScreen(node.getOutputPoint().x, node.getOutputPoint().y);
                Vec2 end = worldToScreen(target.getInputPoint().x, target.getInputPoint().y);

                Draw.color(Color.white);
                Lines.stroke(6f);
                Lines.line(start.x, start.y, end.x, end.y);
            }
        }

        float savedScale = Fonts.outline.getData().scaleX;

        for(Node node : nodes) {
            Vec2 screenPos = worldToScreen(node.x, node.y);
            float screenWidth = node.width * zoom;
            float screenHeight = node.height * zoom;

            Draw.color(node.color.r * 0.3f, node.color.g * 0.3f, node.color.b * 0.3f, 0.8f);
            Fill.rect(screenPos.x + screenWidth/2f, screenPos.y + screenHeight/2f, screenWidth, screenHeight);

            Draw.color(node.color);
            Lines.stroke(8f);
            Lines.rect(screenPos.x, screenPos.y, screenWidth, screenHeight);

            if(StudioMod.showLabels) {
                Draw.color(Color.white);
                float labelScale = Math.min(zoom * 1.2f, 1.2f);
                Fonts.outline.getData().setScale(labelScale);
                Fonts.outline.draw(node.label, screenPos.x + 20f * zoom, screenPos.y + screenHeight - 30f * zoom);

                if(!node.value.isEmpty()) {
                    Draw.color(Color.lightGray);
                    float valueScale = Math.min(zoom * 1.0f, 1.0f);
                    Fonts.outline.getData().setScale(valueScale);
                    String displayValue = node.value.length() > 25 ? node.value.substring(0, 25) + "..." : node.value;
                    Fonts.outline.draw(displayValue, screenPos.x + 20f * zoom, screenPos.y + screenHeight/2f);
                }

                Draw.color(node.color.r * 0.8f, node.color.g * 0.8f, node.color.b * 0.8f, 1f);
                float typeScale = Math.min(zoom * 0.7f, 0.7f);
                Fonts.outline.getData().setScale(typeScale);
                Fonts.outline.draw("[" + node.type.toUpperCase() + "]", screenPos.x + 20f * zoom, screenPos.y + 30f * zoom);
            }

            Vec2 inputScreen = worldToScreen(node.getInputPoint().x, node.getInputPoint().y);
            Draw.color(Color.green);
            Fill.circle(inputScreen.x, inputScreen.y, 18f);
            Draw.color(Color.darkGray);
            Lines.stroke(3f);
            Lines.circle(inputScreen.x, inputScreen.y, 18f);

            Vec2 outputScreen = worldToScreen(node.getOutputPoint().x, node.getOutputPoint().y);
            Draw.color(Color.red);
            Fill.circle(outputScreen.x, outputScreen.y, 18f);
            Draw.color(Color.darkGray);
            Lines.stroke(3f);
            Lines.circle(outputScreen.x, outputScreen.y, 18f);
        }

        Fonts.outline.getData().setScale(savedScale);

        if(connectStart != null) {
            Vec2 start = worldToScreen(connectStart.getOutputPoint().x, connectStart.getOutputPoint().y);
            Draw.color(Color.yellow);
            Lines.stroke(6f);
            Lines.circle(start.x, start.y, 30f);
        }

        Draw.reset();
    }
}