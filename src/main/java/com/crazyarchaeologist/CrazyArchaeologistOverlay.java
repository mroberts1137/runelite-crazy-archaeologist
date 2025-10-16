package com.crazyarchaeologist;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.Map;

public class CrazyArchaeologistOverlay extends Overlay {

    private final CrazyArchaeologistPlugin plugin;
    private final CrazyArchaeologistConfig config;
    private final Client client;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public CrazyArchaeologistOverlay(Client client, CrazyArchaeologistPlugin plugin, CrazyArchaeologistConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay()) {
            return null;
        }

        // Render dangerous tiles
        renderDangerousTiles(graphics);

        // Render projectile paths
        if (config.showProjectilePaths()) {
            renderProjectilePaths(graphics);
        }

        // Render debug info
        if (config.showDebugInfo() && !plugin.getActiveProjectiles().isEmpty()) {
            renderDebugInfo(graphics);
        }

        return null;
    }

    private void renderDangerousTiles(Graphics2D graphics) {
        for (WorldPoint worldPoint : plugin.getDangerousTiles()) {
            LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
            if (localPoint == null) {
                continue;
            }

            Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
            if (poly == null) {
                continue;
            }

            Color fillColor = new Color(255, 0, 0, config.tileAlpha());
            graphics.setColor(fillColor);
            graphics.fillPolygon(poly);

            graphics.setColor(Color.RED);
            graphics.setStroke(new BasicStroke(2));
            graphics.drawPolygon(poly);
        }
    }

    private void renderProjectilePaths(Graphics2D graphics) {
        graphics.setColor(Color.YELLOW);
        graphics.setStroke(new BasicStroke(2));

        for (Map.Entry<Projectile, CrazyArchaeologistPlugin.ProjectileData> entry : plugin.getActiveProjectiles().entrySet()) {
            Projectile proj = entry.getKey();
            CrazyArchaeologistPlugin.ProjectileData data = entry.getValue();

            // Draw line from projectile current position to target
            net.runelite.api.Point projPoint = Perspective.localToCanvas(client,
                    new LocalPoint((int) proj.getX(), (int) proj.getY()),
                    client.getPlane(),
                    proj.getHeight());

            LocalPoint targetLocal = LocalPoint.fromWorld(client, data.targetTile);
            if (targetLocal != null && projPoint != null) {
                net.runelite.api.Point targetPoint = Perspective.localToCanvas(client, targetLocal, client.getPlane());
                if (targetPoint != null) {
                    graphics.drawLine(projPoint.getX(), projPoint.getY(), targetPoint.getX(), targetPoint.getY());

                    // Draw circle at target
                    graphics.fillOval(targetPoint.getX() - 3, targetPoint.getY() - 3, 6, 6);
                }
            }
        }
    }

    private void renderDebugInfo(Graphics2D graphics) {
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Active Projectiles: " + plugin.getActiveProjectiles().size())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Dangerous Tiles: " + plugin.getDangerousTiles().size())
                .build());

        for (Map.Entry<Projectile, CrazyArchaeologistPlugin.ProjectileData> entry : plugin.getActiveProjectiles().entrySet()) {
            Projectile proj = entry.getKey();
            CrazyArchaeologistPlugin.ProjectileData data = entry.getValue();

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("ID: " + proj.getId() + " - " + data.ticksRemaining + " ticks")
                    .build());
        }

        net.runelite.api.Point mouse = client.getMouseCanvasPosition();
        panelComponent.render(graphics);
    }
}