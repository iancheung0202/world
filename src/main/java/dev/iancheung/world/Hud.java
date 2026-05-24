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

    private final Node hudRoot = new Node("HudRoot");
    private final Node minimapNode = new Node("MinimapPanel");

    private BitmapFont font;
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
    private boolean winScreenVisible = false;

    private static final int MINIMAP_RADIUS = 12;
    private static final float MINIMAP_RATIO = 0.26f;
    private static final float MINIMAP_MARGIN_RATIO = 0.022f;
    private static final float SHADOW_OFFSET = 2.0f;
    private static final float MINIMAP_TILE_Z = 1.0f;
    private static final float PLAYER_MARKER_Z = 2.0f;
    private static final float FACING_MARKER_Z = 3.0f;

    private float minimapTileSize;
    private float minimapDiameter;
    private float minimapMargin;

    public Hud(SimpleApplication app, Map map, Player player) {
        this.app = app;
        this.map = map;
        this.player = player;

        font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");

        app.getGuiNode().attachChild(hudRoot);
        hudRoot.attachChild(minimapNode);

        buildTextElements();
        computeMinimapMetrics();
    }

    private float scaleFactor() { return Math.min(app.getCamera().getWidth(), app.getCamera().getHeight()) / 720f; }
    private float hudMargin() { return clamp(18f * scaleFactor(), 16f, 34f); }
    private float hudFontSize() { return font.getCharSet().getRenderedSize() * 1.45f * scaleFactor(); }
    private float winFontSize() { return font.getCharSet().getRenderedSize() * 2.10f * scaleFactor(); }

    private void buildTextElements() {
        if (hudShadowText != null) { hudRoot.detachChild(hudShadowText); }
        if (hudText != null) { hudRoot.detachChild(hudText); }
        if (winShadowText != null) { hudRoot.detachChild(winShadowText); }
        if (winText != null) { hudRoot.detachChild(winText); }

        float m = hudMargin();
        float sh = app.getCamera().getHeight();

        hudShadowText = makeBitmapText(hudFontSize(), new ColorRGBA(0f, 0f, 0f, 0.80f), m + SHADOW_OFFSET, sh - m - SHADOW_OFFSET);
        hudRoot.attachChild(hudShadowText);

        hudText = makeBitmapText(hudFontSize(), ColorRGBA.White, m, sh - m);
        hudRoot.attachChild(hudText);

        winShadowText = makeBitmapText(winFontSize(), new ColorRGBA(0f, 0f, 0f, 0.80f), 0, 0);
        winShadowText.setCullHint(Spatial.CullHint.Always);
        hudRoot.attachChild(winShadowText);

        winText = makeBitmapText(winFontSize(), new ColorRGBA(1f, 0.95f, 0.65f, 1f), 0, 0);
        winText.setCullHint(Spatial.CullHint.Always);
        hudRoot.attachChild(winText);
    }

    private BitmapText makeBitmapText(float size, ColorRGBA color, float x, float y) {
        BitmapText t = new BitmapText(font, false);
        t.setSize(size);
        t.setColor(color);
        t.setLocalTranslation(x, y, 0);
        return t;
    }

    public void setVisible(boolean visible) { hudRoot.setCullHint(visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always); }

    /** Called by Game whenever the window dimensions change. */
    public void onResize() {
        buildTextElements();
        updateScore(lastScore, lastTotalCoins);
        computeMinimapMetrics();
        rebuildMinimapUI();
    }

    public void resetHud(int level, int totalCoins) {
        currentLevel = level;
        lastScore = 0;
        lastTotalCoins = totalCoins;
        updateScore(0, totalCoins);
        hudText.setColor(ColorRGBA.White);
        hudShadowText.setColor(new ColorRGBA(0f, 0f, 0f, 0.80f));
        rebuildMinimapUI();
        setVisible(true);
    }

    public void updateScore(int current, int total) {
        lastScore = current;
        lastTotalCoins = total;
        hudText.setColor(ColorRGBA.White);
        hudShadowText.setColor(new ColorRGBA(0f, 0f, 0f, 0.80f));
        String text = "Coins Collected: " + current + " / " + total + " (Level " + currentLevel + ")";
        hudText.setText(text);
        hudShadowText.setText(text);
        syncShadow(hudText, hudShadowText);
    }

    public void showWarning(String warning) {
        hudText.setColor(new ColorRGBA(1f, 0.30f, 0.20f, 1f));
        hudShadowText.setColor(new ColorRGBA(0f, 0f, 0f, 0.80f));
        hudText.setText(warning);
        hudShadowText.setText(warning);
        syncShadow(hudText, hudShadowText);
    }

    public void clearWarning() {
        updateScore(lastScore, lastTotalCoins);
    }

    public void showWinScreen(int countdown) {
        minimapNode.setCullHint(Spatial.CullHint.Always);

        String text = "ZONE CLEARED!\nNext level in " + countdown + "s";
        winText.setText(text);
        winShadowText.setText(text);

        float sw = app.getCamera().getWidth();
        float sh = app.getCamera().getHeight();
        float x = (sw - winText.getLineWidth()) / 2f;
        float y = (sh / 2f) + winText.getLineHeight();
        winText.setLocalTranslation(x, y, 0);
        winShadowText.setLocalTranslation(x + SHADOW_OFFSET, y - SHADOW_OFFSET, 0);

        if (!winScreenVisible) {
            winText.setCullHint(Spatial.CullHint.Inherit);
            winShadowText.setCullHint(Spatial.CullHint.Inherit);
            winScreenVisible = true;
        }
    }

    public void hideWinScreen() {
        winText.setCullHint(Spatial.CullHint.Always);
        winShadowText.setCullHint(Spatial.CullHint.Always);
        winScreenVisible = false;
        minimapNode.setCullHint(Spatial.CullHint.Inherit);
    }

    private void computeMinimapMetrics() {
        float minDim = Math.min(app.getCamera().getWidth(), app.getCamera().getHeight());
        minimapDiameter = minDim * MINIMAP_RATIO;
        minimapTileSize = minimapDiameter / getMinimapStride();
        minimapMargin = clamp(minDim * MINIMAP_MARGIN_RATIO * 1.1f, 16f, 42f);
    }

    private void rebuildMinimapUI() {
        minimapNode.detachAllChildren();
        minimapTiles.clear();

        float startX = minimapOriginX();
        float startY = minimapOriginY();
        float pad = minimapTileSize * 0.4f;

        Geometry bg = makeColoredQuad("MinimapBG",
                minimapDiameter + pad * 2f, minimapDiameter + pad * 2f,
                new ColorRGBA(0.03f, 0.03f, 0.06f, 0.70f),
                startX - pad, startY - pad, 0f);
        minimapNode.attachChild(bg);

        int stride = getMinimapStride();
        for (int i = 0; i < stride; i++) {
            for (int j = 0; j < stride; j++) {
                Geometry tile = makeColoredQuad("MapTile_" + i + "_" + j,
                        minimapTileSize, minimapTileSize,
                        new ColorRGBA(0.12f, 0.12f, 0.14f, 1f),
                        startX + i * minimapTileSize,
                        startY + j * minimapTileSize,
                        MINIMAP_TILE_Z);
                minimapNode.attachChild(tile);
                minimapTiles.add(tile);
            }
        }

        float pm = minimapTileSize * 0.72f;
        minimapPlayerMarker = makeColoredQuad("PlayerMarker", pm, pm,
                new ColorRGBA(0.97f, 0.97f, 0.98f, 0.95f),
                startX + MINIMAP_RADIUS * minimapTileSize + (minimapTileSize - pm) / 2f,
                startY + MINIMAP_RADIUS * minimapTileSize + (minimapTileSize - pm) / 2f,
                PLAYER_MARKER_Z);
        minimapNode.attachChild(minimapPlayerMarker);

        float fm = minimapTileSize * 0.42f;
        minimapFacingMarker = makeColoredQuad("FacingMarker", fm, fm,
                new ColorRGBA(1f, 0.9f, 0.15f, 1f),
                startX + MINIMAP_RADIUS * minimapTileSize + (minimapTileSize - fm) / 2f,
                startY + MINIMAP_RADIUS * minimapTileSize + (minimapTileSize - fm) / 2f,
                FACING_MARKER_Z);
        minimapFacingMarker.getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
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
            activeCoins.add(new Vector2f(
                    Math.round(cPos.x / Map.TILE_SIZE),
                    Math.round(cPos.z / Map.TILE_SIZE)));
        }

        int stride = getMinimapStride();
        int originX = clampOrigin(pGridX, stride, map.getGridWidth());
        int originY = clampOrigin(pGridY, stride, map.getGridHeight());

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
                    final int wx = worldX, wy = worldY;
                    boolean hasCoin = activeCoins.stream().anyMatch(c -> (int) c.x == wx && (int) c.y == wy);
                    if (hasCoin) {
                        mat.setColor("Color", new ColorRGBA(0.35f, 0.9f, 1f, 1f));
                    } else {
                        byte cell = map.getWorldGrid()[worldX][worldY];
                        mat.setColor("Color", cell == 1
                                ? new ColorRGBA(0.3f, 0.3f, 0.35f, 0.95f)
                                : new ColorRGBA(0.12f, 0.12f, 0.14f, 1f));
                    }
                }
            }
        }

        float playerTileX = clampGrid((stride - 1) - (pGridX - originX), stride);
        float playerTileY = clampGrid(pGridY - originY, stride);
        float ox = minimapOriginX();
        float oy = minimapOriginY();

        float pm = minimapTileSize * 0.72f;
        float markerX = ox + playerTileX * minimapTileSize + (minimapTileSize - pm) / 2f;
        float markerY = oy + playerTileY * minimapTileSize + (minimapTileSize - pm) / 2f;
        minimapPlayerMarker.setLocalTranslation(markerX, markerY, PLAYER_MARKER_Z);

        Vector3f dir = app.getCamera().getDirection();
        float angle = FastMath.atan2(dir.z, -dir.x);
        float facingDist = minimapTileSize * 0.58f;
        float fm = minimapTileSize * 0.42f;
        float facingX = markerX + pm / 2f - fm / 2f + FastMath.cos(angle) * facingDist;
        float facingY = markerY + pm / 2f - fm / 2f + FastMath.sin(angle) * facingDist;
        minimapFacingMarker.setLocalTranslation(facingX, facingY, FACING_MARKER_Z);
    }

    private void syncShadow(BitmapText src, BitmapText shadow) {
        Vector3f p = src.getLocalTranslation();
        shadow.setLocalTranslation(p.x + SHADOW_OFFSET, p.y - SHADOW_OFFSET, 0);
    }

    private Geometry makeColoredQuad(String name, float w, float h, ColorRGBA color, float x, float y, float z) {
        Geometry g = new Geometry(name, new Quad(w, h));
        Material m = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        m.setColor("Color", color);
        m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        g.setMaterial(m);
        g.setLocalTranslation(x, y, z);
        return g;
    }

    private int  getMinimapStride() { return MINIMAP_RADIUS * 2 + 1; }
    private float minimapOriginX() { return app.getCamera().getWidth()  - minimapDiameter - minimapMargin; }
    private float minimapOriginY() { return app.getCamera().getHeight() - minimapDiameter - minimapMargin; }
    private int  clampOrigin(int v, int stride, int worldSz) { return Math.max(0, Math.min(v - MINIMAP_RADIUS, worldSz - stride)); }
    private float clampGrid(int v, int stride) { return Math.max(0, Math.min(v, stride - 1)); }
    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(value, max)); }

    public void cleanup() {
        app.getGuiNode().detachChild(hudRoot);
    }
}