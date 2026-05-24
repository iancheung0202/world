package dev.iancheung.world;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Map {

    private final SimpleApplication app;
    private final BulletAppState bulletAppState;
    private final Node parentNode;

    private final Node terrainNode = new Node("Terrain");
    private final Node coinNode = new Node("Coins");
    private final Node exitFlagNode = new Node("ExitFlagNode");
    private final List<RigidBodyControl> physicalBodies = new ArrayList<>();

    private int gridWidth;
    private int gridHeight;
    public static final float TILE_SIZE = 4.0f;
    private byte[][] worldGrid;

    private final Vector3f spawnLocation = new Vector3f();
    private final Vector3f spawnFacingTarget = new Vector3f();
    private final Vector3f exitGatePosition = new Vector3f();
    private int totalCoins = 0;

    public Map(SimpleApplication app, BulletAppState bulletAppState, Node parentNode) {
        this.app = app;
        this.bulletAppState = bulletAppState;
        this.parentNode = parentNode;

        this.parentNode.attachChild(terrainNode);
        this.parentNode.attachChild(coinNode);
        this.parentNode.attachChild(exitFlagNode);
    }

    public void generateLevel(int level) {
        clearOldLevel();

        gridWidth = 20 + (level * 10);
        gridHeight = 20 + (level * 10);
        worldGrid = new byte[gridWidth][gridHeight];
        Random rand = new Random();

        List<ProceduralRoom> rooms = new ArrayList<>();
        int attempts = 15 + (level * 5);

        for (int i = 0; i < attempts; i++) {
            ProceduralRoom room = new ProceduralRoom(
                    rand.nextInt(gridWidth - 11) + 1,
                    rand.nextInt(gridHeight - 11) + 1,
                    rand.nextInt(4) + 5, rand.nextInt(4) + 5
            );
            if (rooms.stream().noneMatch(room::overlaps)) {
                room.carve(worldGrid);
                rooms.add(room);
            }
        }
        if (rooms.isEmpty()) {
            ProceduralRoom fallback = new ProceduralRoom(5, 5, 5, 5);
            fallback.carve(worldGrid);
            rooms.add(fallback);
        }

        for (int i = 0; i < rooms.size() - 1; i++) {
            ProceduralRoom c = rooms.get(i);
            ProceduralRoom u = rooms.get(i + 1);
            carveHallway(c.centerX(), c.centerY(), u.centerX(), u.centerY());
        }

        build3DEnvironment();

        ProceduralRoom startRoom = rooms.getFirst();
        Vector3f startCoinPos = new Vector3f(startRoom.centerX() * TILE_SIZE, 1.4f, startRoom.centerY() * TILE_SIZE);
        spawnLocation.set((startRoom.centerX() + 1) * TILE_SIZE, 2.0f, startRoom.centerY() * TILE_SIZE);
        spawnFacingTarget.set(startCoinPos.x, spawnLocation.y, startCoinPos.z);

        ProceduralRoom endRoom = rooms.getLast();
        exitGatePosition.set(endRoom.centerX() * TILE_SIZE, 2.5f, endRoom.centerY() * TILE_SIZE);
        buildGoalFlagAsset();

        totalCoins = 0;
        for (ProceduralRoom room : rooms) {
            Vector3f coinPos = new Vector3f(room.centerX() * TILE_SIZE, 1.4f, room.centerY() * TILE_SIZE);
            if (room.centerX() == endRoom.centerX() && room.centerY() == endRoom.centerY()) {
                coinPos.set((room.x + 1) * TILE_SIZE, 1.4f, (room.y + 1) * TILE_SIZE);
            }
            spawnCoin(coinPos);
            totalCoins++;
        }
    }

    private void clearOldLevel() {
        terrainNode.detachAllChildren();
        coinNode.detachAllChildren();
        exitFlagNode.detachAllChildren();
        physicalBodies.forEach(bulletAppState.getPhysicsSpace()::remove);
        physicalBodies.clear();
    }

    private void carveHallway(int x1, int y1, int x2, int y2) {
        int cx = x1;
        while (cx != x2) { worldGrid[cx][y1] = 1; cx += (x2 > x1) ? 1 : -1; }
        int cy = y1;
        while (cy != y2) { worldGrid[x2][cy] = 1; cy += (y2 > y1) ? 1 : -1; }
    }

    private void build3DEnvironment() {
        float hSize = TILE_SIZE / 2.0f;
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                if (worldGrid[x][y] == 1) {
                    Vector3f pos = new Vector3f(x * TILE_SIZE, 0f, y * TILE_SIZE);
                    createBlock("Floor", new Vector3f(pos.x, -0.2f, pos.z), new Vector3f(hSize, 0.2f, hSize), "Textures/floor.png");
                    checkAndBuildWall(x + 1, y, new Vector3f(pos.x + hSize, 2.0f, pos.z), new Vector3f(0.2f, 2.0f, hSize), "LeftRightWall");
                    checkAndBuildWall(x - 1, y, new Vector3f(pos.x - hSize, 2.0f, pos.z), new Vector3f(0.2f, 2.0f, hSize), "LeftRightWall");
                    checkAndBuildWall(x, y + 1, new Vector3f(pos.x, 2.0f, pos.z + hSize), new Vector3f(hSize, 2.0f, 0.2f), "FrontBackWall");
                    checkAndBuildWall(x, y - 1, new Vector3f(pos.x, 2.0f, pos.z - hSize), new Vector3f(hSize, 2.0f, 0.2f), "FrontBackWall");
                }
            }
        }
    }

    private void checkAndBuildWall(int tx, int ty, Vector3f pos, Vector3f extents, String wallType) {
        if (tx < 0 || tx >= gridWidth || ty < 0 || ty >= gridHeight || worldGrid[tx][ty] == 0) {
            createBlock(wallType, pos, extents, "Textures/wall.png");
        }
    }

    private void createBlock(String name, Vector3f pos, Vector3f extents, String texturePath) {
        Box box = new Box(extents.x, extents.y, extents.z);
        Geometry geo = new Geometry(name, box);

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
        try {
            Texture tex = app.getAssetManager().loadTexture(texturePath);
            tex.setWrap(Texture.WrapMode.Repeat);
            mat.setTexture("DiffuseMap", tex);
        } catch (Exception e) {
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", ColorRGBA.DarkGray);
        }

        geo.setMaterial(mat);
        geo.setLocalTranslation(pos);

        float scale = 2.0f;
        if (name.equalsIgnoreCase("Floor")) box.scaleTextureCoordinates(new Vector2f(extents.z / scale, extents.x / scale));
        else if (name.equalsIgnoreCase("LeftRightWall")) box.scaleTextureCoordinates(new Vector2f(extents.z / scale, extents.y / scale));
        else box.scaleTextureCoordinates(new Vector2f(extents.x / scale, extents.y / scale));

        terrainNode.attachChild(geo);

        RigidBodyControl rb = new RigidBodyControl(0.0f);
        geo.addControl(rb);
        bulletAppState.getPhysicsSpace().add(rb);
        physicalBodies.add(rb);
    }

    private void buildGoalFlagAsset() {
        float size = 3.5f;
        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            mat.setTexture("ColorMap", app.getAssetManager().loadTexture("Textures/flag.png"));
        } catch (Exception e) {
            mat.setColor("Color", ColorRGBA.Red);
        }
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

        Quad fQ = new Quad(size, size);
        Geometry fG = new Geometry("FrontFlag", fQ);
        fG.setMaterial(mat);
        fG.setQueueBucket(Bucket.Transparent);
        fG.setLocalTranslation(-size / 2f, -size / 2f, 0.005f);
        exitFlagNode.attachChild(fG);

        Quad bQ = new Quad(size, size);
        bQ.getBuffer(VertexBuffer.Type.TexCoord).updateData(BufferUtils.createFloatBuffer(new float[]{1, 0, 0, 0, 0, 1, 1, 1}));
        Geometry bG = new Geometry("BackFlag", bQ);
        bG.setMaterial(mat);
        bG.setQueueBucket(Bucket.Transparent);
        bG.rotate(0, FastMath.PI, 0);
        bG.setLocalTranslation(size / 2f, -size / 2f, -0.005f);
        exitFlagNode.attachChild(bG);

        exitFlagNode.setLocalTranslation(exitGatePosition);
    }

    private void spawnCoin(Vector3f localPos) {
        Node pivot = new Node("CoinPivot");
        float size = 1.1f;

        Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            mat.setTexture("ColorMap", app.getAssetManager().loadTexture("Textures/coin.png"));
        } catch (Exception e) {
            mat.setColor("Color", ColorRGBA.Yellow);
        }
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

        Quad fQ = new Quad(size, size);
        Geometry fG = new Geometry("CoinFront", fQ);
        fG.setMaterial(mat);
        fG.setQueueBucket(Bucket.Transparent);
        fG.setLocalTranslation(-size / 2f, -size / 2f, 0.005f);
        pivot.attachChild(fG);

        Quad bQ = new Quad(size, size);
        bQ.getBuffer(VertexBuffer.Type.TexCoord).updateData(BufferUtils.createFloatBuffer(new float[]{1, 0, 0, 0, 0, 1, 1, 1}));
        Geometry bG = new Geometry("CoinBack", bQ);
        bG.setMaterial(mat);
        bG.setQueueBucket(Bucket.Transparent);
        bG.rotate(0, FastMath.PI, 0);
        bG.setLocalTranslation(size / 2f, -size / 2f, -0.005f);
        pivot.attachChild(bG);

        pivot.setLocalTranslation(localPos);
        coinNode.attachChild(pivot);
    }

    public int checkCoinPickups(Vector3f playerPos) {
        List<Spatial> toRemove = new ArrayList<>();
        for (Spatial coin : coinNode.getChildren()) {
            if (playerPos.distance(coin.getLocalTranslation()) < 1.6f) {
                toRemove.add(coin);
            }
        }
        toRemove.forEach(Spatial::removeFromParent);
        return toRemove.size();
    }

    public Vector3f getSpawnLocation() { return spawnLocation; }
    public Vector3f getSpawnFacingTarget() { return spawnFacingTarget; }
    public Vector3f getExitGatePosition() { return exitGatePosition; }
    public Node getCoinNode() { return coinNode; }
    public Node getExitFlagNode() { return exitFlagNode; }
    public int getTotalCoins() { return totalCoins; }
    public int getGridWidth() { return gridWidth; }
    public int getGridHeight() { return gridHeight; }
    public byte[][] getWorldGrid() { return worldGrid; }

    private static class ProceduralRoom {
        int x, y, width, height;
        ProceduralRoom(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
        int centerX() { return x + (width / 2); }
        int centerY() { return y + (height / 2); }
        boolean overlaps(ProceduralRoom o) {
            return x < o.x + o.width && x + width > o.x && y < o.y + o.height && y + height > o.y;
        }
        void carve(byte[][] grid) {
            for (int dx = 0; dx < width; dx++) {
                for (int dy = 0; dy < height; dy++) {
                    grid[x + dx][y + dy] = 1;
                }
            }
        }
    }
}