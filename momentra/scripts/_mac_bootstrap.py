import os
import sys
import time
from pathlib import Path

import paramiko

HOST = "santosh-mac.local"
USER = "santoshmalla"
PASSWORD = os.environ.get("MAC_SSH_PASS", "")
PUBKEY = Path.home() / ".ssh" / "id_ed25519.pub"
LOCAL_ROOT = Path(r"g:\momentra_v2\momentra")
REMOTE_ROOT = "/Users/santoshmalla/momentra_v2_agent/momentra"


def connect():
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(
        HOST,
        username=USER,
        password=PASSWORD,
        timeout=20,
        allow_agent=False,
        look_for_keys=False,
    )
    return client


def run(client, cmd, timeout=120):
    print(f"$ {cmd}")
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    code = stdout.channel.recv_exit_status()
    if out.strip():
        print(out.rstrip())
    if err.strip():
        print(err.rstrip(), file=sys.stderr)
    return code, out, err


def ensure_key(client):
    if not PUBKEY.exists():
        return
    pub = PUBKEY.read_text(encoding="utf-8").strip()
    run(
        client,
        "mkdir -p ~/.ssh && chmod 700 ~/.ssh && "
        f"grep -qxF '{pub}' ~/.ssh/authorized_keys 2>/dev/null || echo '{pub}' >> ~/.ssh/authorized_keys && "
        "chmod 600 ~/.ssh/authorized_keys",
    )


def sftp_put_dir(sftp, local: Path, remote: str, skip_parts=None):
    skip_parts = skip_parts or {
        "build",
        "DerivedData",
        "DerivedData-agent",
        ".git",
        "Pods",
        "xcuserdata",
        "node_modules",
    }

    def ensure_dir(path: str):
        parts = path.strip("/").split("/")
        cur = ""
        for p in parts:
            cur = f"{cur}/{p}" if cur else f"/{p}" if path.startswith("/") else p
            # remote paths are absolute under /Users/...
            try:
                sftp.stat(cur if path.startswith("/") else path.split(p)[0] + p)
            except FileNotFoundError:
                pass
        # simpler: mkdir via ssh-style recursive
        dirs = []
        rem = remote if remote.startswith("/") else f"/{remote}"
        # build progressive paths
        bits = rem.strip("/").split("/")
        acc = ""
        for b in bits:
            acc += "/" + b
            dirs.append(acc)
        for d in dirs:
            try:
                sftp.stat(d)
            except FileNotFoundError:
                try:
                    sftp.mkdir(d)
                except OSError:
                    pass

    ensure_dir(remote)

    for root, dirs, files in os.walk(local):
        dirs[:] = [d for d in dirs if d not in skip_parts and not d.endswith(".xcuserdatad")]
        rel = Path(root).relative_to(local).as_posix()
        rdir = remote if rel == "." else f"{remote}/{rel}"
        try:
            sftp.stat(rdir)
        except FileNotFoundError:
            # create nested
            parts = rdir.strip("/").split("/")
            acc = ""
            for p in parts:
                acc += "/" + p
                try:
                    sftp.stat(acc)
                except FileNotFoundError:
                    sftp.mkdir(acc)
        for f in files:
            if f.endswith(".ipa") or f.endswith(".app"):
                continue
            lp = Path(root) / f
            rp = f"{rdir}/{f}"
            sftp.put(str(lp), rp)


def main():
    if not PASSWORD:
        print("MAC_SSH_PASS not set", file=sys.stderr)
        sys.exit(2)
    client = connect()
    try:
        ensure_key(client)
        run(client, "hostname; whoami; sw_vers; xcodebuild -version | head -2")
        run(client, "xcrun xctrace list devices 2>/dev/null | head -40 || true")
        run(client, "xcrun devicectl list devices 2>/dev/null | head -40 || true")
        run(client, f"mkdir -p {REMOTE_ROOT}")
        print("Uploading iOS project via SFTP...")
        sftp = client.open_sftp()
        try:
            sftp_put_dir(sftp, LOCAL_ROOT, REMOTE_ROOT)
        finally:
            sftp.close()
        print("Upload complete.")
        run(client, f"ls -la {REMOTE_ROOT} | head -30")
        run(client, f"test -f {REMOTE_ROOT}/momentra.xcodeproj/project.pbxproj && echo PROJECT_OK")
    finally:
        client.close()


if __name__ == "__main__":
    main()
