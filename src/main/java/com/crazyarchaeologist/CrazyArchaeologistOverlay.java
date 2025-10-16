package com.crazyarchaeologist;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;

public class CrazyArchaeologistOverlay extends Overlay {

    private final CrazyArchaeologistPlugin plugin;
    private final CrazyArchaeologistConfig config;
    private final Client client;

    @Inject
    public CrazyArchaeologistOverlay(CrazyArchaeologistPlugin plugin, CrazyArchaeologistConfig config, Client client) {
        this.plugin = plugin;
        this.config = config;
        this.client = client;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay()) {
            return null;
        }

        renderDangerousTiles(graphics);

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

            // Fill tile with red transparent color
            Color fillColor = new Color(255, 0, 0, config.tileAlpha());
            graphics.setColor(fillColor);
            graphics.fillPolygon(poly);

            // Draw red border
            graphics.setColor(Color.RED);
            graphics.setStroke(new BasicStroke(2));
            graphics.drawPolygon(poly);
        }
    }
}