package model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Cascade;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.NotNull;

@Entity
public class Database {
	public enum CrawlType {
		FILE, LINE
	};

	@Id
	@NotEmpty
	@Length(max = 50)
	private String			name;

	@NotEmpty
	@Length(max = 200)
	private String			reference;
	@NotEmpty
	@Length(max = 200)
	private String			filelink;

	@OneToOne
	private Database		parent;

	@NotEmpty
	@Length(max = 50)
	private String			regex;

	@NotNull
	@Enumerated(EnumType.STRING)
	private CrawlType		crawltype;

	@OneToMany(mappedBy = "entry.database", cascade = CascadeType.ALL)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<EntryFile>	entries	= new HashSet<EntryFile>();

	protected Database() {}

	public Database(String nm, String ref, String link, Database par, String pat, CrawlType type) {
		name = nm;
		reference = ref;
		filelink = link;
		parent = par;
		regex = pat;
		crawltype = type;
	}

	@Override
	public String toString() {
		return name;
	}

	public String getRegex() {
		return regex;
	}

	public CrawlType getCrawltype() {
		return crawltype;
	}

	public Set<EntryFile> getEntries() {
		return entries;
	}
}
