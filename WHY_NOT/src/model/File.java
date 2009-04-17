package model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.hibernate.validator.Length;
import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.NotNull;

@Entity
@IdClass(EntryPK.class)
public class File {
	@Id
	private Databank	databank;
	@Id
	@Length(max = 10)
	private String		pdbid;

	@NotEmpty
	@Length(max = 200)
	private String		path;
	@NotNull
	private Long		time;

	protected File() {}

	public File(Databank db, String id) {
		databank = db;
		pdbid = id;
	}

	public File(Databank db, String id, String path, Long time) {
		databank = db;
		pdbid = id;
		this.path = path;
		this.time = time;
		databank.getFiles().add(this);//TODO: remove?
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return (databank != null ? databank.getName() : null) + "," + pdbid + "," + path + "," + time;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (databank == null ? 0 : databank.hashCode());
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
		File other = (File) obj;
		if (databank == null) {
			if (other.databank != null)
				return false;
		}
		else
			if (!databank.equals(other.databank))
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
}
