package org.soccer.queryExpansion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.soccer.indexing.DocEntity;
import org.soccer.indexing.QueryExecution;

public class queryExpansion {

    static class wordObj{
        String u;
        String v;
        double val;

        public wordObj(String u, String v, double val){
            this.u = u;
            this.v = v;
            this.val = val;
        }

        public String getWordObjString() {
            return this.u+":"+this.v+":"+this.val;
        }

    }

    public queryExpansion(){

    }

    public String buildQueryExpansionString(String query) throws Exception {

        wordObj[][] matrix = pseudoRelevanceFeedBackProcessor(query);

        List<wordObj> list = new ArrayList<>();
        for(wordObj[] words: matrix){
            for(wordObj word: words){
                if (word != null){
                    list.add(word);
                }
            }
        }

        Collections.sort(list, new Comparator<wordObj>() {
            @Override
            public int compare(final wordObj o1, final wordObj o2) {
                return o1.val >= o2.val ? 1 : -1;
            }
        });

        LinkedHashSet<String> set = new LinkedHashSet<>();
        for(int i = list.size() - 1; i > 0; i--){
            set.add(list.get(i).v);
        }

        return query+" "+String.join(" ", set);

    }

    public static wordObj[][] pseudoRelevanceFeedBackProcessor(String query) throws Exception {

        /*

        QUERY INDEX FROM THE GIVEN QUERY TO GET RESULTS

         */
        ArrayList<DocEntity> res = QueryExecution.processQuery(query);

        String[] docs = new String[10];
        int i = 0;
        for(DocEntity dr: res) {
            if (i >= 10){
                break;
            }
            docs[i++] = dr.getContents();
        }

        queryProcessor processor = new queryProcessor();
        processor.parseDocs(Arrays.asList(docs));

        HashMap<String, Map<Integer, Integer>> tokenMap = processor.tokenMap;

        stemHelper stemming = new stemHelper(query);
        stemming.executeStemming(tokenMap);
        Map<String, Set<String>> stemMap = stemming.stemMap;

        wordObj[][] matrix = metricClustering(query, tokenMap, stemMap);

        return matrix;
    }

    public static wordObj[][] metricClustering(String query, HashMap<String, Map<Integer, Integer>> tokenMap, Map<String, Set<String>> stemMap) {
        int n = stemMap.size();
        wordObj[][] matrix = new wordObj[n][n];
        String[] stems = stemMap.keySet().toArray(new String[n]);

        double Cuv;
        Map<Integer, Integer> w1Map;
        Map<Integer, Integer> w2Map;

        for (int i = 0; i < stems.length; i++) {
            for (int j = 0; j < stems.length; j++) {
                if (i==j) {
                    continue;
                }

                Cuv = 0.0;
                Set<String> words1 = stemMap.get(stems[i]);
                Set<String> words2 = stemMap.get(stems[j]);

                for (String w1: words1) {
                    for (String w2: words2) {
                        w1Map = tokenMap.get(w1);
                        w2Map = tokenMap.get(w2);
                        for (Integer num: w1Map.keySet()) {
                            if (w2Map.containsKey(num)) {
                                Cuv += 1.0 / Math.abs(w1Map.get(num) - w2Map.get(num));
                            }
                        }
                    }
                }

                matrix[i][j] = new wordObj(stems[i], stems[j], Cuv);
            }
        }

        wordObj[][] normalized = new wordObj[n][n];

        for (int i = 0; i < stems.length; i++){
            for (int j = 0; j < stems.length; j++){
                if (i==j) {
                    continue;
                }

                Cuv = 0.0;
                if (matrix[i][j] != null) {
                    Cuv = matrix[i][j].val / (stemMap.get(stems[i]).size() * stemMap.get(stems[j]).size());
                }

                normalized[i][j] = new wordObj(stems[i], stems[j], Cuv);
            }
        }

        return TopN(normalized, stems, query, tokenMap, stemMap);
    }

    public static wordObj[][] TopN(wordObj[][] metricMatrix, String[] stems, String query, HashMap<String, Map<Integer, Integer>> tokenMap, Map<String, Set<String>> stemMap) {

        Set<String> strs = new HashSet<>();
        strs.addAll(Arrays.asList(query.split(" ")));

        wordObj[][] elements = new wordObj[strs.size()][3];

        int idx = 0;
        for(String word: strs) {
            final PriorityQueue<wordObj> queue = new PriorityQueue<>(3, new Comparator<wordObj>() {

                @Override
                public int compare(final wordObj o1, final wordObj o2) {
                    return o1.val >= o2.val ? 1 : -1;
                }
            });

            int i = -1;
            for(int k=0; k < stems.length; k++){
                if (stems[k].equalsIgnoreCase(word)){
                    i = k;
                    break;
                }
            }

            if (i == -1) {
                continue;
            }
            for (int j = 0; j < metricMatrix[i].length; j++) {
                if (metricMatrix[i][j] == null || strs.contains(metricMatrix[i][j].u) && !metricMatrix[i][j].u.equals(word) || strs.contains(metricMatrix[i][j].v) && !metricMatrix[i][j].v.equals(word)) {
                    continue;
                }

                if (tokenMap.containsKey(metricMatrix[i][j].v)) {
                    queue.add(metricMatrix[i][j]);
                } else {
                    queue.add(new wordObj(metricMatrix[i][j].u, stemMap.get(metricMatrix[i][j].v).iterator().next(), metricMatrix[i][j].val));
                }

                if (queue.size() > 3){
                    queue.poll();
                }
            }

            elements[idx++] = queue.toArray(new wordObj[3]);
        }

        return elements;

    }

    public static void main(String[] args) throws Exception{

        queryExpansion o = new queryExpansion();

        System.out.println(o.buildQueryExpansionString("messi"));
    }
}

