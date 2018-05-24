package edu.rutgers.winlab.topologyhandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author jiachen
 */
public class TopologyHandler {

    public static HashMap<String, Node> readNodes(String nodesFile) throws IOException {
        HashMap<String, Node> ret = new HashMap<>();
        Files.lines(Paths.get(nodesFile)).forEach(l -> {
            String[] parts = l.split("\t");
            assert parts.length == 3;
            String nodeName = parts[0];
            String city = parts[1];
            Node.NodeType type = Node.NodeType.fromString(parts[2]);
            Node n = new Node(nodeName, city, type);
            Node orig = ret.put(nodeName, n);
            assert orig == null : "Duplicate node name: " + nodeName;
        });
        return ret;
    }

    public static HashMap<String, HashSet<Node>> cityNodes(HashMap<String, Node> nodes) {
        HashMap<String, HashSet<Node>> ret = new HashMap<>();
        nodes.values().forEach(n -> {
            HashSet<Node> cityNodes = ret.get(n.getCity());
            if (cityNodes == null) {
                ret.put(n.getCity(), cityNodes = new HashSet<>());
            }
            cityNodes.add(n);
        });
        return ret;
    }

    public static void writeNodes(HashMap<String, Node> nodes, String fileName) throws IOException {
        Stream<String> stream = nodes.values().stream().
                map(n -> String.format("%s\t%s\t%s", n.getName(), n.getCity(), n.getType().getShortString()));
        Files.write(Paths.get(fileName), (Iterable<String>) stream::iterator);
    }

    public static HashSet<Link> readLinks(String linkFile, HashMap<String, Node> nodes) throws IOException {
        HashSet<Link> links = new HashSet<>();
        Files.lines(Paths.get(linkFile)).forEach(line -> {
            String[] parts = line.split("\t");
            Node n1 = nodes.get(parts[0]);
            assert n1 != null : "Cannot find node: " + parts[0];
            Node n2 = nodes.get(parts[1]);
            assert n2 != null : "Cannot find node: " + parts[1];
            int latency = Integer.parseInt(parts[2]);
            Link link = new Link(n1, n2, latency);
            boolean b = links.add(link);
            assert b : "Duplicate links " + parts[0] + "," + parts[1] + ":" + parts[2];
        });
        return links;
    }

    public static void writeLinks(HashSet<Link> links, String fileName) throws IOException {
        Stream<String> stream = links.stream()
                .map(l -> String.format("%s\t%s\t%d", l.getN1().getName(), l.getN2().getName(), l.getLatencyMs()));
        Files.write(Paths.get(fileName), (Iterable< String>) stream::iterator);
    }

    public static void addEdgeNodes(HashMap<String, Node> coreNodes, int edgePerCoreNode, int minLatency, int maxLatency, HashSet<Node> newNodes, HashSet<Link> newLinks) {

        Map<String, Node[]> cityNodes = TopologyHandler.cityNodes(coreNodes)
                .entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().stream().filter(n -> n.getType() == Node.NodeType.Core).toArray(Node[]::new)));
        Map<String, Integer> cityNodeCounts = cityNodes.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().length));
        cityNodeCounts.forEach((city, count) -> {
            for (long i = 0; i < count * edgePerCoreNode; i++) {
                String nodeName = city + "_" + (i + 1);
                Node n = new Node(nodeName, city, Node.NodeType.Edge);
                boolean b = newNodes.add(n);
                assert b;
            }
        });
        Random rand = new Random(0);
        Map<Node, Integer> nodeLinks = coreNodes.values().stream().filter(n -> n.getType() == Node.NodeType.Core)
                .collect(Collectors.toMap(e -> e, e -> 0));

        newNodes.forEach(n -> {
            Node[] nodes = cityNodes.get(n.getCity());
            int v1 = rand.nextInt(nodes.length);
            int v2 = v1;
            while (v2 == v1) {
                v2 = rand.nextInt(nodes.length);
            }
            Node n1 = nodes[v1], n2 = nodes[v2];
            nodeLinks.merge(n1, 1, Integer::sum);
            nodeLinks.merge(n2, 1, Integer::sum);
            newLinks.add(new Link(n, n1, rand.nextInt(maxLatency - minLatency) + minLatency));
            newLinks.add(new Link(n, n2, rand.nextInt(maxLatency - minLatency) + minLatency));
        });

        nodeLinks.forEach((n, v) -> System.out.printf("%s %d%n", n.getName(), v));
    }

    public static HashMap<Node, Integer> getNodeTotalDistance(HashSet<Link> links, Node start) {
        HashMap<Node, Integer> ret = new HashMap<>();
        ret.put(start, 0);
        HashSet<Node> finished = new HashSet<>();

        Optional<Entry<Node, Integer>> next;
        while ((next = ret.entrySet().stream().filter(e -> !finished.contains(e.getKey()))
                .reduce((e1, e2) -> e1.getValue() <= e2.getValue() ? e1 : e2)).isPresent()) {
            Entry<Node, Integer> nextVal = next.get();
            Node nextNode = nextVal.getKey();
            int nextDistance = nextVal.getValue();

            finished.add(nextNode);
            Map<Node, Integer> connections = links.stream()
                    .filter(l -> (l.getN1() == nextNode && !finished.contains(l.getN2())) || (l.getN2() == nextNode && !finished.contains(l.getN1())))
                    .collect(Collectors.toMap(l -> l.getN1() == nextNode ? l.getN2() : l.getN1(), l -> nextDistance + l.getLatencyMs()));
            connections.forEach((n, d) -> ret.merge(n, d, Integer::min));
        }

        return ret;
    }

}
