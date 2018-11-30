package org.soccer.indexing;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import org.soccer.clustering.FlatClustering;
import org.soccer.clustering.HeirarClustering;

/**
 * @author Michael Mugo
 *
 */


public class IndexCreator {


    public static Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
    private IndexWriter writer;
    public static String indexLocation = "/Users/vivek/Desktop/luceneIndex/index_snow.fl";
    String docLocation = "/Users/vivek/Desktop/data1/";
    String urlLocation = "/Users/vivek/Desktop/urls1.txt/";
    public HashMap<String, String> urlMap = new HashMap<>();

    public void readFiles() throws Exception {
    	getUrlsMap(urlLocation);
        File[] listOfFiles = new File(docLocation).listFiles();
        for (File file : listOfFiles) {
            indexFileOrDirectory(file.getAbsolutePath(), file.getName());
           // System.out.println(file.getName());
        }
        closeIndex();
    }
    public void getUrlsMap(String urlLocation) throws IOException{
    	FileReader fr = new FileReader(new File(urlLocation));
    	BufferedReader br = new BufferedReader(fr);
    	String line ="";
        while ((line = br.readLine()) != null) {
        	String[] tmp =line.split(" ");
        	if(!urlMap.containsKey(tmp[0])){
        		urlMap.put(tmp[0], tmp[1]);
        	}
        }
    }
    public IndexCreator(String indexDir) throws IOException {
    //	FSDirectory dir = FSDirectory.open(FileSystems.getDefault().getPath(indexDir));
        FSDirectory dir = FSDirectory.open(new File(indexDir));
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, analyzer);
        writer = new IndexWriter(dir, config);
    }


    public static void main(String[] args) throws Exception {
        //IndexCreator index = new IndexCreator(IndexCreator.indexLocation);
        //index.readFiles();
	    ArrayList<DocEntity> res = QueryExecution.processQuery("ronaldo");
//		for(DocEntity dr: res){
//			System.out.println("url: "+dr.getUrl());
//			System.out.println("hits: "+dr.getHitScore());
//			System.out.println("rank score: "+dr.getRankScore());
//			System.out.println("cluster id: "+dr.getClusterId());
//			System.out.println("contents: "+ dr.getContents());
//			System.out.println("#############################");
//
//		}
		
		ArrayList<DocEntity> flatClusteredResult = new ArrayList<>();
		ArrayList<DocEntity> avgClusteredResult = new ArrayList<>();
		ArrayList<DocEntity> singleClusteredResult = new ArrayList<>();
		ArrayList<DocEntity> weightedClusteredResult = new ArrayList<>();
		ArrayList<DocEntity> completeClusteredResult = new ArrayList<>();
		
		if (res.size() > 20) {
			flatClusteredResult = FlatClustering.getFlatCluster(res);
			avgClusteredResult = HeirarClustering.getAverageLinkageCluster(res);
			singleClusteredResult = HeirarClustering.getSingleLinkageCluster(res);
			weightedClusteredResult = HeirarClustering.getWeightedLinkageCluster(res);
			completeClusteredResult = HeirarClustering.getCompleteLinkageCluster(res);
		}
		
		System.out.println("Clustering results");
		System.out.println("Flat Clustering: ");
		System.out.println("Results size: " + res.size());
		System.out.println("Flat clustered result size: " + flatClusteredResult.size());
		for (DocEntity dr:flatClusteredResult) {
			System.out.println("Cluster name: " + dr.getClusterId());
		}
		System.out.println("*******************************");
		System.out.println("Average Linkage");
		System.out.println("Avg clustered result size: " + avgClusteredResult.size());
		for (DocEntity dr:avgClusteredResult) {
			System.out.println("Cluster name: " + dr.getClusterId());
		}
		System.out.println("*******************************");
		System.out.println("Single Linkage");
		System.out.println("single clustered result size: " + singleClusteredResult.size());
		for (DocEntity dr:singleClusteredResult) {
			System.out.println("Cluster name: " + dr.getClusterId());
		}
		System.out.println("*******************************");
		System.out.println("Weighted Linkage");
		System.out.println("weight clustered result size: " + weightedClusteredResult.size());
		for (DocEntity dr:weightedClusteredResult) {
			System.out.println("Cluster name: " + dr.getClusterId());
		}
		System.out.println("*******************************");
		System.out.println("Complete Linkage");
		System.out.println("complete clustered result size: " + completeClusteredResult.size());
		for (DocEntity dr:completeClusteredResult) {
			System.out.println("Cluster name: " + dr.getClusterId());
		}
    }

    public void indexFileOrDirectory(String fileName, String fileNameOnly) throws Exception {
        String line = "";
        String url = urlMap.get(fileNameOnly.split(".t")[0]);
        FileReader fr = new FileReader(fileName);
        StringBuilder str = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(fr);
        while ((line = bufferedReader.readLine()) != null) {
        	str.append(line);
        }
        try {
            Document doc = new Document();
            Field f = new Field("content", str.toString(), Field.Store.YES, Field.Index.ANALYZED,Field.TermVector.WITH_POSITIONS_OFFSETS);
            f.setBoost(2.0f);
            doc.add(f);
            doc.add(new StringField("filename", fileNameOnly, Field.Store.YES));
            doc.add(new StringField("url", url, Field.Store.YES));
            writer.addDocument(doc);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            fr.close();
        }
        


    }
    
    public void closeIndex() throws IOException {
        writer.close();
    }

}

