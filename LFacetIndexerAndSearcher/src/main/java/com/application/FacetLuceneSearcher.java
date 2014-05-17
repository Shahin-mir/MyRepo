/**
 * 
 */
package com.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.index.params.DefaultFacetIndexingParams;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.search.params.CountFacetRequest;
import org.apache.lucene.facet.search.params.FacetSearchParams;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * @author Shahin
 * 
 */
public class FacetLuceneSearcher {

	private static Version LUCENE_VERSION = Version.LUCENE_40;

	public static void main(String args[]) throws Exception {
		
		
		
// to do: Load properties from Property file
				Properties prop = new Properties();
				InputStream input = new FileInputStream("config.properties");
				prop.load(input); //load property file
				String indexDirectory = prop.getProperty("indexDirectory");
				String taxonomyDirectory = prop.getProperty("taxonomyDirectory");
				//String fieldtoSearch = prop.getProperty("fieldtoSearch");
				input.close();
				
				IndexReader indexReader = DirectoryReader.open(FSDirectory.open(new File(indexDirectory)));
				IndexSearcher indexSearcher = new IndexSearcher(indexReader);
				TaxonomyReader taxonomyReader = new DirectoryTaxonomyReader(FSDirectory.open(new File(taxonomyDirectory)));
//************************************************
		FacetSearchParams searchParams = new FacetSearchParams(
				new DefaultFacetIndexingParams());
		Query luceneQuery = null;
		System.out.println("Seprate Fields by '/' \n"
						+ "Identify The Field(s) to specify the search or press Enter to search on all the Fields: ");
		Scanner scanIn = new Scanner(System.in);
		String identifiedField = scanIn.nextLine();
		System.out.println("Search : ");
		String fieldtoSearch = scanIn.nextLine();
		
		if (identifiedField.length() != 0) {

			QueryPharser queryPharser = new QueryPharser();
			List<String> map = queryPharser.getQueryMap(identifiedField);

			for (int i = 0; i < map.size(); i++) {
				System.out.println("Searching on Field " + map.get(i));
				searchParams.addFacetRequest(new CountFacetRequest(new CategoryPath(map.get(i)), 100));

				Analyzer analyzer = new MyLowerCaseKeywordAnalyzer();
				MultiFieldQueryParser queryParser = new MultiFieldQueryParser(LUCENE_VERSION, new String[] { map.get(i) }, analyzer);
				luceneQuery = queryParser.parse(fieldtoSearch);

			}// end for
		} else {
			System.out.println("Search : ");

			String allFields = prop.getProperty("allFields");

			QueryPharser queryPharser = new QueryPharser();
			List<String> map = queryPharser.getQueryMap(allFields);
			System.out.println("Searching on all Fields ");

			for (int i = 0; i < map.size(); i++) {
				System.out.println("Searching on Field " + map.get(i));
				searchParams.addFacetRequest(new CountFacetRequest(new CategoryPath(map.get(i)), 100));

				Analyzer analyzer = new MyLowerCaseKeywordAnalyzer();
				MultiFieldQueryParser queryParser = new MultiFieldQueryParser(LUCENE_VERSION, new String[] { map.get(i) }, analyzer);
				luceneQuery = queryParser.parse(fieldtoSearch);

			}// end for
		}//end else	
				
				 scanIn.close(); 
 //*************************************************
        TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(10, true);
		FacetsCollector facetsCollector = new FacetsCollector(searchParams, indexReader, taxonomyReader);
		indexSearcher.search(luceneQuery, MultiCollector.wrap(topScoreDocCollector, facetsCollector));//MultiCollector in order to do the search and collect the results and collect the facet
		
				/*
		if(topScoreDocCollector.topDocs()!=null){
			;*/
		System.out.println("Found:");
		for(ScoreDoc scoreDoc: topScoreDocCollector.topDocs().scoreDocs) { 
			Document document = indexReader.document(scoreDoc.doc);
			
			System.out.printf("Concept Object: conceptId=%s, conceptActive=%s, FSN=%s, relativeIdnType=%s, destinationConceptId=%s, relationType=%s,synonymDetails=%s,score=%f\n",
					document.get("conceptId"), document.get("conceptActive"),
					document.get("FSN"),
					document.get("relativeIdnType"),
					document.get("destinationConceptId"),
					document.get("relationType"),
					document.get("synonymDetails"),
					scoreDoc.score);
		
			
	
		taxonomyReader.close();
		indexReader.close();
		
		}//end for
		
	}//end of main
		
}
