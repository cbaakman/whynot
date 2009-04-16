package model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToMany;

import org.hibernate.annotations.Cascade;
import org.hibernate.validator.Length;

@Entity
@IdClass(EntryPK.class)
public class Entry {
	@Id
	protected Databank		databank;
	@Id
	@Length(max = 10)
	protected String		pdbid;

	@ManyToMany(mappedBy = "entries", cascade = CascadeType.ALL)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<Annotation>	annotations	= new HashSet<Annotation>();

	protected Entry() {}

	public Entry(Databank databank, String pdbid) {
		this.databank = databank;
		this.pdbid = pdbid.toUpperCase();
	}

	@Override
	public String toString() {
		return databank.name + "," + pdbid;
	}
}
