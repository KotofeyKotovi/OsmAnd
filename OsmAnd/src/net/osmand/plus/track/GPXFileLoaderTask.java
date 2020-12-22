package net.osmand.plus.track;

import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.TrackActivity;

import java.io.File;
import java.lang.ref.WeakReference;

public class GPXFileLoaderTask extends AsyncTask<Void, Void, GPXUtilities.GPXFile> {

	private OsmandApplication app;
	private WeakReference<TrackActivity> activityRef;
	private File file;
	private boolean showTemporarily;

	private TrackActivity getTrackActivity() {
		return activityRef.get();
	}

	public GPXFileLoaderTask(@NonNull TrackActivity activity) {
		this.activityRef = new WeakReference<>(activity);
		app = activity.getMyApplication();
		file = activity.getFile();
	}

	protected void onPreExecute() {
		TrackActivity activity = getTrackActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(true);
			Intent intent = activity.getIntent();
			if (intent != null && intent.hasExtra(TrackActivity.SHOW_TEMPORARILY)) {
				showTemporarily = true;
				intent.removeExtra(TrackActivity.SHOW_TEMPORARILY);
			}
		}
	}

	@Override
	protected GPXFile doInBackground(Void... params) {
		long startTime = System.currentTimeMillis();
		GPXFile result;
		if (file == null) {
			result = app.getSavingTrackHelper().getCurrentGpx();
		} else {
			SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedFileByPath(file.getAbsolutePath());
			if (selectedGpxFile != null && selectedGpxFile.getGpxFile() != null && selectedGpxFile.getGpxFile().modifiedTime == file.lastModified()) {
				result = selectedGpxFile.getGpxFile();
			} else {
				result = GPXUtilities.loadGPXFile(file);
			}
		}
		if (result != null) {
			result.addGeneralTrack();
			long timeout = 200 - (System.currentTimeMillis() - startTime);
			if (timeout > 0) {
				try {
					Thread.sleep(timeout);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		return result;
	}

	@Override
	protected void onPostExecute(@Nullable GPXFile result) {
		TrackActivity activity = getTrackActivity();
		if (activity != null) {
			activity.setSupportProgressBarIndeterminateVisibility(false);
			if (result != null) {
				GpxSelectionHelper helper = app.getSelectedGpxHelper();
				if (showTemporarily) {
					helper.selectGpxFile(result, false, false);
				} else {
					SelectedGpxFile selectedGpx = helper.getSelectedFileByPath(result.path);
					if (selectedGpx != null && result.error == null) {
						selectedGpx.setGpxFile(result, app);
					}
				}
			}
			if (!activity.isStopped()) {
				activity.onGPXFileReady(result);
			}
		}
	}
}