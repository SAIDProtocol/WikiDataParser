package edu.rutgers.winlab.wikidataparser;

import static edu.rutgers.winlab.wikidataparser.SqlParser.DataType.DOUBLE;
import static edu.rutgers.winlab.wikidataparser.SqlParser.DataType.INT;
import static edu.rutgers.winlab.wikidataparser.SqlParser.DataType.STRING;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author jiachen
 */
public class WikiParser {

    public static void parsePages(String pageSql, String catPagesFile) throws IOException {
        ReportObject ro = new ReportObject();
        ro.setKey("Total", 0);
        ro.setKey("Cat", 1);

        try (PrintStream ps = new PrintStream(catPagesFile)) {
            Consumer<Object[]> handler = objs -> {
                ro.incrementValue(0);
                if (objs[1] == (Integer) 14) {
                    ps.printf("%d\t%s%n", objs[0], objs[2]);
                    ro.incrementValue(1);
                }
            };
            ro.beginReport();

            SqlParser.parseFile(pageSql,
                    new SqlParser.DataType[]{INT, INT, STRING, STRING, INT, INT, INT, DOUBLE, STRING, STRING, INT, INT, STRING, STRING},
                    handler, ro);
            ro.endReport();
            ps.flush();
        }
    }

    public static void parseCategoryLinks(String categoryLinkkSql,
            String catPagesFile, String catSubCatsFile, String catInfoFile, String catNotFoundFile) throws IOException {

        HashMap<Integer, CatInfo> idPages = new HashMap<>();
        HashMap<String, CatInfo> namePages = new HashMap<>();
        HashMap<String, CatInfo> catNamesNotFound = new HashMap<>();
        ReportObject ro = new ReportObject();
        ro.setKey("Total", 0);
        ro.setKey("SubCat", 1);
        ro.setKey("SubPage", 2);
        ro.setKey("SubFile", 3);
        ro.setKey("NoCatSubCat", 4);
        ro.setKey("NoCatSubPage", 5);
        ro.setKey("NoCatSubFile", 6);
        ro.setKey("SubCatNoCat", 7);
        ro.setKey("NoCat", () -> String.valueOf(catNamesNotFound.size()));
        ro.setKey("Cat", 10);

        ro.beginReport();
        Consumer<String> pageLineHandler = l -> {
            int idx = l.indexOf('\t');
            assert idx > 0;
            int id = Integer.parseInt(l.substring(0, idx));
            String name = l.substring(idx + 1);
            CatInfo pi = new CatInfo(id, name);
            CatInfo opi = idPages.putIfAbsent(id, pi);
            assert opi == null;
            opi = namePages.putIfAbsent(name, pi);
            assert opi == null;
            ro.incrementValue(10);
        };
        Files.lines(Paths.get(catPagesFile)).forEach(pageLineHandler);

        try (PrintStream ps = new PrintStream(catSubCatsFile)) {
            Consumer<Object[]> handler = objs -> {
                ro.incrementValue(0);
                String catName = (String) objs[1];
                CatInfo ci = namePages.get(catName);
                int val = 0;
                if (ci == null) {
                    val = 3;
                    ci = catNamesNotFound.get(catName);
                    if (ci == null) {
                        catNamesNotFound.put(catName, ci = new CatInfo(0, catName));
                    }
                }
                switch ((String) objs[6]) {
                    case "subcat":
                        ro.incrementValue(val + 1);
                        ci.incrementSubCatCount();
                        int childId = (Integer) objs[0];
                        CatInfo childInfo = idPages.get(childId);
                        if (childInfo == null) {
                            ro.incrementValue(7);
                        } else if (val == 0) {
                            ps.printf("%d\t%s\t%d\t%s%n", childInfo.getId(), childInfo.getName(), ci.getId(), ci.getName());
                        }
                        break;
                    case "page":
                        ro.incrementValue(val + 2);
                        ci.incrementPageCount();
                        break;
                    case "file":
                        ro.incrementValue(val + 3);
                        ci.incrementFileCount();
                        break;
                    default:
                        assert false;
                }
            };

            SqlParser.parseFile(categoryLinkkSql,
                    new SqlParser.DataType[]{INT, STRING, STRING, STRING, STRING, STRING, STRING},
                    handler, ro);
            ps.flush();
        }
        ro.endReport();

        Stream<String> s = idPages.values().stream()
                .sorted((p1, p2) -> Integer.compare(p1.getId(), p2.getId()))
                .map(p -> String.format("%d\t%d\t%d\t%d\t%s", p.getId(), p.getSubCatCount(), p.getPageCount(), p.getFileCount(), p.getName()));
        Files.write(Paths.get(catInfoFile), (Iterable<String>) s::iterator);
        Stream<String> s2 = catNamesNotFound.values().stream()
                .sorted((p1, p2) -> p1.getName().compareTo(p2.getName()))
                .map(p -> String.format("%d\t%d\t%d\t%d\t%s", p.getId(), p.getSubCatCount(), p.getPageCount(), p.getFileCount(), p.getName()));
        Files.write(Paths.get(catNotFoundFile), (Iterable<String>) s2::iterator);
    }

