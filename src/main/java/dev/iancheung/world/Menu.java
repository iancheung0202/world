package dev.iancheung.world;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;

import java.util.ArrayList;
import java.util.List;

public class Menu extends BaseAppState implements ActionListener {

    private final Game game;
    private SimpleApplication app;
    private boolean hasPlayed;

    private final Node menuRoot = new Node("MenuRoot");
    private final List<MenuOption> options = new ArrayList<>();

    private BitmapText titleText;
    private BitmapText subtitleText;
    private BitmapText statusText;
    private BitmapText hintText;

    private static final ColorRGBA COL_BACKDROP = new ColorRGBA(0.03f, 0.03f, 0.07f, 0.93f);
    private static final ColorRGBA COL_TITLE = new ColorRGBA(1.0f,  0.95f, 0.70f, 1.0f);
    private static final ColorRGBA COL_SUBTITLE = new ColorRGBA(0.75f, 0.75f, 0.85f, 0.80f);
    private static final ColorRGBA COL_BTN_NORMAL = new ColorRGBA(0.10f, 0.11f, 0.16f, 0.90f);
    private static final ColorRGBA COL_BTN_HOVER = new ColorRGBA(0.20f, 0.22f, 0.35f, 0.96f);
    private static final ColorRGBA COL_BTN_BORDER = new ColorRGBA(0.35f, 0.38f, 0.60f, 0.70f);
    private static final ColorRGBA COL_LABEL_NORMAL = new ColorRGBA(0.90f, 0.90f, 0.95f, 1.0f);
    private static final ColorRGBA COL_LABEL_HOVER = new ColorRGBA(1.0f,  1.0f,  1.0f,  1.0f);
    private static final ColorRGBA COL_STATUS = new ColorRGBA(1.0f,  0.72f, 0.35f, 1.0f);
    private static final ColorRGBA COL_HINT = new ColorRGBA(0.60f, 0.60f, 0.68f, 0.75f);

    private static final float TEXT_SCALE = 1.30f;
    private static final float TITLE_SCALE = 2.20f;
    private static final float HINT_SCALE = 1.05f;

    public Menu(Game game, boolean hasPlayed) {
        this.game = game;
        this.hasPlayed = hasPlayed;
    }

    @Override
    protected void initialize(Application app) {
        this.app = (SimpleApplication) app;
        buildMenu();
    }

    @Override
    protected void cleanup(Application app) {
        this.app.getGuiNode().detachChild(menuRoot);
        unregisterInputs();
    }

    @Override
    protected void onEnable() {
        app.getGuiNode().attachChild(menuRoot);
        registerInputs();
        app.getInputManager().setCursorVisible(true);
    }

    @Override
    protected void onDisable() {
        app.getGuiNode().detachChild(menuRoot);
        unregisterInputs();
    }

