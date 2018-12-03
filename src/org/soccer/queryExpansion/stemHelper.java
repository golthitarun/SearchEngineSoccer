package org.soccer.queryExpansion;

import java.io.File;
import java.io.IOException;
import java.util.*;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

public class stemHelper {

    Map<String, Set<String>> stemMap;
    Set<String> tokensWithoutStemmingSet;
    WordnetStemmer     stemmer;

    public stemHelper(String query) throws IOException {
        this.stemMap = new HashMap<>();

        String current = new java.io.File( "." ).getCanonicalPath();
        String wordNet = current+"/webapps/SearchEngineSoccer-0.0.1-SNAPSHOT/WEB-INF/resources/dict";
        final Dictionary dict = new Dictionary(new File(wordNet));
        dict.open();
        this.stemmer = new WordnetStemmer(dict);

        this.tokensWithoutStemmingSet = new HashSet<>();
        this.tokensWithoutStemmingSet.addAll(Arrays.asList(query.split(" ")));
    }

    public void executeStemming(HashMap<String, Map<Integer, Integer>> tokenMap) {
        Set<String> set = new HashSet<>();
        Set<String> keySet = tokenMap.keySet();
        set.addAll(keySet);

        boolean flag;
        Set<String> Stems = new HashSet<>();
        POS[] values = new POS[] {POS.NOUN, POS.VERB, POS.ADJECTIVE, POS.ADVERB};
        for (String w1: set) {
            flag = false;

            for(String w: this.tokensWithoutStemmingSet) {
                if (w.contains(w1) || w1.contains(w)) {
                    flag = true;
                    break;
                }
            }

            if (flag) {
                continue;
            }

            for(POS pos: values) {
                Stems.addAll(this.stemmer.findStems(w1, pos));
            }
        }

        List<String> stemsFound = helper(keySet, values, Stems);

        Set<String> tokensSet;
        Stems = new HashSet<>();
        Stems.addAll(this.stemMap.keySet());
        for (String w1: Stems) {
            tokensSet = this.stemMap.get(w1);
            flag = false;
            if (tokensSet != null && tokensSet.size() > 1) {
                for (String w2: tokensSet) {
                    if (tokenMap.containsKey(w2)) {
                        flag = true;
                    } else {
                        this.stemMap.remove(w2);
                    }
                }

                for (int i = w1.length() - 1; i > 0; i--){
                    String subStr = w1.substring(0, i);
                    if (this.stemMap.containsKey(subStr)) {
                        Set<String> common = new HashSet<>();
                        common.addAll(this.stemMap.get(w1));
                        common.removeAll(this.stemMap.get(subStr));

                        if (common.size() != this.stemMap.get(w1).size()) {
                            this.stemMap.remove(subStr);
                        }
                    }
                }
            } else if (tokensSet != null && tokensSet.size() == 1) {

                for (String w2: tokensSet){
                    if (!tokenMap.containsKey(w2)) {
                        this.stemMap.remove(w2);
                    } else {
                        flag = true;
                    }
                }
            }

            if (!flag || w1.length() < 3) {
                this.stemMap.remove(w1);
            }
        }
    }

    public List<String> helper(Set<String> keySet, POS[] values, Set<String> Stems){
        List<String> stemsFound = new List<String>() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<String> iterator() {
                return null;
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return null;
            }

            @Override
            public boolean add(String s) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends String> c) {
                return false;
            }

            @Override
            public boolean addAll(int index, Collection<? extends String> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {

            }

            @Override
            public String get(int index) {
                return null;
            }

            @Override
            public String set(int index, String element) {
                return null;
            }

            @Override
            public void add(int index, String element) {

            }

            @Override
            public String remove(int index) {
                return null;
            }

            @Override
            public int indexOf(Object o) {
                return 0;
            }

            @Override
            public int lastIndexOf(Object o) {
                return 0;
            }

            @Override
            public ListIterator<String> listIterator() {
                return null;
            }

            @Override
            public ListIterator<String> listIterator(int index) {
                return null;
            }

            @Override
            public List<String> subList(int fromIndex, int toIndex) {
                return null;
            }
        };
        for (String w1: keySet) {
            boolean flag = false;
            for(POS pos: values) {
                stemsFound = this.stemmer.findStems(w1, pos);
                for (String w: stemsFound){
                    if (Stems.contains(w)){
                        flag = true;
                        this.stemMap.putIfAbsent(w, new HashSet<>());
                        this.stemMap.get(w).add(w1);
                    }
                }
            }

            if (!flag) {
                this.stemMap.putIfAbsent(w1, new HashSet<>());
                this.stemMap.get(w1).add(w1);
            }
        }
        return stemsFound;
    }
}
