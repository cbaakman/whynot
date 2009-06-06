package nl.ru.cmbi.whynot.crawl;

import java.io.IOException;
import java.util.regex.Pattern;

import nl.ru.cmbi.whynot.hibernate.GenericDAO.FileDAO;
import nl.ru.cmbi.whynot.model.Databank;
import nl.ru.cmbi.whynot.model.Entry;
import nl.ru.cmbi.whynot.model.File;
import nl.ru.cmbi.whynot.model.Databank.CrawlType;

import org.apache.log4j.Logger;

public abstract class AbstractCrawler {
	protected Databank	databank;
	protected FileDAO	filedao;
	protected Pattern	pattern;

	public AbstractCrawler(Databank db, FileDAO fldao) {
		databank = db;
		filedao = fldao;
		pattern = Pattern.compile(db.getRegex());
	}

	/**
	 * Adds all FileEntries in the given file or directory and subdirectories to database
	 * 
	 * Extracts the PDBID from the filename/line using regular expression group matching:
	 * the PDBID should be enclosed in parentheses () and be the explicitly matching group 1
	 * @param file
	 */
	public abstract void addEntriesIn(java.io.File file) throws IOException;

	/**
	 * Removes all the invalid FileEntries from database by checking if the file exists,
	 * if the file matches the current regular expression (which might have changed) and
	 * if the timestamp on the file is still the same as the timestamp on the entry
	 */
	public void removeInvalidEntries() {
		filedao.enableFilter("withFile");

		int checked = 0, removed = 0;
		for (Entry entry : databank.getEntries()) {
			File stored = entry.getFile();
			if (stored == null)//Should not happen because of above filter
				continue;//But to be safe, we'll skip just like we used to
			checked++;

			boolean isValid = true;

			//Check if file still exists
			java.io.File found = new java.io.File(stored.getPath());
			if (!found.exists() || found.lastModified() != stored.getTimestamp())
				isValid = false;

			//Check if file still matches regex
			if (databank.getCrawltype() == CrawlType.FILE && !pattern.matcher(stored.getPath()).matches())
				isValid = false;

			//Remove invalid files
			if (!isValid) {
				filedao.makeTransient(entry.getFile());
				entry.setFile(null);
				removed++;
			}
		}

		filedao.disableFilter("withFile");

		Logger.getLogger(getClass()).info(databank.getName() + ": Checked " + checked + ", Removed " + removed);
	}
}
