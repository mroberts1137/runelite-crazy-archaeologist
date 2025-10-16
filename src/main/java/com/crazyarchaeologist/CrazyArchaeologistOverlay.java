package com.crazyarchaeologist;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import javax.inject.Inject;
import java.awt.*;

public class CrazyArchaeologistOverlay extends Overlay {

    private final Client client;
    private final CrazyArchaeologistPlugin plugin;
    private final CrazyArchaeologistConfig config;

    @Inject
    private CrazyArchaeologistOverlay(Client client, CrazyArchaeologistPlugin plugin, CrazyArchaeologistConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showProjectiles() && !config.showDangerTiles()) {
            return null;
        }

        // Draw active projectiles
        if (config.showProjectiles()) {
            for (var entry : plugin.getActiveProjectiles().entrySet()) {
                Projectile projectile = entry.getKey();
                WorldPoint landingPoint = entry.getValue();

                // Draw the projectile's current location
                LocalPoint projLocal = new LocalPoint(projectile.getX1(), projectile.getY1());
                drawProjectile(graphics, projLocal, projectile.getRemainingCycles());

                // Draw where it will land
                if (landingPoint != null) {
                    LocalPoint landLocal = LocalPoint.fromWorld(client, landingPoint);
                    if (landLocal != null) {
                        drawLandingPoint(graphics, landLocal);
                    }
                }
            }
        }

        // Draw dangerous tiles
        if (config.showDangerTiles()) {
            for (CrazyArchaeologistPlugin.TileDanger danger : plugin.getDangerousTiles()) {
                LocalPoint local = LocalPoint.fromWorld(client, danger.location);
                if (local != null) {
                    drawDangerousTile(graphics, local, danger.ticksRemaining);
                }
            }
        }

        return null;
    }

    private void drawProjectile(Graphics2D graphics, LocalPoint point, int remainingCycles) {
        Polygon poly = Perspective.getCanvasTilePoly(client, point);
        if (poly == null) {
            return;
        }

        // Yellow outline for projectile location
        graphics.setColor(new Color(255, 255, 0, 150));
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(poly);

        // Draw remaining cycles text
        net.runelite.api.Point textPoint = Perspective.getCanvasTextLocation(client, graphics, point,
                String.valueOf(remainingCycles), 0);
        if (textPoint != null) {
            graphics.setColor(Color.YELLOW);
            graphics.drawString(String.valueOf(remainingCycles), textPoint.getX(), textPoint.getY());
        }
    }

    private void drawLandingPoint(Graphics2D graphics, LocalPoint point) {
        Polygon poly = Perspective.getCanvasTilePoly(client, point);
        if (poly == null) {
            return;
        }

        // Orange outline for landing point
        graphics.setColor(new Color(255, 165, 0, 200));
        graphics.setStroke(new BasicStroke(3));
        graphics.draw(poly);

        // Fill with semi-transparent orange
        graphics.setColor(new Color(255, 165, 0, 50));
        graphics.fill(poly);
    }

    private void drawDangerousTile(Graphics2D graphics, LocalPoint point, int ticksRemaining) {
        Polygon poly = Perspective.getCanvasTilePoly(client, point);
        if (poly == null) {
            return;
        }

        // Red for dangerous tiles
        int alpha = Math.min(255, ticksRemaining * 50);
        graphics.setColor(new Color(255, 0, 0, Math.min(alpha, 100)));
        graphics.fill(poly);

        graphics.setColor(new Color(255, 0, 0, Math.min(alpha, 200)));
        graphics.setStroke(new BasicStroke(2));
        graphics.draw(poly);

        // Draw tick countdown
        net.runelite.api.Point textPoint = Perspective.getCanvasTextLocation(client, graphics, point,
                String.valueOf(ticksRemaining), 0);
        if (textPoint != null) {
            graphics.setColor(Color.WHITE);
            graphics.drawString(String.valueOf(ticksRemaining), textPoint.getX(), textPoint.getY());
        }
    }
}