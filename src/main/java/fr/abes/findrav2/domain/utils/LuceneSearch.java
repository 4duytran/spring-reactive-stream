package fr.abes.findrav2.domain.utils;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;


public class LuceneSearch {

    public static float Search(String texte,String query) throws ParseException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        MemoryIndex index = new MemoryIndex();
        index.addField("content",texte, analyzer);
        return index.search(new QueryParser("content", analyzer).parse(query));

    }


}
