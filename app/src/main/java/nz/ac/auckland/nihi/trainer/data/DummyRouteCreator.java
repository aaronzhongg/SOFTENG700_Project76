package nz.ac.auckland.nihi.trainer.data;

import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import nz.ac.auckland.cs.odin.android.api.prefs.OdinPreferences;
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

//    private static final double [] DUMMY_COORDINATES1 = new double[] { -36.8519211, 174.7719061, -36.850945, 174.7732118, -36.8502179, 174.7724098,
//            -36.8488952, 174.7735642, -36.8488094, 174.7743917, -36.847454, 174.7736648, -36.8471189, 174.774438, -36.8470708, 174.7746701, -36.8469777,
//            174.7746947, -36.8470321, 174.775208, -36.8469777, 174.7746947, -36.8470708, 174.7746701, -36.8471189, 174.774438, -36.8474142, 174.773768,
//            -36.8504288, 174.7744708, -36.8508302, 174.7753371, -36.8512487, 174.7763483, -36.8504385, 174.7773407, -36.851097,174.7777674, -36.8504385,
//            174.7773407, -36.8512487, 174.7763483, -36.8507905, 174.7752486, -36.8503741, 174.77446, -36.8505404, 174.7741811, -36.8502179, 174.7724098,
//            -36.850945, 174.7732118};
//
//    private static final String[] DUMMY_NAVIGATION1 = new String[]{
//            "Head northeast on Wynyard St toward Charles Nalden Ln", "Turn left onto Alten Rd",
//            "Turn right onto Anzac Ave", "Turn right at Parliament St Take the stairs",
//            "Turn left onto Beach Rd", "Turn right onto Mahuhu Cres",
//            "Slight right", "Turn left", "Turn right", "Head southwest",
//            "Turn left toward Mahuhu Cres", "Turn right toward Mahuhu Cres",
//            "Slight left onto Mahuhu Cres", "Turn left onto Beach Rd",
//            "Turn left to stay on Beach Rd", "Continue onto Parnell Rise",
//            "Turn left onto Augustus Terrace", "Turn right onto Parnell Rd Destination will be on the right",
//            "Head northwest on Parnell Rd toward Eglon St", "Turn left onto Augustus Terrace",
//            "Turn right onto Parnell Rise", "Continue onto Beach Rd", "Turn left onto Churchill St",
//            "Turn right toward Alten Rd", "Sharp left onto Alten Rd",
//            "Turn right onto Wynyard St Destination will be on the right"
//    };

    private static final double[] DUMMY_COORDINATES2 = new double[] { -36.853246899999988,174.7690034, -36.8539852,174.7682619,-36.8521372,174.7665167,
            -36.8504755,174.763048,-36.8508999,174.7645171,-36.84637,174.7661036,-36.8469485,174.7697461,-36.8468204,174.7699715,-36.8468186,174.7705735,
            -36.8512771,174.7685394,-36.851862,174.770357};

    private static final String[] DUMMY_NAVIGATION2 = new String[] {
            "Head southwest on Symonds St toward Wellesley St E", "Turn right onto Wellesley St E", "Slight right to stay on Wellesley St E",
            "Head east on Wellesley St W toward Elliott St", "Turn left onto Queen St", "Turn right onto Shortland St", "Slight left onto Emily Pl",
            "Turn right", "Head south on Princes St toward Shortland St Take the stairs", "Turn left onto Alfred St", "Turn right onto Symonds St"
    };

    private static final double[] DUMMY_COORDINATES_EAST = new double[] {-36.8984218,174.8876015, -36.8965143,174.8881496, -36.8963618, 174.8882676,
            -36.897802,174.8970622, -36.8958801,174.899833, -36.8898941,174.9009862, -36.8907574,174.9011334, -36.891511,174.898174,
            -36.8918565,174.8859266, -36.8917267,174.8824176, -36.892667,174.8824378, -36.8926084,174.8818623, -36.8919345,174.8815763,
            -36.8926084, 174.8818623, -36.8927678,174.8857797, -36.8948642, 174.8855881, -36.8976199, 174.8862124, -36.8978487, 174.8877508};

    private static final String[] DUMMY_NAVIGATION_EAST = new String[] {
            "Head north on Glenmore Rd toward Elimar Dr","Slight right to stay on Glenmore Rd", "At the roundabout, take the 1st exit onto Butley Dr",
            "At the roundabout, take the 2nd exit onto Fortunes Rd", "Slight left onto Pigeon Mountain Rd Destination will be on the right", "Head south on Pigeon Mountain Rd",
            "Turn right", "Turn right", "Turn right toward Bramley Dr", "Turn left toward Bramley Dr", "Turn right onto Bramley Dr", "Turn right Destination will be on the right",
            "Head southeast toward Bramley Dr", "Turn left onto Bramley Dr", "Turn right onto Belmere Rise", "Continue onto Sarah Pl", "Sarah Pl turns left and becomes Elimar Dr",
            "Turn right onto Glenmore Rd Destination will be on the right"
    };

    public static List<Route> createDummyRoutes(Dao<Route, String> routeDAO, String testRouteImagePath, Dao<RCExerciseSummary, String> exerciseSummaryDao) throws SQLException {
        List<Route> routes = new ArrayList<Route>();
        routes.add(createRoute(1, DUMMY_COORDINATES_UOA, "UoA route", testRouteImagePath, routeDAO, 4200, 50, null));
        //routes.add(createRoute(2, DUMMY_COORDINATES_HOWICK_SHORT, "Howick short route", testRouteImagePath, routeDAO, 2300, 30, null));
        //routes.add(createRoute(3, DUMMY_COORDINATES_HOWICK_SHORT, "Howick short route + elevation", testRouteImagePath, routeDAO, 2300, 70, null));
        //routes.add(createRoute(4, DUMMY_COORDINATES_HOWICK_LONG, "Howick long route", testRouteImagePath, routeDAO, 6000, 95, null));
        //routes.add(createRoute(5, DUMMY_COORDINATES_HOWICK_LONG, "Howick long route - elevation", testRouteImagePath, routeDAO, 6000, 35, null));
        routes.add(createRoute(2, DUMMY_COORDINATES_NAVIGATION_TEST, "Navigation Test", testRouteImagePath, routeDAO, 870, 39, DUMMY_NAVIGATION_TEST_INSTRUCTIONS));
        Route route1 = createRoute(3, DUMMY_COORDINATES2, "Route #1", testRouteImagePath, routeDAO, 2729, 140, DUMMY_NAVIGATION2);
        Route route2 = createRoute(4, DUMMY_COORDINATES_EAST, "EAST_5km", testRouteImagePath, routeDAO, 5866, 150, DUMMY_NAVIGATION_EAST);
        routes.add(route1);
//        routes.add(createRoute(3, DUMMY_COORDINATES1, "Route #1", testRouteImagePath, routeDAO, 2641, 114, DUMMY_NAVIGATION1));
//        Iterator<RouteCoordinate> itr = navTest.getGpsCoordinates().iterator();
//        int i = 0;
//        while(itr.hasNext()){
//            RouteCoordinate r = itr.next();
//            r.setInstruction(DUMMY_NAVIGATION_TEST_INSTRUCTIONS[i]);
//            i++;
//        }
//        routeDAO.update(navTest);
//        routes.add(navTest);

        createDummyRunningData(exerciseSummaryDao, route1, route2);
        return routes;
    }

    public static void createDummyRunningData(Dao<RCExerciseSummary, String> summaryDao, Route followedRoute, Route followedRoute2) throws SQLException {

        RCExerciseSummary newSummary = new RCExerciseSummary() {};
        newSummary.setDate(new Date(1505041152L * 1000));
        newSummary.setDurationInSeconds(2158);
        newSummary.setDistanceInMetres(5866);
        newSummary.setFollowedRoute(followedRoute2);
        newSummary.setAvgHeartRate(140);
        newSummary.setMinHeartRate(60);
        newSummary.setMaxHeartRate(180);

        newSummary.setAvgSpeed(9.52f);
        newSummary.setMinSpeed(7f);
        newSummary.setMaxSpeed(11.54f);

        newSummary.setUserId(0);

        summaryDao.assignEmptyForeignCollection(newSummary, "summaryDataChunks");
        summaryDao.create(newSummary);

        SummaryDataChunk[] summaryDataChunks1 = new SummaryDataChunk[] { new SummaryDataChunk(0, new LatLng(-36.8984218,174.8876015), 7f, 60 ),
//                new SummaryDataChunk(2000*60, new LatLng(-36.8965143,174.8881496), 7f, 100 ),
                new SummaryDataChunk(3000*60, new LatLng(-36.8963618, 174.8882676), 8.5f, 114 ),
                new SummaryDataChunk(6000*60, new LatLng(-36.897802,174.8970622), 9f, 122 ),
                new SummaryDataChunk(9000*60, new LatLng(-36.8958801,174.899833), 10f, 133 ),
//                new SummaryDataChunk(10000*60, new LatLng(-36.8898941,174.9009862), 8f, 145 ),
                new SummaryDataChunk(12000*60, new LatLng(-36.8907574,174.9011334), 9.3f, 150 ),
                new SummaryDataChunk(15000*60, new LatLng(-36.891511,174.898174), 11.04f, 167 ),
                new SummaryDataChunk(18000*60, new LatLng(-36.8918565,174.8859266), 9f, 170 ),
                new SummaryDataChunk(21000*60, new LatLng(-36.8917267,174.8824176), 8.5f, 180 ),
//                new SummaryDataChunk(19000*60, new LatLng(-36.892667,174.8824378), 8.5f, 175 ),
//                new SummaryDataChunk(20000*60, new LatLng(-36.8926084,174.8818623), 8.5f, 175 ),
//                new SummaryDataChunk(21000*60, new LatLng(-36.8919345,174.8815763), 8.5f, 175 ),
                new SummaryDataChunk(24000*60, new LatLng(-36.8926084, 174.8818623), 9f, 180 ),
                new SummaryDataChunk(27000*60, new LatLng(-36.8927678,174.8857797), 9.5f, 175 ),
                new SummaryDataChunk(30000*60, new LatLng(-36.8948642, 174.8855881), 8f, 170 ),
                new SummaryDataChunk(33000*60, new LatLng(-36.8976199, 174.8862124), 7.5f, 175 )};
//                new SummaryDataChunk(26000*60, new LatLng(-36.8978487, 174.8877508), 8.5f, 175 )};
        for (SummaryDataChunk s : summaryDataChunks1) {
            newSummary.getSummaryDataChunks().add(s);
        }


        newSummary.setDate(new Date(1502506419L * 1000));
        newSummary.setDurationInSeconds(1154);
        newSummary.setDistanceInMetres(2730);
        newSummary.setFollowedRoute(followedRoute);
        newSummary.setAvgHeartRate(140);
        newSummary.setMinHeartRate(70);
        newSummary.setMaxHeartRate(180);

        newSummary.setAvgSpeed(8.52f);
        newSummary.setMinSpeed(6f);
        newSummary.setMaxSpeed(11.04f);

        newSummary.setUserId(0);


        summaryDao.assignEmptyForeignCollection(newSummary, "summaryDataChunks");
        summaryDao.create(newSummary);

        SummaryDataChunk[] summaryDataChunks = new SummaryDataChunk[] { new SummaryDataChunk(0, new LatLng(-36.853246899999988,174.7690034), 6f, 70 ),
                new SummaryDataChunk(2000*60, new LatLng(-36.8539852,174.7682619), 7f, 100 ),
                new SummaryDataChunk(4000*60, new LatLng(-36.8521372,174.7665167), 8.5f, 114 ),
                new SummaryDataChunk(6000*60, new LatLng(-36.8504755,174.763048), 9f, 122 ),
                new SummaryDataChunk(8000*60, new LatLng(-36.8508999,174.7645171), 10f, 133 ),
                new SummaryDataChunk(10000*60, new LatLng(-36.84637,174.7661036), 8f, 145 ),
                new SummaryDataChunk(12000*60, new LatLng(-36.8469485,174.7697461), 9.3f, 150 ),
//                new SummaryDataChunk(14000*60, new LatLng(-36.8468204,174.7699715), 11.04f, 167 ),
                new SummaryDataChunk(16000*60, new LatLng(-36.8468186,174.7705735), 7f, 170 ),
                new SummaryDataChunk(18000*60, new LatLng(-36.8512771,174.7685394), 7.5f, 180 ),
                new SummaryDataChunk(19233*60, new LatLng(-36.851862,174.770357), 8.5f, 175 )};
        for (SummaryDataChunk s : summaryDataChunks) {
            newSummary.getSummaryDataChunks().add(s);
        }
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
