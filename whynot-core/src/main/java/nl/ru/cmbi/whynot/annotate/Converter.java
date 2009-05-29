package nl.ru.cmbi.whynot.annotate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class Converter {
	public static void main(String[] args) throws IOException, ParseException {
		if (args.length != 1)
			throw new IllegalArgumentException("Usage: converter FILENAME");
		convert(new File(args[0]));
	}

	//Old
	private static Pattern	patternPDBID	= Pattern.compile("PDBID        : (.+)");
	private static Pattern	patternDatabase	= Pattern.compile("Database     : (.+)");
	private static Pattern	patternProperty	= Pattern.compile("Property     : (.+)");
	private static Pattern	patternComment	= Pattern.compile("Comment      : (.+)");

	//New
	public static Pattern	patternCOMMENT	= Pattern.compile("COMMENT: (.+)");
	public static Pattern	patternEntry	= Pattern.compile("(.+),(.+)");

	/**
	 * Try to read the file, and if necessary convert & optimize it.
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws ParseException
	 */
	public static File getFile(File file) throws FileNotFoundException, IOException, ParseException {
		LineNumberReader lnr = new LineNumberReader(new FileReader(file));
		String line = lnr.readLine();
		lnr.close();

		if (line.startsWith("PDBID"))
			return convert(file);
		if (line.startsWith("COMMENT"))
			return optimize(file);
		throw new ParseException("Could not determine Comment file type: Expected PDBID or COMMENT on line 1", 1);
	}

	/**
	 * Converts an old style comment file into new style comment file,
	 * only writing a new comment line if different from previous line.
	 * Old file is replaced with new file named OldFileName.converted.
	 * @param original
	 * @return converted file
	 * @throws IOException
	 * @throws ParseException
	 */
	public static File convert(File original) throws IOException, ParseException {
		Logger.getLogger(Converter.class).info("Converting file " + original.getAbsolutePath());

		LineNumberReader lnr = new LineNumberReader(new FileReader(original));
		File converted = new File(original.getAbsolutePath() + ".converted");
		PrintWriter fw = new PrintWriter(new FileWriter(converted));

		String line, prev_com = null, com, db, id;
		Matcher m;

		while ((line = lnr.readLine()) != null) {
			//PDBID
			if (!(m = patternPDBID.matcher(line)).matches())
				throw new ParseException("Expected " + patternPDBID.pattern() + " on line " + lnr.getLineNumber(), lnr.getLineNumber());
			id = m.group(1);

			//Database
			if (!(m = patternDatabase.matcher(lnr.readLine())).matches())
				throw new ParseException("Expected " + patternDatabase.pattern() + " on line " + lnr.getLineNumber(), lnr.getLineNumber());
			db = m.group(1);

			//Property
			if (!(m = patternProperty.matcher(lnr.readLine())).matches())
				throw new ParseException("Expected " + patternProperty.pattern() + " on line " + lnr.getLineNumber(), lnr.getLineNumber());
			;//Ignore Property

			//Comment
			if (!(m = patternComment.matcher(lnr.readLine())).matches())
				throw new ParseException("Expected " + patternComment.pattern() + " on line " + lnr.getLineNumber(), lnr.getLineNumber());
			com = m.group(1);
			if (!com.equals(prev_com))
				fw.println("COMMENT: " + (prev_com = com));

			//
			if (!lnr.readLine().matches("//"))
				throw new ParseException("Expected \"//\" on line " + lnr.getLineNumber(), lnr.getLineNumber());

			//Add new entry line
			fw.println(db + "," + id);
		}
		lnr.close();
		fw.close();

		original.delete();
		return optimize(converted);
	}

	/**
	 * Optimizes new style comment file by removing duplicate comment
	 * lines, sorting entry lines beneath each comment, and ordering
	 * the comments from small to large.
	 * @param original
	 * @return optimized file
	 * @throws IOException
	 * @throws ParseException
	 */
	public static File optimize(File original) throws IOException, ParseException {
		Logger.getLogger(Converter.class).info("Optimizing file " + original.getAbsolutePath());

		LineNumberReader lnr = new LineNumberReader(new FileReader(original));
		File optimized = new File(original.getAbsolutePath() + ".optimized");

		SortedMap<String, SortedSet<String>> mapje = new TreeMap<String, SortedSet<String>>();

		String line, com = "COMMENT: Empty comment";
		while ((line = lnr.readLine()) != null)
			//Entry 
			if (patternEntry.matcher(line).matches())
				//Add new entry line
				mapje.get(com).add(line);
			else
				//Comment
				if (patternCOMMENT.matcher(line).matches()) {
					if (!mapje.containsKey(com = line))
						mapje.put(line, new TreeSet<String>());
				}
				else
					throw new ParseException("Expected " + patternCOMMENT + " or " + patternEntry + " on line " + lnr.getLineNumber(), lnr.getLineNumber());
		lnr.close();

		PrintWriter fw = new PrintWriter(new FileWriter(optimized));
		while (!mapje.isEmpty()) {
			String smallest = mapje.firstKey();
			for (String comment : mapje.keySet())
				if (mapje.get(comment).size() < mapje.get(smallest).size())
					smallest = comment;

			//Print comment
			fw.println(smallest);
			for (String entryline : mapje.remove(smallest))
				fw.println(entryline);
		}
		fw.close();

		original.delete();
		return optimized;
	}
}
