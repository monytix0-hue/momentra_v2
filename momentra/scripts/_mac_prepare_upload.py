import os
import sys
from pathlib import Path

import paramiko

HOST = "santosh-mac.local"
USER = "santoshmalla"
PASSWORD = os.environ["MAC_SSH_PASS"]
LOCAL_ROOT = Path(r"g:\momentra_v2\momentra")
REMOTE_ROOT = "/Users/santoshmalla/momentra_v2_agent/momentra"
SKIP = {
    "build",
    "DerivedData",
    "DerivedData-agent",
    ".git",
    "Pods",
    "xcuserdata",
    "node_modules",
    "__pycache__",
    "scripts",
}


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


def run(client: paramiko.SSHClient, cmd: str, timeout: int = 300) -> tuple[int, str, str]:
    print(f"$ {cmd}")
    _stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    code = stdout.channel.recv_exit_status()
    if out.strip():
        print(out.rstrip())
    if err.strip():
        print(err.rstrip(), file=sys.stderr)
    print(f"[exit {code}]")
    return code, out, err


def sudo(client: paramiko.SSHClient, cmd: str, timeout: int = 120) -> tuple[int, str, str]:
    # Avoid putting password in shell history via process list as much as possible.
    full = f"sudo -S -p '' {cmd}"
    print(f"$ sudo {cmd}")
    stdin, stdout, stderr = client.exec_command(full, timeout=timeout)
    stdin.write(PASSWORD + "\n")
    stdin.flush()
    stdin.channel.shutdown_write()
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    code = stdout.channel.recv_exit_status()
    if out.strip():
        print(out.rstrip())
    if err.strip():
        print(err.rstrip(), file=sys.stderr)
    print(f"[exit {code}]")
    return code, out, err


def ensure_remote_dirs(sftp: paramiko.SFTPClient, remote: str) -> None:
    parts = remote.strip("/").split("/")
    acc = ""
    for part in parts:
        acc += "/" + part
        try:
            sftp.stat(acc)
        except FileNotFoundError:
            sftp.mkdir(acc)


def upload(client: paramiko.SSHClient) -> None:
    run(client, f"mkdir -p {REMOTE_ROOT}")
    sftp = client.open_sftp()
    count = 0
    try:
        for root, dirs, files in os.walk(LOCAL_ROOT):
            dirs[:] = [d for d in dirs if d not in SKIP and not d.endswith(".xcuserdatad")]
            rel = Path(root).relative_to(LOCAL_ROOT).as_posix()
            rdir = REMOTE_ROOT if rel == "." else f"{REMOTE_ROOT}/{rel}"
            ensure_remote_dirs(sftp, rdir)
            for name in files:
                if name.endswith((".ipa", ".app", ".pyc")):
                    continue
                local = Path(root) / name
                remote = f"{rdir}/{name}"
                sftp.put(str(local), remote)
                count += 1
                if count % 100 == 0:
                    print(f"uploaded {count} files...")
    finally:
        sftp.close()
    print(f"uploaded {count} files total")


def main() -> None:
    client = connect()
    try:
        sudo(client, "xcode-select -s /Applications/Xcode.app/Contents/Developer")
        sudo(client, "xcodebuild -license accept")
        run(client, "xcode-select -p; xcodebuild -version")
        run(client, "xcrun xctrace list devices 2>&1 | head -80")
        run(client, "xcrun devicectl list devices 2>&1 | head -80")
        print("Uploading project...")
        upload(client)
        run(client, f"ls '{REMOTE_ROOT}/momentra.xcodeproj' && echo PROJECT_OK")
    finally:
        client.close()


if __name__ == "__main__":
    main()
