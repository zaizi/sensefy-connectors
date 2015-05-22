package org.apache.manifoldcf.agents.transformation.redlink.tests;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.google.common.collect.Lists;

public class Parsing {
	
	private static final String TEST_STRING = "http://rdf.freebase.com/ns/m.02hpz31-http://rdf.freebase.com/ns/m.01n7"
			+ "http://rdf.freebase.com/ns/m.01c5"
			+ "http://rdf.freebase.com/ns/m.035qyr9"
			+ "http://rdf.freebase.com/ns/m.02_775m";
	
	private static final Pattern prefix = 
    		Pattern.compile("http://rdf.freebase.com/ns/");

	@Test
	public void testHierarchyParsing(){
		List<String> values = Lists.newArrayList();
		String[] map = StringUtils.split(TEST_STRING, '-');
		String entityType = map[0];
		Assert.assertEquals(entityType, "http://rdf.freebase.com/ns/m.02hpz31");
		String hierarchy = map[1];
		Matcher m = prefix.matcher(hierarchy);
		List<Integer> positions = Lists.newArrayList();
		while(m.find())
			positions.add(m.start());
			
		for(int i = 0; i < positions.size() -1;i++)
			values.add(hierarchy.substring(positions.get(i), positions.get(i+1)));
			
		values.add(hierarchy.substring(positions.get(positions.size()-1)));
		
		Assert.assertEquals(values.get(0), "http://rdf.freebase.com/ns/m.01n7");
		Assert.assertEquals(values.get(1), "http://rdf.freebase.com/ns/m.01c5");
		Assert.assertEquals(values.get(2), "http://rdf.freebase.com/ns/m.035qyr9");
		Assert.assertEquals(values.get(3), "http://rdf.freebase.com/ns/m.02_775m");
	}
}
