package nz.ac.auckland.nihi.trainer.services.downloader;

import org.apache.log4j.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DownloaderBroadcastReceiver extends BroadcastReceiver {

	private static final Logger logger = Logger.getLogger(DownloaderBroadcastReceiver.class);

	@Override
	public void onReceive(Context context, Intent intent) {
		logger.debug("onReceive(): Received broadcast, starting background downloader service...");
		Intent downloader = new Intent(context, DownloaderService.class);
		context.startService(downloader);
	}

}
