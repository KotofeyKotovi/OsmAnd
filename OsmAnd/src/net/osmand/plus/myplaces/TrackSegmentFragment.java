package net.osmand.plus.myplaces;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXDataSetType;
import net.osmand.plus.helpers.GpxUiHelper.OrderedLineDataSet;
import net.osmand.plus.myplaces.TrackBitmapDrawer.TrackBitmapDrawerListener;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.track.TrackDisplayHelper;
import net.osmand.plus.widgets.IconPopupMenu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackSegmentFragment extends OsmAndListFragment implements TrackBitmapDrawerListener, SegmentActionsListener {

	private OsmandApplication app;
	private TrackDisplayHelper displayHelper;

	private SegmentGPXAdapter adapter;
	private TrackActivityFragmentAdapter fragmentAdapter;

	private IconPopupMenu generalPopupMenu;
	private IconPopupMenu altitudePopupMenu;
	private GpxDisplayItemType[] filterTypes = new GpxDisplayItemType[] {GpxDisplayItemType.TRACK_SEGMENT};

	private boolean updateEnable;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.app = getMyApplication();
		this.displayHelper = requireTrackActivity().getDisplayHelper();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (fragmentAdapter != null) {
			fragmentAdapter.onActivityCreated(savedInstanceState);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.track_segments_tree, container, false);
		ListView listView = view.findViewById(android.R.id.list);
		listView.setDivider(null);
		listView.setDividerHeight(0);

		fragmentAdapter = new TrackActivityFragmentAdapter(app, this, listView, displayHelper, filterTypes);
		fragmentAdapter.setShowMapOnly(false);
		fragmentAdapter.setTrackBitmapSelectionSupported(true);
		fragmentAdapter.setShowDescriptionCard(false);
		fragmentAdapter.onCreateView(view);

		adapter = new SegmentGPXAdapter(inflater.getContext(), new ArrayList<GpxDisplayItem>(), displayHelper, this);
		setListAdapter(adapter);

		return view;
	}

	@Nullable
	public TrackActivity getTrackActivity() {
		return (TrackActivity) getActivity();
	}

	@NonNull
	public TrackActivity requireTrackActivity() {
		FragmentActivity activity = getActivity();
		if (!(activity instanceof TrackActivity)) {
			throw new IllegalStateException("Fragment " + this + " not attached to an activity.");
		}
		return (TrackActivity) activity;
	}

	public ArrayAdapter<?> getAdapter() {
		return adapter;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
		menu.clear();
		GPXFile gpxFile = displayHelper.getGpx();
		if (gpxFile != null) {
			if (gpxFile.path != null && !gpxFile.showCurrentTrack) {
				Drawable shareIcon = app.getUIUtilities().getIcon((R.drawable.ic_action_gshare_dark));
				MenuItem item = menu.add(R.string.shared_string_share)
						.setIcon(AndroidUtils.getDrawableForDirection(app, shareIcon))
						.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								GPXFile gpx = displayHelper.getGpx();
								FragmentActivity activity = getActivity();
								if (activity != null && gpx != null) {
									GpxUiHelper.shareGpx(activity, new File(gpx.path));
								}
								return true;
							}
						});
				item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
			if (gpxFile.showCurrentTrack) {
				MenuItem item = menu.add(R.string.shared_string_refresh).setIcon(R.drawable.ic_action_refresh_dark)
						.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								if (isUpdateEnable()) {
									updateContent();
									adapter.notifyDataSetChanged();
								}
								return true;
							}
						});
				item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		setUpdateEnable(true);
		updateContent();
	}

	@Override
	public void onPause() {
		super.onPause();
		setUpdateEnable(false);
		if (generalPopupMenu != null) {
			generalPopupMenu.dismiss();
		}
		if (altitudePopupMenu != null) {
			altitudePopupMenu.dismiss();
		}
		if (fragmentAdapter != null) {
			if (fragmentAdapter.splitListPopupWindow != null) {
				fragmentAdapter.splitListPopupWindow.dismiss();
			}
			if (fragmentAdapter.colorListPopupWindow != null) {
				fragmentAdapter.colorListPopupWindow.dismiss();
			}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		fragmentAdapter = null;
	}

	public boolean isUpdateEnable() {
		return updateEnable;
	}

	public void setUpdateEnable(boolean updateEnable) {
		this.updateEnable = updateEnable;
		if (fragmentAdapter != null) {
			fragmentAdapter.setUpdateEnable(updateEnable);
		}
	}

	public void updateContent() {
		adapter.clear();
		adapter.setNotifyOnChange(false);
		for (GpxDisplayItem displayItem : TrackDisplayHelper.flatten(getOriginalGroups())) {
			adapter.add(displayItem);
		}
		adapter.notifyDataSetChanged();
		if (getActivity() != null) {
			updateHeader();
		}
	}

	public void generalPopupMenu(IconPopupMenu menu) {
		generalPopupMenu = menu;
	}

	public void altitudePopupMenu(IconPopupMenu menu) {
		altitudePopupMenu = menu;
	}

	public void updateHeader() {
		if (fragmentAdapter != null) {
			fragmentAdapter.updateHeader(adapter.getCount());
		}
	}

	public void updateSplitView() {
		if (fragmentAdapter != null) {
			fragmentAdapter.updateSplitView();
		}
	}

	@NonNull
	private List<GpxDisplayGroup> getOriginalGroups() {
		return displayHelper.getOriginalGroups(filterTypes);
	}

	@NonNull
	private List<GpxDisplayGroup> getDisplayGroups() {
		return displayHelper.getDisplayGroups(filterTypes);
	}

	@Override
	public void onTrackBitmapDrawing() {
		if (fragmentAdapter != null) {
			fragmentAdapter.onTrackBitmapDrawing();
		}
	}

	@Override
	public void onTrackBitmapDrawn() {
		if (fragmentAdapter != null) {
			fragmentAdapter.onTrackBitmapDrawn();
		}
	}

	@Override
	public boolean isTrackBitmapSelectionSupported() {
		return fragmentAdapter != null && fragmentAdapter.isTrackBitmapSelectionSupported();
	}

	@Override
	public void drawTrackBitmap(Bitmap bitmap) {
		if (fragmentAdapter != null) {
			fragmentAdapter.drawTrackBitmap(bitmap);
		}
	}

	private void saveGpx(final SelectedGpxFile selectedGpxFile, GPXFile gpxFile) {
		new SaveGpxAsyncTask(new File(gpxFile.path), gpxFile, new SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {
				TrackActivity activity = getTrackActivity();
				if (activity != null && AndroidUtils.isActivityNotDestroyed(activity)) {
					activity.setSupportProgressBarIndeterminateVisibility(true);
				}
			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				TrackActivity activity = getTrackActivity();
				if (activity != null) {
					if (selectedGpxFile != null) {
						selectedGpxFile.setDisplayGroups(getDisplayGroups(), app);
						selectedGpxFile.processPoints(app);
					}
					updateContent();
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						activity.setSupportProgressBarIndeterminateVisibility(false);
					}
				}
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@Override
	public void editSegment() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			displayHelper.addNewGpxData(activity);
		}
	}

	@Override
	public void openAnalyzeOnMap(GpxDisplayItem gpxItem, List<ILineDataSet> dataSets, GPXTabItemType tabType) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			LatLon location = null;
			WptPt wpt = null;
			gpxItem.chartTypes = null;
			if (dataSets != null && dataSets.size() > 0) {
				gpxItem.chartTypes = new GPXDataSetType[dataSets.size()];
				for (int i = 0; i < dataSets.size(); i++) {
					OrderedLineDataSet orderedDataSet = (OrderedLineDataSet) dataSets.get(i);
					gpxItem.chartTypes[i] = orderedDataSet.getDataSetType();
				}
				if (gpxItem.chartHighlightPos != -1) {
					TrkSegment segment = null;
					for (Track t : gpxItem.group.getGpx().tracks) {
						for (TrkSegment s : t.segments) {
							if (s.points.size() > 0 && s.points.get(0).equals(gpxItem.analysis.locationStart)) {
								segment = s;
								break;
							}
						}
						if (segment != null) {
							break;
						}
					}
					if (segment != null) {
						OrderedLineDataSet dataSet = (OrderedLineDataSet) dataSets.get(0);
						float distance = gpxItem.chartHighlightPos * dataSet.getDivX();
						for (WptPt p : segment.points) {
							if (p.distance >= distance) {
								wpt = p;
								break;
							}
						}
						if (wpt != null) {
							location = new LatLon(wpt.lat, wpt.lon);
						}
					}
				}
			}
			if (location == null) {
				location = new LatLon(gpxItem.locationStart.lat, gpxItem.locationStart.lon);
			}
			if (wpt != null) {
				gpxItem.locationOnMap = wpt;
			} else {
				gpxItem.locationOnMap = gpxItem.locationStart;
			}

			OsmandSettings settings = app.getSettings();
			settings.setMapLocationToShow(location.getLatitude(), location.getLongitude(),
					settings.getLastKnownMapZoom(),
					new PointDescription(PointDescription.POINT_TYPE_WPT, gpxItem.name),
					false,
					gpxItem);

			MapActivity.launchMapActivityMoveToTop(activity);
		}
	}

	@Override
	public void openSplitInterval(GpxDisplayItem gpxItem, TrkSegment trkSegment, boolean joinSegments) {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SplitSegmentDialogFragment.showInstance(activity.getSupportFragmentManager(), gpxItem,
					trkSegment, displayHelper.isJoinSegments());
		}
	}

	@Override
	public void deleteAndSaveSegment(TrkSegment segment) {
		if (deleteSegment(segment)) {
			GPXFile gpx = displayHelper.getGpx();
			if (gpx != null) {
				boolean showOnMap = TrackActivityFragmentAdapter.isGpxFileSelected(app, gpx);
				SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpx, showOnMap, false);
				saveGpx(showOnMap ? selectedGpxFile : null, gpx);
			}
		}
	}

	@Override
	public void onPointSelected(double lat, double lon) {
		if (fragmentAdapter != null) {
			fragmentAdapter.updateSelectedPoint(lat, lon);
		}
	}

	@Override
	public void onChartTouch() {
		getListView().requestDisallowInterceptTouchEvent(true);
	}

	@Override
	public void scrollBy(int px) {
		ListView listView = getListView();
		listView.setSelectionFromTop(listView.getFirstVisiblePosition(), listView.getChildAt(0).getTop() - px);
	}

	private boolean deleteSegment(TrkSegment segment) {
		if (segment != null) {
			GPXFile gpx = displayHelper.getGpx();
			if (gpx != null) {
				return gpx.removeTrkSegment(segment);
			}
		}
		return false;
	}
}