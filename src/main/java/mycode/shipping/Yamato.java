package mycode.shipping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import static mycode.shipping.Shipping.detailMap;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Yamato extends Shipping {

    static {
        ngStatus = Arrays.asList(new String[]{"返品", "返品完了"});
        okStatus = Arrays.asList(new String[]{"配達完了", "投函完了", "配達完了（宅配ボックス）"});
        questionStatus = Arrays.asList(new String[]{"調査中（ご不在）", "調査中（転居先不明）", "持戻（ポスト投函不可）", "持戻（住所不明）", "調査中", "持戻（転居）"});
        progressStatus = Arrays.asList(new String[]{"作業店通過", "依頼受付（再配達）", "持戻", "持戻（ご不在）", "持戻（休業）", "配達中", "配達予定", "配達店到着", "配達日・時間帯指定（保管中）", "保管中", "投函予定", "発送", "依頼受付（日・時間帯変更）", "ご来店予定（保管中）", "保管中（ご指定店）"});
        statusList = Collections.synchronizedList(new ArrayList());
        detailMap = Collections.synchronizedMap(new TreeMap<String, String>());
        detailSet = Collections.synchronizedSet(new TreeSet<String>());
    }

    public Yamato(Semaphore semaphore, String topNumber) {
        super(semaphore, topNumber);
    }

    public Yamato(Semaphore semaphore, ArrayList<String> numbers) {
        super(semaphore, numbers);
    }

    public static void runThreads(Semaphore semaphore, ArrayList<String> numbers) throws InterruptedException {
        ArrayList<Yamato> yamatos = new ArrayList<>();
        ArrayList<String> subArray = new ArrayList();
        while (!numbers.isEmpty()) {
            subArray.add(numbers.remove(0));
            if (subArray.size() == 10) {
                Yamato yamato = new Yamato(semaphore, new ArrayList(subArray));
                subArray.clear();
                yamatos.add(yamato);
                yamato.start();
                if (yamatos.size() == 1000) {
                    System.out.println(numbers.size());
                    for (Yamato y : yamatos) {
                        y.join();
                    }
                    yamatos.clear();
                }
            }
        }
        if (!subArray.isEmpty()) {
            Yamato yamato = new Yamato(semaphore, subArray);
            yamatos.add(yamato);
            yamato.start();
        }
        for (Yamato y : yamatos) {
            y.join();
        }
    }

    @Override
    public void run() {
        try {
            semaphore.acquire();
            Connection connect = Jsoup.connect("http://toi.kuronekoyamato.co.jp/cgi-bin/tneko");
            if (this.numbers != null) {
                connect.data("number00", "1");
                for (int i = 0; i < this.numbers.size(); i++) {
                    connect.data("number" + padding(i + 1), this.numbers.get(i));
                }

            } else {
                connect.data("number00", "1").data("number01", this.topNumber);
                for (int i = 2; i <= 10; i++) {
                    String next = this.next();
                    connect.data("number" + padding(i), next);
                }
            }
            Document post;
            while (true) {
                try {
                    post = connect.post();
                    break;
                } catch (Throwable t) {
                }
            }
            Elements select = post.select(".ichiran tr .denpyo");
            for (Element e : select) {
                String denpyo = e.text();
                String status = e.nextElementSibling().nextElementSibling().text();
                String day = e.nextElementSibling().text();
                if (!status.isEmpty()) {
                    statusList.add(getCode(status) + "\t" + status);
                }
                if (ngStatus.contains(status)) {
                    Elements details = post.select(".bold:contains(" + denpyo + ")").first().parent().parent().parent().nextElementSibling().nextElementSibling().nextElementSibling().nextElementSibling().nextElementSibling().select(".odd,.even");
                    StringBuilder sb = new StringBuilder();
                    for (Element tr : details) {
                        if (sb.length() == 0) {
                            sb.append(tr.select("td:eq(1)").text());
                        } else {
                            sb.append("→").append(tr.select("td:eq(1)").text());
                        }
                    }
                    detailSet.add(denpyo + "\t" + status + "\t" + day + "\t" + new String(sb).replaceFirst("^(発送→|投函予定→|荷物受付→)+", "").replaceAll("(作業店通過→)+", "作業店通過→").replaceFirst("(→返品|→投函予定)+$", ""));
                    detailMap.put(denpyo.replace("-", ""), status + "\t" + day);
                } else {
                    detailSet.add(denpyo + "\t" + status + "\t" + day + "\t");
                    detailMap.put(denpyo.replace("-", ""), status + "\t" + day);
                }
            }
            semaphore.release();
        } catch (InterruptedException ex) {
            Logger.getLogger(Yamato.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
