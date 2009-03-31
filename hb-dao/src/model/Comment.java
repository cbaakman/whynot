package model;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

@Entity
public class Comment {
	@Id
	private String		text;

	@ManyToOne
	private Author		author;

	@ManyToMany(mappedBy = "comments")
	private Set<Entry>	entries;

	protected Comment() {}

	public Comment(String comment, Author auth) {
		text = comment;
		author = auth;
	}
}
