package model;

import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.hibernate.validator.Length;

@Entity
@IdClass(EntryPK.class)
public class Entry implements Comparable<Entry> {
	@Id
	private Databank				databank;
	@Id
	@Length(max = 10)
	private String					pdbid;

	@OneToMany(mappedBy = "entry", cascade = CascadeType.ALL)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	@Sort(type = SortType.NATURAL)
	private SortedSet<Annotation>	annotations	= new TreeSet<Annotation>();

	protected Entry() {}

	public Entry(Databank db, String id) {
		databank = db;
		pdbid = id.toUpperCase();
		databank.getEntries().add(this);
	}

	public Databank getDatabank() {
		return databank;
	}

	public String getPdbid() {
		return pdbid;
	}

	public SortedSet<Annotation> getAnnotations() {
		return annotations;
	}

	@Override
	public String toString() {
		return databank.getName() + "," + pdbid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (databank == null ? 0 : databank.getName().hashCode());
		result = prime * result + (pdbid == null ? 0 : pdbid.hashCode());
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
		Entry other = (Entry) obj;
		if (databank == null) {
			if (other.databank != null)
				return false;
		}
		else
			if (!databank.getName().equals(other.databank.getName()))
				return false;
		if (pdbid == null) {
			if (other.pdbid != null)
				return false;
		}
		else
			if (!pdbid.equals(other.pdbid))
				return false;
		return true;
	}

	public int compareTo(Entry o) {
		int db = getDatabank().getName().compareTo(o.getDatabank().getName());
		if (db != 0)
			return db;
		return getPdbid().compareTo(o.getPdbid());
	}
}
