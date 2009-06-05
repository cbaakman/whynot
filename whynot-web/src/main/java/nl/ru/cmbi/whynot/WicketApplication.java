package nl.ru.cmbi.whynot;

import nl.ru.cmbi.whynot.about.AboutPage;
import nl.ru.cmbi.whynot.comment.CommentPage;
import nl.ru.cmbi.whynot.databank.DatabankPage;
import nl.ru.cmbi.whynot.home.HomePage;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;

public class WicketApplication extends WebApplication {
	@Override
	protected void init() {
		addComponentInstantiationListener(new SpringComponentInjector(this));

		mountBookmarkablePage("about", AboutPage.class);
		mountBookmarkablePage("databanks", DatabankPage.class);
		mountBookmarkablePage("comments", CommentPage.class);
	}

	@Override
	public Class<? extends WebPage> getHomePage() {
		return HomePage.class;
	}
}
