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
public class Databank {
	public enum CrawlType {
		FILE, LINE
	};

	@Id
	@NotEmpty
	@Length(max = 50)
	private String		name;

	@NotEmpty
	@Length(max = 200)
	private String		reference;
	@NotEmpty
	@Length(max = 200)
	private String		filelink;

	@OneToOne
	private Databank	parent;

	@NotEmpty
	@Length(max = 50)
	private String		regex;

	@NotNull
	@Enumerated(EnumType.STRING)
	private CrawlType	crawltype;

	@OneToMany(mappedBy = "databank", cascade = CascadeType.ALL)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<Entry>	entries	= new HashSet<Entry>();

	@OneToMany(mappedBy = "databank", cascade = CascadeType.ALL)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<File>	files	= new HashSet<File>();

	protected Databank() {}

	public Databank(String name, String reference, String filelink, Databank parent, String regex, CrawlType crawltype) {
		this.name = name;
		this.reference = reference;
		this.filelink = filelink;
		this.parent = parent;
		this.regex = regex;
		this.crawltype = crawltype;
	}

	public String getName() {
		return name;
	}

	public String getReference() {
		return reference;
	}

	public String getFilelink() {
		return filelink;
	}

	public Databank getParent() {
		return parent;
	}

	public String getRegex() {
		return regex;
	}

	public CrawlType getCrawltype() {
		return crawltype;
	}

	public Set<Entry> getEntries() {
		return entries;
	}

	public Set<File> getFiles() {
		return files;
	}

	@Override
	public String toString() {
		return name + "," + reference + "," + filelink + "," + (parent != null ? parent.getName() : null) + "," + regex + "," + crawltype;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (name == null ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Databank other = (Databank) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		}
		else
			if (!name.equals(other.name))
				return false;
		return true;
	}
}