    public static HashMap<Integer, CatInfo> readCategoryRelationships(String catSubCatsFile, ReportObject ro) throws IOException {
        HashMap<Integer, CatInfo> cats = new HashMap<>();
        ro.setKey("Lines", 0);
        ro.setKey("Cats", () -> String.valueOf(cats.size()));

        Files.lines(Paths.get(catSubCatsFile))
                .forEach(l -> {
                    ro.incrementValue(0);
                    String[] parts = l.split("\t");
                    assert parts.length == 4;
                    Integer childId = Integer.parseInt(parts[0]);
                    Integer parentId = Integer.parseInt(parts[2]);
                    CatInfo child, parent;
                    child = cats.get(childId);
                    if (child == null) {
                        cats.put(childId, child = new CatInfo(childId, parts[1]));
                    }
                    parent = cats.get(parentId);
                    if (parent == null) {
                        cats.put(parentId, parent = new CatInfo(parentId, parts[3]));
                    }
                    parent.addChild(child);
                });
        return cats;
    }

    public static HashSet<CatInfo> doBFS(CatInfo cStart, int maxLev, ReportObject ro) throws IOException {

        HashSet<CatInfo> target = new HashSet<>();
        HashMap<CatInfo, Integer> levels = new HashMap<>();
        ArrayDeque<CatInfo> todo = new ArrayDeque<>();
        ro.setKey("Subset", () -> String.valueOf(target.size()));

        levels.put(cStart, 0);
        todo.offer(cStart);
        CatInfo curr;
        while ((curr = todo.poll()) != null) {
            if (!target.add(curr)) {
                continue;
            }
            int lvl = levels.get(curr) + 1;
            curr.forEachChild(c -> {
                if (levels.merge(c, lvl, Integer::min) < maxLev) {
                    todo.offer(c);
                }
            });
        }
        return target;
    }

    public static void writeCatSubCats(HashSet<CatInfo> categories, String catSubCatsFile) throws IOException {
        try (PrintStream ps = new PrintStream(catSubCatsFile)) {
            categories.forEach(c -> {
                c.forEachParent(p -> {
                    if (!categories.contains(p)) {
                        return;
                    }
                    ps.printf("%d\t%s\t%d\t%s%n", c.getId(), c.getName(), p.getId(), p.getName());
                });
            });
        }
    }

    public static void writeCatSubCatRelationships(HashSet<CatInfo> categories, String catSubCatRelationshipsFile) throws IOException {
        Stream<String> str = categories.stream()
                .map(p -> String.format("%d\t%d\t%s\t%s",
                p.getId(),
                p.getParentsStream().filter(c -> categories.contains(c)).count(),
                p.getName(),
                Arrays.toString(p.getChildrenStream().filter(c -> categories.contains(c)).map(c -> c.getId()).toArray())
        ));
        Files.write(Paths.get(catSubCatRelationshipsFile), (Iterable<String>) str::iterator);

    }

    public static ArrayDeque<CatInfo> topologicalSort(Collection<CatInfo> cats) {
        ArrayDeque<CatInfo> ret = new ArrayDeque<>();
        ArrayList<CatInfo> unmarked = new ArrayList<>(cats);
        HashSet<CatInfo> tmpMarked = new HashSet<>(), marked = new HashSet<>();
        while (!unmarked.isEmpty()) {
            topologicalSortVisit(unmarked.remove(0), ret, tmpMarked, marked);
        }
        return ret;
    }

