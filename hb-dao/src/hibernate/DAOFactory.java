package hibernate;

import interfaces.DatabaseDAO;

import org.hibernate.Session;

public abstract class DAOFactory {

	/**
	 * Creates a standalone DAOFactory that returns unmanaged DAO
	 * beans for use in any environment Hibernate has been configured
	 * for. Uses HibernateUtil/SessionFactory and Hibernate context
	 * propagation (CurrentSessionContext), thread-bound or transaction-bound,
	 * and transaction scoped.
	 */
	@SuppressWarnings("unchecked")
	public static final Class	HIBERNATE	= HibernateDAOFactory.class;

	/**
	 * Factory method for instantiation of concrete factories.
	 */
	@SuppressWarnings("unchecked")
	public static DAOFactory instance(Class factory) {
		try {
			return (DAOFactory) factory.newInstance();
		}
		catch (Exception ex) {
			throw new RuntimeException("Couldn't create DAOFactory: " + factory);
		}
	}

	@Deprecated
	//TODO: Remove deprecated: Added before switching around session handling
	public abstract Session getCurrentSession();

	// Add your DAO interfaces here
	public abstract DatabaseDAO getDatabaseDAO();
}
