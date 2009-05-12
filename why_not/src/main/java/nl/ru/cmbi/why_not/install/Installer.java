package nl.ru.cmbi.why_not.install;

import nl.ru.cmbi.why_not.hibernate.DAOFactory;
import nl.ru.cmbi.why_not.hibernate.HibernateDAOFactory;
import nl.ru.cmbi.why_not.hibernate.GenericDAO.DatabankDAO;
import nl.ru.cmbi.why_not.model.Databank;
import nl.ru.cmbi.why_not.model.Databank.CrawlType;

import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

public class Installer {

	public static void main(String[] args) throws Exception {
		new Installer().drop().create().fill();
	}

	private Configuration	cfg;
	private DAOFactory		factory;

	public Installer() {
		cfg = new AnnotationConfiguration().configure();
		factory = new HibernateDAOFactory();
	}

	public Installer drop() {
		new SchemaExport(cfg).drop(true, true);
		return this;
	}

	public Installer create() {
		new SchemaExport(cfg).create(true, true);
		return this;
	}

	public Installer fill() throws Exception {
		Transaction transact = null;
		try {
			transact = factory.getSession().beginTransaction();
			DatabankDAO dbdao = factory.getDatabankDAO();

			Databank pdb, dssp;
			dbdao.makePersistent(pdb = new Databank("PDB", "http://www.pdb.org/", "http://www.pdb.org/pdb/download/downloadFile.do?fileFormat=pdb&compression=NO&structureId=", ".*/pdb([\\d\\w]{4})\\.ent(\\.gz)?", CrawlType.FILE));
			dbdao.makePersistent(dssp = new Databank("DSSP", "http://swift.cmbi.ru.nl/gv/dssp/", "http://mrs.cmbi.ru.nl/mrs-3/entry.do?db=dssp&id=", pdb, ".*/([\\d\\w]{4})\\.dssp", CrawlType.FILE));
			dbdao.makePersistent(new Databank("HSSP", "http://swift.cmbi.ru.nl/gv/hssp/", "http://mrs.cmbi.ru.nl/mrs-3/entry.do?db=hssp&id=", dssp, ".*/([\\d\\w]{4})\\.hssp", CrawlType.FILE));
			dbdao.makePersistent(new Databank("PDBFINDER", "http://swift.cmbi.ru.nl/gv/pdbfinder/", "http://mrs.cmbi.ru.nl/mrs-3/entry.do?db=pdbfinder2&id=", pdb, "ID           : ([\\d\\w]{4})", CrawlType.LINE));
			dbdao.makePersistent(new Databank("PDBREPORT", "http://swift.cmbi.ru.nl/gv/pdbreport/", "http://www.cmbi.ru.nl/pdbreport/cgi-bin/nonotes?PDBID=", pdb, ".*pdbreport.*/([\\d\\w]{4})", CrawlType.FILE));
			dbdao.makePersistent(new Databank("PDB_REDO", "http://www.cmbi.ru.nl/pdb_redo/", "http://www.cmbi.ru.nl/pdb_redo/cgi-bin/redir2.pl?pdbCode=", pdb, ".*pdb_redo.*/([\\d][\\d\\w]{3})", CrawlType.FILE));
			dbdao.makePersistent(new Databank("NRG-CING", "http://nmr.cmbi.ru.nl/cing/", "http://www.cmbi.ru.nl/cgi-bin/cing.pl?id=", pdb, ".*/([\\d\\w]{4})\\.exist", CrawlType.FILE));
			dbdao.makePersistent(new Databank("RECOORD", "http://www.ebi.ac.uk/msd-srv/docs/NMR/recoord/main.html", "http://www.cmbi.ru.nl/whynot/recoord_trampoline/", pdb, ".*/([\\d\\w]{4})_cns_w\\.pdb", CrawlType.FILE));
			dbdao.makePersistent(new Databank("STRUCTUREFACTORS", "http://www.pdb.org/", "http://www.pdb.org/pdb/download/downloadFile.do?fileFormat=STRUCTFACT&compression=NO&structureId=", pdb, ".*/r([\\d\\w]{4})sf\\.ent", CrawlType.FILE));

			transact.commit();
			return this;
		}
		catch (Exception e) {
			if (transact != null)
				transact.rollback();
			throw e;
		}
	}
}
