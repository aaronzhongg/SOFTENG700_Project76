package Tests;

import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;

import java.io.IOException;
import java.sql.SQLException;

import nz.ac.auckland.nihi.trainer.data.Route;

import static com.j256.ormlite.android.apptools.OrmLiteConfigUtil.writeConfigFile;

/**
 * Created by alex on 7/9/2017.
 */

public class DbConfigGenerator extends OrmLiteConfigUtil {
    private static final Class<?>[] classes = new Class[] {
            Route.class
    };
    public static void main(String[] args) throws SQLException, IOException {
        System.out.println("Okay man");
        //writeConfigFile("ormlite_config.txt", classes);
        writeConfigFile("ormlite_config.txt");
    }
}
