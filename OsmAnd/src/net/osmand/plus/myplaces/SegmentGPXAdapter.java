package net.osmand.plus.myplaces;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.track.TrackDisplayHelper;
import net.osmand.plus.views.controls.PagerSlidingTabStrip;
import net.osmand.plus.views.controls.WrapContentHeightViewPager;

import java.util.List;

class SegmentGPXAdapter extends ArrayAdapter<GpxDisplayItem> {

	private OsmandApplication app;
	private TrackDisplayHelper displayHelper;
	private SegmentActionsListener segmentActionsListener;

	SegmentGPXAdapter(@NonNull Context context, @NonNull List<GpxDisplayItem> items,
					  @NonNull TrackDisplayHelper displayHelper,
					  @NonNull SegmentActionsListener segmentActionsListener) {
		super(context, R.layout.gpx_list_item_tab_content, items);
		this.app = (OsmandApplication) context.getApplicationContext();
		this.displayHelper = displayHelper;
		this.segmentActionsListener = segmentActionsListener;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@NonNull
	@Override
	public View getView(int position, View convertView, @NonNull ViewGroup parent) {
		View row = convertView;
		PagerSlidingTabStrip tabLayout;
		WrapContentHeightViewPager pager;
		boolean create = false;
		if (row == null) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			row = inflater.inflate(R.layout.gpx_list_item_tab_content, parent, false);

			boolean light = app.getSettings().isLightContent();
			tabLayout = row.findViewById(R.id.sliding_tabs);
			tabLayout.setTabBackground(R.color.color_transparent);
			tabLayout.setIndicatorColorResource(light ? R.color.active_color_primary_light : R.color.active_color_primary_dark);
			tabLayout.setIndicatorBgColorResource(light ? R.color.divider_color_light : R.color.divider_color_dark);
			tabLayout.setIndicatorHeight(AndroidUtils.dpToPx(app, 1f));
			if (light) {
				tabLayout.setTextColor(tabLayout.getIndicatorColor());
				tabLayout.setTabInactiveTextColor(ContextCompat.getColor(row.getContext(), R.color.text_color_secondary_light));
			}
			tabLayout.setTextSize(AndroidUtils.spToPx(app, 12f));
			tabLayout.setShouldExpand(true);
			pager = row.findViewById(R.id.pager);
			pager.setSwipeable(false);
			pager.setOffscreenPageLimit(2);
			create = true;
		} else {
			tabLayout = row.findViewById(R.id.sliding_tabs);
			pager = row.findViewById(R.id.pager);
		}
		GpxDisplayItem item = getItem(position);
		if (item != null) {
			pager.setAdapter(new GPXItemPagerAdapter(tabLayout, item, displayHelper, segmentActionsListener));
			if (create) {
				tabLayout.setViewPager(pager);
			} else {
				tabLayout.notifyDataSetChanged(true);
			}
		}
		return row;
	}
}
