package mycode.shipping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.TreeMultiset;
import java.util.concurrent.Semaphore;

public abstract class Shipping extends Thread {

    public Semaphore semaphore;
    public String topNumber;
    public static long pageVolume;
    public static List<String> ngStatus;
    public static List<String> okStatus;
    public static List<String> questionStatus;
    public static List<String> progressStatus;
    public static List<String> statusList;
    public static Set<String> detailSet;
    public static Map<String, String> detailMap;
    public static TreeMultiset<String> statusMultiset;
    public ArrayList<String> numbers = null;

    public Shipping(Semaphore semaphore, String topNumber) {
        this.topNumber = topNumber;
        this.semaphore = semaphore;
    }

    public Shipping(Semaphore semaphore, ArrayList<String> numbers) {
        this.numbers = numbers;
        this.semaphore = semaphore;
    }

    public String next() {
        Long num = ((Long.parseLong(this.topNumber) / 10) + 1L);
        this.topNumber = num + "" + (num % 7);
        return this.topNumber;
    }

    public static ArrayList<String> makeTopNumbers(Long topoftop, int time, long pageVolume) {
        ArrayList<String> topNumbers = new ArrayList<>();
        for (int i = 0; i < time; i++) {
            long mod = topoftop % 7;
            topNumbers.add(topoftop + "" + mod);
            topoftop = topoftop + pageVolume;
        }
        return topNumbers;
    }

    public static String getCode(String status) {
        if (okStatus.contains(status)) {
            return "○";
        } else if (ngStatus.contains(status)) {
            return "×";
        } else if (questionStatus.contains(status)) {
            return "△";
        } else if (progressStatus.contains(status)) {
            return "□";
        } else {
            return "－";
        }
    }

    public static String padding(int i) {
        if (i == 10) {
            return "10";
        } else {
            return "0" + i;
        }
    }

    public static List<String> statusList() {
        return statusList;
    }

    public static Set<String> detailSet() {
        return detailSet;
    }

    public static Map<String, String> detailMap() {
        return detailMap;
    }

    public static TreeMultiset<String> statusMultiset() {
        return statusMultiset;
    }
}
