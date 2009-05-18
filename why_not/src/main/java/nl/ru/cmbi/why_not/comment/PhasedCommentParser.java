package nl.ru.cmbi.why_not.comment;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nl.ru.cmbi.why_not.hibernate.GenericDAO.AnnotationDAO;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.CommentDAO;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.DatabankDAO;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.EntryDAO;
import nl.ru.cmbi.why_not.model.Annotation;
import nl.ru.cmbi.why_not.model.Comment;
import nl.ru.cmbi.why_not.model.Databank;
import nl.ru.cmbi.why_not.model.Entry;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhasedCommentParser {
	public static final String	append			= ".done";
	public static FileFilter	commentFilter	= new FileFilter() {
													@Override
													public boolean accept(File pathname) {
														return pathname.isFile() && !pathname.getName().contains(PhasedCommentParser.append);
													}
												};

	private Pattern				patternComment	= Pattern.compile("COMMENT: (.+)");
	private Pattern				patternEntry	= Pattern.compile("(.+),(.+)");

	@Autowired
	private AnnotationDAO		anndao;
	@Autowired
	private CommentDAO			comdao;
	@Autowired
	private DatabankDAO			dbdao;
	@Autowired
	private EntryDAO			entdao;

	@Transactional
	public void storeComments(File file) throws IOException, ParseException {
		LineNumberReader lnr = new LineNumberReader(new FileReader(file));
		String line;
		Matcher matcher;
		Comment comment = new Comment("Unspecified comment");
		while ((line = lnr.readLine()) != null)
			if ((matcher = patternComment.matcher(line)).matches())
				if (!comment.getText().equals(matcher.group(1)))
					comment = comdao.findOrCreateByExample(new Comment(matcher.group(1)));
	}

	@Transactional
	public void storeEntries(File file) throws IOException, ParseException {
		for (Databank db : dbdao.findAll()) {
			int skipped = 0, added = 0;
			LineNumberReader lnr = new LineNumberReader(new FileReader(file));
			String line;
			Matcher matcher;
			Pattern pattern = Pattern.compile(db.getName() + ",(.+)");
			while ((line = lnr.readLine()) != null)
				if ((matcher = pattern.matcher(line)).matches())
					if (db.getEntries().add(new Entry(db, matcher.group(1).toLowerCase())))
						added++;
					else
						skipped++;
			System.err.println(added + " / " + skipped);
		}
	}

	@Transactional
	public void storeAnnotations(File file) throws IOException, ParseException {
		LineNumberReader lnr = new LineNumberReader(new FileReader(file));
		long time = System.currentTimeMillis();
		int added = 0, skipped = 0;
		String line;
		Matcher matcher;
		Comment comment = new Comment("Unspecified comment");
		Databank databank = new Databank("Unspecified databank");
		Entry entry = new Entry(databank, "Unspecified");
		List<Annotation> annotations = new ArrayList<Annotation>();
		while ((line = lnr.readLine()) != null)
			if ((matcher = patternComment.matcher(line)).matches()) {
				System.err.println(lnr.getLineNumber());
				if (!comment.getText().equals(matcher.group(1))) {
					comment.getAnnotations().addAll(annotations);
					comment = comdao.findByText(matcher.group(1));
				}
			}
			else
				if ((matcher = patternEntry.matcher(line)).matches()) {
					if (!databank.getName().equals(matcher.group(1)))
						databank = dbdao.findByName(matcher.group(1));

					entry = entdao.findByDatabankAndPdbid(databank, matcher.group(2));
					annotations.add(new Annotation(comment, entry, time));
				}
				else
					throw new ParseException("Expected: " + patternComment.pattern() + " OR " + patternEntry.pattern() + " at line " + lnr.getLineNumber(), lnr.getLineNumber());
		comment.getAnnotations().addAll(annotations);

		lnr.close();

		file.renameTo(new File(file.getAbsolutePath() + PhasedCommentParser.append));
		Logger.getLogger(PhasedCommentParser.class).info("Added " + added + ", skipped " + skipped + " annotations from file: " + file.getAbsolutePath());
	}

	@Transactional
	public void old_comment(File file) throws IOException, ParseException {
		//Current comment found or created
		Comment comment = new Comment("No comment specified");

		//Databank available as simple caching mechanism
		Databank databank = new Databank("Unknown databank");

		Entry entry;

		//Assign this time to all annotations
		long time = System.currentTimeMillis();

		int added = 0, skipped = 0;

		//Read file
		LineNumberReader lnr = new LineNumberReader(new FileReader(file));
		String line;
		Matcher matcher;
		while ((line = lnr.readLine()) != null)
			//Try reading comment line
			if ((matcher = patternComment.matcher(line)).matches()) {
				if (!comment.getText().equals(matcher.group(1)))
					comment = comdao.findOrCreateByExample(new Comment(matcher.group(1)));
			}
			else
				//Try reading entry line
				if ((matcher = patternEntry.matcher(line)).matches()) {
					if (!databank.getName().equals(matcher.group(1)))
						if (null == (databank = dbdao.findByName(matcher.group(1))))
							throw new ParseException("No databank found for name " + matcher.group(1) + " at line " + lnr.getLineNumber(), lnr.getLineNumber());

					//Find or create entry
					String pdbid = matcher.group(2).toLowerCase();
					if (null == (entry = entdao.findByDatabankAndPdbid(databank, pdbid))) {
						entry = new Entry(databank, pdbid);
						databank.getEntries().add(entry);
					}

					//Create & store annotation
					Annotation ann = new Annotation(comment, entry, time);
					if (entry.getAnnotations().add(ann))
						added++;
					else {
						Logger.getLogger(PhasedCommentParser.class).warn("Annotation found, skipping: " + ann);
						skipped++;
					}
				}
				else
					throw new ParseException("Expected: " + patternComment.pattern() + " OR " + patternEntry.pattern() + " at line " + lnr.getLineNumber(), lnr.getLineNumber());
		lnr.close();

		file.renameTo(new File(file.getAbsolutePath() + PhasedCommentParser.append));
		Logger.getLogger(PhasedCommentParser.class).info("Added " + added + ", skipped " + skipped + " annotations from file: " + file.getAbsolutePath());
	}

	@Transactional
	public void old_uncomment(File file) throws IOException, ParseException {
		//Current comment found or created
		Comment comment = new Comment("No comment specified");

		//Databank available as simple caching mechanism
		Databank databank = new Databank("Unknown databank");

		Entry entry;

		int removed = 0, skipped = 0;

		//Read file
		LineNumberReader lnr = new LineNumberReader(new FileReader(file));
		String line;
		Matcher matcher;
		while ((line = lnr.readLine()) != null)
			//Try reading comment line
			if ((matcher = patternComment.matcher(line)).matches()) {
				if (!comment.getText().equals(matcher.group(1)))
					comment = comdao.findOrCreateByExample(new Comment(matcher.group(1)));
			}
			else
				//Try reading entry line
				if ((matcher = patternEntry.matcher(line)).matches()) {
					if (!databank.getName().equals(matcher.group(1)))
						if (null == (databank = dbdao.findByName(matcher.group(1))))
							throw new ParseException("No databank found for name " + matcher.group(1) + " at line " + lnr.getLineNumber(), lnr.getLineNumber());

					//Find entry
					String pdbid = matcher.group(2).toLowerCase();
					if (null == (entry = entdao.findByDatabankAndPdbid(databank, pdbid))) {
						Logger.getLogger(PhasedCommentParser.class).warn("Entry not found, skipping: " + comment + "," + line);
						skipped++;
						continue;
					}

					//Find annotation
					Annotation ann = new Annotation(comment, entry, 1L);
					if (!entry.getAnnotations().contains(ann)) {
						Logger.getLogger(PhasedCommentParser.class).warn("Annotation not found, skipping: " + ann);
						skipped++;
						continue;
					}

					//Remove annotation
					comment.getAnnotations().remove(ann);
					entry.getAnnotations().remove(ann);
					anndao.makeTransient(ann);
					removed++;
				}
				else
					throw new ParseException("Expected: " + patternComment.pattern() + " OR " + patternEntry.pattern() + " at line " + lnr.getLineNumber(), lnr.getLineNumber());
		lnr.close();

		file.renameTo(new File(file.getAbsolutePath() + PhasedCommentParser.append));
		Logger.getLogger(PhasedCommentParser.class).info("Removed " + removed + ", skipped " + skipped + " annotations from file: " + file.getAbsolutePath());
	}

	/**
	 * Cleanup unused comments
	 */
	@Transactional
	public void cleanUpComments() {
		int count = 0;
		for (Comment comment : comdao.findAll()) {
			entdao.enableFilter("withComment", "comment", comment.getText());
			if (entdao.countAll() == 0) {
				comdao.makeTransient(comment);
				count++;
			}
			entdao.disableFilter("withComment");
		}
		Logger.getLogger(PhasedCommentParser.class).info("Cleaned up " + count + " unused comments");
	}

	/**
	 * Cleanup unused entries
	 */
	@Transactional
	public void cleanUpEntries() {
		int count = 0;
		entdao.enableFilter("withoutFile");
		entdao.enableFilter("withoutComment", "comment", "%");
		for (Entry entry : entdao.findAll()) {
			entdao.makeTransient(entry);
			count++;
		}
		entdao.disableFilter("withoutFile");
		entdao.disableFilter("withoutComment");
		Logger.getLogger(PhasedCommentParser.class).info("Cleaned up " + count + " unused entries");
	}
}
