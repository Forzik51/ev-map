#!/usr/bin/env bash
# Validate V14-V19 against a temporary copy; never migrate the working database.
set -euo pipefail
umask 077
sql_dir=$(realpath "${1:?Usage: verify-event-form-migrations.sh /directory/with/V14-V19}")
work=$(mktemp -d /tmp/evmap-migration-check-XXXXXXXX)
database=evmap_verify_events_$(date -u +%Y%m%d%H%M%S)_$$
created=0
cleanup() {
    if (( created )); then sudo -u postgres dropdb --if-exists "$database"; fi
    if [[ $work == /tmp/evmap-migration-check-* && -d $work ]]; then rm -rf -- "$work"; fi
}
trap cleanup EXIT
files=(V14__event_schedule_and_limits.sql V15__location_geometry_and_geocoding.sql V16__event_tags_and_category_requests.sql V17__event_photo_order.sql V18__event_creation_requests.sql V19__optional_profile_fields.sql)
for file in "${files[@]}"; do [[ -f $sql_dir/$file ]] || { echo "Missing migration: $file" >&2; exit 1; }; done
sudo -u postgres pg_dump -Fc --no-owner --no-acl evmap > "$work/database.dump"
sudo -u postgres createdb "$database"
created=1
sudo -u postgres pg_restore --no-owner --no-acl -d "$database" < "$work/database.dump"
counts="SELECT (SELECT count(*) FROM evmap.app_user),(SELECT count(*) FROM evmap.event),(SELECT count(*) FROM evmap.photo),(SELECT count(*) FROM evmap.location)"
before=$(sudo -u postgres psql -d "$database" -Atc "$counts")
{
    printf 'BEGIN;\nSET search_path=evmap,public;\n'
    for file in "${files[@]}"; do cat "$sql_dir/$file"; printf '\n'; done
    printf 'COMMIT;\n'
} > "$work/migrations.sql"
sudo -u postgres psql -v ON_ERROR_STOP=1 -d "$database" < "$work/migrations.sql" > "$work/migrations.log"
after=$(sudo -u postgres psql -d "$database" -Atc "$counts")
[[ $before == "$after" ]] || { echo 'Record counts changed unexpectedly.' >&2; exit 1; }
echo "V14-V19 applied successfully to a database copy. Preserved counts (users|events|photos|locations): $after"
