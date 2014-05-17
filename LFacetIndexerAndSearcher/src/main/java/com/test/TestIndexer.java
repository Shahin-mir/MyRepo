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
import java.io.FileInputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.application.*;

/**
 * @author Shahin
 * 
 */
class TestIndexer {
	private static Version LUCENE_VERSION = Version.LUCENE_40;

	public static void main(String args[]) throws Exception {
		if (args.length != 2) {
			System.err
					.println("Parameters: [index directory] [taxonomy directory]");
			System.exit(1);
		}

		String indexDirectory = args[0];
		String taxonomyDirectory = args[1];
		// String jsonFileName = args[2];

		/*IndexWriterConfig writerConfig = new IndexWriterConfig(LUCENE_VERSION,
				new WhitespaceAnalyzer(LUCENE_VERSION));*/
		IndexWriterConfig writerConfig = new IndexWriterConfig(LUCENE_VERSION,new MyLowerCaseKeywordAnalyzer());
		
		
		writerConfig.setOpenMode(OpenMode.CREATE);
		IndexWriter indexWriter = new IndexWriter(FSDirectory.open(new File(indexDirectory)), writerConfig);

		TaxonomyWriter taxonomyWriter = new DirectoryTaxonomyWriter(new MMapDirectory(new File(taxonomyDirectory)), OpenMode.CREATE);
		CategoryDocumentBuilder categoryDocumentBuilder = new CategoryDocumentBuilder(taxonomyWriter, new DefaultFacetIndexingParams());
		
		// ****Iterate throught a folder
		DirectoryStream<Path> ds = Files.newDirectoryStream(FileSystems.getDefault().getPath(
		"C://ProjectWorkplace//facet-lucene-example-master//"));
		for (Path p : ds) {

			File file = p.toFile();

			if (file.isFile() && file.getName().endsWith(".json")) {

				String content = IOUtils.toString(new FileInputStream(file));

				JSONArray bookArray = new JSONArray(content);
				
				
				Field idField = new IntField("id", 0, Store.YES );
				//NumericField idField = new NumericField("id");
				Field titleField = new TextField("title", "", Store.YES);
				Field authorsField = new TextField("authors", "", Store.YES);
				Field bookCategoryField = new TextField("book_category", "",
						Store.YES);
				Field testField = new TextField("test", "", Store.YES);

				for (int i = 0; i < bookArray.length(); i++) {
					Document document = new Document();

					JSONObject book = bookArray.getJSONObject(i);
					int id = book.getInt("id");
					String title = book.getString("title");
					String bookCategory = book.getString("book_category");
					String test = book.getString("test");

					List<CategoryPath> categoryPaths = new ArrayList<CategoryPath>();
					String authorsString = "";
					JSONArray authors = book.getJSONArray("authors");
					for (int j = 0; j < authors.length(); j++) {
						String author = authors.getString(j);
						if (j > 0) {
							authorsString += " , ";
						}
						categoryPaths.add(new CategoryPath("author", author));
						authorsString += author;
					}
					categoryPaths.add(new CategoryPath("book_category"
							+ bookCategory, '/'));
					categoryDocumentBuilder.setCategoryPaths(categoryPaths);
					categoryDocumentBuilder.build(document);

					idField.setIntValue(id);
					titleField.setStringValue(title);
					authorsField.setStringValue(authorsString);
					System.out.println(authorsString);
					bookCategoryField.setStringValue(bookCategory);
					testField.setStringValue(test);

					document.add(idField);
					document.add(titleField);
					document.add(authorsField);
					document.add(bookCategoryField);
					document.add(testField);

					indexWriter.addDocument(document);

					System.out
							.printf("Book: id=%d, title=%s, book_category=%s, authors=%s\n",
									id, title, bookCategory, authors);
				}
			}// end of if
		}// end of for loop
		taxonomyWriter.commit();
		taxonomyWriter.close();

		indexWriter.commit();
		indexWriter.close();

	}//end of main
	
	
}