    private static void topologicalSortVisit(CatInfo n, ArrayDeque<CatInfo> sort, HashSet<CatInfo> tmpMarked, HashSet<CatInfo> marked) {
        if (marked.contains(n)) {
            return;
        }
        if (tmpMarked.contains(n)) {

            tmpMarked.stream().map(c -> String.format("%d\t%s", c.getId(), c.getName())).forEach(System.err::println);
            System.err.printf("%d\t%s%n", n.getId(), n.getName());
            System.err.flush();
            throw new IllegalArgumentException("Topology is not a DAG");
        }
        tmpMarked.add(n);
        n.forEachChild(m -> topologicalSortVisit(m, sort, tmpMarked, marked));
        tmpMarked.remove(n);
        marked.add(n);
        sort.addFirst(n);

    }

    public static void generateNames(HashMap<Integer, CatInfo> cats, HashSet<CatInfo> newCats, HashMap<CatInfo, CatInfo[]> catMappings) throws IOException {
        ArrayDeque<CatInfo> sorted = topologicalSort(cats.values());

        int[] catId = new int[]{1};
        CatInfo root = new CatInfo(catId[0]++, "/");
        newCats.add(root);

        sorted.forEach(c -> {
            if (c.isOrphan()) {
                CatInfo newCat = new CatInfo(catId[0]++, root.getName() + c.getName());
                newCats.add(newCat);
                root.addChild(newCat);
                catMappings.put(c, new CatInfo[]{newCat});
            } else {
                Stream<CatInfo> childNames = c.getParentsStream()
                        .map(p -> Stream.of(catMappings.get(p))) // names of parents
                        .reduce(Stream::concat) // combine
                        .get()
                        .map(n -> {
                            CatInfo newCat = new CatInfo(catId[0]++, n.getName() + "/" + c.getName());
                            newCats.add(newCat);
                            n.addChild(newCat);
                            return newCat;
                        });
                catMappings.put(c, childNames.toArray(CatInfo[]::new));
            }
        });
    }

    public static void writeNameMapping(HashMap<CatInfo, CatInfo[]> catMappings, String catNameMappingsFile) throws IOException {
        try (PrintStream ps = new PrintStream(catNameMappingsFile)) {
            catMappings.forEach((c, s) -> {
                ps.printf("%d\t%s\t%d", c.getId(), c.getName(), s.length);
                for (CatInfo n : s) {
                    ps.printf("\t%d\t%s", n.getId(), n.getName());
                }
                ps.println();
            });
            ps.flush();
        }
    }

    public static void filterCategoryInfo(String catInfoFile, HashMap<Integer, CatInfo> categories) throws IOException {
        Files.lines(Paths.get(catInfoFile)).forEach(l -> {
            String[] parts = l.split("\t");
            assert parts.length == 5;
            CatInfo ci = categories.get(Integer.parseInt(parts[0]));
            if (ci != null) {
                ci.setPageCount(Integer.parseInt(parts[2]));
                ci.setFileCount(Integer.parseInt(parts[3]));
            }
        });
    }

    public static void writeCategoryInfo(Stream<CatInfo> categories, String subsetCatInfoFile) throws IOException {
        Files.write(Paths.get(subsetCatInfoFile),
                (Iterable<String>) categories.map(c -> String.format("%d\t%d\t%d\t%d\t%s", c.getId(), c.getChildrenCount(), c.getPageCount(), c.getFileCount(), c.getName()))::iterator);
    }

    public static Map<CatInfo, Integer> getDirectPublications(Stream<CatInfo> categories) {
        return categories.collect(Collectors.toMap(c -> c, c -> {
            int count = c.getPageCount() + c.getFileCount();
            if (count == 0 && c.getChildrenCount() == 0) {
                return 1;
            }
            return count;
        }));
    }

