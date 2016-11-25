import time
from unittest.mock import mock_open, patch, call, Mock

from nose.tools import eq_

from whynot.default_settings import DATABANKS
from whynot.crawlers import DirCrawler, FileCrawler, crawl


@patch('os.path.exists', return_value=True)
@patch('os.path.isdir', return_value=True)
@patch('os.path.getmtime', return_value=123456)
def test_dir_crawler_hssp(mock_exists, mock_isdir, mock_getmtime):
    hssp_databank = DATABANKS[4]
    assert hssp_databank['name'] == 'hssp'

    with patch('os.walk') as mock_walk:
        mock_walk.return_value = [
            (
                '/srv/data/hssp',
                [],
                ['1crn.hssp.bz2', '1z1l.hssp.bz2', '2h8u.hssp.bz2',
                 '3qxq.hssp.bz2', '4qhh.hssp.bz2', 'ignoreme.txt']
            ),
        ]

        entries = DirCrawler.crawl(hssp_databank['source'],
                                   hssp_databank['regex'])
        mock_walk.assert_called_with(hssp_databank['source'])
        eq_(len(entries), 5)


@patch('os.path.exists', return_value=True)
@patch('os.path.isdir', return_value=True)
@patch('os.path.getmtime', return_value=123456)
def test_dir_crawler_pdb(mock_exists, mock_isdir, mock_getmtime):
    pdb_databank = DATABANKS[1]
    assert pdb_databank['name'] == 'pdb'

    with patch('os.walk') as mock_walk:
        mock_walk.return_value = [
            (
                '/srv/data/pdb/flat',
                ('obsolete',),
                ('pdb100d.ent', 'pdb104d.ent', 'pdb9rat.ent', 'pdb9rnt.ent',
                 'nothis.html')
            ),
        ]

        entries = DirCrawler.crawl(pdb_databank['source'],
                                   pdb_databank['regex'])
        mock_walk.assert_called_with(pdb_databank['source'])
        eq_(len(entries), 4)


@patch('os.path.exists', return_value=True)
@patch('os.path.getmtime', return_value=123456)
def test_file_crawler(mock_exists, mock_getmtime):
    pdbfinder_databank = DATABANKS[5]
    assert pdbfinder_databank['name'] == 'pdbfinder'

    lines = 'ID           : 100D\nID           : 101D'
    with patch('whynot.crawlers.open', mock_open(read_data=lines)):
        entries = FileCrawler.crawl(pdbfinder_databank['source'],
                                    pdbfinder_databank['regex'])
        eq_(len(entries), 2)


@patch('whynot.crawlers.storage')
def test_crawl(mock_storage):
    entries = [
        {
            'pdb_id': '1crn', 'file_path': '/srv/data/pdb/flat/pdb1crn.ent',
            'mtime': time.time(), 'comment': None,
        },
        {
            'pdb_id': '2ltw', 'file_path': '/srv/data/pdb/flat/pdb2ltw.ent',
            'mtime': time.time(), 'comment': None,
        },
    ]
    mock_dir_crawler = Mock()
    mock_dir_crawler.crawl.return_value = entries
    mock_dir_crawler.__name__ = 'MockDirCrawler'
    databank = DATABANKS[1]
    databank['crawler'] = mock_dir_crawler

    crawl(databank)

    # mock_storage.db.entries.bulk_write.assert_called_once()
    mock_storage.db.entries.delete_many.assert_has_calls([
        call({'databank_name': 'pdb', 'pdb_id': {'$nin': ['1crn', '2ltw']}})
    ])
