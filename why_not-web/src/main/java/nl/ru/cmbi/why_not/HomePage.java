package nl.ru.cmbi.why_not;

import nl.ru.cmbi.why_not.Main.RandomNumberGenerator;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.EntryDAO;

import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Homepage
 */
public class HomePage extends WebPage {

	private static final long		serialVersionUID	= 1L;

	// TODO Add any page properties or variables here

	@SpringBean
	private EntryDAO				entrydao;

	@SpringBean
	private RandomNumberGenerator	rng;

	/**
	 * Constructor that is invoked when page is invoked without a session.
	 * 
	 * @param parameters
	 *            Page parameters
	 */
	public HomePage(final PageParameters parameters) {

		// Add the simplest type of label
		add(new Label("message", "If you see this message wicket is properly configured and running"));
		add(new Label("count", new Model<String>() {
			@Override
			public String getObject() {
				//return "" + entrydao.countAll();
				return "A completely random number: " + rng.getRandomNumber();
			}
		}));

		// TODO Add your page's components here
	}
}
