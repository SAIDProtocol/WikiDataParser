package edu.rutgers.winlab.resulthandler;

import edu.rutgers.winlab.wikidataparser.Tuple2;
import edu.rutgers.winlab.wikidataparser.Tuple3;
import edu.rutgers.winlab.wikidataparser.Tuple4;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author jiachen
 */
public class ResultHandler {

    // resultFile: pubId subId latency
    public static Stream<Tuple3<Integer, String, Long>> readResultFile(String resultFileName) throws IOException {
        return Files.lines(Paths.get(resultFileName))
                .map(l -> {
                    String[] parts = l.split("\t");
                    assert parts.length == 3;
                    int pubId = Integer.parseInt(parts[0]);
                    String subId = parts[1];
                    long time = Long.parseLong(parts[2]);
                    return new Tuple3<>(pubId, subId, time);
                });
    }

    public static <T> ArrayList<Tuple2<T, Double>> getFullCDF(T[] sortedValues) {
        ArrayList<Tuple2<T, Double>> ret = new ArrayList<>();
        double step = 1.0 / (sortedValues.length - 1);

        ret.add(new Tuple2<>(sortedValues[0], 0.0));

        for (int i = 1; i < sortedValues.length - 1; i++) {
            if (Objects.equals(sortedValues[i - 1], sortedValues[i])
                    && Objects.equals(sortedValues[i + 1], sortedValues[i])) {
                continue;
            }
            ret.add(new Tuple2<>(sortedValues[i], step * i));
        }
        ret.add(new Tuple2<>(sortedValues[sortedValues.length - 1], 1.0));
        return ret;
    }

    public static <T> ArrayList<Tuple2<T, Double>> getSampledCDF(T[] sortedValues, int pointCount) {
        ArrayList<Tuple2<T, Double>> ret = new ArrayList<>();
        double cStep = 1.0 / (pointCount - 1), vStep = sortedValues.length * 1.0 / pointCount;

        ret.add(new Tuple2<>(sortedValues[0], 0.0));

        for (int i = 1; i < pointCount - 1; i++) {
            ret.add(new Tuple2<>(sortedValues[(int) (i * vStep)], i * cStep));
        }
        ret.add(new Tuple2<>(sortedValues[sortedValues.length - 1], 1.0));

        return ret;
    }

    // rawResult: pubId subId latency
    // return: pId -> (min, max, avg)
    public static Stream<Entry<Integer, Tuple3<Long, Long, Double>>> getPerPacketLatency(Stream<Tuple3<Integer, String, Long>> rawResult) {
        // min, max, sum, count
        Tuple2<Long, Integer> total = new Tuple2<>(0L, 0);
        HashMap<Integer, Tuple4<Long, Long, Long, Integer>> tmp = new HashMap<>();
        rawResult.forEach(t -> {
            int pubId = t.getV1();
            long latency = t.getV3();
            total.setV1(total.getV1() + latency);
            total.setV2(total.getV2() + 1);
            Tuple4<Long, Long, Long, Integer> res = tmp.get(pubId);
            if (res == null) {
                tmp.put(pubId, res = new Tuple4<>(Long.MAX_VALUE, Long.MIN_VALUE, 0L, 0));
            }
            if (latency < res.getV1()) {
                res.setV1(latency);
            }
            if (latency > res.getV2()) {
                res.setV2(latency);
            }
            res.setV3(res.getV3() + latency);
            res.setV4(res.getV4() + 1);
        });
        System.out.printf("total: %,d, count: %,d%n", total.getV1(), total.getV2());
        return tmp.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(),
                e -> new Tuple3<>(e.getValue().getV1(), e.getValue().getV2(), ((double) e.getValue().getV3()) / e.getValue().getV4())))
                .entrySet().stream().sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()));
    }

}
