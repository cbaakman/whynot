java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler PDB /data/raw/pdb/;

java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler PDBFINDER /data/raw/pdbfinder/PDBFIND.TXT;
java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler PDBREPORT /data/raw/pdbreport;

java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler DSSP /data/uncompressed/dssp/;
java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler HSSP /data/uncompressed/hssp/;

java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler RECOORD /data/raw/recoord/;

java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler NMR http://nmr.cmbi.ru.nl/NRG-CING/entry_list_nmr.csv;
java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler NRG http://nmr.cmbi.ru.nl/NRG-CING/entry_list_nrg.csv;
java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler NRG-DOCR http://nmr.cmbi.ru.nl/NRG-CING/entry_list_nrg_docr.csv;
java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler NRG-CING http://nmr.cmbi.ru.nl/NRG-CING/entry_list_done.csv;

java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler STRUCTUREFACTORS /data/uncompressed/structure_factors/;
java -cp dependency/*:whynot-apps-2.0-090612.jar nl.ru.cmbi.whynot.crawl.Crawler PDB_REDO /data/raw/pdb_redo/;
