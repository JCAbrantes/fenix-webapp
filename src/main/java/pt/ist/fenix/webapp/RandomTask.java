package pt.ist.fenix.webapp;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.fenixedu.academic.util.ConnectionManager;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.backend.jvstmojb.ojb.OJBMetadataGenerator;
import pt.ist.fenixframework.backend.jvstmojb.repository.DbUtil;
import pt.ist.fenixframework.dml.DomainClass;
import pt.ist.fenixframework.dml.DomainModel;
import pt.ist.fenixframework.dml.Slot;

public class RandomTask extends CustomTask {

    @Override
    public TxMode getTxMode() {

        return TxMode.READ;

    }

    @Override
    public void runTask() throws IOException {

        DomainModel domainModel = FenixFramework.getDomainModel();
        try {
            Connection currentSQLConnection = ConnectionManager.getCurrentSQLConnection();
            Statement statement = currentSQLConnection.createStatement();
            for (DomainClass dc : domainModel.getDomainClasses()) {
                for (Slot slot : dc.getSlotsList()) {
                    if (slot.getSlotType().getFullname().contains("LocalizedString")) {
                        String tableName = OJBMetadataGenerator.getExpectedTableName(dc);
                        String columnName = DbUtil.convertToDBStyle(slot.getName());

                        String queryFormat1 = "SELECT COUNT(*) FROM %s WHERE %s like '%%en-UK%%';";
                        String query1 = String.format(queryFormat1, tableName, columnName, columnName);

                        ResultSet resultSet = statement.executeQuery(query1);

                        while (resultSet.next()) {
                            int count = resultSet.getInt(1);
                            if (count > 0) {
                                String queryFormat = "update %s set %s  = REPLACE(%s ,'\"en-UK\":','\"en-GB\":');";
                                String query = String.format(queryFormat, tableName, columnName, columnName);

                                taskLog("-- %s%n", query);
                                taskLog("%s%n", query);
                            }
                        }

                    }
                }
            }
        } catch (Exception exception) {

        }

    }
}
