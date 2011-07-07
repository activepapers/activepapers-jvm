from org.jfree.data.xy import XYSeries, XYSeriesCollection
from org.jfree.chart import ChartFactory, ChartFrame
from org.jfree.chart.plot import PlotOrientation

def plot(title, x_label, y_label, *curves):
    dataset = XYSeriesCollection()
    for legend, curve in curves:
        series = XYSeries(legend)
        for x, y in curve:
            series.add(x, y)
        dataset.addSeries(series)
    chart = ChartFactory.createXYLineChart(title, x_label, y_label,
                                           dataset,
                                           PlotOrientation.VERTICAL,
                                           True, True, False)
    frame = ChartFrame(title, chart)
    frame.setVisible(True)
    frame.setSize(400, 300)
