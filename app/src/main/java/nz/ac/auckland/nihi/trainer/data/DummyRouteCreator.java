package nz.ac.auckland.nihi.trainer.data;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import nz.ac.auckland.nihi.trainer.data.Route;

/**
 * Created by alex on 6/14/2017.
 */

public class DummyRouteCreator {
    //4.2 KM, 50m elevation (guessing)
    private static final double[] DUMMY_COORDINATES_UOA = new double[] { -36.853320902918384, 174.75763320922852,
            -36.84555973141201, 174.7609806060791, -36.84411730298869, 174.7635555267334, -36.84470114634267,
            174.76655960083008, -36.85091708446578, 174.76441383361816, -36.84985251214405, 174.7608518600464,
            -36.854076373106125, 174.7584056854248, -36.8537673180227, 174.75733280181885, -36.853320902918384,
            174.75759029388428 };

    //2.3KM, 30m elevation. 70 for testing (guess values)
    private static final double[] DUMMY_COORDINATES_HOWICK_SHORT = new double[]{-36.896006, 174.925966, -36.897622, 174.929977,
            -36.899417, 174.929024, -36.898782, 174.927061, -36.895662, 174.922471, -36.892590, 174.925780,-36.895403, 174.925744
    };
    //6Km, 95m elevation, 35 for testing (guess values)
    private static final double[] DUMMY_COORDINATES_HOWICK_LONG = new double[]{
            -36.895403, 174.925744, -36.892602, 174.925829, -36.892478, 174.929305, -36.893057, 174.930195, -36.894323, 174.929514,
            -36.895739, 174.931772, -36.899420, 174.929031, -36.898812, 174.927110, -36.896950, 174.928035, -36.895534, 174.925744
    };

    private static final double[] DUMMY_COORDINATES_NAVIGATION_TEST = new double[]{
            -36.8964414,174.9265492, -36.897630100000008,174.9300025, -36.8984667,174.9295522, -36.897630100000008,174.9300025
    };

    private static final String[] DUMMY_NAVIGATION_TEST_INSTRUCTIONS = new String[]{
            "Head southeast on Howe St toward Wellington St", "Turn right onto Moore St Destination will be on the right",
            "Head northeast on Moore St toward Abercrombie St", "Turn left onto Howe St Destination will be on the left"
    };

    public static List<Route> createDummyRoutes(Dao<Route, String> routeDAO, String testRouteImagePath) throws SQLException {
        List<Route> routes = new ArrayList<Route>();
        routes.add(createRoute(1, DUMMY_COORDINATES_UOA, "UoA route", testRouteImagePath, routeDAO, 4200, 50, null));
        routes.add(createRoute(2, DUMMY_COORDINATES_HOWICK_SHORT, "Howick short route", testRouteImagePath, routeDAO, 2300, 30, null));
        routes.add(createRoute(3, DUMMY_COORDINATES_HOWICK_SHORT, "Howick short route + elevation", testRouteImagePath, routeDAO, 2300, 70, null));
        routes.add(createRoute(4, DUMMY_COORDINATES_HOWICK_LONG, "Howick long route", testRouteImagePath, routeDAO, 6000, 95, null));
        routes.add(createRoute(5, DUMMY_COORDINATES_HOWICK_LONG, "Howick long route - elevation", testRouteImagePath, routeDAO, 6000, 35, null));
        routes.add(createRoute(6, DUMMY_COORDINATES_NAVIGATION_TEST, "Navigation Test", testRouteImagePath, routeDAO, 870, 39, DUMMY_NAVIGATION_TEST_INSTRUCTIONS));
//        Iterator<RouteCoordinate> itr = navTest.getGpsCoordinates().iterator();
//        int i = 0;
//        while(itr.hasNext()){
//            RouteCoordinate r = itr.next();
//            r.setInstruction(DUMMY_NAVIGATION_TEST_INSTRUCTIONS[i]);
//            i++;
//        }
//        routeDAO.update(navTest);
//        routes.add(navTest);
        return routes;
    }

    private static nz.ac.auckland.nihi.trainer.data.Route createRoute(long id, double[] coordinates, String name , String testRouteImagePath,
                                    Dao<Route, String> routeDAO, double length, double elevation, String[] instructions) throws SQLException {
        Route route = new Route();
        route.setUserId(id);
        route.setName(name);

        //route.setThumbnailFileName(testRouteImagePath);

        route.setFavorite(false);
        route.setCreatorName("alex");

        route.setLength(length);
        route.setElevation(elevation);

        routeDAO.assignEmptyForeignCollection(route, "gpsCoordinates");
        routeDAO.create(route);
        int j = 0;
        for (int i = 0; i < coordinates.length; i += 2) {
            double lat = coordinates[i];
            double lng = coordinates[i + 1];
            RouteCoordinate coord = new RouteCoordinate();
            coord.setLatitude(lat);
            coord.setLongitude(lng);
            coord.setRoute(route);
            if(instructions != null){
                coord.setInstruction(instructions[j]);
                j++;
            }
            route.getGpsCoordinates().add(coord);
        }
        return route;
    }
}