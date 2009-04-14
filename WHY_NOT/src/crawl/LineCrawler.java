package crawl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;

import model.Database;
import model.EntryFile;

public class LineCrawler extends AbstractCrawler {
	/**
	 * Flat file crawler
	 * @param db
	 */
	public LineCrawler(Database db) {
		super(db);
	}

	@Override
	public int addEntriesIn(String filepath) throws IOException {
		int count = 0;
		long lastmodified = new File(filepath).lastModified();
		BufferedReader bf = new BufferedReader(new FileReader(filepath));
		for (String line = ""; (line = bf.readLine()) != null;) {
			Matcher m = pattern.matcher(line);
			if (m.matches())
				if (database.getEntries().add(new EntryFile(database, m.group(1), filepath, lastmodified)))
					count++;
		}
		bf.close();
		return count;
	}
}
