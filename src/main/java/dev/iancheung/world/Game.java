package dev.iancheung.world;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class Game extends BaseAppState implements ActionListener {

    private SimpleApplication app;
    private BulletAppState bulletAppState;

    public enum GameState { PLAYING, LEVEL_COMPLETE, MENU }
    private GameState currentState = GameState.PLAYING;

    private Map map;
    private Player player;
    private Hud hud;
    private Menu menu;

    private int score = 0;
    private int currentLevel = 1;
    private float levelCompleteTimer = 5.0f;
    private float flagWobbleTimer = 0f;
    private boolean hasPlayed = false;

    private int lastResW = -1;
    private int lastResH = -1;

    private final Node gameplayRoot = new Node("GameplayRoot");
    private PointLight playerLight;

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        this.app.getRootNode().attachChild(gameplayRoot);

        this.app.getInputManager().deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
        this.app.getInputManager().addMapping("OpenMenu", new KeyTrigger(KeyInput.KEY_ESCAPE));
        this.app.getInputManager().addListener(this, "OpenMenu");

        this.bulletAppState = new BulletAppState();
        this.app.getStateManager().attach(bulletAppState);

        this.map = new Map(this.app, bulletAppState, gameplayRoot);
        this.player = new Player(this.app, bulletAppState);
        this.hud = new Hud(this.app, map, player);

        setupAtmosphere();
        startNewLevel();
        openMenu();
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
        if (menu != null && menu.isEnabled()) {
            menu.setEnabled(false);
        }
        currentState = GameState.PLAYING;
        player.setInputEnabled(true);
        app.getInputManager().setCursorVisible(false);
        app.getFlyByCamera().setEnabled(true);

        map.generateLevel(currentLevel);
        player.spawnPlayer(map.getSpawnLocation(), map.getSpawnFacingTarget());
        hud.resetHud(currentLevel, map.getTotalCoins());
        hud.setVisible(true);
    }

    public void openMenu() {
        if (currentState == GameState.MENU) {
            return;
        }

        currentState = GameState.MENU;
        player.freezePlayer();
        player.setInputEnabled(false);
        app.getFlyByCamera().setEnabled(false);
        app.getInputManager().setCursorVisible(true);
        hud.setVisible(false);

        if (menu == null) {
            menu = new Menu(this, hasPlayed);
            app.getStateManager().attach(menu);
        } else {
            menu.setHasPlayed(hasPlayed);
            menu.setEnabled(true);
        }
    }

    public void closeMenu() {
        if (currentState != GameState.MENU) {
            return;
        }

        hasPlayed = true;
        currentState = GameState.PLAYING;
        player.setInputEnabled(true);
        app.getInputManager().setCursorVisible(false);
        app.getFlyByCamera().setEnabled(true);
        hud.setVisible(true);

        if (menu != null) {
            menu.setEnabled(false);
        }
    }

    @Override
    public void update(float tpf) {
        int w = app.getCamera().getWidth();
        int h = app.getCamera().getHeight();
        if (w != lastResW || h != lastResH) {
            lastResW = w;
            lastResH = h;
            hud.onResize();
            if (menu != null && menu.isEnabled()) {
                menu.onResize();
            }
        }

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

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed || !("OpenMenu".equals(name))) {
            return;
        }

        if (currentState == GameState.PLAYING) {
            openMenu();
        } else if (currentState == GameState.MENU) {
            closeMenu();
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
                hud.showWarning("Gate Locked! Collect all coins first (" + score + " / " + map.getTotalCoins() + ")");
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