    private void buildMenu() {
        menuRoot.detachAllChildren();
        options.clear();

        float sw = app.getCamera().getWidth();
        float sh = app.getCamera().getHeight();
        float minDim = Math.min(sw, sh);
        float uiScale = minDim / 900f;

        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        // NOTE: For sharper text, replace the path above with a Hiero-generated
        // high-resolution bitmap font, e.g. "Interface/Fonts/HighRes.fnt".
        // Generate one from any TTF using jME SDK's built-in Hiero tool.

        float baseSize = font.getCharSet().getRenderedSize();
        float buttonWidth = clamp(sw * 0.34f, 300f, 520f);
        float buttonHeight = clamp(sh * 0.075f, 52f, 82f);
        float buttonGap = clamp(sh * 0.018f, 10f, 20f);
        float topMargin = clamp(sh * 0.10f, 54f, 112f);
        float titleTextSize = baseSize * clamp(TITLE_SCALE * uiScale, 1.65f, 2.85f);
        float subtitleTextSize = baseSize * clamp(HINT_SCALE * 0.95f * uiScale, 0.95f, 1.35f);
        float hintTextSize = baseSize * clamp(HINT_SCALE * 0.92f * uiScale, 0.92f, 1.25f);
        float buttonTextSize = baseSize * clamp(TEXT_SCALE * uiScale, 1.05f, 1.55f);
        float buttonBorder = clamp(buttonHeight * 0.04f, 2f, 3.5f);

        Geometry backdrop = new Geometry("MenuBackdrop", new Quad(sw, sh));
        Material backdropMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        backdropMat.setColor("Color", COL_BACKDROP);
        backdropMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        backdrop.setMaterial(backdropMat);
        backdrop.setLocalTranslation(0, 0, 0);
        menuRoot.attachChild(backdrop);

        titleText = new BitmapText(font, false);
        titleText.setSize(titleTextSize);
        titleText.setColor(COL_TITLE);
        titleText.setText("MISSION CONTROL");
        float titleX = (sw - titleText.getLineWidth()) / 2f;
        float titleY = sh - topMargin;
        titleText.setLocalTranslation(titleX, titleY, 2f);
        menuRoot.attachChild(titleText);

        BitmapText titleShadow = new BitmapText(font, false);
        titleShadow.setSize(titleTextSize);
        titleShadow.setColor(new ColorRGBA(0f, 0f, 0f, 0.6f));
        titleShadow.setText("MISSION CONTROL");
        titleShadow.setLocalTranslation(titleX + 2.5f, titleY - 2.5f, 1f);
        menuRoot.attachChild(titleShadow);

        subtitleText = new BitmapText(font, false);
        subtitleText.setSize(subtitleTextSize);
        subtitleText.setColor(COL_SUBTITLE);
        subtitleText.setText("Collect every coin. Find the exit.");
        float subX = (sw - subtitleText.getLineWidth()) / 2f;
        float subtitleY = titleY - titleText.getLineHeight() - clamp(sh * 0.022f, 14f, 26f);
        subtitleText.setLocalTranslation(subX, subtitleY, 2f);
        menuRoot.attachChild(subtitleText);

        Geometry divider = new Geometry("Divider", new Quad(buttonWidth, 1.5f));
        Material divMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        divMat.setColor("Color", new ColorRGBA(0.4f, 0.4f, 0.6f, 0.5f));
        divMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        divider.setMaterial(divMat);
        float divX = (sw - buttonWidth) / 2f;
        float dividerY = subtitleY - subtitleText.getLineHeight() - clamp(sh * 0.018f, 10f, 20f);
        divider.setLocalTranslation(divX, dividerY, 1.5f);
        menuRoot.attachChild(divider);

        statusText = new BitmapText(font, false);
        statusText.setSize(hintTextSize);
        statusText.setColor(COL_STATUS);
        statusText.setText("");
        statusText.setLocalTranslation(divX, dividerY - statusText.getLineHeight() - 8f, 2f);
        menuRoot.attachChild(statusText);

        float centerX = (sw - buttonWidth) / 2f;
        float totalH = 3 * buttonHeight + 2 * buttonGap;
        float startY = (sh - totalH) / 2f + 2f * (buttonHeight + buttonGap);

        String primaryLabel = hasPlayed ? "Resume" : "Play";
        addButton(font, buttonTextSize, primaryLabel, "MenuPrimary", centerX, startY, buttonWidth, buttonHeight, buttonBorder);
        addButton(font, buttonTextSize, "Settings", "MenuSettings", centerX, startY - (buttonHeight + buttonGap), buttonWidth, buttonHeight, buttonBorder);
        addButton(font, buttonTextSize, "Quit",     "MenuQuit",     centerX, startY - (buttonHeight + buttonGap) * 2f, buttonWidth, buttonHeight, buttonBorder);

        hintText = new BitmapText(font, false);
        hintText.setSize(hintTextSize);
        hintText.setColor(COL_HINT);
        hintText.setText((hasPlayed ? "R  Resume" : "P  Play") + "  S  Settings  Q  Quit  —  or click");
        float hintX = (sw - hintText.getLineWidth()) / 2f;
        hintText.setLocalTranslation(hintX, clamp(sh * 0.045f, 24f, 42f), 2f);
        menuRoot.attachChild(hintText);
    }

