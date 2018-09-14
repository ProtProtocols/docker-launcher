package org.protprotocols.dockerlauncher.util;

public class Constants {

    public final static String FXML_DLG_LOAD_IMAGE = "fxml/dlg_load_image.fxml";
    public final static String FXML_DLG_IMAGE_SETTINGS = "fxml/dlg_image_settings.fxml";

    public final static String LOW_RES_CSS = "css/low_res.css";
    public final static String HIGH_RES_CSS = "css/high_res.css";

    public final static int LOW_RES_WIDTH = 600;
    public final static int LOW_RES_HEIGHT = 400;

    public final static int HIGH_RES_WIDTH = 1200;
    public final static int HIGH_RES_HEIGHT = 800;

    public final static double HIGH_DPI = 150;

    public final static String PROPERTY_IMAGE_NAME = "docker_image";
    public final static String PROPERTY_IMAGE_VERSIONS = "docker_image_releases";

    private Constants() {
        // this class should not be created
    }
}
