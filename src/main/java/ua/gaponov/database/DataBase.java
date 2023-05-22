package ua.gaponov.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static ua.gaponov.conf.Config.*;

public class DataBase {

    private static final Logger LOG = LoggerFactory.getLogger(DataBase.class);
    public static final String DELETE_SQL = "delete from access_log where date_event = ?";
    public static final String INSERT_SQL = "INSERT INTO access_log(user_id, date_event) select u.id, ? from users as u where u.card_no = ?";
    public static final String UPDATE_SQL = "update options set last_updates = now()";

    public void saveEvents(List<Event> events) {

        try (Connection con = DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmtDelete = con.prepareStatement(DELETE_SQL);
             PreparedStatement stmtInsert = con.prepareStatement(INSERT_SQL);
             PreparedStatement stmtUpdate = con.prepareStatement(UPDATE_SQL)
        ) {
            for (Event event : events) {
                stmtDelete.setString(1, event.getDate());
                stmtDelete.execute();

                stmtInsert.setString(1, event.getDate());
                stmtInsert.setString(2, event.getCardNo());
                stmtInsert.execute();
            }
            stmtUpdate.execute();
        } catch (SQLException e) {
            LOG.error("Error create connection. {}", e.toString());
        }
    }
}
