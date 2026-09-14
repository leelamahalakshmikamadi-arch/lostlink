package com.lost.link.lost.link_backend.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImageFeatureService {

    private static final int IMAGE_SIZE = 32;

    public List<Double> extract(MultipartFile image) {
        try {
            BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
            if (bufferedImage == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is not a readable image");
            }
            return extract(bufferedImage);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded image could not be read", exception);
        }
    }

    public List<Double> extract(BufferedImage source) {
        BufferedImage resized = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(source, 0, 0, IMAGE_SIZE, IMAGE_SIZE, null);
        graphics.dispose();

        List<Double> features = new ArrayList<>(IMAGE_SIZE * IMAGE_SIZE + 24);
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                Color color = new Color(resized.getRGB(x, y));
                features.add((color.getRed() + color.getGreen() + color.getBlue()) / (3.0 * 255.0));
            }
        }

        double[] histogram = new double[24];
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                Color color = new Color(resized.getRGB(x, y));
                histogram[color.getRed() / 64]++;
                histogram[8 + color.getGreen() / 64]++;
                histogram[16 + color.getBlue() / 64]++;
            }
        }
        for (double bin : histogram) {
            features.add(bin / (IMAGE_SIZE * IMAGE_SIZE));
        }
        return features;
    }

    public double similarity(List<Double> first, List<Double> second) {
        if (first == null || second == null || first.size() != second.size()) {
            return 0;
        }
        double distance = 0;
        for (int index = 0; index < first.size(); index++) {
            double difference = first.get(index) - second.get(index);
            distance += difference * difference;
        }
        return Math.max(0, 1 - Math.sqrt(distance / first.size()) * 2);
    }
}