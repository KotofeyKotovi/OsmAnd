package net.osmand.plus.track;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.data.QuadRect;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class TrackDisplayHelper {

	private final OsmandApplication app;

	private File file = null;
	private GPXFile gpxFile;
	private GpxDataItem gpxDataItem;

	private List<GpxDisplayGroup> displayGroups;
	private final List<GpxDisplayGroup> originalGroups = new ArrayList<>();

	private long modifiedTime = -1;


	public final List<String> options = new ArrayList<>();
	public final List<Double> distanceSplit = new ArrayList<>();
	public final TIntArrayList timeSplit = new TIntArrayList();
	public int selectedSplitInterval;

	public TrackDisplayHelper(OsmandApplication app) {
		this.app = app;
	}


	public void prepareSplitIntervalAdapterData(GpxDisplayItemType[] filterTypes) {
		final List<GpxDisplayGroup> groups = getDisplayGroups(filterTypes);

		options.add(app.getString(R.string.shared_string_none));
		distanceSplit.add(-1d);
		timeSplit.add(-1);
		addOptionSplit(30, true, groups); // 50 feet, 20 yards, 20
		// m
		addOptionSplit(60, true, groups); // 100 feet, 50 yards,
		// 50 m
		addOptionSplit(150, true, groups); // 200 feet, 100 yards,
		// 100 m
		addOptionSplit(300, true, groups); // 500 feet, 200 yards,
		// 200 m
		addOptionSplit(600, true, groups); // 1000 feet, 500 yards,
		// 500 m
		addOptionSplit(1500, true, groups); // 2000 feet, 1000 yards, 1 km
		addOptionSplit(3000, true, groups); // 1 mi, 2 km
		addOptionSplit(6000, true, groups); // 2 mi, 5 km
		addOptionSplit(15000, true, groups); // 5 mi, 10 km

		addOptionSplit(15, false, groups);
		addOptionSplit(30, false, groups);
		addOptionSplit(60, false, groups);
		addOptionSplit(120, false, groups);
		addOptionSplit(150, false, groups);
		addOptionSplit(300, false, groups);
		addOptionSplit(600, false, groups);
		addOptionSplit(900, false, groups);
		addOptionSplit(1800, false, groups);
		addOptionSplit(3600, false, groups);
	}


	private void addOptionSplit(int value, boolean distance, @NonNull List<GpxDisplayGroup> model) {
		if (model.size() > 0) {
			if (distance) {
				double dvalue = OsmAndFormatter.calculateRoundedDist(value, app);
				options.add(OsmAndFormatter.getFormattedDistanceInterval(app, value));
				distanceSplit.add(dvalue);
				timeSplit.add(-1);
				if (Math.abs(model.get(0).getSplitDistance() - dvalue) < 1) {
					selectedSplitInterval = distanceSplit.size() - 1;
				}
			} else {
				options.add(OsmAndFormatter.getFormattedTimeInterval(app, value));
				distanceSplit.add(-1d);
				timeSplit.add(value);
				if (model.get(0).getSplitTime() == value) {
					selectedSplitInterval = distanceSplit.size() - 1;
				}
			}
		}
	}

	@Nullable
	public GPXFile getGpx() {
		return gpxFile;
	}

	@Nullable
	public GpxDataItem getGpxDataItem() {
		return gpxDataItem;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public void setGpx(GPXFile result) {
		this.gpxFile = result;
		if (file == null) {
			this.gpxFile = app.getSavingTrackHelper().getCurrentGpx();
		}
	}

	public void setGpxDataItem(GpxDataItem gpxDataItem) {
		this.gpxDataItem = gpxDataItem;
	}

	public QuadRect getRect() {
		if (getGpx() != null) {
			return getGpx().getRect();
		} else {
			return new QuadRect(0, 0, 0, 0);
		}
	}

	public boolean setJoinSegments(boolean joinSegments) {
		if (gpxDataItem != null) {
			boolean updated = app.getGpxDbHelper().updateJoinSegments(gpxDataItem, joinSegments);

			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(gpxFile.path);
			if (updated && selectedGpxFile != null) {
				selectedGpxFile.setJoinSegments(joinSegments);
			}
			return updated;
		}
		return false;
	}

	public boolean isJoinSegments() {
		return gpxDataItem != null && gpxDataItem.isJoinSegments();
	}

	public List<GpxDisplayGroup> getGpxFile(boolean useDisplayGroups) {
		if (gpxFile == null) {
			return new ArrayList<>();
		}
		if (gpxFile.modifiedTime != modifiedTime) {
			modifiedTime = gpxFile.modifiedTime;
			GpxSelectionHelper selectedGpxHelper = app.getSelectedGpxHelper();
			displayGroups = selectedGpxHelper.collectDisplayGroups(gpxFile);
			originalGroups.clear();
			for (GpxSelectionHelper.GpxDisplayGroup g : displayGroups) {
				originalGroups.add(g.cloneInstance());
			}
			if (file != null) {
				SelectedGpxFile sf = selectedGpxHelper.getSelectedFileByPath(gpxFile.path);
				if (sf != null && file != null && sf.getDisplayGroups(app) != null) {
					displayGroups = sf.getDisplayGroups(app);
				}
			}
		}
		if (useDisplayGroups) {
			return displayGroups;
		} else {
			return originalGroups;
		}
	}

	@NonNull
	public List<GpxDisplayGroup> getOriginalGroups(GpxDisplayItemType[] filterTypes) {
		return filterGroups(false, filterTypes);
	}

	@NonNull
	public List<GpxDisplayGroup> getDisplayGroups(GpxDisplayItemType[] filterTypes) {
		return filterGroups(true, filterTypes);
	}

	private boolean hasFilterType(GpxDisplayItemType filterType, GpxDisplayItemType[] filterTypes) {
		for (GpxDisplayItemType type : filterTypes) {
			if (type == filterType) {
				return true;
			}
		}
		return false;
	}

	@NonNull
	private List<GpxDisplayGroup> filterGroups(boolean useDisplayGroups, GpxDisplayItemType[] filterTypes) {
		List<GpxDisplayGroup> groups = new ArrayList<>();
		for (GpxDisplayGroup group : getGpxFile(useDisplayGroups)) {
			if (hasFilterType(group.getType(), filterTypes)) {
				groups.add(group);
			}
		}
		return groups;
	}

	public static List<GpxDisplayItem> flatten(List<GpxDisplayGroup> groups) {
		ArrayList<GpxDisplayItem> list = new ArrayList<>();
		for (GpxDisplayGroup g : groups) {
			list.addAll(g.getModifiableList());
		}
		return list;
	}
}
