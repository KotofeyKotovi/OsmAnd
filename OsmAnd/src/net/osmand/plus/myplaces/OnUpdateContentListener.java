package net.osmand.plus.myplaces;

import android.widget.ListView;

import net.osmand.plus.widgets.IconPopupMenu;

public interface OnUpdateContentListener {
	void updateContent();

	void generalPopupMenu(IconPopupMenu menu);

	void altitudePopupMenu(IconPopupMenu menu);

	ListView getListView();
}
