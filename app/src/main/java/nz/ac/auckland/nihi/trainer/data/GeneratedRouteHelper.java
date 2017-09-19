package nz.ac.auckland.nihi.trainer.data;

import com.j256.ormlite.dao.Dao;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;

/**
 * Created by Aaron on 19/07/17.
 */

public class GeneratedRouteHelper {

    public static nz.ac.auckland.nihi.trainer.data.Route createRoute(long id, String routeJson, String name, Dao<Route, String> routeDAO) throws SQLException {
        Route route = new Route();
        route.setUserId(id);
        route.setName(name);

        try {
            JSONObject object = new JSONObject(routeJson);

            //route.setThumbnailFileName(testRouteImagePath);

            route.setFavorite(false);
            route.setCreatorName("alex");

            route.setLength(object.getDouble("distance"));
            route.setElevation(object.getDouble("elevation"));

            routeDAO.assignEmptyForeignCollection(route, "gpsCoordinates");
            routeDAO.create(route);

            JSONArray points = object.getJSONArray("points");
            for (int i = 0; i < points.length(); i++) {
                JSONObject point = points.getJSONObject(i);
                double lat = point.getDouble("lat");
                double lng = point.getDouble("lng");
                String instruction = point.getString("instruction");
                RouteCoordinate coord = new RouteCoordinate();
                coord.setLatitude(lat);
                coord.setLongitude(lng);
                coord.setInstruction(instruction);
                route.getGpsCoordinates().add(coord);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return route;
    }
}
