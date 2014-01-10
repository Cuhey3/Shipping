package mycode.shipping;

import com.google.common.collect.TreeMultiset;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Sagawa extends Shipping {

    static Pattern dayPattern = Pattern.compile("\\d{4}年\\d{1,2}月\\d{1,2}日");

    static {
        ngStatus = Arrays.asList(new String[]{"返品", "返品完了"});
        okStatus = Arrays.asList(new String[]{"配達終了", "配達完了", "投函完了", "配達完了（宅配ボックス）"});
        questionStatus = Arrays.asList(new String[]{"調査中（ご不在）", "調査中（転居先不明）", "持戻（ポスト投函不可）", "持戻（住所不明）", "調査中", "持戻（転居）"});
        progressStatus = Arrays.asList(new String[]{"作業店通過", "依頼受付（再配達）", "持戻", "持戻（ご不在）", "持戻（休業）", "配達中", "配達予定", "配達店到着", "配達日・時間帯指定（保管中）", "保管中", "投函予定", "発送", "依頼受付（日・時間帯変更）", "ご来店予定（保管中）", "保管中（ご指定店）"});
        statusList = Collections.synchronizedList(new ArrayList());
        detailSet = Collections.synchronizedSet(new TreeSet<String>());
        detailMap = Collections.synchronizedMap(new TreeMap<String, String>());
        statusMultiset = TreeMultiset.create();
    }

    public Sagawa(Semaphore semaphore, ArrayList<String> numbers) {
        super(semaphore, numbers);
    }

    public static void runThreads(Semaphore semaphore, ArrayList<String> allNumber, int formSize, int executeSize) throws InterruptedException {
        ArrayList<Sagawa> sagawas = new ArrayList<>();
        ArrayList<String> subArray = new ArrayList();
        while (!allNumber.isEmpty()) {
            subArray.add(allNumber.remove(0));
            if (subArray.size() == formSize) {
                Sagawa sagawa = new Sagawa(semaphore, new ArrayList(subArray));
                subArray.clear();
                sagawas.add(sagawa);
                sagawa.start();
                if (sagawas.size() == executeSize) {
                    for (Sagawa y : sagawas) {
                        y.join();
                    }
                    sagawas.clear();
                }
            }
        }
        if (!subArray.isEmpty()) {
            Sagawa sagawa = new Sagawa(semaphore, subArray);
            sagawas.add(sagawa);
            sagawa.start();
        }
        for (Sagawa y : sagawas) {
            y.join();
        }
        for (String s : statusList) {
            statusMultiset.add(s);
        }
    }

    @Override
    public void run() {
        try {
            semaphore.acquire();
            StringBuilder url = new StringBuilder("http://k2k.sagawa-exp.co.jp/p/web/okurijosearch.do?okurijoNo=");
            for (String n : this.numbers) {
                url.append(n).append(",");
            }
            Connection connect = Jsoup.connect(new String(url).replaceFirst(",$", ""));
            Document post;
            while (true) {
                try {
                    post = connect.post();
                    break;
                } catch (IOException t) {
                }
            }
            post.select(".ichiran-table-header").remove();
            Elements outline = post.select(".ichiran-bg-toiawase_meisai tr .ichiran-fg-src-2");
            Elements detail = post.select(".ichiran-bg.syosai-bg-src");
            for (int i = 0; i < outline.size(); i++) {
                Element ol = outline.get(i);
                Element dt = detail.get(i);
                String detailText = dt.select(".syosai-dt1-src").eq(7).text();
                Matcher matcher = dayPattern.matcher(detailText);
                String day = "";
                if (matcher.find()) {
                    day = matcher.group(0);
                }
                String denpyo = ol.text();
                String status = ol.nextElementSibling().nextElementSibling().text();
                if (!status.isEmpty()) {
                    statusList.add(getCode(status) + "\t" + status);
                }
                detailSet.add(denpyo + "\t" + status + "\t" + day + "\t");
                detailMap.put(denpyo, status + "\t" + day);
            }
            semaphore.release();
        } catch (InterruptedException ex) {
            Logger.getLogger(Sagawa.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
