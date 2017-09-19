package nz.ac.auckland.nihi.trainer.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nz.ac.auckland.nihi.trainer.R.string;
import nz.ac.auckland.nihi.trainer.data.RCExerciseSummary;
import nz.ac.auckland.nihi.trainer.data.Route;
import nz.ac.auckland.nihi.trainer.data.RouteCoordinate;
import nz.ac.auckland.nihi.trainer.data.SummaryDataChunk;

import org.apache.log4j.Logger;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class LocationUtils {

	public static LatLng getLocation(LatLng initialLocation, double bearingInDegrees, double distanceInKm) {

		double distance = distanceInKm / 6371.0;
		double bearingInRad = toRad(bearingInDegrees);

		double lat1 = toRad(initialLocation.latitude);
		double lon1 = toRad(initialLocation.longitude);

		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distance) + Math.cos(lat1) * Math.sin(distance)
				* Math.cos(bearingInRad));

		double lon2 = lon1
				+ Math.atan2(Math.sin(bearingInRad) * Math.sin(distance) * Math.cos(lat1),
						Math.cos(distance) - Math.sin(lat1) * Math.sin(lat2));

		if (Double.isNaN(lat2) || Double.isNaN(lon2)) {
			return null;
		} else {
			return new LatLng(toDeg(lat2), toDeg(lon2));
		}
	}

	private static double toRad(double num) {
		return num * Math.PI / 180.0;
	}

	private static double toDeg(double num) {
		return num * 180.0 / Math.PI;
	}

	// Order of arguments: key, width, height, scale
	private static final String STATIC_MAP_BASE_URL = "http://maps.googleapis.com/maps/api/staticmap?key=%s&sensor=true&size=%dx%d&scale=%d&visual_refresh=true&path=";

	private static final String STATIC_MAP_PATH_BASE_ARG = "weight:5|color:red|enc:%s";

	public static final String SAVED_MAP_IMAGE_DIRECTORY = "route_thumbnails";

	private static final String CHARSET = "UTF-8";

	private static final Logger logger = Logger.getLogger(LocationUtils.class);

	/**
	 * Routes longer than this number of coordinates will be pre-trimmed before bothering to try and encode them.
	 */
	private static final int INITIAL_TRACK_SIZE_LIMIT = 1000;

	/**
	 * Encoded routes longer than this will be trimmed by a certain degree and then re-encoded until they are less than
	 * this length.
	 */
	private static final int ENCODED_ROUTE_LENGTH_LIMIT = 1000;

	/**
	 * If re-encoding is required due to routes still being too long after the first pass, the new target number of
	 * route coordinates will be set to this factor of the previous target.
	 */
	private static final double ENCODED_ROUTE_TRIM_FACTOR = 0.8;

	/**
	 * Synchronously generates a static map for the given route, saves it in local storage, and returns the filename of
	 * the map.
	 * 
	 */
	public static String generateStaticMap(Context context, Route route, int width, int height, int scale)
			throws IOException {

		try {

			String fileName = "map_" + route.getId() + ".png";

			String pts = encodeRoute(route);

			// Create valid static maps URL, obtain image from it
			Bitmap image = generateStaticMapImage(context, width, height, scale, pts);

			// Save the image
			File mapFile = saveMapImage(context, fileName, image);

			// Return the name of the file.
			return mapFile.getPath();

		} catch (Exception e) {
			logger.error(e);
			return null;
		}

		// } catch (IOException e) {
		// logger.error(e);
		// throw e;
		// } catch (Exception e) {
		// logger.error(e);
		// throw new RuntimeException(e);
		// }

	}

	/**
	 * Encodes the given {@link Route} as a Google Encoded Polyline.
	 * 
	 * @param route
	 * @return
	 */
	private static String encodeRoute(Route route) {

		// Get the GPS coordinates that make up the route, but only INITIAL_TRACK_SIZE_LIMIT at most (use the thinning
		// algorithm to get rid of extras).
		List<RouteCoordinate> coords = thinCoordsList(route.getGpsCoordinates(), INITIAL_TRACK_SIZE_LIMIT);

		String encoded_points = null;
		boolean reEncode;
		do {

			// Encode the route
			double prevLat = 0;
			double prevLng = 0;
			encoded_points = "";

			for (RouteCoordinate coord : coords) {
				double lat = coord.getLatitude();
				double lng = coord.getLongitude();
				encoded_points += encodePoint(prevLat, prevLng, lat, lng);
				prevLat = lat;
				prevLng = lng;
			}

			// If the encoded route is too long, setup for re-encoding.
			reEncode = (encoded_points.length() > ENCODED_ROUTE_LENGTH_LIMIT);
			if (reEncode) {
				int oldSize = coords.size();
				coords = thinCoordsList(coords, (int) (coords.size() * ENCODED_ROUTE_TRIM_FACTOR));
				logger.warn("encodeRoute(): Encoded route was too long with coordinate size = " + oldSize
						+ ". Will try again with coordinate size = " + coords.size() + ".");
			}

		} while (reEncode);

		logger.info("encodeRoute(): Encoded route length = " + encoded_points.length());
		return encoded_points;

	}

	/**
	 * Encodes the given point as part of an encoded polyline, given the given previous point.
	 * 
	 * @param plat
	 * @param plng
	 * @param lat
	 * @param lng
	 * @return
	 */
	private static String encodePoint(double plat, double plng, double lat, double lng) {

		int late5 = (int) Math.round(lat * 1e5);
		int plate5 = (int) Math.round(plat * 1e5);

		int lnge5 = (int) Math.round(lng * 1e5);
		int plnge5 = (int) Math.round(plng * 1e5);

		int dlng = lnge5 - plnge5;
		int dlat = late5 - plate5;

		return encodeSignedNumber(dlat) + encodeSignedNumber(dlng);

	}

	private static String encodeSignedNumber(int num) {
		int sgn_num = num << 1;

		if (num < 0) {
			sgn_num = ~(sgn_num);
		}

		return (encodeNumber(sgn_num));
	}

	private static String encodeNumber(int num) {
		String encodeString = "";

		while (num >= 0x20) {
			char[] chars = Character.toChars((0x20 | (num & 0x1f)) + 63);
			encodeString += new String(chars);
			// encodeString += (String.fromCharCode((0x20 | (num & 0x1f)) + 63));
			num >>= 5;
		}

		// encodeString += (String.fromCharCode(num + 63));
		char[] chars = Character.toChars(num + 63);
		encodeString += new String(chars);
		return encodeString;
	}

	/**
	 * Removes coordinates from the given coordinate list until there are at maximum the given number of points. If
	 * points need to be removed, it will remove them evenly throughout the list.
	 * 
	 * @param coords
	 * @param maxSize
	 * @return
	 */
	private static List<RouteCoordinate> thinCoordsList(Collection<RouteCoordinate> coords, int maxSize) {
		ArrayList<RouteCoordinate> unfiltered = new ArrayList<RouteCoordinate>(coords);

		// final int numPoints = TRACK_SIZE_LIMIT;
		if (unfiltered.size() > maxSize) {

			logger.warn("thinCoordsList(): Route too big (" + unfiltered.size() + " pts). Downsizing to approx. "
					+ maxSize + " pts.");

			ArrayList<RouteCoordinate> filtered = new ArrayList<RouteCoordinate>();

			int i = 0;
			int counter = 0;
			while (i < unfiltered.size()) {

				filtered.add(unfiltered.get(i));
				counter++;
				i = Math.round((float) counter * (float) unfiltered.size() / (float) maxSize);

			}

			logger.warn("thinCoordsList(): Route downsized to " + filtered.size() + " pts.");

			return filtered;
		}

		// to save CPU power, this is a shortcut if filtering isn't required.
		else {
			logger.debug("thinCoordsList(): Route downsize not required.");
			return unfiltered;
		}
	}

	/**
	 * Saves the given image under the directory given by {@link #SAVED_MAP_IMAGE_DIRECTORY}, with the given file name.
	 * 
	 */
	private static File saveMapImage(Context context, String fileName, Bitmap image) throws FileNotFoundException,
			IOException {

		// Get a reference to the directory where we'll save this image.
		// Create that directory if it doesn't exist.
		File externalFilesDir = context.getExternalFilesDir(null);
		File savedImagesDir = new File(externalFilesDir, SAVED_MAP_IMAGE_DIRECTORY);
		if (!savedImagesDir.exists()) {
			savedImagesDir.mkdirs();
		}
		File mapFile = new File(savedImagesDir, fileName);

		// Do the actual saving
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(mapFile));
		boolean saved = image.compress(CompressFormat.PNG, 100, out);
		out.flush();
		out.close();
		if (!saved) {
			throw new IOException("Could not save map image (image.compress returned false).");
		}

		// Return the full path to the saved image.
		return mapFile;
	}

	/**
	 * Queries the Google Static Maps API for a map image representing a map with the path given by
	 * <code>encodedPoints</code>.
	 * 
	 * @param context
	 * @param width
	 * @param height
	 * @param scale
	 * @param encodedPoints
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	private static Bitmap generateStaticMapImage(Context context, int width, int height, int scale, String encodedPoints)
			throws UnsupportedEncodingException, MalformedURLException, IOException {

		String key = URLEncoder.encode(context.getString(string.static_maps_api_key), CHARSET);
		String baseUri = String.format(STATIC_MAP_BASE_URL, key, width, height, scale);
		String uriString = baseUri + URLEncoder.encode(String.format(STATIC_MAP_PATH_BASE_ARG, encodedPoints), CHARSET);
		// logger.info("generateStaticMap(): About to generate thumbnail for Route with " + coords.size()
		// + " GPS values.");
		// logger.info("generateStaticMap(): About to generate route thumbnail from the following URL: " + uriString);
		URL url = new URL(uriString);
		URLConnection conn = url.openConnection();
		Bitmap image = BitmapFactory.decodeStream(conn.getInputStream());
		conn.getInputStream().close();
		return image;
	}

	/**
	 * Converts the given {@link Route} to an array of {@link LatLng} objects - required for display on a Google map
	 * view.
	 * 
	 * @param route
	 * @return
	 */
	public static LatLng[] toLatLngArray(Route route) {
		Collection<RouteCoordinate> gpsCoordinates = route.getGpsCoordinates();
		if (gpsCoordinates != null) {
			LatLng[] arr = new LatLng[gpsCoordinates.size()];
			int counter = 0;
			for (RouteCoordinate coord : gpsCoordinates) {
				arr[counter++] = new LatLng(coord.getLatitude(), coord.getLongitude());
			}
			return arr;
		} else {
			return new LatLng[0];
		}
	}

	public static LatLng[] toLatLng(RCExerciseSummary e) {
		Collection<SummaryDataChunk> summaryDataChunks = e.getSummaryDataChunks();
		if (summaryDataChunks != null) {
			LatLng[] arr = new LatLng[summaryDataChunks.size()];
			int counter = 0;
			for (SummaryDataChunk s: summaryDataChunks) {
				arr[counter++] = new LatLng(s.getLatitude(), s.getLongitude());
			}
			return arr;
		} else {
			return new LatLng[0];
		}
	}

	/**
	 * Converts the given {@link Route} to a list of {@link Location} objects. Doing this allows us to access the
	 * distance algorithms built into the class.
	 * 
	 * @param route
	 * @return
	 */
	public static List<Location> toLocations(Route route) {
		ArrayList<Location> locs = new ArrayList<Location>();
		Collection<RouteCoordinate> gpsCoordinates = route.getGpsCoordinates();
		for (RouteCoordinate coord : gpsCoordinates) {
			Location l = new Location(Route.class.getName());
			l.setLatitude(coord.getLatitude());
			l.setLongitude(coord.getLongitude());
			locs.add(l);
		}
		return locs;
	}

	/**
	 * Gets the length of the given route in meters. This is calculated by summing up the distances in meters between
	 * consecutive points on the route.
	 * 
	 * @param route
	 * @return
	 */
	public static double getRouteLengthInMeters(Route route) {
		List<Location> locs = toLocations(route);
		double length = 0;
		for (int i = 0; i < locs.size() - 1; i++) {
			length += locs.get(i).distanceTo(locs.get(i + 1));
		}
		return length;
	}

	/**
	 * Gets the distance to the given route in meters. This is calculated by finding the closest point on the route to
	 * the given location, then calculating the distance between the current location and that point.
	 * 
	 * @param route
	 * @param myLocation
	 * @return
	 */
	public static double getDistanceToRouteInMeters(Route route, Location myLocation) {
		List<Location> locs = toLocations(route);
		double distance = Double.POSITIVE_INFINITY;
		for (Location l : locs) {
			distance = Math.min(distance, l.distanceTo(myLocation));
		}
		if (distance == Double.POSITIVE_INFINITY) {
			return 0;
		} else {
			return distance;
		}
	}

	/**
	 * Calculate distance between two points in latitude and longitude
	 *
	 * lat1, lon1 Start point lat2, lon2, end point
	 * @returns Distance in Meters
	 */
	public static double distanceBetweenCoordinates(double lat1, double lat2, double lon1,
								  double lon2) {

		final int R = 6371; // Radius of the earth

		double latDistance = Math.toRadians(lat2 - lat1);
		double lonDistance = Math.toRadians(lon2 - lon1);
		double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters

		double height = 0; //Ignoring height, therefore accuracy is diminished over long distances

		distance = Math.pow(distance, 2) + Math.pow(height, 2);

		return Math.sqrt(distance);
	}

}
