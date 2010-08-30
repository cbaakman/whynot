package nl.ru.cmbi.whynot.hibernate;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;

import nl.ru.cmbi.whynot.model.DomainObject;

/**
 * Abstract base class for all other Domain Access Objects.
 * 
 * @author tbeek
 * @param <T>
 *            the type of DomainObject persisted in implementing DAO.
 */
public abstract class GenericHibernateDAO<T extends DomainObject> implements GenericDAO<T> {
	private Class<T>		persistentClass;

	@Autowired
	private SessionFactory	sessionFactory;

	/**
	 * Retrieve type argument <T extends {@link DomainObject} and set it as persistentClass.
	 */
	@SuppressWarnings("unchecked")
	public GenericHibernateDAO() {
		persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	protected Session getSession() {
		return sessionFactory.getCurrentSession();
	}

	@Override
	public long countAll() {
		return (Integer) createCriteria().setProjection(Projections.rowCount()).uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public T find(Long id) {
		return (T) createCriteria(Restrictions.idEq(id)).uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<T> getAll() {
		return createCriteria().list();
	}

	/**
	 * Use this inside subclasses as a convenience method.
	 */
	protected Criteria createCriteria(Criterion... criterion) {
		Criteria crit = getSession().createCriteria(persistentClass);
		for (Criterion c : criterion)
			crit.add(c);
		return crit;
	}

	@Override
	public void makePersistent(T entity) {
		getSession().saveOrUpdate(entity);
	}

	@Override
	public void makeTransient(T entity) {
		getSession().delete(entity);
	}
}
