"""Lightsail buckets use object deletion for retention; no S3 lifecycle API is needed."""
import datetime as dt
import json
import re
import subprocess
import sys

bucket = sys.argv[1]
cutoff = dt.datetime.now(dt.timezone.utc) - dt.timedelta(days=14)
base = ["aws", "s3api"]
options = ["--bucket", bucket, "--region", "eu-north-1"]
result = subprocess.check_output(base + ["list-objects-v2"] + options + ["--prefix", "daily/", "--output", "json"])
objects = json.loads(result).get("Contents", [])
for item in objects:
    if not re.fullmatch(r"daily/[0-9]{8}T[0-9]{6}Z\.tar\.gz", item["Key"]):
        continue
    modified = dt.datetime.fromisoformat(item["LastModified"].replace("Z", "+00:00"))
    if modified < cutoff:
        subprocess.run(base + ["delete-object"] + options + ["--key", item["Key"]], check=True, stdout=subprocess.DEVNULL)
        print(f"Expired backup removed: {item['Key']}")
