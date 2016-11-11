package com.HKUST.gMission.SpacialCroudsourcing.assign;

import com.HKUST.gMission.SpacialCroudsourcing.Point;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.AssignmentHistory;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralTask;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralWorker;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.WTMatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.*;

public class RDB_SC_Sampling {

    private static BigDecimal One = new BigDecimal("1", MathContext.DECIMAL128);
    private static BigDecimal Two = new BigDecimal("2");
    private static BigDecimal Zero = new BigDecimal("0");
    private static BigInteger OneInt = new BigInteger("1");
    private static BigInteger TwoInt = new BigInteger("2");
    private static BigInteger ZeroInt = new BigInteger("0");

    // maps of tasks and workers
    private static Map<Integer, GeneralTask> taskMap = new HashMap<Integer, GeneralTask>();
    private static Map<Integer, GeneralWorker> workerMap = new HashMap<Integer, GeneralWorker>();

    // available pairs of worker-tasks, maps worker index to task indexes
    private static Map<Integer, List<Integer>> availableTasks = new HashMap<Integer, List<Integer>>();

    // list of temporal result, maps task index to worker indexes
    private static List<Map<Integer, List<Integer>>> tempResult = new ArrayList<Map<Integer, List<Integer>>>();

    private static List<Double> sd = new ArrayList<Double>();

    private static List<Double> td = new ArrayList<Double>();

    private static List<Double> minReliability = new ArrayList<Double>();

    private static AssignmentHistory history;

    public static Logger logger = LogManager.getLogger();

