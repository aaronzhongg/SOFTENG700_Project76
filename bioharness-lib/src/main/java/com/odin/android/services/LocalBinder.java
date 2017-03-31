package com.odin.android.services;

import java.lang.ref.WeakReference;

import android.app.Service;
import android.os.Binder;

/**
 * A local {@link Binder} created to prevent memory leaks.
 * 
 * @author Thiranjith Weerasinghe
 * 
 * @param <S>
 *            {@link Service}
 * @see http://groups.google.com/group/cw-android/browse_thread/thread/d026cfa71e48039b/c3b41c728fedd0e7?show_docid=
 *      c3b41c728fedd0e7
 */
public class LocalBinder<S> extends Binder {
	private WeakReference<S> mService;

	public LocalBinder(S service) {
		mService = new WeakReference<S>(service);
	}

	public S getService() {
		return mService.get();
	}

	public void close() {
		mService = null;
	}
}
