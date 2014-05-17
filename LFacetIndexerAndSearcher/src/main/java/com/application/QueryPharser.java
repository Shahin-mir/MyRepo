package com.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryPharser {
	
	
	public  List<String> getQueryMap(String query)  
	 {  int i = 0;
	     String[] params = query.split("/");  //String are seprated by /
	     List<String> map = new ArrayList<String>();
	     
	     for (String param : params)  
	     {  
	         map.add(i, param);  
	         i++;
	     }  
	     return map;  
	 }  
}
