package io.github.giantnuker.fabric.informedload;

import java.awt.*;

public class SplashThemeConfig {
    public String background = "239,50,61";
    public String progressbarOutside = "255,255,255";
    public String progressbarInside = "255,255,255";
    public String progressbarBackground = "239,50,61";
    public String progressTextColor = "0,0,0";
    public String textColor = "255,255,255";

    public static int colorString(String color) {
        String[] rgb = color.split(",");
        if (rgb.length != 3) {
            throw new IllegalArgumentException(color + " is not a valid color string. Too many values - format is '255,255,255'");
        } else {
            int[] rgb2 = new int[3];
            for (int i = 0; i < rgb.length; i++) {
                rgb2[i] = Integer.valueOf(rgb[i]);
            }
            return new Color(rgb2[0], rgb2[1], rgb2[2]).getRGB();
        }
    }

    public transient int ibackground = colorString(background);
    public transient int iprogressbarOutside = colorString(progressbarOutside);
    public transient int iprogressbarInside = colorString(progressbarInside);
    public transient int iprogressbarBackground = colorString(progressbarBackground);
    public transient int iprogressTextColor = colorString(progressTextColor);
    public transient int itextColor = colorString(textColor);
}
