package nl.ru.cmbi.whynot.hibernate;

import java.util.List;

import nl.ru.cmbi.whynot.hibernate.GenericDAO.EntryDAO;
import nl.ru.cmbi.whynot.model.Databank;
import nl.ru.cmbi.whynot.model.Entry;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;

@Service
public class EntryHibernateDAO extends GenericHibernateDAO<Entry, Long> implements EntryDAO {
	public Entry findByDatabankAndPdbid(Databank databank, String pdbid) {
		Criteria crit = createCriteria(Restrictions.naturalId().set("databank", databank).set("pdbid", pdbid));
		return (Entry) crit.uniqueResult();
	}

	public boolean contains(String pdbid) {
		Criteria crit = createCriteria(Restrictions.naturalId().set("pdbid", pdbid));
		crit.setProjection(Projections.rowCount());
		return 0 < (Integer) crit.uniqueResult();
	}

	//Present
	@SuppressWarnings("unchecked")
	public List<Entry> getPresent(Databank db) {
		return getSession().createFilter(db.getEntries(), "where this.file is not null").list();
	}

	public int countPresent(Databank db) {
		Criteria crit = createCriteria(Restrictions.eq("databank", db), Restrictions.isNotNull("file"));
		return (Integer) crit.setProjection(Projections.rowCount()).uniqueResult();
	}

	//Valid
	@SuppressWarnings("unchecked")
	public List<Entry> getValid(Databank db) {
		return getSession().createFilter(db.getEntries(), "where this.file is not null and (select par.file from this.databank.parent.entries par where par.pdbid = this.pdbid) is not null").list();
	}

	public int countValid(Databank db) {
		Criteria crit = createCriteria(Restrictions.eq("databank", db), Restrictions.isNotNull("file"));
		crit.add(Restrictions.sqlRestriction("(select parent.file_id from Entry parent where parent.pdbid = {alias}.pdbid and parent.databank_id = ?) is not null", db.getParent().getId(), Hibernate.LONG));
		return (Integer) crit.setProjection(Projections.rowCount()).uniqueResult();
	}

	//Obsolete
	@SuppressWarnings("unchecked")
	public List<Entry> getObsolete(Databank db) {
		return getSession().createFilter(db.getEntries(), "where (select par.file from this.databank.parent.entries par where par.pdbid = this.pdbid) is null").list();
	}

	public int countObsolete(Databank db) {
		Criteria crit = createCriteria(Restrictions.eq("databank", db));
		crit.add(Restrictions.sqlRestriction("(select parent.file_id from Entry parent where parent.pdbid = {alias}.pdbid and parent.databank_id = ?) is null", db.getParent().getId(), Hibernate.LONG));
		return (Integer) crit.setProjection(Projections.rowCount()).uniqueResult();
	}

	//Annotated
	@SuppressWarnings("unchecked")
	public List<Entry> getAnnotated(Databank db) {
		return getSession().createFilter(db.getEntries(), "where this.annotations is not empty").list();
	}

	public int countAnnotated(Databank db) {
		Criteria crit = createCriteria(Restrictions.eq("databank", db), Restrictions.isNotEmpty("annotations"));
		return (Integer) crit.setProjection(Projections.rowCount()).uniqueResult();
	}

	//Missing
	@SuppressWarnings("unchecked")
	public List<Entry> getMissing(Databank child) {
		Criteria crit = createCriteria(Restrictions.eq("databank", child.getParent()), Restrictions.isNotNull("file"));
		crit.add(Restrictions.sqlRestriction("(select child.file_id from Entry child where {alias}.pdbid = child.pdbid and child.databank_id = ?) is null", child.getId(), Hibernate.LONG));
		return crit.addOrder(Order.asc("pdbid")).list();
	}

	public int countMissing(Databank child) {
		Criteria crit = createCriteria(Restrictions.eq("databank", child.getParent()), Restrictions.isNotNull("file"));
		crit.add(Restrictions.sqlRestriction("(select child.file_id from Entry child where {alias}.pdbid = child.pdbid and child.databank_id = ?) is null", child.getId(), Hibernate.LONG));
		return (Integer) crit.setProjection(Projections.rowCount()).uniqueResult();
	}

	//Unannotated
	@SuppressWarnings("unchecked")
	public List<Entry> getUnannotated(Databank child) {
		Criteria crit = createCriteria(Restrictions.eq("databank", child.getParent()), Restrictions.isNotNull("file"));
		crit.add(Restrictions.sqlRestriction("(select child from Entry child where {alias}.pdbid = child.pdbid and child.databank_id = ?) is null", child.getId(), Hibernate.LONG));
		return crit.addOrder(Order.asc("pdbid")).list();
	}

	public int counUnannotated(Databank child) {
		Criteria crit = createCriteria(Restrictions.eq("databank", child.getParent()), Restrictions.isNotNull("file"));
		crit.add(Restrictions.sqlRestriction("(select child from Entry child where {alias}.pdbid = child.pdbid and child.databank_id = ?) is null", child.getId(), Hibernate.LONG));
		return (Integer) crit.setProjection(Projections.rowCount()).uniqueResult();
	}
}
