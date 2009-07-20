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
			Logger.getLogger(Crawler.class).info("Crawler start.");
			Crawler crawler = (Crawler) SpringUtil.getContext().getBean("crawler");

			//Should run before addCrawled
			crawler.removeChanged(args[0]);

			//Should run after removeChanged
			crawler.addCrawled(args[0], args[1]);

			Logger.getLogger(Crawler.class).info("Crawler done.");
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
	 * Removes entries from databank if
	 * <li>file on path does not exist
	 * <li>timestamp differs from timestamp of file on path
	 * <li>path does not match databank regex (which might have changed)
	 * <li>no file or parent entry file exists
	 */
	@Transactional
	public void removeChanged(String name) {
		Databank databank = dbdao.findByName(name);
		Pattern regex = Pattern.compile(databank.getRegex());
		boolean matchRegex = databank.getCrawltype() == CrawlType.FILE;
		int removed = 0;
		for (Entry entry : entrydao.getPresent(databank)) {
			String path = entry.getFile().getPath();
			File file = new File(path);
			//Check if file still exists
			if (!file.exists() || file.lastModified() != entry.getFile().getTimestamp() ||
			//Check if file still matches regex
			matchRegex && !regex.matcher(path).matches()) {
				//Remove entry
				databank.getEntries().remove(entry);
				entrydao.makeTransient(entry);
				removed++;
			}
		}
		for (Entry entry : entrydao.getObsolete(databank))
			if (entry.getFile() == null) {
				entrydao.makeTransient(entry);
				removed++;
			}
		Logger.getLogger(getClass()).info(databank.getName() + ": Removing " + removed + " changed Entries");
	}

	/**
	 * Adds all FileEntries in the given file or directory and subdirectories to database.
	 * Takes great care to delete old files when possible and to clear present annotations.
	 * <br/><br/>
	 * Extracts the PDBID from the filename/line using regular expression group matching:
	 * the PDBID should be enclosed in () and be the explicitly matching group number 1
	 * 
	 * Note: Strongly expects removeChanged to have run before
	 * @param file
	 */
	@Transactional
	public void addCrawled(String dbname, String path) throws IOException {
		Databank db = dbdao.findByName(dbname);
		switch (db.getCrawltype()) {
		case FILE:
			new FileCrawler(db, entrydao).crawl(getFile(path));
			break;
		case LINE:
			new LineCrawler(db, entrydao, filedao).crawl(getFile(path));
			break;
		default:
			throw new IllegalArgumentException("Invalid CrawlType");
		}
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
			path = path.substring(path.lastIndexOf('/') + 1);
			path.replaceAll("[^\\w]", "");
			File downloaded = new File("download/" + path);
			if (!downloaded.exists() || downloaded.lastModified() != con.getLastModified()) {
				//Overwrite file
				BufferedReader bf = new BufferedReader(new InputStreamReader(con.getInputStream()));
				PrintWriter pw = new PrintWriter(new FileWriter(downloaded));
				String line;
				while ((line = bf.readLine()) != null)
					pw.println(line);
				pw.close();
				bf.close();
				downloaded.setLastModified(con.getLastModified());
				Logger.getLogger(Crawler.class).info("Downloaded " + downloaded.getAbsolutePath());
			}
			path = downloaded.getAbsolutePath();
		}
		return new File(path);
	}
}
