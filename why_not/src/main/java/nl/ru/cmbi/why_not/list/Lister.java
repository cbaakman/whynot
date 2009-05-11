package nl.ru.cmbi.why_not.list;

import java.util.SortedSet;

import nl.ru.cmbi.why_not.hibernate.DAOFactory;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.DatabankDAO;
import nl.ru.cmbi.why_not.model.Databank;
import nl.ru.cmbi.why_not.model.Entry;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Lister {
	public static void main(String[] args) throws Exception {
		String dbname = "DATABASE";
		String fileFilter = "withFile|withoutFile";
		String parentFilter = "withParentFile|withoutParentFile";
		String commentFilter = "withComment|withoutComment|withOlderComment";
		String comment = "[\"Example comment\"]";

		if (args.length != 4 && args.length != 5 || !args[1].matches(fileFilter) || !args[2].matches(parentFilter) || !args[3].matches(commentFilter))
			throw new IllegalArgumentException("Usage: list DATABASE " + fileFilter + " " + parentFilter + " " + commentFilter + " " + comment);

		dbname = args[0];
		fileFilter = args[1];
		parentFilter = args[2];
		commentFilter = args[3];
		comment = "%"; //Wildcard
		if (args.length == 5)
			comment = args[4];

		ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "spring.xml" });
		Lister lister = (Lister) context.getBean("lister");
		lister.list(dbname, fileFilter, parentFilter, commentFilter, comment);
	}

	@Autowired
	private DAOFactory	DAOFactory;

	@Transactional
	public void list(String dbname, String fileFilter, String parentFilter, String commentFilter, String comment) throws Exception {
		Transaction transact = null;
		try {
			Session session = DAOFactory.getSession();
			transact = session.beginTransaction(); //Plain JDBC

			DatabankDAO dbdao = DAOFactory.getDatabankDAO();
			Databank db = dbdao.findByNaturalId(Restrictions.naturalId().set("name", dbname));
			if (db == null)
				new IllegalArgumentException("Databank with name " + dbname + " not found.");

			DAOFactory.getSession().enableFilter(fileFilter);
			DAOFactory.getSession().enableFilter(parentFilter);
			DAOFactory.getSession().enableFilter(commentFilter).setParameter("comment", comment);

			SortedSet<Entry> entries = db.getEntries();
			System.out.println("#" + dbname + " " + fileFilter + " " + parentFilter + " " + commentFilter + ": " + entries.size() + " entries");
			for (Entry entry : entries)
				System.out.println(entry + "," + (entry.getFile() != null ? entry.getFile().getTimestamp() : -1));

			transact.commit(); //Plain JDBC
			Logger.getLogger(Lister.class).debug("list DATABASE " + fileFilter + " " + parentFilter + " " + commentFilter + " \"" + comment + "\": Succes");
		}
		catch (Exception e) {
			if (transact != null)
				transact.rollback();
			Logger.getLogger(Lister.class).error("list DATABASE " + fileFilter + " " + parentFilter + " " + commentFilter + " \"" + comment + "\": Failure");
			throw e;
		}
	}
}
