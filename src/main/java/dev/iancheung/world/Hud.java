package dev.iancheung.world;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

import java.util.ArrayList;
import java.util.List;

public class Hud {

    private final SimpleApplication app;
    private final Map map;
    private final Player player;

    private final Node minimapNode = new Node("MinimapPanel");
    private BitmapText hudText;
    private BitmapText hudShadowText;
    private BitmapText winText;
    private BitmapText winShadowText;

    private final List<Geometry> minimapTiles = new ArrayList<>();
    private Geometry minimapPlayerMarker;
    private Geometry minimapFacingMarker;
    private int currentLevel = 1;
    private int lastScore = 0;
    private int lastTotalCoins = 0;

    private static final int MINIMAP_RADIUS = 12;
    private static final float MINIMAP_SCREEN_RATIO = 0.3f;
    private static final float MINIMAP_MARGIN = 30f;
    private static final float HUD_TEXT_SIZE = 1.55f;
    private static final float WIN_TEXT_SIZE = 2.15f;
    private static final float TEXT_SHADOW_OFFSET = 2.0f;
    private static final float MINIMAP_TILE_Z = 1.0f;
    private static final float PLAYER_MARKER_Z = 2.0f;
    private static final float FACING_MARKER_Z = 3.0f;
    private float minimapTileSize = 8.0f;
    private float minimapDiameter = (MINIMAP_RADIUS * 2 + 1) * minimapTileSize;

    public Hud(SimpleApplication app, Map map, Player player) {
        this.app = app;
        this.map = map;
        this.player = player;

        app.getGuiNode().attachChild(minimapNode);
        initFontsAndTexts();
    }

    private void initFontsAndTexts() {
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        hudShadowText = new BitmapText(font, false);
        hudShadowText.setSize(font.getCharSet().getRenderedSize() * HUD_TEXT_SIZE);
        hudShadowText.setColor(new ColorRGBA(0f, 0f, 0f, 0.8f));
        hudShadowText.setLocalTranslation(27, app.getCamera().getHeight() - 27, 0);
        app.getGuiNode().attachChild(hudShadowText);

        hudText = new BitmapText(font, false);
        hudText.setSize(font.getCharSet().getRenderedSize() * HUD_TEXT_SIZE);
        hudText.setColor(ColorRGBA.White);
        hudText.setLocalTranslation(25, app.getCamera().getHeight() - 29, 0);
        app.getGuiNode().attachChild(hudText);

        winShadowText = new BitmapText(font, false);
        winShadowText.setSize(font.getCharSet().getRenderedSize() * WIN_TEXT_SIZE);
        winShadowText.setColor(new ColorRGBA(0f, 0f, 0f, 0.8f));

        winText = new BitmapText(font, false);
        winText.setSize(font.getCharSet().getRenderedSize() * WIN_TEXT_SIZE);
        winText.setColor(new ColorRGBA(1f, 0.95f, 0.65f, 1f));
    }

    public void resetHud(int level, int totalCoins) {
        currentLevel = level;
        lastScore = 0;
        lastTotalCoins = totalCoins;
        updateScore(0, totalCoins);
        hudText.setColor(ColorRGBA.White);
        hudShadowText.setText(hudText.getText());
        hudShadowText.setColor(new ColorRGBA(0f, 0f, 0f, 0.8f));
        rebuildMinimapUI();
    }

    public void updateScore(int current, int total) {
        lastScore = current;
        lastTotalCoins = total;

        String text = "LEVEL: " + currentLevel + " | COINS: " + current + " / " + total;
        hudText.setText(text);
        hudShadowText.setText(text);
        hudShadowText.setLocalTranslation(hudText.getLocalTranslation().x + TEXT_SHADOW_OFFSET, hudText.getLocalTranslation().y - TEXT_SHADOW_OFFSET, 0);
    }

    public void showWarning(String warning) {
        hudText.setColor(new ColorRGBA(1f, 0.3f, 0.2f, 1f));
        hudShadowText.setColor(new ColorRGBA(0f, 0f, 0f, 0.8f));
        hudText.setText(warning);
        hudShadowText.setText(warning);
    }

    public void clearWarning() {
        hudText.setColor(ColorRGBA.White);
        updateScore(lastScore, lastTotalCoins);
    }

