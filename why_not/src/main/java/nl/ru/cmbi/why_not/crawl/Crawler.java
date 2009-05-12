package nl.ru.cmbi.why_not.crawl;

import java.io.IOException;

import nl.ru.cmbi.why_not.hibernate.SpringUtil;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.DatabankDAO;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.FileDAO;
import nl.ru.cmbi.why_not.model.Databank;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class Crawler {
	public static void main(String[] args) throws Exception {
		if (args.length == 2)
			((Crawler) SpringUtil.getContext().getBean("crawler")).crawl(args[0], args[1]);
		else
			throw new IllegalArgumentException("Usage: crawler DATABASE DIRECTORY/FILE");
	}

	@Autowired
	private DatabankDAO	dbdao;
	@Autowired
	private FileDAO		filedao;

	public void crawl(String dbname, String path) throws IOException {
		Databank db = dbdao.findByNaturalId(Restrictions.naturalId().set("name", dbname));

		AbstractCrawler fc;
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
		fc.addEntriesIn(path);
		fc.removeInvalidEntries();

		Logger.getLogger(Crawler.class).info(dbname + ": Succes");
	}
}
