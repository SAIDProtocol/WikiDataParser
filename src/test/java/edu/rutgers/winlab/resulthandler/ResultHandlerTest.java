/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.resulthandler;

import edu.rutgers.winlab.wikidataparser.Tuple2;
import edu.rutgers.winlab.wikidataparser.Tuple3;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jiachen
 */
public class ResultHandlerTest {

    public ResultHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void test1() throws IOException {
        
        Stream<Tuple3<Integer, String, Long>> raw = ResultHandler.readResultFile("/users/jiachen/NetBeansProjects/NeworkSimulator/resultlv6hierarchical/output.txt");

//        Long[] vals = raw.map(t -> t.getV3()).toArray(Long[]::new);
//        Arrays.sort(vals);
////        ArrayList<Tuple2<Long, Double>> cdf = ResultHandler.getFullCDF(vals);
//        ArrayList<Tuple2<Long, Double>> cdf = ResultHandler.getSampledCDF(vals, 4000);
//        Files.write(Paths.get("/users/jiachen/NetBeansProjects/NeworkSimulator/outputCDF.txt"),
//                (Iterable<String>) cdf.stream().map(t -> String.format("%d\t%.10f", t.getV1(), t.getV2()))::iterator);
//
//        Files.write(Paths.get("/users/jiachen/NetBeansProjects/NeworkSimulator/outputPerPacket.txt"),
//                (Iterable<String>) ResultHandler.getPerPacketLatency(raw)
//                        .map(e -> String.format("%d\t%d\t%d\t%.10f", e.getKey(), e.getValue().getV1(), e.getValue().getV2(), e.getValue().getV3()))::iterator
//        );
        ResultHandler.getPerPacketLatency(raw);
    }

}
