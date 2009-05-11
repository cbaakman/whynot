package nl.ru.cmbi.why_not.hibernate;

import nl.ru.cmbi.why_not.hibernate.DAOFactory;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.DatabankDAO;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.EntryDAO;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.FileDAO;
import nl.ru.cmbi.why_not.model.Databank;
import nl.ru.cmbi.why_not.model.Entry;

import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.junit.Ignore;
import org.junit.Test;

public class DatabankTest {
	@Test
	@Ignore
	public void addEntry() throws Exception {
		Transaction transact = null;
		try {
			DAOFactory factory = DAOFactory.instance(DAOFactory.HIBERNATE);
			transact = factory.getSession().beginTransaction(); //Plain JDBC
			DatabankDAO dbdao = factory.getDatabankDAO();

			Databank db = dbdao.findByNaturalId(Restrictions.naturalId().set("name", "PDB"));

			db.getEntries().add(new Entry(db, "1TIM"));

			transact.commit();
		}
		catch (Exception e) {
			if (transact != null)
				transact.rollback();
			throw e;
		}
	}

	@Test
	public void addFile() throws Exception {
		DAOFactory factory = DAOFactory.instance(DAOFactory.HIBERNATE);
		Transaction transact = null;
		try {
			transact = factory.getSession().beginTransaction(); //Plain JDBC
			DatabankDAO dbdao = factory.getDatabankDAO();
			Databank db = dbdao.findByNaturalId(Restrictions.naturalId().set("name", "PDB"));

			EntryDAO entdao = factory.getEntryDAO();
			Entry ent = entdao.findByNaturalId(Restrictions.naturalId().set("databank", db).set("pdbid", "1tim"));

			FileDAO fldao = factory.getFileDAO();
			//STORE
			//ent.setFile(new File(new java.io.File("/home/tbeek/Desktop/raw/stats")));

			//DELETE
			fldao.makeTransient(ent.getFile());
			ent.setFile(null);

			System.out.println(ent.getFile());

			transact.commit();
		}
		catch (Exception e) {
			if (transact != null)
				transact.rollback();
			throw e;
		}
	}
}