    /**
     * Adds a button with a border frame, fill quad, and centered label.
     * Stores button geometry + label in the option list for hover highlighting.
     */
    private void addButton(BitmapFont font, float textSize, String label, String action, float x, float y,
                           float buttonWidth, float buttonHeight, float borderThickness) {
        Geometry border = new Geometry(action + "Border", new Quad(buttonWidth, buttonHeight));
        Material borderMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        borderMat.setColor("Color", COL_BTN_BORDER);
        borderMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        border.setMaterial(borderMat);
        border.setLocalTranslation(x, y, 1f);
        menuRoot.attachChild(border);

        float fillW = buttonWidth  - borderThickness * 2f;
        float fillH = buttonHeight - borderThickness * 2f;
        Geometry fill = new Geometry(action + "Fill", new Quad(fillW, fillH));
        Material fillMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        fillMat.setColor("Color", COL_BTN_NORMAL);
        fillMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        fill.setMaterial(fillMat);
        fill.setLocalTranslation(x + borderThickness, y + borderThickness, 1.5f);
        menuRoot.attachChild(fill);

        BitmapText text = new BitmapText(font, false);
        text.setSize(textSize);
        text.setColor(COL_LABEL_NORMAL);
        text.setText(label);
        float labelX = x + (buttonWidth  - text.getLineWidth())  / 2f;
        float labelY = y + (buttonHeight + text.getLineHeight())  / 2f - 4f;
        text.setLocalTranslation(labelX, labelY, 2f);
        menuRoot.attachChild(text);

        options.add(new MenuOption(action, x, y, buttonWidth, buttonHeight, fill, text));
    }

    private void registerInputs() {
        app.getInputManager().addMapping("MenuPrimary",  new KeyTrigger(com.jme3.input.KeyInput.KEY_P));
        app.getInputManager().addMapping("MenuPrimary",  new KeyTrigger(com.jme3.input.KeyInput.KEY_R));
        app.getInputManager().addMapping("MenuSettings", new KeyTrigger(com.jme3.input.KeyInput.KEY_S));
        app.getInputManager().addMapping("MenuQuit",     new KeyTrigger(com.jme3.input.KeyInput.KEY_Q));
        app.getInputManager().addMapping("MenuClick",    new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(this, "MenuPrimary", "MenuSettings", "MenuQuit", "MenuClick");
    }

    private void unregisterInputs() {
        app.getInputManager().deleteMapping("MenuPrimary");
        app.getInputManager().deleteMapping("MenuSettings");
        app.getInputManager().deleteMapping("MenuQuit");
        app.getInputManager().deleteMapping("MenuClick");
        app.getInputManager().removeListener(this);
    }

    @Override
    public void update(float tpf) {
        Vector2f cursor = app.getInputManager().getCursorPosition();
        for (MenuOption opt : options) {
            boolean hovered = opt.contains(cursor.x, cursor.y);
            opt.fill.getMaterial().setColor("Color", hovered ? COL_BTN_HOVER   : COL_BTN_NORMAL);
            opt.label.setColor(hovered ? COL_LABEL_HOVER : COL_LABEL_NORMAL);
        }
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) {
            return;
        }

        switch (name) {
            case "MenuPrimary" -> closeMenu();
            case "MenuSettings" -> {
                statusText.setText("Settings coming soon.");
                statusText.setLocalTranslation(
                        (app.getCamera().getWidth() - statusText.getLineWidth()) / 2f,
                        statusText.getLocalTranslation().y,
                        statusText.getLocalTranslation().z);
            }
            case "MenuQuit"  -> app.stop();
            case "MenuClick" -> handleMouseClick();
            default -> { /* ignored */ }
        }
    }

    private void handleMouseClick() {
        Vector2f cursor = app.getInputManager().getCursorPosition();
        for (MenuOption opt : options) {
            if (opt.contains(cursor.x, cursor.y)) {
                onAction(opt.action, true, 0f);
                return;
            }
        }
    }

    private void closeMenu() {
        statusText.setText("");
        game.closeMenu();
    }

    public void setHasPlayed(boolean hasPlayed) {
        this.hasPlayed = hasPlayed;
        if (app != null) {
            refreshMenu();
        }
    }

    public void onResize() {
        if (app != null) {
            refreshMenu();
        }
    }

    private void refreshMenu() {
        boolean enabled = isEnabled();
        unregisterInputs();
        buildMenu();
        if (enabled) {
            registerInputs();
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    private static final class MenuOption {
        final String action;
        final float x, y, width, height;
        final Geometry fill;
        final BitmapText label;

        MenuOption(String action, float x, float y, float width, float height, Geometry fill, BitmapText label) {
            this.action = action;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.fill = fill;
            this.label = label;
        }

        boolean contains(float cx, float cy) {
            return cx >= x && cx <= x + width && cy >= y && cy <= y + height;
        }
    }
}