    public void showWinScreen(int countdown) {
        minimapNode.setCullHint(Spatial.CullHint.Always);
        String text = "ZONE CLEARED!\nNext level ready in " + countdown + "s...";
        winText.setText(text);
        winShadowText.setText(text);

        float x = (app.getCamera().getWidth() / 2f) - (winText.getLineWidth() / 2f);
        float y = (app.getCamera().getHeight() / 2f) + (winText.getLineHeight() / 2f);
        winText.setLocalTranslation(x, y, 0);
        winShadowText.setLocalTranslation(x + TEXT_SHADOW_OFFSET, y - TEXT_SHADOW_OFFSET, 0);

        if (winShadowText.getParent() == null) {
            app.getGuiNode().attachChild(winShadowText);
        }
        if (winText.getParent() == null) {
            app.getGuiNode().attachChild(winText);
        }
    }

    public void hideWinScreen() {
        app.getGuiNode().detachChild(winText);
        app.getGuiNode().detachChild(winShadowText);
        minimapNode.setCullHint(Spatial.CullHint.Never);
    }

    private void rebuildMinimapUI() {
        minimapNode.detachAllChildren();
        minimapTiles.clear();

        refreshMinimapMetrics();
        float startX = getMinimapOriginX();
        float startY = getMinimapOriginY();

        Quad bgQuad = new Quad(minimapDiameter + 10f, minimapDiameter + 10f);
        Geometry bg = new Geometry("MinimapBG", bgQuad);
        Material bgMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        bgMat.setColor("Color", new ColorRGBA(0.05f, 0.05f, 0.08f, 0.6f));
        bgMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        bg.setMaterial(bgMat);
        bg.setLocalTranslation(startX - 5f, startY - 5f, 0);
        minimapNode.attachChild(bg);

        int stride = getMinimapStride();
        for (int i = 0; i < stride; i++) {
            for (int j = 0; j < stride; j++) {
                Quad q = new Quad(minimapTileSize, minimapTileSize);
                Geometry tile = new Geometry("MapTile_" + i + "_" + j, q);
                Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
                tile.setMaterial(m);
                tile.setLocalTranslation(startX + (i * minimapTileSize), startY + (j * minimapTileSize), MINIMAP_TILE_Z);
                minimapNode.attachChild(tile);
                minimapTiles.add(tile);
            }
        }

        Quad pQ = new Quad(minimapTileSize * 0.72f, minimapTileSize * 0.72f);
        minimapPlayerMarker = new Geometry("PlayerMarker", pQ);
        Material pMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        pMat.setColor("Color", new ColorRGBA(0.97f, 0.97f, 0.98f, 0.95f));
        minimapPlayerMarker.setMaterial(pMat);
        minimapPlayerMarker.setLocalTranslation(startX + (MINIMAP_RADIUS * minimapTileSize) + (minimapTileSize * 0.14f), startY + (MINIMAP_RADIUS * minimapTileSize) + (minimapTileSize * 0.14f), PLAYER_MARKER_Z);
        minimapNode.attachChild(minimapPlayerMarker);

        Quad fQ = new Quad(minimapTileSize * 0.42f, minimapTileSize * 0.42f);
        minimapFacingMarker = new Geometry("FacingMarker", fQ);
        Material fMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        fMat.setColor("Color", new ColorRGBA(1f, 0.9f, 0.15f, 1f));
        fMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        minimapFacingMarker.setMaterial(fMat);
        minimapFacingMarker.setLocalTranslation(startX + (MINIMAP_RADIUS * minimapTileSize) + (minimapTileSize * 0.29f), startY + (MINIMAP_RADIUS * minimapTileSize) + (minimapTileSize * 0.29f), FACING_MARKER_Z);
        minimapNode.attachChild(minimapFacingMarker);
    }

