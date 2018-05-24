/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.rutgers.winlab.wikidataparser;

import static edu.rutgers.winlab.wikidataparser.SqlParser.readDouble;
import static edu.rutgers.winlab.wikidataparser.SqlParser.readInt;
import static edu.rutgers.winlab.wikidataparser.SqlParser.readString;
import static edu.rutgers.winlab.wikidataparser.SqlParser.skipEmptyString;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author jiachen
 */
public class WikiDataParserTest {

    public WikiDataParserTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

//    @Test
    public void testSqlParser() {
        String line = "10110, \'asdfas\\\'\\\"dfasdf\',  \t -123, -.234, 1234";
        int[] pos = new int[]{7};
        char[] l = line.toCharArray();
        Object[] res = new Object[4];
        int[] resIdx = new int[]{0};
        readString(l, pos, res, resIdx);
        System.out.printf("str: \"%s\"%nremaining: \"%s\"%n", res[0], new String(l, pos[0], line.length() - pos[0]));
        pos[0]++;
        skipEmptyString(l, pos, res, resIdx);
        System.out.printf("remaining: \"%s\"%n", new String(l, pos[0], line.length() - pos[0]));
        readInt(l, pos, res, resIdx);
        System.out.printf("int: %d%nremaining: \"%s\"%n", res[1], new String(l, pos[0], line.length() - pos[0]));
        pos[0]++;
        skipEmptyString(l, pos, res, resIdx);
        readDouble(l, pos, res, resIdx);
        System.out.printf("double: %f%nremaining: \"%s\"%n", res[2], new String(l, pos[0], line.length() - pos[0]));

        pos[0]++;
        skipEmptyString(l, pos, res, resIdx);
        readInt(l, pos, res, resIdx);
        System.out.printf("int: %d%nremaining: \"%s\"%n", res[3], new String(l, pos[0], line.length() - pos[0]));
    }

