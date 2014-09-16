package nl.ru.cmbi.whynot.model;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.NaturalId;

@Data
@Entity
@EqualsAndHashCode(callSuper = false, of = { "comment", "entry" })
@Table(indexes= {
		@Index(name = "annotation_comment_index", columnList = "comment_id"),
		@Index(name = "annotation_entry_index", columnList = "entry_id")
})
public class Annotation extends DomainObject implements Comparable<Annotation> {
	@NaturalId
	@ManyToOne
	@Cascade(value = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.SAVE_UPDATE })
	@NotNull
	@Setter(AccessLevel.NONE)
	private Comment	comment;

	@NaturalId
	@ManyToOne
	@Cascade(value = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.SAVE_UPDATE })
	@NotNull
	@Setter(AccessLevel.NONE)
	private Entry	entry;

	@Setter(AccessLevel.NONE)
	@SuppressWarnings("unused")
	private Long	timestamp;

	protected Annotation() {/*Hibernate requirement*/}

	public Annotation(final Comment comment, final Entry entry, final Long timestamp) {
		this.comment = comment;
		this.entry = entry;
		this.timestamp = timestamp;
	}

	@Override
	public int compareTo(final Annotation o) {
		int value = comment.compareTo(o.comment);
		if (value != 0)
			return value;
		return entry.compareTo(o.entry);
	}
}