    public static ArrayList<Tuple2<Integer, CatInfo>> generatePublications(
            Map<CatInfo, Integer> catDirectPublications,
            int publicationRepeats,
            Function<Double, Double> poissonLambda,
            int randSeed,
            ReportObject ro) {
        ro.setKey("Time", 200);
        ro.setKey("Pubs", 201);

        ArrayList<Tuple2<Integer, CatInfo>> ret = new ArrayList<>();
        ArrayList<CatInfo> directPublications = new ArrayList<>();
        int total = 0;
        for (Map.Entry<CatInfo, Integer> entry : catDirectPublications.entrySet()) {
            for (int i = 0; i < entry.getValue() * publicationRepeats; i++) {
                directPublications.add(entry.getKey());
            }
            total += entry.getValue() * publicationRepeats;

        }

        Iterator<CatInfo> randomDirectPublications = randomSort(directPublications.stream(), 0).iterator();
        int max = 0;
        Random rand = new Random(randSeed);
        while (randomDirectPublications.hasNext()) {
            int v = WikiParser.poissonRandomNumber(poissonLambda.apply(ro.getValue(201) * 1.0 / total), rand);
            max = Math.max(max, v);
            for (int i = 0; i < v && randomDirectPublications.hasNext(); i++) {
                ret.add(new Tuple2<>(ro.getValue(200), randomDirectPublications.next()));
                ro.incrementValue(201);
            }
            ro.incrementValue(200);
        }
        System.out.printf("Max=%,d%n", max);
        return ret;
    }

    public static void writePublications(ArrayList<Tuple2<Integer, CatInfo>> publications, String publicationsFile) throws IOException {
        try (PrintStream ps = new PrintStream(publicationsFile)) {
            for (int i = 0; i < publications.size(); i++) {
                Tuple2<Integer, CatInfo> pub = publications.get(i);
                ps.printf("%d\t%d\t%d\t%s%n", i, pub.getV1(), pub.getV2().getId(), pub.getV2().getName());
            }
            ps.flush();
        }
    }

    // pubId, time, cat
    public static Stream<Tuple3<Integer, Integer, CatInfo>> readPublications(String publicationsFile, HashMap<Integer, CatInfo> subCats) throws IOException {

        return Files.lines(Paths.get(publicationsFile)).map(l -> {
            String[] parts = l.split("\t");
            assert parts.length == 4;
            int pubId = Integer.parseInt(parts[0]);
            int time = Integer.parseInt(parts[1]);
            int catId = Integer.parseInt(parts[2]);
            CatInfo cat = subCats.get(catId);
            assert cat != null;
            return new Tuple3<>(pubId, time, cat);
        });
    }

    public static ArrayList<Tuple2<Integer, Integer>> getPublicationFrequency(int step, Stream<Tuple3<Integer, Integer, CatInfo>> publications) {
        ArrayList<Tuple2<Integer, Integer>> ret = new ArrayList<>();
        Iterator<Tuple3<Integer, Integer, CatInfo>> it = publications.iterator();
        if (!it.hasNext()) {
            return ret;
        }
        Tuple3<Integer, Integer, CatInfo> pub = it.next();
        int time = pub.getV2() / step * step;
        Tuple2<Integer, Integer> t = new Tuple2<>(time, 1);
        ret.add(t);
        while (it.hasNext()) {
            pub = it.next();
            time = pub.getV2() / step * step;
            if (time == t.getV1()) {
                t.setV2(t.getV2() + 1);
            } else {
                t = new Tuple2<>(time, 1);
                ret.add(t);
            }
        }
        return ret;
    }

    public static void writePublicationFrequency(Stream<Tuple2<Integer, Integer>> frequency, String publicationFrequencyFile) throws IOException {
        Files.write(Paths.get(publicationFrequencyFile), (Iterable<String>) frequency.map(t -> String.format("%d\t%d", t.getV1(), t.getV2()))::iterator);
    }

