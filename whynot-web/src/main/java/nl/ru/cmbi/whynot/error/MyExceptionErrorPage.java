package nl.ru.cmbi.whynot.error;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;

import nl.ru.cmbi.whynot.feedback.FeedbackPanelWrapper;

public class MyExceptionErrorPage extends WebPage {
	/**
	 * Print feedback & stacktrace.
	 * 
	 * @param page
	 * @param e
	 */
	public MyExceptionErrorPage(Page page, RuntimeException e) {
		add(new FeedbackPanelWrapper("feedback"));

		add(new Label("page", page == null ? "null" : page.toString()));

		StringWriter s = new StringWriter();
		e.printStackTrace(new PrintWriter(s));
		add(new Label("stacktrace", s.toString()));
	}

	@Override
	public boolean isErrorPage() {
		return true;
	}
}
