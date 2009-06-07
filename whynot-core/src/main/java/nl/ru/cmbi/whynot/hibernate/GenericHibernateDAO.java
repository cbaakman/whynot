package nl.ru.cmbi.whynot.hibernate;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Example;
import org.springframework.beans.factory.annotation.Autowired;

public class GenericHibernateDAO<T, ID extends Serializable> implements GenericDAO<T, ID> {
	private Class<T>		persistentClass;

	@Autowired
	private SessionFactory	sessionFactory;

	@SuppressWarnings("unchecked")
	public GenericHibernateDAO() {
		persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	protected Class<T> getPersistentClass() {
		return persistentClass;
	}

	protected Session getSession() {
		return sessionFactory.getCurrentSession();
	}

	public Long countAll() {
		return (Long) getSession().createQuery("select count(*) from " + persistentClass.getName()).uniqueResult();
	}

	//Finders
	@SuppressWarnings("unchecked")
	public T findById(ID id, boolean lock) {
		T entity;
		if (lock)
			entity = (T) getSession().get(getPersistentClass(), id, LockMode.UPGRADE);
		else
			entity = (T) getSession().get(getPersistentClass(), id);

		return entity;
	}

	public List<T> findAll() {
		return findByCriteria();
	}

	@SuppressWarnings("unchecked")
	public T findByExample(T exampleInstance, String... excludeProperty) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		Example example = Example.create(exampleInstance);
		for (String exclude : excludeProperty)
			example.excludeProperty(exclude);
		crit.add(example);
		return (T) crit.uniqueResult();
	}

	public T findOrCreateByExample(T exampleInstance, String... excludeProperty) {
		T entity = findByExample(exampleInstance, excludeProperty);
		if (entity == null)
			return makePersistent(exampleInstance);
		return entity;
	}

	/**
	* Use this inside subclasses as a convenience method.
	*/
	@SuppressWarnings("unchecked")
	protected List<T> findByCriteria(Criterion... criterion) {
		Criteria crit = getSession().createCriteria(getPersistentClass());
		for (Criterion c : criterion)
			crit.add(c);
		return crit.list();
	}

	//Save / Delete
	public T makePersistent(T entity) {
		getSession().saveOrUpdate(entity);
		return entity;
	}

	public void makeTransient(T entity) {
		getSession().delete(entity);
	}

	//Filters
	public void enableFilter(String filterName, String... params) {
		Filter filter = getSession().enableFilter(filterName);
		String key = null;
		for (String par : params)
			if (key == null)
				key = par;
			else {
				filter.setParameter(key, par);
				key = null;
			}
	}

	public void disableFilter(String filterName) {
		getSession().disableFilter(filterName);
	}
}
