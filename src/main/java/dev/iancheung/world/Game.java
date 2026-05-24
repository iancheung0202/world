package dev.iancheung.world;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Game extends BaseAppState {

    private SimpleApplication app;
    private BulletAppState bulletAppState;

    public enum GameState { PLAYING, LEVEL_COMPLETE }
    private GameState currentState = GameState.PLAYING;

    private Map map;
    private Player player;
    private Hud hud;

    private int score = 0;
    private int currentLevel = 1;
    private float levelCompleteTimer = 5.0f;
    private float flagWobbleTimer = 0f;

    private final Node gameplayRoot = new Node("GameplayRoot");
    private PointLight playerLight;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.app.getRootNode().attachChild(gameplayRoot);

        this.bulletAppState = new BulletAppState();
        this.app.getStateManager().attach(bulletAppState);

        this.map = new Map(this.app, bulletAppState, gameplayRoot);
        this.player = new Player(this.app, bulletAppState);
        this.hud = new Hud(this.app, map, player);

        setupAtmosphere();
        startNewLevel();
    }

    private void setupAtmosphere() {
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.2f, 0.2f, 0.3f, 1.0f));
        gameplayRoot.addLight(ambient);

        playerLight = new PointLight();
        playerLight.setColor(ColorRGBA.White.mult(1.5f));
        playerLight.setRadius(25f);
        gameplayRoot.addLight(playerLight);
    }

    public void startNewLevel() {
        score = 0;
        levelCompleteTimer = 5.0f;
        currentState = GameState.PLAYING;

        map.generateLevel(currentLevel);
        player.spawnPlayer(map.getSpawnLocation(), map.getSpawnFacingTarget());
        hud.resetHud(currentLevel, map.getTotalCoins());
    }

    @Override
    public void update(float tpf) {
        if (currentState == GameState.PLAYING) {
            player.updateMovement(tpf);
            playerLight.setPosition(player.getPhysicsLocation());

            animateAssets(tpf);
            checkCollisions();
            hud.updateMinimap();
        } else if (currentState == GameState.LEVEL_COMPLETE) {
            animateLevelCompleteState(tpf);
        }
    }

    private void animateAssets(float tpf) {
        flagWobbleTimer += tpf * 3.5f;
        float wobbleOffset = FastMath.sin(flagWobbleTimer) * 0.35f;
        Vector3f gatePos = map.getExitGatePosition();
        map.getExitFlagNode().setLocalTranslation(gatePos.x, gatePos.y + wobbleOffset, gatePos.z);
        map.getExitFlagNode().rotate(0, 2.0f * tpf, 0);

        for (Spatial coin : map.getCoinNode().getChildren()) {
            coin.rotate(0, 2.5f * tpf, 0);
        }
    }

    private void checkCollisions() {
        Vector3f playerPos = player.getPhysicsLocation();

        int collected = map.checkCoinPickups(playerPos);
        if (collected > 0) {
            score += collected;
            hud.updateScore(score, map.getTotalCoins());
        }

        float distanceToExit = playerPos.distance(map.getExitGatePosition());
        if (distanceToExit < 3.8f) {
            if (score >= map.getTotalCoins()) {
                currentState = GameState.LEVEL_COMPLETE;
                player.freezePlayer();
            } else {
                hud.showWarning("GATE LOCKED! Collect all coins first! (" + score + "/" + map.getTotalCoins() + ")");
            }
        } else {
            hud.clearWarning();
        }
    }

    private void animateLevelCompleteState(float tpf) {
        flagWobbleTimer += tpf * 3.5f;
        Vector3f gatePos = map.getExitGatePosition();
        map.getExitFlagNode().setLocalTranslation(gatePos.x, gatePos.y + (FastMath.sin(flagWobbleTimer) * 0.35f), gatePos.z);
        map.getExitFlagNode().rotate(0, 2.0f * tpf, 0);

        levelCompleteTimer -= tpf;
        int displaySeconds = Math.max(0, (int) Math.ceil(levelCompleteTimer));
        hud.showWinScreen(displaySeconds);

        if (levelCompleteTimer <= 0f) {
            hud.hideWinScreen();
            currentLevel++;
            startNewLevel();
        }
    }

    @Override
    protected void cleanup(Application app) {
        this.app.getRootNode().detachChild(gameplayRoot);
        this.app.getStateManager().detach(bulletAppState);
        hud.cleanup();
    }

    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}