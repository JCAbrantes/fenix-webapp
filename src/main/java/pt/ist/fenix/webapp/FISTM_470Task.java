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

public class FISTM_470Task extends CustomTask {

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
                    if (slot.getSlotType().getFullname().contains("joda.time.DateTime")) {
                        String tableName = OJBMetadataGenerator.getExpectedTableName(dc);
                        String columnName = DbUtil.convertToDBStyle(slot.getName());

                        String queryFormat1 =
                                "SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE table_name = '%s' AND COLUMN_NAME = '%s';";
                        String query1 = String.format(queryFormat1, tableName, columnName);

                        ResultSet resultSet = statement.executeQuery(query1);

                        while (resultSet.next()) {
                            String columnType = resultSet.getString(1);
                            if (columnType.equals("timestamp")) {
                                taskLog("SELECT '%s of %s' AS ' ';%n", columnName, tableName);
                                taskLog("ALTER TABLE %s CHANGE %s %s datetime NULL default NULL;%n", tableName, columnName,
                                        columnName);
                            }
                        }
                    }
                }
            }
        } catch (Exception exception) {
            throw new Error(exception);
        }
    }
}