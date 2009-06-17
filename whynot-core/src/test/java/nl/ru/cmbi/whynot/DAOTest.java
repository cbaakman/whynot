package nl.ru.cmbi.whynot;

import java.util.List;

import nl.ru.cmbi.whynot.hibernate.GenericDAO.AnnotationDAO;
import nl.ru.cmbi.whynot.hibernate.GenericDAO.CommentDAO;
import nl.ru.cmbi.whynot.hibernate.GenericDAO.DatabankDAO;
import nl.ru.cmbi.whynot.hibernate.GenericDAO.EntryDAO;
import nl.ru.cmbi.whynot.hibernate.GenericDAO.FileDAO;
import nl.ru.cmbi.whynot.model.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/spring.xml" })
@Transactional
@SuppressWarnings("unused")
public class DAOTest {
	@Autowired
	private AnnotationDAO	anndao;
	@Autowired
	private CommentDAO		comdao;
	@Autowired
	private DatabankDAO		dbdao;
	@Autowired
	private EntryDAO		entdao;
	@Autowired
	private FileDAO			filedao;

	@Test
	//@Ignore
	public void doSomethingToDAO() {
		List<Entry> entries = entdao.getPresent(dbdao.findByName("DSSP"));
		System.out.println(entries.size());
	}
}
