package mycode.shipping.app;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.TreeMultiset;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import mycode.shipping.Yamato;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFCreationHelper;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFHyperlink;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;

public class DeliverStatus {

    public static TreeMultiset<String> multiset;

    public static void main(String[] args) throws IOException, InterruptedException {
        try {
            CSVReader reader = new CSVReader(new FileReader("【名称変更不可】伝票番号つきデータ統合.csv"));
            List<String[]> readAll = reader.readAll();
            ArrayList<String> numbers = new ArrayList<>();
            for (String[] record : readAll) {
                numbers.add(record[record.length - 1]);
            }
            multiset = TreeMultiset.create();
            Semaphore semaphore = new Semaphore(35);
            Runtime.getRuntime().exec("Msg console /time:5 追跡を開始しました。5～15秒ほどお待ちください。");
            Yamato.runThreads(semaphore, numbers);
            for (String s : Yamato.statusList()) {
                multiset.add(s);
            }

            StringBuilder sb = new StringBuilder("\n" + new Date().toLocaleString() + " 確認分\r\n");
            sb.append("------------------------------内訳------------------------------\r\n\r\n");
            for (String s : multiset.elementSet()) {
                sb.append(s).append("\t").append(multiset.count(s)).append("\r\n");
            }
            sb.append("\r\n-----------------------------返品分-----------------------------\r\n\r\n");
            Iterator<String> iterator = Yamato.detailSet().iterator();
            while (iterator.hasNext()) {
                String next = ((String) iterator.next());
                if (next.contains("返品完了") || next.contains("返品")) {
                    sb.append(next.replace("-", "")).append("\r\n");
                }
            }
            sb.append("\r\n---------------------------貼り付け用---------------------------\r\n\r\n");
            iterator = Yamato.detailSet().iterator();
            while (iterator.hasNext()) {
                sb.append(((String) iterator.next()).replace("-", "")).append("\r\n");
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String format = sdf.format(new Date());
            File file = new File("追跡結果" + format + ".txt");
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(new String(sb));
            }
            HSSFWorkbook wb = new HSSFWorkbook();
            HSSFCellStyle cellStyle = wb.createCellStyle();
            HSSFFont font = wb.createFont();
            font.setUnderline(HSSFFont.U_SINGLE);
            font.setColor(HSSFColor.BLUE.index);
            cellStyle.setFont(font);
            HSSFSheet sheet = wb.createSheet();
            for (int i = 0; i < readAll.size(); i++) {
                String[] record = readAll.get(i);
                HSSFRow row = sheet.createRow(i);
                for (int j = 0; j < record.length; j++) {
                    HSSFCell cell = row.createCell(j);
                    cell.setCellValue(record[j]);
                }
                String denpyo = record[record.length - 1];
                String get = Yamato.detailMap.get(denpyo);
                HSSFCell cell = row.createCell(record.length);
                cell.setCellValue(get.split("\t")[0]);
                cell = row.createCell(record.length + 1);
                cell.setCellValue(get.split("\t")[1]);
                cell = row.createCell(record.length + 2);
                HSSFCreationHelper helper = wb.getCreationHelper();

                HSSFHyperlink hyperlink = helper.createHyperlink(1);
                hyperlink.setAddress("http://jizen.kuronekoyamato.co.jp/jizen/servlet/crjz.b.NQ0010?id=" + denpyo);
                cell.setHyperlink(hyperlink);
                cell.setCellValue("http://jizen.kuronekoyamato.co.jp/jizen/servlet/crjz.b.NQ0010?id=" + denpyo);
                cell.setCellStyle(cellStyle);
            }

            try (FileOutputStream out = new FileOutputStream("追跡結果" + format + ".xls")) {
                wb.write(out);
            }
            Runtime.getRuntime().exec("Msg console 追跡が完了しました。テキストとエクセルを確認してください。");
        } catch (Throwable t) {
            t.printStackTrace();
            Runtime.getRuntime().exec("Msg console 処理は終了しませんでした。\n該当のエクセルを開いている時は閉じて再度実行してください。");
        }
    }
}
