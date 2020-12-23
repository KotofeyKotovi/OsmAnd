package net.osmand.plus.myplaces;

import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;

import java.util.List;

interface SegmentActionsListener {

	void editSegment();

	void openAnalyzeOnMap(GpxDisplayItem gpxItem, List<ILineDataSet> dataSets, GPXTabItemType tabType);

	void openSplitInterval(GpxDisplayItem gpxItem, TrkSegment trkSegment, boolean joinSegments);

	void deleteAndSaveSegment(TrkSegment segment);

	void onPointSelected(double lat, double lon);

	void onChartTouch();

	void scrollBy(int px);
}
