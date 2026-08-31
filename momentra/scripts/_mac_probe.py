import os
import sys

import paramiko

HOST = "santosh-mac.local"
USER = "santoshmalla"
PASSWORD = os.environ.get("MAC_SSH_PASS", "")


def main() -> None:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        client.connect(HOST, username=USER, timeout=15, look_for_keys=True, allow_agent=False)
        print("auth=key")
    except Exception as exc:
        print(f"key failed: {exc}")
        client.connect(
            HOST,
            username=USER,
            password=PASSWORD,
            timeout=20,
            allow_agent=False,
            look_for_keys=False,
        )
        print("auth=password")

    cmds = [
        "echo PATH=$PATH",
        "ls /Applications | grep -i xcode || true",
        "xcode-select -p 2>&1 || true",
        "which xcodebuild; xcodebuild -version 2>&1 || true",
        "system_profiler SPUSBDataType 2>/dev/null | grep -E 'iPhone|Serial Number|Product ID' | head -40 || true",
        "xcrun xcdevice list 2>&1 | head -80 || true",
        "xcrun xctrace list devices 2>&1 | head -80 || true",
        "xcrun devicectl list devices 2>&1 | head -80 || true",
        "df -h / | tail -1",
        "ls -la ~ | head -40",
    ]
    for cmd in cmds:
        print("====", cmd)
        _stdin, stdout, stderr = client.exec_command(cmd, timeout=90)
        out = stdout.read().decode("utf-8", errors="replace")
        err = stderr.read().decode("utf-8", errors="replace")
        code = stdout.channel.recv_exit_status()
        sys.stdout.write(out)
        if err.strip():
            sys.stderr.write(err)
        print(f"[exit {code}]")
    client.close()


if __name__ == "__main__":
    main()
