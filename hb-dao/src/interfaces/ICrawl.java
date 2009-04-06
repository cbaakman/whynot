package interfaces;

import java.util.Set;

import model.Database;
import model.EntryFile;

public interface ICrawl {
	public Database retrieveDatabase(String name);

	public void storeAll(Set<EntryFile> entries);

	public void removeAll(Set<EntryFile> invalids);
}
