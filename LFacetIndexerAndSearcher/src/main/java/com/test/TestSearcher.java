/*
 * Copyright (c) 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.test;

import java.io.File;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.index.params.DefaultFacetIndexingParams;
import org.apache.lucene.facet.search.FacetsCollector;
import org.apache.lucene.facet.search.params.CountFacetRequest;
import org.apache.lucene.facet.search.params.FacetSearchParams;
import org.apache.lucene.facet.search.results.FacetResult;
import org.apache.lucene.facet.search.results.FacetResultNode;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.complexPhrase.ComplexPhraseQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.application.*;;

/**
 * @author Shahin
 * 
 */
class TestSearcher {
	private static Version LUCENE_VERSION = Version.LUCENE_40;
	public static void main(String args[]) throws Exception {
		if (args.length != 3) {
			System.err.println("Parameters: [index directory] [taxonomy directory] [query]");
			System.exit(1);
		}
		
		String indexDirectory = args[0];
		String taxonomyDirectory = args[1];
		String query = args[2];
		
		IndexReader indexReader = DirectoryReader.open(FSDirectory.open(new File(indexDirectory)));
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);

		TaxonomyReader taxonomyReader = new DirectoryTaxonomyReader(FSDirectory.open(new File(taxonomyDirectory)));//read the categories by taxonomyReader

		FacetSearchParams searchParams = new FacetSearchParams(new DefaultFacetIndexingParams());
		searchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("authors"), 100));// those facet with multiple value, 100 is number of category that we want to keep
		searchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("book_category"), 100));// those facet with multiple value
		searchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("id"), 100));// we are savin the info,why not show in found????
		searchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("test"), 100));
		searchParams.addFacetRequest(new CountFacetRequest(new CategoryPath("title"), 100));
		
		
		//MultiFieldQueryParser searches for the SAME query keywords accross ALL specified fields
	   // MultiFieldQueryParser queryParser = new MultiFieldQueryParser(LUCENE_VERSION, new String[] {"test"},new MyLowerCaseKeywordAnalyzer());
			

		//********************************

          //Analyzer analyzer = new WhitespaceAnalyzer(LUCENE_VERSION); 
		  //Analyzer analyzer = new StandardAnalyzer(LUCENE_VERSION);
		   Analyzer analyzer = new MyLowerCaseKeywordAnalyzer(); 
          // QueryParser queryParser = new QueryParser(LUCENE_VERSION, "test", analyzer);
           MultiFieldQueryParser queryParser = new MultiFieldQueryParser(LUCENE_VERSION, new String[] {"id"},analyzer);
           Query luceneQuery = queryParser.parse(query);
        
	
		//**************************************
		//this searchers for the query keywords ONLY in the specified field
		//QueryParser testQP = new QueryParser(LUCENE_VERSION, "authors", new StandardAnalyzer(LUCENE_VERSION));
		
			
		//Query luceneQuery = testQP.parse(query);
		//Collectors to get top results and facets
		TopScoreDocCollector topScoreDocCollector = TopScoreDocCollector.create(10, true);
		FacetsCollector facetsCollector = new FacetsCollector(searchParams, indexReader, taxonomyReader);//count for a set of categories the number of matching documents:
		
		
		indexSearcher.search(luceneQuery, MultiCollector.wrap(topScoreDocCollector, facetsCollector));//MultiCollector in order to do the search and collect the results and collect the facet
		System.out.println("Found:");
		
		for(ScoreDoc scoreDoc: topScoreDocCollector.topDocs().scoreDocs) { // provide access to each result intopDocs 
			Document document = indexReader.document(scoreDoc.doc);
			System.out.printf("- book: id=%s, title=%s, book_category=%s, authors=%s,test=%s, score=%f\n",
					document.get("id"), document.get("title"),
					document.get("book_category"),
					document.get("authors"),
					document.get("test"),
					scoreDoc.score);
		}

		System.out.println("Facets:");
		for(FacetResult facetResult: facetsCollector.getFacetResults()) {
			System.out.println("- " + facetResult.getFacetResultNode().getLabel());
			for(FacetResultNode facetResultNode: facetResult.getFacetResultNode().getSubResults()) {
				System.out.printf("    - %s (%f)\n", facetResultNode.getLabel().toString(),
						facetResultNode.getValue());
				for(FacetResultNode subFacetResultNode: facetResultNode.getSubResults()) {
					System.out.printf("        - %s (%f)\n", subFacetResultNode.getLabel().toString(),
							subFacetResultNode.getValue());
				}
			}
		}
		taxonomyReader.close();
		indexReader.close();
	}
}