    // pub_id, pub_time, sub_id, cat_id (receive guid)
    public static ArrayList<Tuple4<Integer, Integer, String, Integer>> getDeliveries(
            Stream<Tuple3<Integer, Integer, CatInfo>> publications,
            HashMap<CatInfo, HashSet<String>> subscriptions) {
        ArrayList<Tuple4<Integer, Integer, String, Integer>> ret = new ArrayList<>();
        HashMap<CatInfo, HashSet<CatInfo>> subsets = new HashMap<>();
        publications.forEach(t -> {
            int pubId = t.getV1();
            int pubTime = t.getV2();
            CatInfo pubCat = t.getV3();
            HashSet<CatInfo> subset = subsets.get(pubCat);
            if (subset == null) {
                subsets.put(pubCat, subset = new HashSet<>());
                ArrayDeque<CatInfo> todos = new ArrayDeque<>();
                todos.add(pubCat);
                CatInfo curr;
                while ((curr = todos.poll()) != null) {
                    if (!subset.add(curr)) {
                        continue;
                    }
                    curr.forEachParent(p -> todos.offer(p));
                }
            }
            subset.forEach(ancestor -> {
                int guid = ancestor.getId();
                subscriptions.get(ancestor).forEach(subscriber -> {
                    ret.add(new Tuple4<>(pubId, pubTime, subscriber, guid));
                });
            });
        });
        return ret;
    }

    public static void writeDeliveries(ArrayList<Tuple4<Integer, Integer, String, Integer>> deliveries, String deliversFile) throws IOException {
        Files.write(Paths.get(deliversFile),
                (Iterable<String>) deliveries.stream().map(t -> String.format("%d\t%d\t%s\t%d", t.getV1(), t.getV2(), t.getV3(), t.getV4()))::iterator);
    }

    public static <T> Stream<T> randomSort(Stream<T> vals, long randomSeed) {
        Random rand = new Random(randomSeed);
        return vals
                .map(val -> new Tuple2<>(val, rand.nextDouble()))
                .sorted((t1, t2) -> t1.getV2().compareTo(t2.getV2()))
                .map(t -> t.getV1());
    }

    public static HashMap<Integer, CatInfo> generateSubscriptions(Stream<CatInfo> categories, int subsPerCat, long randomSeed) {
        HashMap<Integer, CatInfo> subs = new HashMap<>();
        CatInfo[] cats = categories
                .sorted((e1, e2) -> Integer.compare(e1.getId(), e2.getId()))
                .toArray(CatInfo[]::new);
        Integer[] ids = new Integer[cats.length * subsPerCat];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = i;
        }
        ids = randomSort(Stream.of(ids), 0).toArray(Integer[]::new);
        for (int i = 0; i < ids.length; i++) {
            CatInfo cat = cats[i / subsPerCat];
            subs.put(ids[i] + 1, cat);
        }
        return subs;
    }

    public static void writeSubscriptions(HashMap<Integer, CatInfo> subs, String subscriptionFileName) throws IOException {
        Files.write(Paths.get(subscriptionFileName),
                (Iterable<String>) subs.entrySet().stream()
                        .map(e -> String.format("S%s\t%d\t%s", e.getKey(), e.getValue().getId(), e.getValue().getName()))::iterator);
    }

    public static HashMap<CatInfo, HashSet<String>> readSubscriptions(HashMap<Integer, CatInfo> subs, String subscriptionFileName) throws IOException {
        HashMap<CatInfo, HashSet<String>> subscribers = new HashMap<>();
        Files.lines(Paths.get(subscriptionFileName)).forEach(l -> {
            String[] parts = l.split("\t");
            assert parts.length == 3;
            String subName = parts[0];
            int catId = Integer.parseInt(parts[1]);
            CatInfo cat = subs.get(catId);
            assert cat != null;
            HashSet<String> tmp = subscribers.get(cat);
            if (tmp == null) {
                subscribers.put(cat, tmp = new HashSet<>());
            }
            boolean added = tmp.add(subName);
            assert added;
        });
        return subscribers;
    }

    private static final int STEP = 500;

    public static int poissonRandomNumber(double lambda, Random rand) {
        double lambdaLeft = lambda;
        int k = 0;
        double p = 1;
        do {
            k = k + 1;
            double u = rand.nextDouble();
            p = p * u;
            while (p < 1 && lambdaLeft > 0) {
                if (lambdaLeft > STEP) {
                    p = p * Math.exp(STEP);
                    lambdaLeft = lambdaLeft - STEP;
                } else {
                    p = p * Math.exp(lambdaLeft);
                    lambdaLeft = 0;
                }
            }
        } while (p > 1);
        return k - 1;
    }

}
