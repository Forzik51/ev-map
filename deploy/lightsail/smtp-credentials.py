"""Read create-access-key JSON from stdin; save SES credentials without printing secrets."""
import base64
import hashlib
import hmac
import json
import os
import sys

credentials = json.load(sys.stdin)["AccessKey"]
signature = ("AWS4" + credentials["SecretAccessKey"]).encode()
for value in ("11111111", "eu-north-1", "ses", "aws4_request", "SendRawEmail"):
    signature = hmac.new(signature, value.encode(), hashlib.sha256).digest()
password = base64.b64encode(b"\x04" + signature).decode()
fd = os.open(sys.argv[1], os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
with os.fdopen(fd, "w") as output:
    output.write(f"MAIL_USERNAME={credentials['AccessKeyId']}\nMAIL_PASSWORD={password}\n")