    public void updateMinimap() {
        if (minimapNode.getCullHint() == Spatial.CullHint.Always) return;

        Vector3f pPos = player.getPhysicsLocation();
        int pGridX = Math.round(pPos.x / Map.TILE_SIZE);
        int pGridY = Math.round(pPos.z / Map.TILE_SIZE);

        int exitX = Math.round(map.getExitGatePosition().x / Map.TILE_SIZE);
        int exitY = Math.round(map.getExitGatePosition().z / Map.TILE_SIZE);

        List<Vector2f> activeCoins = new ArrayList<>();
        for (Spatial child : map.getCoinNode().getChildren()) {
            Vector3f cPos = child.getLocalTranslation();
            activeCoins.add(new Vector2f(Math.round(cPos.x / Map.TILE_SIZE), Math.round(cPos.z / Map.TILE_SIZE)));
        }

        int stride = getMinimapStride();
        int originX = clampVisibleOrigin(pGridX, stride, map.getGridWidth());
        int originY = clampVisibleOrigin(pGridY, stride, map.getGridHeight());

        for (int i = 0; i < stride; i++) {
            for (int j = 0; j < stride; j++) {
                int displayX = stride - 1 - i;
                int worldX = originX + i;
                int worldY = originY + j;

                Geometry tile = minimapTiles.get(displayX * stride + j);
                Material mat = tile.getMaterial();

                if (worldX == exitX && worldY == exitY) {
                    mat.setColor("Color", new ColorRGBA(0.15f, 0.9f, 0.3f, 1f));
                } else {
                    final int wx = worldX;
                    final int wy = worldY;
                    boolean hasCoin = activeCoins.stream().anyMatch(c -> (int) c.x == wx && (int) c.y == wy);

                    if (hasCoin) {
                        mat.setColor("Color", new ColorRGBA(0.35f, 0.9f, 1f, 1f));
                    } else {
                        byte cell = map.getWorldGrid()[worldX][worldY];
                        mat.setColor("Color", cell == 1 ? new ColorRGBA(0.3f, 0.3f, 0.35f, 0.95f) : new ColorRGBA(0.12f, 0.12f, 0.14f, 1f));
                    }
                }
            }
        }

        float playerTileX = clampToGrid((stride - 1) - (pGridX - originX), stride);
        float playerTileY = clampToGrid(pGridY - originY, stride);

        float playerMarkerX = getMinimapOriginX() + (playerTileX * minimapTileSize) + (minimapTileSize * 0.14f);
        float playerMarkerY = getMinimapOriginY() + (playerTileY * minimapTileSize) + (minimapTileSize * 0.14f);
        minimapPlayerMarker.setLocalTranslation(playerMarkerX, playerMarkerY, PLAYER_MARKER_Z);

        Vector3f dir = app.getCamera().getDirection();
        float angle = FastMath.atan2(dir.z, -dir.x);

        float facingOffset = minimapTileSize * 0.55f;
        float facingX = playerMarkerX + (FastMath.cos(angle) * facingOffset);
        float facingY = playerMarkerY + (FastMath.sin(angle) * facingOffset);

        minimapFacingMarker.setLocalTranslation(facingX, facingY, FACING_MARKER_Z);

        minimapFacingMarker.getMaterial().setColor("Color", new ColorRGBA(1f, 0.9f, 0.15f, 1f));
    }

    private int clampVisibleOrigin(int playerGrid, int stride, int worldSize) {
        int maxOrigin = Math.max(0, worldSize - stride);
        return Math.max(0, Math.min(playerGrid - MINIMAP_RADIUS, maxOrigin));
    }

    private float clampToGrid(int value, int stride) {
        return Math.max(0, Math.min(value, stride - 1));
    }

    private float getMinimapOriginX() {
        return app.getCamera().getWidth() - minimapDiameter - MINIMAP_MARGIN;
    }

    private float getMinimapOriginY() {
        return app.getCamera().getHeight() - minimapDiameter - MINIMAP_MARGIN;
    }

    private void refreshMinimapMetrics() {
        float availableSize = Math.min(app.getCamera().getWidth(), app.getCamera().getHeight()) * MINIMAP_SCREEN_RATIO;
        minimapDiameter = availableSize;
        minimapTileSize = minimapDiameter / getMinimapStride();
    }

    private int getMinimapStride() {
        return MINIMAP_RADIUS * 2 + 1;
    }

    public void cleanup() {
        app.getGuiNode().detachChild(minimapNode);
        app.getGuiNode().detachChild(hudShadowText);
        app.getGuiNode().detachChild(hudText);
        app.getGuiNode().detachChild(winShadowText);
        if (winText.getParent() != null) app.getGuiNode().detachChild(winText);
    }
}