    @Test
    public void testWikiParser() throws IOException {
        String prefix = "/users/jiachen/Wiki/";

        String pageSql = prefix + "enwiki-20180420-page.sql";
        String categoryLinkSql = prefix + "enwiki-20180420-categorylinks.sql";
        String catPagesFile = prefix + "cat_pages.txt";
        String catSubCatsFile = prefix + "cat_subcats.txt";
        String catInfoFile = prefix + "cat_info.txt";
        String catNotFoundFile = prefix + "cat_notFound.txt";
//        int rootCategoryId = 4677693; // Category: Computer networking
//        int maxLev = 4;
        int rootCategoryId = 46773746; // Category: Disaster management
        int maxLev = 6;
        String subsetCatInfoFile = prefix + "subset_cat_info.txt";
        String subsetCatSubCatsFile = prefix + "subset_cat_subcats.txt";
        String subsetCatSubCatRelationshipsFile = prefix + "subset_cat_subcat_relationships.txt";
        String subsetCatNameMappingsFile = prefix + "subset_cat_namemappings.txt";
        String subsetCatNameSubCatsFile = prefix + "subset_cat_name_subcats.txt";
        String subsubsetCatSubCatsFile = prefix + "subsubset_cat_subcats.txt";
        String subsubsetCatSubCatRelationshipsFile = prefix + "subsubset_cat_subcat_relationships.txt";
        String subscriptionFile = prefix + "subset_cat_subscriptions.txt";
        String publicationFile = prefix + "subset_publications.txt";
        String publicationFrequencyFile = prefix + "subset_publication_frequency.txt";
        String deliveriesFile = prefix + "subset_deliveries.txt";
        int subsPerCat = 6;
        int publicationRepeats = 60;
        int publicationFrequencyStep = 100000; //100ms
        double poissonLambdaMin = 0.0015, poissonLambdaAdd = 0.0005;

        ReportObject ro = new ReportObject();
        ro.beginReport();

//        //Parse raw data
//        WikiParser.parsePages(pageSql, catPagesFile);
//        WikiParser.parseCategoryLinks(categoryLinkSql, catPagesFile, catSubCatsFile, catInfoFile, catNotFoundFile);
//
//        //BFS 
//        HashMap<Integer, CatInfo> cats = WikiParser.readCategoryRelationships(catSubCatsFile, ro);
//        HashSet<CatInfo> subset = WikiParser.doBFS(cats.get(rootCategoryId), maxLev, ro);
//        WikiParser.writeCatSubCats(subset, subsetCatSubCatsFile);
//        WikiParser.writeCatSubCatRelationships(subset, subsetCatSubCatRelationshipsFile);
//
//        //Modify relationship
//        HashMap<Integer, CatInfo> subCats = WikiParser.readCategoryRelationships(subsetCatSubCatsFile, ro);
//        HashSet<CatInfo> subset = new HashSet<>(subCats.values());
//        WikiParser.writeCatSubCatRelationships(subset, subsetCatSubCatRelationshipsFile);
//        WikiParser.filterCategoryInfo(catInfoFile, subCats);
//        WikiParser.writeCategoryInfo(subCats.values().stream(), subsetCatInfoFile);
//
//        //Generate names
//        HashMap<Integer, CatInfo> subCats = WikiParser.readCategoryRelationships(subsetCatSubCatsFile, ro);
//        HashSet<CatInfo> newCats = new HashSet<>();
//        HashMap<CatInfo, CatInfo[]> catMappings = new HashMap<>();
//        WikiParser.generateNames(subCats, newCats, catMappings);
//        WikiParser.writeCatSubCats(newCats, subsetCatNameSubCatsFile);
//        WikiParser.writeNameMapping(catMappings, subsetCatNameMappingsFile);
//
//        //Cleanup namespace
//        HashMap<Integer, CatInfo> subCats = WikiParser.readCategoryRelationships(subsetCatSubCatsFile, ro);
//        HashSet<CatInfo> subsubset = WikiParser.doBFS(subCats.get(rootCategoryId), maxLev, ro);
//        WikiParser.writeCatSubCats(subsubset, subsubsetCatSubCatsFile);
//        WikiParser.writeCatSubCatRelationships(subsubset, subsubsetCatSubCatRelationshipsFile);
//        HashMap<Integer, CatInfo> subsubcats = WikiParser.readCategoryRelationships(subsubsetCatSubCatsFile, ro);
//        HashSet<CatInfo> newCats = new HashSet<>();
//        HashMap<CatInfo, CatInfo[]> catMappings = new HashMap<>();
//        WikiParser.generateNames(subsubcats, newCats, catMappings);
//        WikiParser.writeCatSubCats(newCats, subsubsetCatNameSubCatsFile);
//        WikiParser.writeNameMapping(catMappings, subsubsetCatNameMappingsFile);
//
//        //Generate subscription
//        HashMap<Integer, CatInfo> subCats = WikiParser.readCategoryRelationships(subsetCatSubCatsFile, ro);
//        HashMap<Integer, CatInfo> subs = WikiParser.generateSubscriptions(subCats.values().stream(), subsPerCat, 0);
//        WikiParser.writeSubscriptions(subs, subscriptionFile);
//
//        //Generate publications
//        HashMap<Integer, CatInfo> subCats = WikiParser.readCategoryRelationships(subsetCatSubCatsFile, ro);
//        WikiParser.filterCategoryInfo(subsetCatInfoFile, subCats);
//        Map<CatInfo, Integer> catDirectPublications = WikiParser.getDirectPublications(subCats.values().stream());
//        ro.setKey("d", 999);
//        ArrayList<Tuple2<Integer, CatInfo>> publications = WikiParser.generatePublications(catDirectPublications, publicationRepeats, d -> {
//            ro.setValue(999, (int) (d * 1000000));
////            return poissonLambdaMin;
////            return poissonLambdaMin + poissonLambdaAdd;
//            return poissonLambdaMin + d * poissonLambdaAdd;
//        }, 0, ro);
//        WikiParser.writePublications(publications, publicationFile);
//
//        //Get publication frequency
//        HashMap<Integer, CatInfo> subCats = WikiParser.readCategoryRelationships(subsetCatSubCatsFile, ro);
//        Stream<Tuple3<Integer, Integer, CatInfo>> publications = WikiParser.readPublications(publicationFile, subCats);
//        ArrayList<Tuple2<Integer,Integer>> frequencies = WikiParser.getPublicationFrequency(publicationFrequencyStep, publications);
//        WikiParser.writePublicationFrequency(frequencies.stream(), publicationFrequencyFile);
//
//        //Generate receives
        HashMap<Integer, CatInfo> subCats = WikiParser.readCategoryRelationships(subsetCatSubCatsFile, ro);
        HashMap<CatInfo, HashSet<String>> subscriptions = WikiParser.readSubscriptions(subCats, subscriptionFile);
        Stream<Tuple3<Integer, Integer, CatInfo>> publications = WikiParser.readPublications(publicationFile, subCats);
        ArrayList<Tuple4<Integer, Integer, String, Integer>> deliveries = WikiParser.getDeliveries(publications, subscriptions);
        WikiParser.writeDeliveries(deliveries, deliveriesFile);

        ro.endReport();
    }

}
