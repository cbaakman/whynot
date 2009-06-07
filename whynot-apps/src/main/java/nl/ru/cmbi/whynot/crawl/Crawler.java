package nl.ru.cmbi.whynot.crawl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.regex.Pattern;

import nl.ru.cmbi.whynot.hibernate.GenericDAO.DatabankDAO;
import nl.ru.cmbi.whynot.hibernate.GenericDAO.EntryDAO;
import nl.ru.cmbi.whynot.hibernate.GenericDAO.FileDAO;
import nl.ru.cmbi.whynot.model.Databank;
import nl.ru.cmbi.whynot.model.Entry;
import nl.ru.cmbi.whynot.model.Databank.CrawlType;
import nl.ru.cmbi.whynot.util.SpringUtil;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Crawler {
	public static void main(String[] args) throws Exception {
		if (args.length == 2) {
			Crawler crawler = (Crawler) SpringUtil.getContext().getBean("crawler");
			crawler.crawl(args[0], args[1]);
			crawler.validate(args[0]);
		}
		else
			throw new IllegalArgumentException("Usage: crawler DATABASE DIRECTORY/FILE");
	}

	@Autowired
	private DatabankDAO	dbdao;
	@Autowired
	private EntryDAO	entrydao;
	@Autowired
	private FileDAO		filedao;

	/**
	 * Adds all FileEntries in the given file or directory and subdirectories to database
	 * 
	 * Extracts the PDBID from the filename/line using regular expression group matching:
	 * the PDBID should be enclosed in () and be the explicitly matching group number 1
	 * @param file
	 */
	@Transactional
	public void crawl(String dbname, String path) throws IOException {
		Databank db = dbdao.findByExample(new Databank(dbname), "id", "reference", "filelink", "parent", "regex", "crawltype", "entries");
		ICrawler fc;
		switch (db.getCrawltype()) {
		case FILE:
			fc = new FileCrawler(db, filedao);
			break;
		case LINE:
			fc = new LineCrawler(db, filedao);
			break;
		default:
			throw new IllegalArgumentException("Invalid CrawlType");
		}
		fc.addEntriesIn(getFile(path));
		Logger.getLogger(getClass()).info(dbname + ": Succes");
	}

	/**
	 * Removes all the invalid FileEntries from database by checking if the file exists,
	 * if the file matches the current regular expression (which might have changed) and
	 * if the timestamp on the file is still the same as the timestamp on the entry
	 */
	@Transactional
	public void validate(String dbname) {
		Databank databank = dbdao.findByName(dbname);
		Pattern pattern = Pattern.compile(databank.getRegex());

		List<Entry> entrieswithfiles = entrydao.getValid(databank);
		entrieswithfiles.addAll(entrydao.getObsolete(databank));

		int checked = 0, removed = 0;
		for (Entry entry : entrieswithfiles) {
			nl.ru.cmbi.whynot.model.File stored = entry.getFile();
			if (stored == null)//Should not happen, but to play safe skip
				continue;
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
			if (!isValid && databank.getCrawltype() == CrawlType.FILE) {
				//FIXME LINE CRAWLED FILES ARENT REMOVED OR UPDATED HERE
				filedao.makeTransient(stored);
				entry.setFile(null);
				removed++;
			}
		}
		entrieswithfiles = null;

		Logger.getLogger(getClass()).info(databank.getName() + ": Checked " + checked + ", Removed " + removed);
	}

	/**
	 * Gets the file on the supplied path. If the path starts with http://
	 * we first store a local copy with the same timestamp and return that.
	 * @param path
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static File getFile(String path) throws IOException, MalformedURLException {
		if (path.startsWith("http://")) {
			File dirDownload = new File("download/");
			//Make sure download directory exist
			if (!dirDownload.isDirectory() && !dirDownload.mkdir())
				throw new FileNotFoundException(dirDownload.getAbsolutePath());

			//Open URL
			URLConnection con = new URL(path).openConnection();
			File downloaded = new File("download/" + path.replaceAll("[^\\w]", ""));
			if (!downloaded.exists() || downloaded.lastModified() != con.getLastModified()) {
				//Overwrite file
				BufferedReader bf = new BufferedReader(new InputStreamReader(con.getInputStream()));
				PrintWriter pw = new PrintWriter(new FileWriter(downloaded));
				String line;
				while ((line = bf.readLine()) != null)
					pw.println(line);
				pw.close();
				downloaded.setLastModified(con.getLastModified());
				Logger.getLogger(Crawler.class).info("Downloaded " + downloaded.getAbsolutePath());
			}
			path = downloaded.getAbsolutePath();
		}
		return new File(path);
	}
}
