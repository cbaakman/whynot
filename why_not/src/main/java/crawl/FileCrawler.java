package crawl;

import java.io.File;
import java.io.FileFilter;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;

import model.Databank;
import model.EntryPK;
import dao.interfaces.FileDAO;

public class FileCrawler extends AbstractCrawler {
	private FileFilter	entryfilter, directoryfilter;

	/**
	 * Recursive directory crawler
	 * @param db
	 */
	public FileCrawler(Databank db) {
		super(db);
		entryfilter = new FileFilter() {
			public boolean accept(File pathname) {
				return pattern.matcher(pathname.getAbsolutePath()).matches();
			}
		};
		directoryfilter = new FileFilter() {
			public boolean accept(File pathname) {
				//Sometimes entries are directories: Do not crawl these directories
				return !entryfilter.accept(pathname) && pathname.isDirectory();
			}
		};
	}

	@Override
	public int addEntriesIn(String path) {
		FileDAO fldao = Crawler.factory.getFileDAO();

		int count = 0;
		for (File dir : dirAndAllSubdirs(new File(path)))
			for (File file : dir.listFiles(entryfilter)) {
				Matcher m = pattern.matcher(file.getAbsolutePath());
				if (m.matches()) {
					String id = m.group(1).toLowerCase();
					model.File ef = fldao.findById(new EntryPK(database, id), true);
					if (ef != null) {
						ef.setPath(file.getAbsolutePath());
						ef.setTimestamp(file.lastModified());
					}
					else {
						new model.File(database, id, file.getAbsolutePath(), file.lastModified());
						count++;
					}
				}
			}
		return count;
	}

	/**
	 * Creates set of directories containing argument and all recursive
	 * subdirectories in argument, excluding directories that match entryfilter
	 */
	private SortedSet<File> dirAndAllSubdirs(File directory) {
		SortedSet<File> directories = new TreeSet<File>();
		directories.add(directory); // Add this
		for (File subdir : directory.listFiles(directoryfilter))
			directories.addAll(dirAndAllSubdirs(subdir)); // Add recursive subdirs
		return directories;
	}
}
