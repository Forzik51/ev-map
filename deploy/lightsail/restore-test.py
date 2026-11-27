"""Verify a backup using an isolated database; never overwrite the live database or media."""
import hashlib
import json
import subprocess
import tarfile
import tempfile
from pathlib import Path

state = json.loads(Path('/var/lib/evmap/smoke-test.json').read_text())
archives = sorted(Path('/var/backups/evmap').glob('*.tar.gz'))
archive = archives[-1]
database = 'evmap_restore_check'
created = False


def postgres(*arguments):
    return subprocess.check_output(['runuser', '-u', 'postgres', '--', *arguments], text=True).strip()


with tempfile.TemporaryDirectory(prefix='evmap-restore-') as directory:
    work = Path(directory)
    try:
        # PostgreSQL must be able to traverse the temporary directory and read the dump.
        work.chmod(0o755)
        bucket = Path('/etc/evmap/backup.env').read_text().strip().split('=', 1)[1]
        downloaded = work / archive.name
        subprocess.run(['aws', 's3', 'cp', f's3://{bucket}/daily/{archive.name}', str(downloaded),
                        '--region', 'eu-north-1', '--only-show-errors'], check=True)
        with archive.open('rb') as source, downloaded.open('rb') as target:
            assert hashlib.file_digest(source, 'sha256').digest() == hashlib.file_digest(target, 'sha256').digest()
        with tarfile.open(downloaded) as backup:
            members = backup.getmembers()
            assert all(item.name == 'database.dump' or item.name.startswith('var/lib/evmap/media') for item in members)
            assert not any(item.name.endswith('app.env') for item in members)
            backup.extractall(work, filter='data')
        (work / 'database.dump').chmod(0o644)
        postgres('createdb', database)
        created = True
        postgres('pg_restore', '--exit-on-error', '--no-owner', '--no-privileges', '-d', database, str(work / 'database.dump'))
        result = postgres('psql', '-d', database, '-At', '-v', 'ON_ERROR_STOP=1', '-c',
            f"SELECT (SELECT count(*) FROM evmap.app_user WHERE id={int(state['userId'])}) || ',' || "
            f"(SELECT count(*) FROM evmap.event WHERE id={int(state['eventId'])}) || ',' || "
            f"(SELECT count(*) FROM evmap.message WHERE chat_id={int(state['chatId'])})")
        assert result.startswith('1,1,') and int(result.split(',')[2]) > 0, result
        media = state['mediaPath'].removeprefix('/media/')
        restored = work / 'var/lib/evmap/media' / media
        live = Path('/var/lib/evmap/media') / media
        assert hashlib.sha256(restored.read_bytes()).digest() == hashlib.sha256(live.read_bytes()).digest()
        print('PASS: downloaded backup restored into an isolated PostgreSQL database; user, event, chat and identical photo bytes verified; application secrets excluded.')
    finally:
        if created:
            postgres('dropdb', database)
