package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.DialogPreference;

import net.osmand.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.BooleanPreference;
import net.osmand.plus.settings.backend.OsmandPreference;
import net.osmand.plus.settings.fragments.ApplyQueryType;
import net.osmand.plus.settings.fragments.OnConfirmPreferenceChange;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.settings.fragments.RouteParametersFragment.RELIEF_SMOOTHNESS_FACTOR;

public class ElevationDateBottomSheet extends BooleanPreferenceBottomSheet {

	public static final String TAG = ElevationDateBottomSheet.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(ElevationDateBottomSheet.class);
	private int selectedEntryIndex = -1;
	private ListPreferenceEx listPreference;
	private final List<BottomSheetItemWithCompoundButton> reliefFactorParameters = new ArrayList<>();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		listPreference = getListPreference();
		int contentPaddingSmall = getMyApplication().getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		final SwitchPreferenceEx switchPreference = getSwitchPreferenceEx();
		if (switchPreference == null) {
			return;
		}
		OsmandPreference preference = getMyApplication().getSettings().getPreference(getPrefId());
		if (!(preference instanceof BooleanPreference)) {
			return;
		}
		Context themedCtx = UiUtilities.getThemedContext(getMyApplication(), nightMode);

		CharSequence summaryOn = switchPreference.getSummaryOn();
		CharSequence summaryOff = switchPreference.getSummaryOff();
		final String on = summaryOn == null || summaryOn.toString().isEmpty()
				? getString(R.string.shared_string_enabled) : summaryOn.toString();
		final String off = summaryOff == null || summaryOff.toString().isEmpty()
				? getString(R.string.shared_string_disabled) : summaryOff.toString();
		final int activeColor = AndroidUtils.resolveAttribute(themedCtx, R.attr.active_color_basic);
		final int disabledColor = AndroidUtils.resolveAttribute(themedCtx, android.R.attr.textColorSecondary);
		final BooleanPreference pref = (BooleanPreference) preference;
		boolean checked = pref.getModeValue(getAppMode());
		final BottomSheetItemWithCompoundButton[] preferenceBtn = new BottomSheetItemWithCompoundButton[1];
		preferenceBtn[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(checked)
				.setTitle(checked ? on : off)
				.setTitleColorId(checked ? activeColor : disabledColor)
				.setCustomView(getCustomButtonView(checked))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean newValue = !pref.getModeValue(getAppMode());
						enableItems(newValue);
						Fragment targetFragment = getTargetFragment();
						if (targetFragment instanceof OnConfirmPreferenceChange) {
							ApplyQueryType applyQueryType = getApplyQueryType();
							if (applyQueryType == ApplyQueryType.SNACK_BAR) {
								applyQueryType = ApplyQueryType.NONE;
							}
							OnConfirmPreferenceChange confirmationInterface =
									(OnConfirmPreferenceChange) targetFragment;
							if (confirmationInterface.onConfirmPreferenceChange(
									switchPreference.getKey(), newValue, applyQueryType)) {
								switchPreference.setChecked(newValue);
								preferenceBtn[0].setTitle(newValue ? on : off);
								preferenceBtn[0].setChecked(newValue);
								preferenceBtn[0].setTitleColorId(newValue ? activeColor : disabledColor);
								updateCustomButtonView(v, newValue);

								if (targetFragment instanceof OnPreferenceChanged) {
									((OnPreferenceChanged) targetFragment).onPreferenceChanged(switchPreference.getKey());
								}
							}
						}
					}
				})
				.create();
		if (isProfileDependent()) {
			preferenceBtn[0].setCompoundButtonColorId(getAppMode().getIconColorInfo().getColor(nightMode));
		}
		items.add(new TitleItem(getString(R.string.routing_attr_height_obstacles_name)));
		items.add(preferenceBtn[0]);
		items.add(new DividerSpaceItem(getMyApplication(), contentPaddingSmall));
		items.add(new LongDescriptionItem(getString(R.string.elevation_data)));
		items.add(new DividerSpaceItem(getMyApplication(), contentPaddingSmall));
		String[] entries = listPreference.getEntries();
		for (int i = 0; i < entries.length; i++) {
			final BottomSheetItemWithCompoundButton[] preferenceItem = new BottomSheetItemWithCompoundButton[1];
			preferenceItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
					.setChecked(i == selectedEntryIndex)
					.setButtonTintList(AndroidUtils.createCheckedColorStateList(ctx, R.color.icon_color_default_light,
							isProfileDependent() ? getAppMode().getIconColorInfo().getColor(nightMode) : getActiveColorId()))
					.setTitle(entries[i])
					.setTag(i)
					.setLayoutId(R.layout.bottom_sheet_item_with_radio_btn_left)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							selectedEntryIndex = (int) preferenceItem[0].getTag();
							Object[] entryValues = listPreference.getEntryValues();
							if (entryValues != null && selectedEntryIndex >= 0) {
								Object value = entryValues[selectedEntryIndex];
								if (listPreference.callChangeListener(value)) {
									listPreference.setValue(value);
								}
								Fragment target = getTargetFragment();
								if (target instanceof OnPreferenceChanged) {
									((OnPreferenceChanged) target).onPreferenceChanged(listPreference.getKey());
								}
							}
							updateItems();
						}
					})
					.create();
			reliefFactorParameters.add(preferenceItem[0]);
			items.add(preferenceItem[0]);
		}
	}

	private SwitchPreferenceEx getSwitchPreferenceEx() {
		return (SwitchPreferenceEx) getPreference();
	}

	private ListPreferenceEx getListPreference() {
		if (listPreference == null) {
			DialogPreference.TargetFragment targetFragment = (DialogPreference.TargetFragment) getTargetFragment();
			if (targetFragment != null) {
				listPreference = targetFragment.findPreference(RELIEF_SMOOTHNESS_FACTOR);
			}
		}
		return listPreference;
	}

	private void updateItems() {
		for (BaseBottomSheetItem item : reliefFactorParameters) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				boolean checked = item.getTag().equals(selectedEntryIndex);
				((BottomSheetItemWithCompoundButton) item).setChecked(checked);
			}
		}
	}

	private void enableItems(boolean enable) {
		for (BaseBottomSheetItem item : reliefFactorParameters) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				item.getView().setEnabled(enable);
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager fm, String prefId, Fragment target, boolean usedOnMap,
									@Nullable ApplicationMode appMode, ApplyQueryType applyQueryType,
									boolean profileDependent) {
		try {
			if (fm.findFragmentByTag(ElevationDateBottomSheet.TAG) == null) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);

				ElevationDateBottomSheet fragment = new ElevationDateBottomSheet();
				fragment.setArguments(args);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setAppMode(appMode);
				fragment.setApplyQueryType(applyQueryType);
				fragment.setTargetFragment(target, 0);
				fragment.setProfileDependent(profileDependent);
				fragment.show(fm, ScreenTimeoutBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}

