package nl.ru.cmbi.whynot.statistics;

import java.text.SimpleDateFormat;
import java.util.Date;

import nl.ru.cmbi.whynot.hibernate.GenericDAO.AnnotationDAO;
import nl.ru.cmbi.whynot.hibernate.GenericDAO.CommentDAO;
import nl.ru.cmbi.whynot.hibernate.GenericDAO.DatabankDAO;
import nl.ru.cmbi.whynot.hibernate.GenericDAO.EntryDAO;
import nl.ru.cmbi.whynot.hibernate.GenericDAO.FileDAO;
import nl.ru.cmbi.whynot.home.HomePage;
import nl.ru.cmbi.whynot.model.Annotation;
import nl.ru.cmbi.whynot.model.File;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

@MountPath(path = "statistics")
public class StatisticsPage extends HomePage {
	@SpringBean
	private AnnotationDAO	anndao;
	@SpringBean
	private CommentDAO		comdao;
	@SpringBean
	private DatabankDAO		dbdao;
	@SpringBean
	private EntryDAO		entdao;
	@SpringBean
	private FileDAO			filedao;

	SimpleDateFormat		sdf	= new SimpleDateFormat("dd/MM/yyyy HH:mm");

	public StatisticsPage() {
		// Counts
		add(new Label("annotations", "" + anndao.countAll()));
		add(new Label("comments", "" + comdao.countAll()));
		add(new Label("databanks", "" + dbdao.countAll()));
		add(new Label("entries", "" + entdao.countAll()));
		add(new Label("files", "" + filedao.countAll()));

		// Most recent annotations
		ListView<Annotation> lva = new ListView<Annotation>("annotation", anndao.getRecent()) {
			@Override
			protected void populateItem(final ListItem<Annotation> item) {
				Annotation ann = item.getModelObject();
				item.add(new Label("comment", ann.getComment().getText()).setEscapeModelStrings(false));
				item.add(new Label("databank", ann.getEntry().getDatabank().getName()));
				item.add(new Label("pdbid", ann.getEntry().getPdbid()));
				item.add(new Label("timestamp", sdf.format(new Date(ann.getTimestamp()))));
			}
		};
		add(lva);

		// Most recent files
		ListView<File> lvf = new ListView<File>("file", filedao.getRecent()) {
			@Override
			protected void populateItem(final ListItem<File> item) {
				File file = item.getModelObject();
				String path = file.getPath();
				item.add(new Label("path", path));
				item.add(new Label("timestamp", sdf.format(new Date(file.getTimestamp()))));
			}
		};
		add(lvf);

	}

}
