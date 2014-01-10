package mycode.shipping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Yubin extends Shipping {

    static List<String> skip = Arrays.asList(new String[]{"srv_searchActionForm", "srv_sequenceNoSearchActionForm", "error", "pathDetail"});

    public Yubin(Semaphore semaphore, String topNumber) {
        super(semaphore, topNumber);
    }

    public static void runThreads(Semaphore semaphore, ArrayList<String> topNumbers) throws InterruptedException {
        ArrayList<Yubin> yubins = new ArrayList<>();
        for (String s : topNumbers) {
            Yubin yubin = new Yubin(semaphore, s);
            yubins.add(yubin);
            yubin.start();
        }
        for (Yubin y : yubins) {
            y.join();
        }
    }

    @Override
    public void run() {
        try {
            semaphore.acquire();
            Document get = null;
            while (get == null) {
                try {
                    get = Jsoup.connect("https://trackings.post.japanpost.jp/services/srv/sequenceNoSearch/")
                            .data("requestNo", this.topNumber)
                            .data("count", "100")
                            .data("sequenceNoSearch", "追跡スタート")
                            .data("locale", "ja")
                            .get();
                } catch (Throwable t) {
                }
            }
            if (get != null) {
                Elements anchor = get.select("[summary=照会結果] a");
                for (Element e : anchor) {
                    Elements tds = e.parent().parent().select("td");
                    System.out.println(tds.get(2).text() + "\t" + tds.get(3).text() + "\t" + tds.get(0).text());
                }
            }
            semaphore.release();
        } catch (InterruptedException ex) {
            Logger.getLogger(Yubin.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
