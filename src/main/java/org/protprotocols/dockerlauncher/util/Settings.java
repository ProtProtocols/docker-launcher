package org.protprotocols.dockerlauncher.util;

public class Settings {
    private static String css;
    private static int sceneWidth;
    private static int sceneHeight;

    public static String getCss() {
        return css;
    }

    public static void setCss(String css) {
        Settings.css = css;
    }

    public static int getSceneWidth() {
        return sceneWidth;
    }

    public static void setSceneWidth(int sceneWidth) {
        Settings.sceneWidth = sceneWidth;
    }

    public static int getSceneHeight() {
        return sceneHeight;
    }

    public static void setSceneHeight(int sceneHeight) {
        Settings.sceneHeight = sceneHeight;
    }
}
