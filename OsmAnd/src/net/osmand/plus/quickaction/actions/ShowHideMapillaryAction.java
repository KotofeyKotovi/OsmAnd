package net.osmand.plus.quickaction.actions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapillary.MapillaryPlugin;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.settings.backend.OsmandSettings;

public class ShowHideMapillaryAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(33,
			"mapillary.showhide", ShowHideMapillaryAction.class)
			.nameRes(R.string.quick_action_showhide_mapillary_title)
			.iconRes(R.drawable.ic_action_mapillary).nonEditable()
			.category(QuickActionType.CONFIGURE_MAP);

	public ShowHideMapillaryAction() {
		super(TYPE);
	}

	public ShowHideMapillaryAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(MapActivity activity) {
		OsmandApplication app = activity.getMyApplication();
		OsmandSettings settings = app.getSettings();
		boolean enabled = settings.SHOW_MAPILLARY.get();
		settings.SHOW_MAPILLARY.set(!enabled);
		MapillaryPlugin mapillaryPlugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
		if (mapillaryPlugin != null) {
			mapillaryPlugin.updateLayers(activity.getMapView(), activity);
		}
	}

	@Override
	public void drawUI(ViewGroup parent, MapActivity activity) {

		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_with_text, parent, false);

		((TextView) view.findViewById(R.id.text)).setText(
				R.string.quick_action_showhide_mapillary_descr);

		parent.addView(view);
	}

	@Override
	public String getActionText(OsmandApplication application) {

		return application.getSettings().SHOW_MAPILLARY.get()
				? application.getString(R.string.quick_action_mapillary_hide)
				: application.getString(R.string.quick_action_mapillary_show);
	}

	@Override
	public boolean isActionWithSlash(OsmandApplication application) {
		return application.getSettings().SHOW_MAPILLARY.get();
	}
}
