package dev.iancheung.world;

import com.jme3.app.SimpleApplication;
import com.jme3.system.AppSettings;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class Main extends SimpleApplication {

    private static final String APP_NAME = "Mission Control";

    public static void main(String[] args) {
        Main app = new Main();
        AppSettings settings = new AppSettings(true);
        Rectangle usableBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        settings.setTitle(APP_NAME);
        settings.setResizable(true);
        settings.setCenterWindow(false);
        settings.setWindowSize(usableBounds.width, usableBounds.height);
        settings.setFrameRate(60);
        settings.setSamples(4);
        settings.setGammaCorrection(true);
        if (!(System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("mac"))) {
            settings.setIcons(loadAppIcons());
        }
        app.setSettings(settings);
        app.start();
    }

    private static BufferedImage[] loadAppIcons() {
        try {
            BufferedImage logo = ImageIO.read(Objects.requireNonNull(Main.class.getResourceAsStream("/logo.png")));
            return new BufferedImage[]{logo};
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Failed to load application icon from /logo.png", exception);
        }
    }

    @Override
    public void simpleInitApp() {
        setDisplayStatView(false);
        setDisplayFps(false);
        stateManager.attach(new Game());
    }
}