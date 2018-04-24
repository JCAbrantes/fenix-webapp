package pt.ist.fenix.webapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.Atomic.TxMode;
/*
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.renates.domain.thesis.RenatesUtil;
import pt.ist.renates.domain.thesis.ThesisId;
import pt.webservice.renates.RenatesWS;
import pt.webservice.renates.RenatesWSSoap;*/

public class TestTask extends CustomTask {

    private final static Locale PT = new Locale("pt", "PT");

    @Override
    public TxMode getTxMode() {

        return TxMode.READ;

    }

    @Override
    public void runTask() throws IOException {

        /*RenatesWS ss = new RenatesWS(RenatesWS.WSDL_LOCATION);
        RenatesWSSoap port = ss.getRenatesWSSoap12();
        
        taskLog("Cheking thesis TIDs \n");
        
        for (Thesis thesis : RenatesUtil.getRenatesThesis()) {
            if (thesis.getThesisId() == null || thesis.getThesisId().getId() == null) {
                String internalId = RenatesUtil.getThesisId(thesis);
                String tid = port.tid(internalId);
                if (tid != null) {
                    thesis.setThesisId(new ThesisId(tid));
                }
                taskLog("Thesis: %s TID: %s%n", internalId, tid);
            }
        }*/

    }

    public void testExcel() throws IOException {
        POIFSFileSystem fs = new POIFSFileSystem(new FileInputStream(new File("test.xls")));
        HSSFWorkbook wb = new HSSFWorkbook(fs);
        HSSFSheet sheet = wb.getSheetAt(0);
        HSSFRow row;
        HSSFCell cell;

        int rows = sheet.getPhysicalNumberOfRows();

        int cols = 0; // No of columns
        int tmp = 0;

        // This trick ensures that we get the data properly even if it doesn't start from first few rows
        for (int i = 0; i < 10 || i < rows; i++) {
            row = sheet.getRow(i);
            if (row != null) {
                tmp = sheet.getRow(i).getPhysicalNumberOfCells();
                if (tmp > cols) {
                    cols = tmp;
                }
            }
        }

        for (int r = 0; r < rows; r++) {
            row = sheet.getRow(r);
            if (row != null) {
                for (int c = 0; c < cols; c++) {
                    cell = row.getCell(c);
                    if (cell != null) {
                        taskLog("%s%n", cell.getStringCellValue());
                    }
                }
            }
        }
    }

}
