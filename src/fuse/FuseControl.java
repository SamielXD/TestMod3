package fuse;

import arc.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.graphics.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

public class FuseControls {
    
    private Table container;
    private Touchpad moveStick;
    private Touchpad aimStick;
    private Vec2 moveInput = new Vec2();
    private Vec2 aimInput = new Vec2();
    
    public FuseControls() {
        buildUI();
        
        Events.run(Trigger.update, this::update);
    }
    
    private void buildUI() {
        container = new Table();
        container.setFillParent(true);
        container.bottom();
        
        Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle();
        style.background = null;
        style.knob = new TextureRegionDrawable(Core.atlas.white()).tint(Color.valueOf("84f490").a(0.3f));
        
        moveStick = new Touchpad(10f, style);
        aimStick = new Touchpad(10f, style);
        
        Table leftTable = new Table();
        leftTable.add(moveStick).size(150f).pad(20f);
        
        Table rightTable = new Table();
        rightTable.add(aimStick).size(150f).pad(20f);
        
        container.add(leftTable).expandX().left().bottom();
        container.add().grow();
        container.add(rightTable).expandX().right().bottom();
    }
    
    private void update() {
        if (!FuseMod.enabled || Vars.player == null) return;
        
        Unit unit = Vars.player.unit();
        if (unit == null) return;
        
        moveInput.set(moveStick.getKnobPercentX(), moveStick.getKnobPercentY());
        aimInput.set(aimStick.getKnobPercentX(), aimStick.getKnobPercentY());
        
        if (moveInput.len() > 0.1f) {
            float angle = moveInput.angle() + unit.rotation - 90f;
            Vec2 velocity = new Vec2().trns(angle, moveInput.len() * unit.type.speed);
            unit.vel.set(velocity);
        }
        
        if (aimInput.len() > 0.1f) {
            float targetAngle = aimInput.angle();
            unit.lookAt(targetAngle);
            unit.controlWeapons(true);
        }
        
        if (Core.input.keyDown(KeyCode.w)) {
            unit.moveAt(new Vec2(0, 1).rotate(unit.rotation - 90f).scl(unit.type.speed));
        }
        if (Core.input.keyDown(KeyCode.s)) {
            unit.moveAt(new Vec2(0, -1).rotate(unit.rotation - 90f).scl(unit.type.speed));
        }
        if (Core.input.keyDown(KeyCode.a)) {
            unit.moveAt(new Vec2(-1, 0).rotate(unit.rotation - 90f).scl(unit.type.speed));
        }
        if (Core.input.keyDown(KeyCode.d)) {
            unit.moveAt(new Vec2(1, 0).rotate(unit.rotation - 90f).scl(unit.type.speed));
        }
    }
    
    public void show() {
        if (!Core.scene.getChildren().contains(container, true)) {
            Core.scene.add(container);
        }
    }
    
    public void hide() {
        container.remove();
    }
}