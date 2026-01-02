package fuse;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.ui.*;

public class FuseHUD {
    
    private Table container;
    private Label healthLabel;
    private Label speedLabel;
    private Label ammoLabel;
    
    public FuseHUD() {
        buildUI();
    }
    
    private void buildUI() {
        container = new Table();
        container.setFillParent(true);
        container.top();
        
        Table cockpitPanel = new Table(Styles.black6);
        cockpitPanel.margin(10f);
        
        healthLabel = new Label("");
        speedLabel = new Label("");
        ammoLabel = new Label("");
        
        healthLabel.setColor(Color.green);
        speedLabel.setColor(Color.cyan);
        ammoLabel.setColor(Color.orange);
        
        cockpitPanel.add("[#84f490]FUSE MODE").padBottom(5f).row();
        cockpitPanel.add(healthLabel).left().row();
        cockpitPanel.add(speedLabel).left().row();
        cockpitPanel.add(ammoLabel).left().row();
        
        Table crosshairTable = new Table();
        crosshairTable.add(new Image(Core.atlas.white()).setScaling(Scaling.fit))
            .size(4f, 20f).color(Color.valueOf("ff6b6b"));
        crosshairTable.row();
        crosshairTable.add(new Image(Core.atlas.white()).setScaling(Scaling.fit))
            .size(20f, 4f).color(Color.valueOf("ff6b6b"));
        
        container.add(cockpitPanel).pad(20f).top().left().row();
        container.add(crosshairTable).expand().center();
        
        Time.run(5f, () -> {
            Events.run(Trigger.update, this::update);
        });
    }
    
    private void update() {
        if (!FuseMod.enabled || Vars.player == null) return;
        
        Unit unit = Vars.player.unit();
        if (unit == null) return;
        
        float healthPercent = (unit.health / unit.maxHealth) * 100f;
        healthLabel.setText(Strings.format("[#84f490]HP: @%", (int)healthPercent));
        
        float speed = unit.vel.len();
        speedLabel.setText(Strings.format("[cyan]SPD: @", (int)speed));
        
        if (unit.type.weapons.size > 0) {
            ammoLabel.setText(Strings.format("[orange]AMMO: @", unit.ammo));
        } else {
            ammoLabel.setText("[orange]AMMO: N/A");
        }
        
        if (healthPercent < 30f) {
            healthLabel.setColor(Color.red);
        } else if (healthPercent < 60f) {
            healthLabel.setColor(Color.yellow);
        } else {
            healthLabel.setColor(Color.green);
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