package com.sg.collison.manager;

import com.sg.collison.data.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DrawPoints {

    private static String directoryName;
    @Value("${points.radius}")
    private double radius;
    @Value("${points.number}")
    private Integer numberOfPoints;
    @Value("${draw.compression.quality}")
    private float COMPRESSION_QUALITY;

    @Value("${draw.scale.factor}")
    private double SCALE_FACTOR;

    @Value("${draw.scale.point}")
    private double SCALE_POINT;
    // Cache for associating point IDs with colors
    private Map<Long, Color> idToColor = new ConcurrentHashMap<>();

    // Method to draw points on an image and save it as JPEG
    public void drawPoints(Set<Point> points, String filename, int width, int height, double radiusIn, int clusterSize, int sizeBiggestCluster, double dxMass, double dyMass) {
        int totalWidth = width + width / 4;
        double radius = radiusIn * SCALE_POINT;  // Scaling factor for bigger points

        if (directoryName == null) {
            directoryName = generateUniqueDirectoryName();
        }
        // Scaling factor for creating a higher resolution image

        // Create a high-resolution image for better quality
        BufferedImage highResImage = new BufferedImage((int) (totalWidth * SCALE_FACTOR), (int) (height * SCALE_FACTOR), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = highResImage.createGraphics();
        g2d.scale(SCALE_FACTOR, SCALE_FACTOR);  // Apply scaling factor to the graphics context

        // Setting rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Fill the background color
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.fillRect(width, 0, width / 4, height);

        for (Point point : points) {
            Color baseColor = idToColor.get(point.getId());
            if (baseColor == null) {
                // Generate a random color if not found in cache
                baseColor = new Color((int) (Math.random() * 160), (int) (Math.random() * 160), (int) (Math.random() * 160));
                idToColor.put(point.getId(), baseColor);
            }

            // Create a radial gradient for each point with multi-level transparency and shading
            float[] dist = {0.0f, 0.7f, 0.85f, 1.0f};

            // Color at the very center: darkened version of baseColor
            Color centerColor = new Color(baseColor.getRed() / 3, baseColor.getGreen() / 3, baseColor.getBlue() / 3, 255);

            // Color from center to almost edge: semi-transparent version of baseColor
            Color middleColor1 = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0);

            // Color at the edge: less transparent version of baseColor
            Color middleColor2 = new Color(baseColor.getRed() + 60, baseColor.getGreen() + 60, baseColor.getBlue() + 60, 64);

            // Color just outside the edge: more transparent to give a fading effect
            Color edgeColor = new Color(baseColor.getRed() + 60, baseColor.getGreen() + 60, baseColor.getBlue() + 60, 16);

            Color[] colors = {centerColor, middleColor1, middleColor2, edgeColor};

            Point2D center = new Point2D.Float((float) point.getGlobalX() + (float) radius / 2, (float) point.getGlobalY() + (float) radius / 2);

            RadialGradientPaint rgp = new RadialGradientPaint(center,
                    (float) radius,
                    center,
                    dist,
                    colors,
                    MultipleGradientPaint.CycleMethod.NO_CYCLE);


            g2d.setPaint(rgp);
            Ellipse2D.Double circle = new Ellipse2D.Double(point.getGlobalX(), point.getGlobalY(), radius, radius);
            g2d.fill(circle);  // Draw the circle
        }


        // Iterate through each point and draw it
//        for (Point point : points) {
//            Color baseColor = idToColor.get(point.getId());
//            if (baseColor == null) {
//                // Generate a random color if not found in cache
//                baseColor = new Color((int) (Math.random() * 192), (int) (Math.random() * 192), (int) (Math.random() * 192));
//                idToColor.put(point.getId(), baseColor);
//            }
//
//            // Create a radial gradient for each point with a darker center
//            float[] dist = {0.0f, 1.0f};
//            Color[] colors = {baseColor.darker(), baseColor};
//            RadialGradientPaint rgp = new RadialGradientPaint(
//                    new Point2D.Float((float) point.getGlobalX() + (float) radius / 2, (float) point.getGlobalY() + (float) radius / 2),
//                    (float) radius,
//                    dist,
//                    colors
//            );
//
//            g2d.setPaint(rgp);
//            Ellipse2D.Double circle = new Ellipse2D.Double(point.getGlobalX(), point.getGlobalY(), radius, radius);
//            g2d.fill(circle);  // Draw the circle
//        }

        // Add statistical texts to the image
        Font font = new Font("Arial", Font.BOLD, 16);
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);

        DecimalFormat df = new DecimalFormat("####.#");
        String[] lines = {
                "Clusters total   : " + clusterSize,
                "Biggest cluster size: " + sizeBiggestCluster,
                "Dx mass move: " + df.format(dxMass),
                "Dy mass move: " + df.format(dyMass)
        };

        // Position and draw each line of text
        int textX = width + 20;
        int textY = 30;
        int lineHeight = g2d.getFontMetrics().getHeight() + 5;
        for (String line : lines) {
            g2d.drawString(line, textX, textY);
            textY += lineHeight;
        }

        // Rescale the image back to the original resolution
        BufferedImage finalImage = new BufferedImage(totalWidth, height, BufferedImage.TYPE_INT_RGB);
        finalImage.getGraphics().drawImage(highResImage, 0, 0, totalWidth, height, null);

        // Save the image as a JPEG file with adjusted compression settings
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No writers found");
        }
        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(COMPRESSION_QUALITY);  // Set compression quality

        // Write the image to file
        try {
            File imageFile = new File(directoryName + "/" + filename);
            FileImageOutputStream output = new FileImageOutputStream(imageFile);
            writer.setOutput(output);
            IIOImage iioImage = new IIOImage(finalImage, null, null);
            writer.write(null, iioImage, param);
            writer.dispose();
            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Release graphic resources
        g2d.dispose();
    }

    private synchronized String generateUniqueDirectoryName() {
        String baseName = numberOfPoints + "_points_" + radius + "_radius";
        File directory = new File(baseName);
        int counter = 1;

        while (directory.exists()) {
            directory = new File(baseName + "_" + counter++);
        }

        if (!directory.mkdir()) {
            throw new RuntimeException("Failed to create directory");
        }

        return directory.getName();
    }


}
