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

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.OsmAndListFragment;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.myplaces.TrackBitmapDrawer.TrackBitmapDrawerListener;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;
import net.osmand.plus.track.TrackDisplayHelper;
import net.osmand.plus.widgets.IconPopupMenu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackSegmentFragment extends OsmAndListFragment implements TrackBitmapDrawerListener, OnUpdateContentListener {

	private GpxDisplayItemType[] filterTypes;

	private OsmandApplication app;
	private TrackActivityFragmentAdapter fragmentAdapter;
	private SegmentGPXAdapter adapter;
	private TrackDisplayHelper displayHelper;

	private boolean updateEnable;

	private IconPopupMenu generalPopupMenu;
	private IconPopupMenu altitudePopupMenu;

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

		filterTypes = new GpxDisplayItemType[1];
		filterTypes[0] = GpxDisplayItemType.TRACK_SEGMENT;

		fragmentAdapter = new TrackActivityFragmentAdapter(app, this, listView, displayHelper, filterTypes);
		fragmentAdapter.setShowMapOnly(false);
		fragmentAdapter.setTrackBitmapSelectionSupported(true);
		fragmentAdapter.setShowDescriptionCard(false);
		fragmentAdapter.onCreateView(view);

		adapter = new SegmentGPXAdapter(inflater.getContext(), new ArrayList<GpxDisplayItem>(), displayHelper, requireTrackActivity(), fragmentAdapter, filterTypes, this);
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
		GPXFile gpxFile = getGpx();
		if (gpxFile != null) {
			if (gpxFile.path != null && !gpxFile.showCurrentTrack) {
				Drawable shareIcon = app.getUIUtilities().getIcon((R.drawable.ic_action_gshare_dark));
				MenuItem item = menu.add(R.string.shared_string_share)
						.setIcon(AndroidUtils.getDrawableForDirection(app, shareIcon))
						.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(MenuItem item) {
								GPXFile gpx = getGpx();
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

	@Nullable
	private GPXFile getGpx() {
		return displayHelper.getGpx();
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

	@Override
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

	@Override
	public void generalPopupMenu(IconPopupMenu menu) {
		generalPopupMenu = menu;
	}

	@Override
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

	public enum GPXTabItemType {
		GPX_TAB_ITEM_GENERAL,
		GPX_TAB_ITEM_ALTITUDE,
		GPX_TAB_ITEM_SPEED
	}

	public static void saveGpx(final SelectedGpxFile selectedGpxFile, GPXFile gpxFile,
							   final ActionBarProgressActivity activity, final OsmandApplication app,
							   final TrackDisplayHelper displayHelper, final GpxDisplayItemType[] filterTypes,
							   final OnUpdateContentListener listener) {
		new SaveGpxAsyncTask(new File(gpxFile.path), gpxFile, new SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {
				if (activity != null && AndroidUtils.isActivityNotDestroyed(activity)) {
					activity.setSupportProgressBarIndeterminateVisibility(true);
				}
			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				if (activity != null) {
					if (selectedGpxFile != null) {
						selectedGpxFile.setDisplayGroups(displayHelper.getDisplayGroups(filterTypes), app);
						selectedGpxFile.processPoints(app);
					}
					if (listener != null) {
						listener.updateContent();
					}
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						activity.setSupportProgressBarIndeterminateVisibility(false);
					}
				}
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}