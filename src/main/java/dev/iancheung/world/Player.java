package dev.iancheung.world;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class Player implements ActionListener {

    private final SimpleApplication app;
    private final CharacterControl physicsControl;

    private boolean inputEnabled = true;
    private boolean left = false, right = false, up = false, down = false, jump = false;
    private final Vector3f walkDirection = new Vector3f();

    private float bobTimer = 0f;
    private static final float BOB_SPEED = 14f;
    private static final float BOB_AMOUNT = 0.05f;
    private static final float EYE_HEIGHT = 1.8f;

    public Player(SimpleApplication app, BulletAppState bulletAppState) {
        this.app = app;

        app.getFlyByCamera().setEnabled(true);
        app.getFlyByCamera().setMoveSpeed(0);
        app.getFlyByCamera().setRotationSpeed(2f);

        float aspect = (float) app.getCamera().getWidth() / app.getCamera().getHeight();
        app.getCamera().setFrustumPerspective(65f, aspect, 0.1f, 1000f);

        physicsControl = new CharacterControl(new CapsuleCollisionShape(0.6f, 1.8f, 1), 0.3f);
        physicsControl.setGravity(45f);
        physicsControl.setJumpSpeed(16f);
        bulletAppState.getPhysicsSpace().add(physicsControl);

        registerInputs();
    }

    private void registerInputs() {
        app.getInputManager().addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        app.getInputManager().addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        app.getInputManager().addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
        app.getInputManager().addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
        app.getInputManager().addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));

        app.getInputManager().addListener(this, "Left", "Right", "Up", "Down", "Jump");
    }

    public void spawnPlayer(Vector3f location, Vector3f lookTarget) {
        physicsControl.setPhysicsLocation(location);
        physicsControl.setWalkDirection(Vector3f.ZERO);
        app.getCamera().setLocation(new Vector3f(location.x, location.y + EYE_HEIGHT, location.z));
        app.getCamera().lookAt(new Vector3f(lookTarget.x, location.y + EYE_HEIGHT, lookTarget.z), Vector3f.UNIT_Y);
        clearInputs();
    }

    public void freezePlayer() {
        physicsControl.setWalkDirection(Vector3f.ZERO);
        clearInputs();
    }

    public void setInputEnabled(boolean inputEnabled) {
        this.inputEnabled = inputEnabled;
        if (!inputEnabled) {
            clearInputs();
        }
    }

    private void clearInputs() {
        left = right = up = down = jump = false;
    }

    public void updateMovement(float tpf) {
        Vector3f camDir = app.getCamera().getDirection().clone().setY(0).normalizeLocal();
        Vector3f camLeft = app.getCamera().getLeft().clone().setY(0).normalizeLocal();

        Vector3f targetDir = new Vector3f();
        if (up) targetDir.addLocal(camDir);
        if (down) targetDir.subtractLocal(camDir);
        if (left) targetDir.addLocal(camLeft);
        if (right) targetDir.subtractLocal(camLeft);
        targetDir.normalizeLocal();

        float interpolationFactor = targetDir.lengthSquared() == 0 ? 12f : 8f;
        walkDirection.interpolateLocal(targetDir.mult(0.15f), interpolationFactor * tpf);
        physicsControl.setWalkDirection(walkDirection);

        if (jump && physicsControl.onGround()) {
            physicsControl.jump();
        }

        float currentEyeHeight = EYE_HEIGHT;
        if (targetDir.lengthSquared() > 0 && physicsControl.onGround()) {
            bobTimer += tpf * BOB_SPEED;
            currentEyeHeight += FastMath.sin(bobTimer) * BOB_AMOUNT;
        } else {
            bobTimer = 0;
        }
        Vector3f pPos = physicsControl.getPhysicsLocation();
        app.getCamera().setLocation(new Vector3f(pPos.x, pPos.y + currentEyeHeight, pPos.z));
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!inputEnabled) {
            return;
        }

        if (name.equals("Left")) left = isPressed;
        if (name.equals("Right")) right = isPressed;
        if (name.equals("Up")) up = isPressed;
        if (name.equals("Down")) down = isPressed;
        if (name.equals("Jump")) jump = isPressed;
    }

    public Vector3f getPhysicsLocation() { return physicsControl.getPhysicsLocation(); }
}