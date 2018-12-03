package org.soccer.queryExpansion;

import com.uwyn.jhighlight.tools.FileUtils;
import edu.gslis.indexes.IndexWrapper;
import edu.gslis.indexes.IndexWrapperFactory;
import edu.gslis.lucene.main.LuceneRunQuery;
import edu.gslis.lucene.main.config.RunQueryConfig;
import edu.gslis.queries.GQueries;
import edu.gslis.queries.GQuery;
import edu.gslis.searchhits.SearchHit;
import edu.gslis.searchhits.SearchHits;
import edu.gslis.textrepresentation.FeatureVector;
import org.apache.commons.lang.StringUtils;
import org.soccer.indexing.DocEntity;
import org.soccer.indexing.QueryExecution;
import edu.gslis.utils.Stopper;
import edu.gslis.lucene.expansion.Rocchio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RocchioAlgorithm {

    double alpha;
    double beta;
    double gamma;

    private Stopper stoplist = new Stopper();

    public RocchioAlgorithm(){
        this.alpha = 1;
        this.beta = 0.75;
        this.gamma = 0.25;
    }
    public RocchioAlgorithm(double alpha, double beta, double gamma){
        this.alpha = alpha;
        this.beta = beta;
        this.gamma = gamma;
    }

    public void implementRochio(String InputQuery, String indexpath, String stopWordsPath) throws Exception {

        // BUILD THE CONFIG TO RUN IR TOOLS FOR ROCCHIO EXPANSION.
        GQueries gqueries = null;
        gqueries.read(InputQuery);

        RunQueryConfig config = new RunQueryConfig();
        config.setIndex(indexpath);
        config.setStopwords(stopWordsPath);
        config.setQueries(gqueries);

        config.setFbAlpha(this.alpha);
        config.setFbBeta(this.beta);

        String indexPath = config.getIndex();
        String docnoField = config.getDocno();
        if (StringUtils.isEmpty(docnoField))
            docnoField = "docno";
        String similarityModel = config.getSimilarity();
        String stopwordsPath = config.getStopwords();

        Stopper stopper = new Stopper();
        if (!StringUtils.isEmpty(stopwordsPath))
            stopper = new Stopper(stopwordsPath);

        // Read the similarity used during index creation
        Map<String, String> indexMetadata = readIndexMetadata(indexPath);
        if (StringUtils.isEmpty(similarityModel) && indexMetadata.get("similarity") != null)
            similarityModel = indexMetadata.get("similarity");

        if (!StringUtils.isEmpty(config.getSimilarity()))
            similarityModel = config.getSimilarity();

        // Setup the index searcher
        IndexWrapper index = IndexWrapperFactory.getIndexWrapper(indexPath);

        if (config.getFbDocs() > 0 && config.getFbTerms() > 0) {
            System.err.println("Running Rocchio expansion: " + config.getFbDocs() + "," + config.getFbTerms() +
                    "," + config.getFbAlpha() + "," + config.getFbBeta());
        }

        // Run each query
        for (int i=0; i<config.getQueries().numQueries(); i++) {
            GQuery query = config.getQueries().getIthQuery(i);
            if (stopper != null)
                query.applyStopper(stopper);

            SearchHits hits = index.runQuery(query, config.getNumResults(), similarityModel);

            if (config.getFbDocs() > 0 && config.getFbTerms() > 0) {

                Map<String, String> params = getParamsFromModel(config.getSimilarity());
                double b = Double.parseDouble(params.get("b"));
                double k1 = Double.parseDouble(params.get("k1"));

                Rocchio rocchioFb = new Rocchio(config.getFbAlpha(), config.getFbBeta(), k1, b);
                rocchioFb.setStopper(stopper);
                rocchioFb.expandQuery(index, query, config.getFbDocs(), config.getFbTerms());

                hits = index.runQuery(query, config.getNumResults(), similarityModel);
            }
            hits.rank();

            int rank=0;
            for (SearchHit hit: hits.hits()) {
                System.out.println(query.getTitle() + " Q0 " + hit.getDocno() + " " + rank + " "  + hit.getScore() + " " + config.getRunName());
                rank++;
            }
        }
    }

    private Map<String, String> getParamsFromModel(String model) {
        Map<String, String> params = new HashMap<String, String>();
        String[] fields = model.split(",");
        // Parse the model spec
        for (String field : fields) {
            String[] nvpair = field.split(":");
            params.put(nvpair[0], nvpair[1]);
        }
        return params;
    }

    private Map<String, String> readIndexMetadata(String indexPath)
    {
        Map<String, String> map = new HashMap<String, String>();
        return map;
    }

//    public static void main(String[] args) throws Exception{
//
//        // call implement rocchio method
//        RocchioAlgorithm rocchioAlgo = new RocchioAlgorithm();
//
//        String indexPath = "/Users/tharunngolthi/Downloads/luceneIndex/index_snow.fl";
//        String query = "/Users/tharunngolthi/IdeaProjects/SearchEngineSoccer/IndriQueryFile";
//
//        String current = new java.io.File( "." ).getCanonicalPath();
//        String StopWordFile = current + "/stopwords";
//
//        rocchioAlgo.implementRochio(query, indexPath, StopWordFile);
//    }
}
