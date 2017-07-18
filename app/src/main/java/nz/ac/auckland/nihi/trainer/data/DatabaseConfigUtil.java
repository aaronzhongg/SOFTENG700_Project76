package nz.ac.auckland.nihi.trainer.data;

import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by alex on 6/13/2017.
 */

public class DatabaseConfigUtil extends OrmLiteConfigUtil {
    public static void main(String[] args) throws SQLException, IOException {
        System.out.println("Okay man");
        writeConfigFile("ormlite_config.txt");
    }
}
