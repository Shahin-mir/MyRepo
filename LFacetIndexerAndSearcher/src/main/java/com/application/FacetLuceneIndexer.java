
package com.application;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.index.CategoryDocumentBuilder;
import org.apache.lucene.facet.index.params.DefaultFacetIndexingParams;
import org.apache.lucene.facet.taxonomy.CategoryPath;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.json.JSONArray;
import org.json.JSONObject;

class FacetLuceneIndexer {
	private static Version LUCENE_VERSION = Version.LUCENE_40;
	public static void main(String args[]) throws Exception {
		
// to do: Load properties from Property file
		Properties prop = new Properties();
		InputStream input = new FileInputStream("config.properties");
		
		prop.load(input); //load property file
		String indexDirectory = prop.getProperty("indexDirectory");
		String taxonomyDirectory = prop.getProperty("taxonomyDirectory");
		String jObjectDirectory = prop.getProperty("jObjectDirectory");
		input.close();

		IndexWriterConfig writerConfig = new IndexWriterConfig(LUCENE_VERSION, new WhitespaceAnalyzer(LUCENE_VERSION));
		writerConfig.setOpenMode(OpenMode.CREATE);
		IndexWriter indexWriter = new IndexWriter(FSDirectory.open(new File(indexDirectory)), writerConfig);//new File(indexDirectory)
//It contains information about all the categories that ever existed in any document in the index.
		TaxonomyWriter taxonomyWriter = new DirectoryTaxonomyWriter(new MMapDirectory(new File(taxonomyDirectory)), OpenMode.CREATE);//new File(taxonomyDirectory)
// DirectoryTaxonomyWriter manages a hierarchical taxonomy of categories by adding categories to the taxonomy
		CategoryDocumentBuilder categoryDocumentBuilder = new CategoryDocumentBuilder(taxonomyWriter, new DefaultFacetIndexingParams());
//A CategoryDocumentBuilder is created and later, set with the appropriate list of categories,
		
		
// ****Iterate throught a folder
		DirectoryStream<Path> ds = Files.newDirectoryStream(FileSystems.getDefault().getPath(jObjectDirectory));
				
		for (Path p : ds) {

			File file = p.toFile();

			 if (file.isFile() && file.getName().endsWith(".json")) {
					
		String content = IOUtils.toString(new FileInputStream(file));
		JSONArray conceptArray = new JSONArray(content);

		Field conceptIdField = new TextField("conceptId", "", Store.YES);
		Field conceptActiveField = new TextField("conceptActive", "", Store.YES);
		Field FSNField = new TextField("FSN", "", Store.YES);
		Field relationField = new TextField("relativeIdnType", "", Store.YES);
		Field synonymDetailsField = new TextField("synonymDetails", "", Store.YES);
		

		for(int i = 0 ; i < conceptArray.length() ; i++) {
			Document document = new Document();

			JSONObject concept = conceptArray.getJSONObject(i);
			String conceptId = concept.getString("conceptId");
			String conceptActive = concept.getString("conceptActive");
			String FSN = concept.getString("FSN");
//A CategoryPath holds a sequence of string components, specifying the hierarchical name of a category. 
    		List<CategoryPath> categoryPaths = new ArrayList<CategoryPath>(); 
			String relationString = "";
			String SynonymString = "";
			JSONArray relations = concept.getJSONArray("relativeIdnType");
			JSONArray synonyms = concept.getJSONArray("synonymDetails");
			
			
			for(int j = 0 ; j < relations.length() ; j++) {
				String relation = relations.getString(j);
				if (j > 0) {
					relationString += ", ";
				}
//Categories that should be added to the document are accumulated in the categories list.				
				categoryPaths.add(new CategoryPath("relativeIdnType", relation)); 
				relationString += relation;
			}
			
			for(int j = 0 ; j < synonyms.length() ; j++) {
				String synonym = synonyms.getString(j);
				if (j > 0) {
					SynonymString += ", ";
				}
//Categories that should be added to the document are accumulated in the categories list.				
				categoryPaths.add(new CategoryPath("synonymDetails", synonym));
				SynonymString += synonym;
			}
			
			
			categoryPaths.add(new CategoryPath("FSN" + FSN));
// set with the appropriate list of categories, and invoked to build - that is, to populate the document with categories
			categoryDocumentBuilder.setCategoryPaths(categoryPaths); 
			categoryDocumentBuilder.build(document);

			conceptIdField.setStringValue(conceptId);
			conceptActiveField.setStringValue(conceptActive);
			relationField.setStringValue(relationString);
			FSNField.setStringValue(FSN);
			synonymDetailsField.setStringValue(SynonymString);

			document.add(conceptIdField);
			document.add(conceptActiveField);
			document.add(relationField);
			document.add(FSNField);
			document.add(synonymDetailsField);
//Add the document to the index. As a result, category info is saved also in the regular search index, for supporting facet aggregation at search time
			indexWriter.addDocument(document);

			System.out.printf("Concept: Concept id=%s, concept Active=%s, FSN=%s, destinationId and relationType=%s, Synonyms=%s\n",
				conceptId, conceptActive, FSN, relations, synonyms);
		}//end of for loop for Concept array
		
	}//end of if
}// end of for loop
		taxonomyWriter.commit();
		taxonomyWriter.close();

		indexWriter.commit();
		indexWriter.close();
	}//end of exception
}
