/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.urugn.openvx;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

/**
 *
 * @author UruGN
 */
public class VxCanvas extends Canvas {

    GraphicsContext g2d;
    private Image _img;
    double width = 0;
    double height = 0;

    public VxCanvas() {
        g2d = getGraphicsContext2D();
    }

    void clear() {
        g2d.clearRect(0, 0, 0, 0);
        g2d.fill();

    }

    void update(Image _img) {
        this._img = _img;
        clear();

        if (_img != null) {
            width = _img.getWidth();
            height = _img.getHeight();
            setWidth(width);
            setHeight(height);

            g2d.drawImage(_img, 0, 0);

        }
    }
    
    ObservableNumberValue getFitWidth() {
        final DoubleProperty w = new SimpleDoubleProperty(null, "width", width);
        return new DoubleBinding() {

            {
                super.bind(w);
            }

            @Override
            protected double computeValue() {
                return w.get();//220 to cover the leftpane width
            }

        };
    }

    ObservableNumberValue getFitHeight() {
        final DoubleProperty h = new SimpleDoubleProperty(null, "height", height);
        return new DoubleBinding() {

            {
                super.bind(h);
            }

            @Override
            protected double computeValue() {
                return h.get();
            }

        };
    }
}
