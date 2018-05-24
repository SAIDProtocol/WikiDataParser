package edu.rutgers.winlab.topologyhandler;

import edu.rutgers.winlab.wikidataparser.WikiParser;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jiachen
 */
public class TopologyHandlerTest {

    public TopologyHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Test
    public void testReadNodes() throws IOException {
        String topologyPrefix = "/users/jiachen/Rocketfuel/1221/";
        String wikiPrefix = "/users/jiachen/Wiki/";

//        HashMap<String, Node> nodes = TopologyHandler.readNodes(topologyPrefix + "coreNodes.txt");
//        nodes.values().forEach(n -> System.out.printf("n=%s, c=%s, t=%s%n", n.getName(), n.getCity(), n.getType()));
//        HashMap<String, HashSet<Node>> cityNodes = TopologyHandler.cityNodes(nodes);
//        Map<String, Long> cityNodeCounts = cityNodes.entrySet().stream()
//                .collect(Collectors.toMap(
//                        e -> e.getKey(),
//                        e -> e.getValue().stream().filter(n -> n.getType() == Node.NodeType.Core).count()
//                ));
//
//        cityNodeCounts.forEach((k, v) -> System.out.printf("%s %d%n", k, v));
//        HashSet<Link> links = TopologyHandler.readLinks(topologyPrefix + "coreLatencies.txt", nodes);
//        links.forEach(l->System.out.printf("%s %s: %d%n", l.getN1().getName(), l.getN2().getName(), l.getLatencyMs()));
//        HashSet<Node> newNodes = new HashSet<>();
//        HashSet<Link> newLinks = new HashSet<>();
//        TopologyHandler.addEdgeNodes(nodes, 7, 2, 8, newNodes, newLinks);
//        nodes.putAll(newNodes.stream().collect(Collectors.toMap(e -> e.getName(), e -> e)));
//        links.addAll(newLinks);
//        TopologyHandler.writeNodes(nodes, topologyPrefix + "allNodes7.txt");
//        TopologyHandler.writeLinks(links, topologyPrefix + "allLinks7.txt");
        HashMap<String, Node> nodes = TopologyHandler.readNodes(topologyPrefix + "allNodes7.txt");
//        HashSet<Link> links = TopologyHandler.readLinks(topologyPrefix + "allLinks7.txt", nodes);
//
//        nodes.values().stream()
//                .collect(Collectors.toMap(n -> n, n -> TopologyHandler.getNodeTotalDistance(links, n).values().stream().reduce(Integer::sum).get()))
//                .forEach((n, v) -> System.out.printf("%s\t%d%n", n.getName(), v));
        String subscriptionFile = wikiPrefix + "subset_cat_subscriptions.txt";

        Node[] edgeNodes = nodes.values().stream().filter(n -> n.getType() == Node.NodeType.Edge).toArray(Node[]::new);

        String[] lines = Files.lines(Paths.get(subscriptionFile)).toArray(String[]::new);
        int subscriberPerEdgeNode = (int) Math.ceil(lines.length * 1.0 / edgeNodes.length);
        System.out.printf("subs per node: %d%n", subscriberPerEdgeNode);

        Node[] nodePool = new Node[edgeNodes.length * subscriberPerEdgeNode];
        for (int i = 0; i < subscriberPerEdgeNode; i++) {
            System.arraycopy(edgeNodes, 0, nodePool, i * edgeNodes.length, edgeNodes.length);
        }
        Node[] sortedNodes = WikiParser.randomSort(Stream.of(nodePool), 0).toArray(Node[]::new);
        try (PrintStream ps = new PrintStream(wikiPrefix + "subset_location_cat_subscriptions.txt")) {
            for (int i = 0; i < lines.length; i++) {
                ps.printf("%s\t%s%n", sortedNodes[i].getName(), lines[i]);
            }
            ps.flush();
        }

    }

}