    /**
     * give an assignment X that P(X > (1-portion)Max) > confidence
     *
     * @param tasks
     * @param workers
     * @param currentTime
     * @param portion portion=0.2 means the answer is expected to be the 20% best answers
     * @param confidence
     * @return a list of WTMatch
     */
    public static List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers, long currentTime,
                                       String portion, String confidence, AssignmentHistory history) {
        taskMap.clear();
        workerMap.clear();
        availableTasks.clear();
        tempResult.clear();
        sd.clear();
        td.clear();
        minReliability.clear();
        RDB_SC_Sampling.history = history;

        // build maps
        for (GeneralTask t : tasks) {
            taskMap.put(t.id, t);
        }

        for (GeneralWorker w : workers) {
            workerMap.put(w.id, w);
        }

        // compute all possible pairs
        for (GeneralTask t : tasks) {
            for (GeneralWorker w : workers) {
                if (canReach(w, t, currentTime)) {
                    if (!availableTasks.containsKey(w.id)) {
                        availableTasks.put(w.id, new ArrayList<Integer>());
                    }
                    availableTasks.get(w.id).add(t.id);
                }
            }
        }

        // compute K
        // BigDecimal total = One;
        // firstly compute possibility p that one possible world is picked
        // double p = 1.0;
        // for (int i : availableTasks.keySet()) {
            // p /= availableTasks.get(i).size();
            // logger.info("[RDB SC TEST][available task of worker " + i + " is " + availableTasks.get(i).size() + "]");
            // total = total.multiply(new BigDecimal(availableTasks.get(i).size()));
        // }
        BigInteger k = calculateK(null, portion, confidence);
        if (k.compareTo(ZeroInt) == 0) {
            k = OneInt;
        }

        // perform k samplings
        while (k.compareTo(ZeroInt) > 0) {
            AssignmentHistory tempHistory = new AssignmentHistory();
            Map<Integer, List<Integer>> pick = new HashMap<Integer, List<Integer>>();
            List<Integer> taskIds = new ArrayList<Integer>();
            for (GeneralWorker w : workers) {
                if (availableTasks.containsKey(w.id) && availableTasks.get(w.id).size() > 0) {
                    taskIds.clear();
                    taskIds.addAll(availableTasks.get(w.id));
                    // remove full tasks
                    boolean cont = true;
                    while (cont) {
                        cont = false;
                        int idx = 0;
                        for (int i = 0; i < taskIds.size(); i++) {
                            int tid = taskIds.get(i);
                            if (tempHistory.getAssignedNumOfTask(tid) >= taskMap.get(tid).getRequirement()) {
                                cont = true;
                                idx = i;
                            }
                        }
                        if (cont) {
                            taskIds.remove(idx);
                        }
                    }
                    while (taskIds.size() > w.getCapacity()) {
                        int taskIdx = ((int) Math.floor(Math.random() * taskIds.size()));
                        taskIds.remove(taskIdx);
                    }
                    for (int tId : taskIds) {
                        if (!pick.containsKey(tId)) {
                            pick.put(tId, new ArrayList<Integer>());
                        }
                        pick.get(tId).add(w.id);
                        tempHistory.add(w.id, tId);
                    }
                }
            }
            sd.add(calculateSD(pick));
            td.add(calculateTD(pick));
            minReliability.add(calculateMinReliability(pick));
            tempResult.add(pick);
            k = k.subtract(OneInt);
        }

        int maxDominantScore = 0;
        int bestIdx = 0;
        for (int i = 0; i < tempResult.size(); i++) {
            int dominantScore = 0;
            for (int j = 0; j < tempResult.size(); j++) {
                if (dominant(i, j)) {
                    dominantScore++;
                }
            }
            if (dominantScore > maxDominantScore) {
                maxDominantScore = dominantScore;
                bestIdx = i;
            }
        }

        List<WTMatch> result = new ArrayList<WTMatch>();
        for (int taskId : tempResult.get(bestIdx).keySet()) {
            for (int workerId : tempResult.get(bestIdx).get(taskId)) {
                result.add(new WTMatch(taskId, workerId));
            }
        }
        return result;
    }

    private static boolean dominant(int i, int j) {
        return td.get(i) >= td.get(j)
                && sd.get(i) >= sd.get(j)
                && minReliability.get(i) >= minReliability.get(j);
    }

    private static Double calculateMinReliability(Map<Integer, List<Integer>> pick) {
        double minR = -1;
        for (int taskId : pick.keySet()) {
            double tempR = 1;
            for (int workerId : pick.get(taskId)) {
                GeneralWorker w = workerMap.get(workerId);
                tempR *= 1 - w.reliability;
            }
            if (minR < 0 || minR > 1 - tempR) {
                minR = 1 - tempR;
            }
        }
        return minR;
    }

    private static double calculateSD(Map<Integer, List<Integer>> pick) {
        double sd = 0;
        List<Double> angles = new ArrayList<Double>();
        for (int taskId : pick.keySet()) {
            angles.clear();
            for (int workerId : pick.get(taskId)) {
                GeneralWorker w = workerMap.get(workerId);
                GeneralTask t = taskMap.get(taskId);
                double angle = Math.atan2(t.longitude - w.longitude, t.latitude - w.latitude);
                if (angle < 0) {
                    angle += Math.PI * 2;
                }
                angles.add(angle);
            }
            Collections.sort(angles);
            for (int i = 0; i < angles.size(); i++) {
                int j = (i + 1) % angles.size();
                double dAngle = (angles.get(j) - angles.get(i)) / (Math.PI * 2);
                if (dAngle < 0) {
                    dAngle = -dAngle;
                }
                sd += -dAngle * Math.log(dAngle);
            }
        }
        return sd;
    }

    private static double calculateTD(Map<Integer, List<Integer>> pick) {
        double td = 0;
        List<Double> times = new ArrayList<Double>();
        for (int taskId : pick.keySet()) {
            times.clear();
            GeneralTask t = taskMap.get(taskId);
            times.add((double) t.arrivalTime);
            times.add((double) t.expiryTime);
            for (int workerId : pick.get(taskId)) {
                GeneralWorker w = workerMap.get(workerId);
                double distance = Math.sqrt((w.latitude - t.latitude) * (w.latitude - t.latitude)
                        + (w.longitude - t.longitude) * (w.longitude - t.longitude));
                // todo shoule be real time
                double time = distance;
                times.add(time);
            }
            Collections.sort(times);
            for (int i = 0; i < times.size() - 1; i++) {
                int j = i + 1;
                double dTime = (times.get(j) - times.get(i)) / (t.expiryTime - t.arrivalTime);
                td += -dTime * Math.log(dTime);
            }
        }
        return td;
    }

    public static boolean canReach(GeneralWorker w, GeneralTask t, double currentTime) {
        // see if the distance is too long
        return w.region.contains(new Point(t.longitude, t.latitude)) && !history.has(w.id, t.id);
        /*
        double distance = Math.sqrt((w.latitude - t.latitude) * (w.latitude - t.latitude)
                + (w.longitude - t.longitude) * (w.longitude - t.longitude));
        if (w.velocity * (t.expiryTime - currentTime) < distance || w.velocity * (t.arrivalTime - currentTime) > distance) {
            return false;
        }

        // see if the task is in the angle of the direction of the worker
        double angle = Math.atan2(t.longitude - w.longitude, t.latitude - w.latitude);
        if (angle < 0) {
            angle += Math.PI * 2;
        }

        return (angle >= w.minDirection && angle <= w.maxDirection)
                || (w.minDirection < 0 && angle >= w.minDirection + Math.PI * 2);
        */
    }

    public static boolean canReach(GeneralWorker w, GeneralTask t, double currentTime, AssignmentHistory history) {
        // see if the distance is too long
        return w.region.contains(new Point(t.longitude, t.latitude)) && !history.has(w.id, t.id);
    }

    /**
     *
     * @param total total number of possible worlds. N in the paper
     * @param portion E in the paper
     * @param confidence ks in the paper
     * @return K
     */
    public static BigInteger calculateK(BigDecimal total, String portion, String confidence) {
//        logger.info("[RDB SC Sampling][start compute K]" +
//                    "[portion = " + portion + "][confidence = " + confidence + "]");
        /*
        // according to the paper, p is equal to 1/N
        // original formula is (p*(1-portion)*N*e - 1 + p) / (1 - p + e * p)
        // now can be changed to ((1-portion)*e - 1 + p) / (1 - p + e * p)
        // ..................... (N*(1-portion)*e - N + 1) / (N - 1 + e)
        BigDecimal temp = new BigDecimal(1-portion);
        BigDecimal E = new BigDecimal(Math.E);
        BigDecimal min = temp.multiply(E).multiply(total).subtract(total).add(One).
                divide(total.subtract(One).add(E), 20, BigDecimal.ROUND_DOWN);
        BigDecimal max = total.multiply(temp);
        BigInteger M = max.toBigInteger();
        BigDecimal K;
        BigDecimal thre = new BigDecimal("100");
        while (max.compareTo(thre) > 0 && min.compareTo(max) < 0) {
            K = min.add(max).divide(Two, 20, BigDecimal.ROUND_DOWN);
            if (Pr(K.toBigInteger(), M, total.toBigInteger()) > 1 - confidence) {
                min = K.add(One);
            } else {
                max = K;
            }
        }
        logger.info("[RDB SC Sampling][K = " + max.toPlainString() + "]");
        */
        // K is usually very small (less than 30)
        // use a good approximate to get a slightly bigger K efficiently.
        long result = 1;
        // never use the constructor of double value
        BigDecimal currentConf = new BigDecimal(portion, MathContext.DECIMAL128);
        BigDecimal target = new BigDecimal(confidence, MathContext.DECIMAL128);
        while (currentConf.compareTo(target) > 0) {
            // logger.info("[result = " + result + ", cf = " + currentConf.toPlainString() + ", c = " + target.toPlainString() + "]");
            result += 1;
            currentConf = currentConf.multiply(currentConf, MathContext.DECIMAL128);
        }
        //return max.toBigInteger().add(OneInt);
        if (result < 110) {
            result = 110;
        }
//        logger.info("[RDB SC Sampling][K = " + result + "]");
        return new BigInteger(String.valueOf(result));
    }

    /**
     * calculate the possibility P(the result <= the rth best answer after k
     * times)
     *
     * @param k     choose k times, note that k < r
     * @param r     to get the rth best answer. r in the paper
     * @param total total number of answers, equal to 1/p
     * @return the possibility
     */
    public static double Pr(BigInteger k, BigInteger r, BigInteger total) {
//        logger.info("[RDB SC Sampling][start compute Pr][k = " + k.toString() + "][r = " + r.toString() + "]" +
//                    "[total = " + total.toString() + "]");
        double ans = 0;
        /*
        * calculate C(r, k) * (1-p)^n * (p/(1-p))^k
        *           P = (1-p)^n * (p/(1-p))^k * r!/(k!*(r-k)!)
        * rename as P = (1-p)^np * (p/(1-p))^np1 * n1!/m1!m2!
        * note that result < 1, use number of factors to get accurate result.
        */
        BigDecimal n = new BigDecimal(total);
        BigDecimal result = One;
        BigInteger npc = total;
        BigDecimal np = n.subtract(One).divide(n, 20, BigDecimal.ROUND_DOWN);
        BigInteger np1c = k;
        BigDecimal np1 = One.divide(n.subtract(One), 20, BigDecimal.ROUND_DOWN);
        BigInteger n1c = r;
        BigDecimal n1 = new BigDecimal(n1c);
        BigInteger m1c = k;
        BigDecimal m1 = new BigDecimal(m1c);
        BigInteger m2c = r.subtract(k);
        BigDecimal m2 = new BigDecimal(m2c);

        while (npc.compareTo(ZeroInt) > 0 || np1c.compareTo(ZeroInt) > 0 || n1c.compareTo(OneInt) > 0 ||
                m1c.compareTo(OneInt) > 0 || m2c.compareTo(OneInt) > 0) {
//            if (npc.mod(new BigInteger("10000")).compareTo(ZeroInt) == 0) {
//                logger.info("[TEST][npc " + npc.toString() + "]");
//            }
            if (n1c.compareTo(OneInt) > 0) {
                result = result.multiply(n1);
                // logger.info("[multi " + n1.toPlainString() + " = " + result.toPlainString() + "]");
                n1c = n1c.subtract(OneInt);
                n1 = n1.subtract(One);
            }
            if (m1c.compareTo(OneInt) > 0) {
                result = result.divide(m1, 20, BigDecimal.ROUND_DOWN);
                // logger.info("[divide " + m1.toPlainString() + " = " + result.toPlainString() + "]");
                m1c = m1c.subtract(OneInt);
                m1 = m1.subtract(One);
            }
            if (m2c.compareTo(OneInt) > 0) {
                result = result.divide(m2, 20, BigDecimal.ROUND_DOWN);
                // logger.info("[divide " + m2.toPlainString() + " = " + result.toPlainString() + "]");
                m2c = m2c.subtract(OneInt);
                m2 = m2.subtract(One);
            }
            if (npc.compareTo(ZeroInt) > 0) {
                result = result.multiply(np);
                // logger.info("[multi " + np.toPlainString() + " = " + result.toPlainString() + "]");
                npc = npc.subtract(OneInt);
            }
            if (np1c.compareTo(ZeroInt) > 0) {
                result = result.multiply(np1);
                // logger.info("[multi " + np1.toPlainString() + " = " + result.toPlainString() + "]");
                np1c = np1c.subtract(OneInt);
            }
        }
        /*
        for (long x = 1; x <= r; x++) {
            // calculate P(result == the rth best answer)
            double temp = 1;

            long n = k - 1;
            long m1 = x - 1;
            long m2 = k - x;
            long np = k;
            long np1 = n - k;
            while (n > 1 || m1 > 1 || m2 > 1 || np > 0 || np1 > 0) {
                logger.info("[n m1 m2 np np1 : " + n + "" + m1 + "" + m2 + "" + np + "" + np1 + "]");
                while (temp <= 1 && n > 1) {
                    temp *= n;
                    n--;
                }
                while ((temp >= 1 || n == 1) && m1 > 1) {
                    temp /= m1;
                    m1--;
                }
                while ((temp >= 1 || n == 1) && m2 > 1) {
                    temp /= m2;
                    m2--;
                }
                while ((temp >= 1 || n == 1) && np > 0) {
                    temp /= total;
                    np--;
                }
                while ((temp >= 1 || n == 1) && np1 > 0) {
                    temp = temp * (total - 1) / total;
                    np1--;
                }
            }
            ans += temp;
        }
        */
//        logger.info("[RDB SC Sampling][Pr = " + result.doubleValue() + "]");
        return result.doubleValue();
    }
}
