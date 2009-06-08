package nl.ru.cmbi.whynot.hibernate;

import java.util.List;

import nl.ru.cmbi.whynot.hibernate.GenericDAO.EntryDAO;
import nl.ru.cmbi.whynot.model.Databank;
import nl.ru.cmbi.whynot.model.Entry;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntryHibernateDAO extends GenericHibernateDAO<Entry, Long> implements EntryDAO {
	public Entry findByDatabankAndPdbid(Databank databank, String pdbid) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.add(Restrictions.naturalId().set("databank", databank).set("pdbid", pdbid));
		return (Entry) crit.uniqueResult();
	}

	@Autowired
	DatabankDAO	databankdao;

	@Override
	public Entry getParent(Entry entry) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.add(Restrictions.naturalId().set("databank", entry.getDatabank().getParent()));
		crit.add(Restrictions.naturalId().set("pdbid", entry.getPdbid()));
		return (Entry) crit.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	public List<Entry> getChildren(Entry entry) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		crit.add(Restrictions.in("databank", databankdao.getChildren(entry.getDatabank())));
		crit.add(Restrictions.naturalId().set("pdbid", entry.getPdbid()));
		return crit.list();
	}

	public int removeEntriesWithoutBothFileAndParentFile() {
		int removed = 0;
		for (Databank child : databankdao.findAll()) {
			Query q = getSession().createQuery("delete from Annotation where entry_id in (select child.id from Entry child where file is null and child.databank = :child_db and (select parent.file from Entry parent where parent.pdbid = child.pdbid and parent.databank = :parent_db) is null)");
			q.setParameter("child_db", child);
			q.setParameter("parent_db", child.getParent());
			q.executeUpdate();

			Query q2 = getSession().createQuery("delete from Entry child where file is null and child.databank = :child_db and (select parent.file from Entry parent where parent.pdbid = child.pdbid and parent.databank = :parent_db) is null");
			q2.setParameter("child_db", child);
			q2.setParameter("parent_db", child.getParent());
			removed += q2.executeUpdate();
		}
		if (0 < removed)
			Logger.getLogger(getClass()).info("Removed " + removed + " entries with comment, but without both file and without parent file: Not missing!");
		return removed;
	}

	//TODO Rewrite some of these queries to projections

	@SuppressWarnings("unchecked")
	public List<Entry> getValid(Databank child) {//Child file present, parent file present
		Query q = getSession().createQuery("from Entry child where file is not null and child.databank = :child_db and (select parent.file from Entry parent where parent.pdbid = child.pdbid and parent.databank = :parent_db) is not null");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return q.list();
	}

	public long getValidCount(Databank child) {//Child file present, parent file present
		Query q = getSession().createQuery("select count(*) from Entry child where file is not null and child.databank = :child_db and (select parent.file from Entry parent where parent.pdbid = child.pdbid and parent.databank = :parent_db) is not null");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return (Long) q.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	public List<Entry> getObsolete(Databank child) {//Child file present, no parent file
		Query q = getSession().createQuery("from Entry child where file is not null and child.databank = :child_db and (select parent.file from Entry parent where parent.pdbid = child.pdbid and parent.databank = :parent_db) is null");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return q.list();
	}

	public long getObsoleteCount(Databank child) {//Child file present, no parent file
		Query q = getSession().createQuery("select count(*) from Entry child where file is not null and child.databank = :child_db and (select parent.file from Entry parent where parent.pdbid = child.pdbid and parent.databank = :parent_db) is null");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return (Long) q.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	public List<Entry> getMissing(Databank child) {//Parent file present, no child file
		Query q = getSession().createQuery("from Entry parent where file is not null and parent.databank = :parent_db and (select child.file from Entry child where parent.pdbid = child.pdbid and child.databank = :child_db) is null");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return q.list();
	}

	public long getMissingCount(Databank child) {//Parent file present, no child file
		Query q = getSession().createQuery("select count(*) from Entry parent where file is not null and parent.databank = :parent_db and (select child.file from Entry child where parent.pdbid = child.pdbid and child.databank = :child_db) is null");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return (Long) q.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	public List<Entry> getUnannotated(Databank child) {//Parent file present, no child (because no file & no annotation => no child)
		Query q = getSession().createQuery("from Entry parent where file is not null and parent.databank = :parent_db and (select child from Entry child where parent.pdbid = child.pdbid and child.databank = :child_db) is null");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return q.list();
	}

	public long getUnannotatedCount(Databank child) {//Parent file present, no child (because no file & no annotation => no child)
		Query q = getSession().createQuery("select count(*) from Entry parent where file is not null and parent.databank = :parent_db and (select child from Entry child where parent.pdbid = child.pdbid and child.databank = :child_db) is null");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return (Long) q.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	public List<Entry> getAnnotated(Databank child) {//No child file (annotations implicitly present or there wouldnt be a child)
		Query q = getSession().createQuery("from Entry child where child.file is null and child.databank = :child_db");
		q.setParameter("child_db", child);
		return q.list();
	}

	public long getAnnotatedCount(Databank child) {//No child file (annotations implicitly present or there wouldnt be a child)
		Query q = getSession().createQuery("select count(*) from Entry child where child.file is null and child.databank = :child_db");
		q.setParameter("child_db", child);
		return (Long) q.uniqueResult();
	}
}
