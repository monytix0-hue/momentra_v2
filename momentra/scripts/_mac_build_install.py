import os
import sys

import paramiko

HOST = "santosh-mac.local"
USER = "santoshmalla"
PASSWORD = os.environ.get("MAC_SSH_PASS", "")
REMOTE = "/Users/santoshmalla/momentra_v2_agent/momentra"
DEVICE_UDID = "00008110-00016CAA2E29401E"
DEVICE_CORE = "0EADCC1C-3542-5715-8D12-AD3B00CFE369"
BUNDLE = "resolvingpoint.momentra"


def connect() -> paramiko.SSHClient:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        client.connect(HOST, username=USER, timeout=20, look_for_keys=True, allow_agent=False)
    except Exception:
        client.connect(
            HOST,
            username=USER,
            password=PASSWORD,
            timeout=20,
            allow_agent=False,
            look_for_keys=False,
        )
    return client


def run(client: paramiko.SSHClient, cmd: str, timeout: int = 3600) -> int:
    print(f"$ {cmd}", flush=True)
    _stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout, get_pty=True)
    while True:
        line = stdout.readline()
        if not line:
            break
        safe = line.encode("ascii", errors="replace").decode("ascii")
        sys.stdout.write(safe)
        sys.stdout.flush()
    code = stdout.channel.recv_exit_status()
    err = stderr.read().decode("utf-8", errors="replace")
    if err.strip():
        print(err.encode("ascii", errors="replace").decode("ascii"), file=sys.stderr)
    print(f"[exit {code}]", flush=True)
    return code


def main() -> None:
    client = connect()
    try:
        # Prefer listing schemes after packages already resolved
        run(
            client,
            f"cd {REMOTE} && xcodebuild -list -json 2>/dev/null | /usr/bin/python3 -c "
            "'import sys,json; d=json.load(sys.stdin); print(\"\\n\".join(d.get(\"project\",{}).get(\"schemes\",[])))'",
            timeout=600,
        )
        code = run(
            client,
            f"cd {REMOTE} && xcodebuild "
            f"-project momentra.xcodeproj "
            f"-scheme momentra "
            f"-configuration Debug "
            f"-destination 'platform=iOS,id={DEVICE_UDID}' "
            f"-derivedDataPath {REMOTE}/build/DerivedData-agent "
            f"-allowProvisioningUpdates "
            f"CODE_SIGN_STYLE=Automatic "
            f"DEVELOPMENT_TEAM=TY9S2C44WR "
            f"build",
            timeout=3600,
        )
        if code != 0:
            sys.exit(code)
        # Find .app
        run(
            client,
            f"APP=$(find {REMOTE}/build/DerivedData-agent/Build/Products -name 'momentra.app' -type d | head -1); "
            f"echo APP=$APP; "
            f"xcrun devicectl device install app --device {DEVICE_CORE} \"$APP\"; "
            f"xcrun devicectl device process launch --device {DEVICE_CORE} {BUNDLE} || true",
            timeout=600,
        )
    finally:
        client.close()


if __name__ == "__main__":
    main()
