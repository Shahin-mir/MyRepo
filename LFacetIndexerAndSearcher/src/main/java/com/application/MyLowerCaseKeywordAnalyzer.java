
package com.application;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.lv.LatvianStemFilter;
import org.apache.lucene.analysis.miscellaneous.KeywordMarkerFilter;
import org.apache.lucene.analysis.miscellaneous.TrimFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.util.Version;

/**
 * @author Shahin
 *
 */
public class MyLowerCaseKeywordAnalyzer extends Analyzer{
	
	private static Version LUCENE_VERSION = Version.LUCENE_40;

	@Override
	public TokenStreamComponents createComponents(String fieldName,Reader reader) {
		
		final Tokenizer source = new StandardTokenizer(LUCENE_VERSION, reader);
	    TokenStream sink = new StandardFilter(LUCENE_VERSION, source);
	    sink = new LowerCaseFilter(LUCENE_VERSION, sink);
	    return new TokenStreamComponents(source, sink);
	    
	    
	    /*Tokenizer tok = new KeywordTokenizer(reader); 
        TokenFilter filter = new LowerCaseFilter(LUCENE_VERSION, tok); 
       // filter = new TrimFilter(filter, true); 
        return new TokenStreamComponents(tok, filter); */
		
		
	}

	
}


	
	

