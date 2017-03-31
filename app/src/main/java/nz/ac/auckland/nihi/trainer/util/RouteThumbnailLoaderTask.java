package nz.ac.auckland.nihi.trainer.util;

import java.io.File;

import nz.ac.auckland.nihi.trainer.data.Route;

import org.apache.log4j.Logger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

import com.j256.ormlite.dao.Dao;

public class RouteThumbnailLoaderTask extends AsyncTask<Void, Void, Bitmap> {

	private static final Logger logger = Logger.getLogger(RouteThumbnailLoaderTask.class);

	private final View loadingProgressBar;
	private final ImageView thumbnailView;
	private final Route route;
	private final Dao<Route, String> routeDao;
	private final Context context;

	public RouteThumbnailLoaderTask(Context context, Route route, Dao<Route, String> routeDao, ImageView thumbnailView,
			View loadingProgressBar) {
		this.loadingProgressBar = loadingProgressBar;
		this.thumbnailView = thumbnailView;
		this.route = route;
		this.routeDao = routeDao;
		this.context = context;
	}

	@Override
	protected void onPreExecute() {
		if (loadingProgressBar != null) {
			loadingProgressBar.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected Bitmap doInBackground(Void... params) {
		try {

			String fileName = null;

			// Only try to generate one thumbnail at a time for a given route.
			synchronized (route) {
				// When we get into this method, make sure the thumbnail hasn't been generated since we started.
				if (route.getThumbnailFileName() == null || !new File(route.getThumbnailFileName()).exists()) {

					// Track track = LocationUtils.toTrack(route);
					fileName = LocationUtils.generateStaticMap(context, route/* track */, 360, 300, 2);
					route.setThumbnailFileName(fileName);
					routeDao.update(route);
				}

				// If it has, then just return the filename.
				else {
					fileName = route.getThumbnailFileName();
				}
			}

			if (fileName != null) {
				return BitmapFactory.decodeFile(fileName);
			} else {
				return null;
			}

		} catch (Exception e) {
			logger.warn("doInBackground(): Error generating static map: " + e.getMessage(), e);
			return null;
		}
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		if (loadingProgressBar != null) {
			loadingProgressBar.setVisibility(View.GONE);
		}

		thumbnailView.setImageBitmap(result);
	}

}