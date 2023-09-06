package com.sg.collison.manager;

import com.sg.collison.data.Point;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class DrawPoints {
    public static void drawPoints(List<Point> points, String filename, int width, int height, int radius) {
        // Utwórz pusty obraz
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Utwórz kontekst graficzny
        Graphics2D g2d = bufferedImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Ustaw tło na biało
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Ustaw kolor na czerwony
        g2d.setColor(Color.BLACK);

        for (Point point : points) {
//            int x = (int) Math.round(point.getGlobalX());
//            int y = (int) Math.round(point.getGlobalY());
//            g2d.fillOval(x - radius/2, y-radius/2, radius, radius);

            Ellipse2D.Double circle = new Ellipse2D.Double(point.getGlobalX(), point.getGlobalY(), radius, radius);
            g2d.fill(circle);

        }

        // Zapisz obraz do pliku
        File file = new File(filename);
        try {
            ImageIO.write(bufferedImage, "jpg", file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Zamknij kontekst graficzny, zwalniając zasoby
        g2d.dispose();
    }
}
