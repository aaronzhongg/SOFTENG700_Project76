package nz.ac.auckland.nihi.trainer.data;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Created by alex on 6/14/2017.
 */

public class DummyRouteCreator {
    private static final double[] DUMMY_COORDINATES = new double[] { -36.853320902918384, 174.75763320922852,
            -36.84555973141201, 174.7609806060791, -36.84411730298869, 174.7635555267334, -36.84470114634267,
            174.76655960083008, -36.85091708446578, 174.76441383361816, -36.84985251214405, 174.7608518600464,
            -36.854076373106125, 174.7584056854248, -36.8537673180227, 174.75733280181885, -36.853320902918384,
            174.75759029388428 };

    public static Route createDummyRoute(Dao<Route, String> routeDAO, String testRouteImagePath) throws SQLException {
        Route route = new Route();
        route.setUserId(1);
        route.setName("route_test");
        route.setThumbnailFileName(testRouteImagePath);
        route.setFavorite(false);
        route.setCreatorName("alex");
        routeDAO.assignEmptyForeignCollection(route, "gpsCoordinates");
        routeDAO.create(route);
        for (int i = 0; i < DUMMY_COORDINATES.length; i += 2) {
            double lat = DUMMY_COORDINATES[i];
            double lng = DUMMY_COORDINATES[i + 1];
            RouteCoordinate coord = new RouteCoordinate();
            coord.setLatitude(lat);
            coord.setLongitude(lng);
            coord.setRoute(route);
            route.getGpsCoordinates().add(coord);
        }
        return route;
    }
}
