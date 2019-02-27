/*
 * Copyright (c) 2008-2018, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package support;

import com.hazelcast.core.IList;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
import com.hazelcast.jet.datamodel.TimestampedItem;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.format.DateTimeFormatter;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

/**
 * Displays a live bar chart of each stock and its current trading volume
 * on the simulated stock exchange.
 */
public class TradingVolumeGui {
    private static final int WINDOW_X = 100;
    private static final int WINDOW_Y = 100;
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 650;
    private static final int INITIAL_TOP_Y = 5_000_000;
    private static final int TIME_RANGE = 60;
    private static final int Y_RANGE_UPPER_INITIAL = 3000;
    private static final double SCALE_Y = 1_000_000;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    private final IList<TimestampedItem<Long>> volumeList;
    private String itemListenerId;

    public TradingVolumeGui(IList<TimestampedItem<Long>> volumeList) {
        this.volumeList = volumeList;
        EventQueue.invokeLater(this::startGui);
    }

    private void startGui() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        CategoryPlot plot = createChartFrame(dataset);
        plot.getRangeAxis().setRange(0, Y_RANGE_UPPER_INITIAL);
        for (Long ts = 2L; ts <= 60; ts += 2) {
            dataset.addValue(0L, "", ts);
        }
        ItemAddedListener<TimestampedItem<Long>> itemListener = tsItem -> {
            Long x = tsItem.timestamp() / 1_000;
            double y = tsItem.item() / SCALE_Y;
            EventQueue.invokeLater(() -> dataset.addValue(y, "", x));
        };
        itemListenerId = volumeList.addItemListener(itemListener, true);
    }

    private CategoryPlot createChartFrame(CategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
                "Trading Volume Over Time", "Time", "Trading Volume, USD", dataset,
                PlotOrientation.VERTICAL, false, true, false);
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.DARK_GRAY);
        plot.getDomainAxis().setCategoryMargin(0.0);
        plot.setRangeGridlinePaint(Color.DARK_GRAY);
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setBarPainter(new StandardBarPainter());

        final JFrame frame = new JFrame();
        frame.setBackground(Color.WHITE);
        frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
        frame.setTitle("Hazelcast Jet Early Window Results Sample");
        frame.setBounds(WINDOW_X, WINDOW_Y, WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLayout(new BorderLayout());
        frame.add(new ChartPanel(chart));
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent windowEvent) {
                volumeList.removeItemListener(itemListenerId);
            }
        });
        frame.setVisible(true);
        return plot;
    }

    interface ItemAddedListener<T> extends ItemListener<T> {
        void item(T item);

        @Override default void itemRemoved(ItemEvent<T> event) {
        }
        @Override default void itemAdded(ItemEvent<T> event) {
            item(event.getItem());
        }
    }
}