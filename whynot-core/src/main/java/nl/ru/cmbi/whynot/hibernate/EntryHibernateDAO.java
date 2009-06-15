package nl.ru.cmbi.whynot.hibernate;

import java.util.Collection;
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

	public boolean contains(String pdbid) {
		return !findByCriteria(Restrictions.naturalId().set("pdbid", pdbid)).isEmpty();
	}

	//TODO: Use these to filter collections internally
	//TODO: Also verify these work.. ;)
	@SuppressWarnings("unchecked")
	private List<Entry> withFile(Collection<Entry> collection) {
		return getSession().createFilter(collection, "where this.file is not null").list();
	}

	@SuppressWarnings("unchecked")
	private List<Entry> withoutFile(Collection<Entry> collection) {
		return getSession().createFilter(collection, "where this.file is null").list();
	}

	@SuppressWarnings("unchecked")
	private List<Entry> withAnnotations(Collection<Entry> collection) {
		return getSession().createFilter(collection, "where (select ann from Annotations ann where ann.entry_id = this.id) is not empty").list();
	}

	@SuppressWarnings("unchecked")
	private List<Entry> withoutAnnotations(Collection<Entry> collection) {
		return getSession().createFilter(collection, "where (select ann from Annotations ann where ann.entry_id = this.id) is empty").list();
	}

	@Autowired
	private DatabankDAO	databankdao;

	public int removeEntriesWithoutBothFileAndParentFile() {
		int removed = 0;
		for (Databank child : databankdao.findAll()) {
			//Delete Annotations from entries without file or parent file
			Query q1 = getSession().createQuery("delete from Annotation where entry_id in (select child.id from Entry child where file is null and child.databank = :child_db and (select parent.file from Entry parent where parent.pdbid = child.pdbid and parent.databank = :parent_db) is null)");
			q1.setParameter("child_db", child);
			q1.setParameter("parent_db", child.getParent());
			q1.executeUpdate();

			getSession().flush();

			//Delete empty comments
			Query q2 = getSession().createQuery("delete from Comment where id not in (select ann.comment.id from Annotation ann)");
			q2.executeUpdate();

			//Delete entries without file or parent file
			Query q3 = getSession().createQuery("delete from Entry child where file is null and child.databank = :child_db and (select parent.file from Entry parent where parent.pdbid = child.pdbid and parent.databank = :parent_db) is null");
			q3.setParameter("child_db", child);
			q3.setParameter("parent_db", child.getParent());
			removed += q3.executeUpdate();
		}
		if (0 < removed)
			Logger.getLogger(getClass()).info("Removed " + removed + " entries with comment, but without both file and without parent file: Not missing!");
		return removed;
	}

	//Child file present, parent file present
	private String	valid	= "from Entry child where file is not null and child.databank = :child_db and (select parent.file from Entry parent where parent.pdbid = child.pdbid and parent.databank = :parent_db) is not null";

	@SuppressWarnings("unchecked")
	public List<Entry> getValid(Databank child) {
		Query q = getSession().createQuery(valid + " order by pdbid");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return q.list();
	}

	public long getValidCount(Databank child) {
		Query q = getSession().createQuery("select count(*) " + valid);
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return (Long) q.uniqueResult();
	}

	//Child file present, no parent file
	private String	obsolete	= "from Entry child where file is not null and child.databank = :child_db and (select parent.file from Entry parent where parent.pdbid = child.pdbid and parent.databank = :parent_db) is null";

	@SuppressWarnings("unchecked")
	public List<Entry> getObsolete(Databank child) {
		Query q = getSession().createQuery(obsolete + " order by pdbid");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return q.list();
	}

	public long getObsoleteCount(Databank child) {
		Query q = getSession().createQuery("select count(*) " + obsolete);
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return (Long) q.uniqueResult();
	}

	//Parent file present, no child file
	private String	missing	= "from Entry parent where file is not null and parent.databank = :parent_db and (select child.file from Entry child where parent.pdbid = child.pdbid and child.databank = :child_db) is null";

	@SuppressWarnings("unchecked")
	public List<Entry> getMissing(Databank child) {
		Query q = getSession().createQuery(missing + " order by pdbid");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return q.list();
	}

	public long getMissingCount(Databank child) {
		Query q = getSession().createQuery("select count(*) " + missing);
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return (Long) q.uniqueResult();
	}

	//Parent file present, no child (because no file & no annotation => no child)
	private String	unannotated	= "from Entry parent where file is not null and parent.databank = :parent_db and (select child from Entry child where parent.pdbid = child.pdbid and child.databank = :child_db) is null";

	@SuppressWarnings("unchecked")
	public List<Entry> getUnannotated(Databank child) {
		Query q = getSession().createQuery(unannotated + " order by pdbid");
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return q.list();
	}

	public long getUnannotatedCount(Databank child) {
		Query q = getSession().createQuery("select count(*) " + unannotated);
		q.setParameter("child_db", child);
		q.setParameter("parent_db", child.getParent());
		return (Long) q.uniqueResult();
	}

	//No child file (annotations implicitly present or there wouldnt be a child)
	private String	annotated	= "from Entry child where child.file is null and child.databank = :child_db";

	@SuppressWarnings("unchecked")
	public List<Entry> getAnnotated(Databank child) {
		Query q = getSession().createQuery(annotated + " order by pdbid");
		q.setParameter("child_db", child);
		return q.list();
	}

	public long getAnnotatedCount(Databank child) {
		Query q = getSession().createQuery("select count(*) " + annotated);
		q.setParameter("child_db", child);
		return (Long) q.uniqueResult();
	}
}
