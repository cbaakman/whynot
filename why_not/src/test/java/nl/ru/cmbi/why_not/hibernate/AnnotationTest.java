package nl.ru.cmbi.why_not.hibernate;

import nl.ru.cmbi.why_not.hibernate.GenericDAO.CommentDAO;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.DatabankDAO;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.EntryDAO;
import nl.ru.cmbi.why_not.model.Annotation;
import nl.ru.cmbi.why_not.model.Comment;
import nl.ru.cmbi.why_not.model.Databank;
import nl.ru.cmbi.why_not.model.Entry;

import org.hibernate.criterion.Restrictions;
import org.junit.Test;


public class AnnotationTest extends DAOTest {
	@Test
	public void storeAnnotation() throws Exception {
		transaction = factory.getSession().beginTransaction();

		CommentDAO comdao = factory.getCommentDAO();
		DatabankDAO dbdao = factory.getDatabankDAO();
		EntryDAO entdao = factory.getEntryDAO();

		//Find / create comment
		Comment comment = new Comment("Dit is mijn comment");
		Comment strdCom = comdao.findByNaturalId(Restrictions.naturalId().set("text", comment.getText()));
		if (strdCom != null)
			comment = strdCom;

		//Find databank
		Databank db = dbdao.findByNaturalId(Restrictions.naturalId().set("name", "PDB"));
		if (db == null)
			throw new Exception("DB NOT FOUND");

		//Find / create & store entry
		Entry entry = new Entry(db, "xTim");
		Entry strdEnt = entdao.findByNaturalId(Restrictions.naturalId().set("databank", db).set("pdbid", entry.getPdbid()));
		if (strdEnt != null)
			entry = strdEnt;
		else
			db.getEntries().add(entry);

		//Create & store annotation
		Annotation ann = new Annotation(comment, entry, System.currentTimeMillis());
		entry.getAnnotations().add(ann);

		transaction.commit();
	}

}