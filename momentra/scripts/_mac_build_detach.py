#!/usr/bin/env python3
"""Build/install momentra on a connected iPhone via SSH to the Mac.

Usage (from Windows, after Mac is reachable):
  python momentra/scripts/_mac_build_detach.py [host]

Requires SSH key auth to user santoshmalla. Unlock login keychain on the Mac
interactively if codesign fails (do not put passwords in this script).
"""

from __future__ import annotations

import sys
import time

import paramiko

HOSTS = [sys.argv[1]] if len(sys.argv) > 1 else ["192.168.68.107", "santosh-mac.local"]
USER = "santoshmalla"
UDID = "00008110-00016CAA2E29401E"
BUNDLE = "resolvingpoint.momentra"
PROJECT = "/Volumes/coding/momentra_v2/momentra"


def main() -> int:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    connected = None
    for host in HOSTS:
        try:
            client.connect(host, username=USER, timeout=12, look_for_keys=True, allow_agent=False)
            connected = host
            print("CONNECTED", host)
            break
        except Exception as exc:  # noqa: BLE001
            print("FAIL", host, exc)
    if not connected:
        return 1

    build = f"""
set -e
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
cd {PROJECT}
rm -rf /tmp/momentra_derived_vol
mkdir -p /tmp/momentra_derived_vol /tmp/momentra_build_vol
xcodebuild -project momentra.xcodeproj -scheme momentra -configuration Debug \\
  -destination 'platform=iOS,id={UDID}' \\
  -derivedDataPath /tmp/momentra_derived_vol \\
  CODE_SIGN_STYLE=Automatic DEVELOPMENT_TEAM=TY9S2C44WR build \\
  2>&1 | tee /tmp/momentra_build_vol/xcodebuild.log | tail -n 80
"""
    print("BUILD_START", time.strftime("%H:%M:%S"))
    _, stdout, _ = client.exec_command(build, timeout=900)
    out = stdout.read().decode("utf-8", "replace")
    print(out[-8000:])
    print("BUILD_END", time.strftime("%H:%M:%S"))
    if "BUILD SUCCEEDED" not in out:
        client.close()
        return 2

    install = f"""
APP=$(find /tmp/momentra_derived_vol/Build/Products/Debug-iphoneos -maxdepth 1 -name 'momentra.app' | head -n 1)
xcrun devicectl device install app --device {UDID} "$APP"
xcrun devicectl device process launch --device {UDID} {BUNDLE}
"""
    _, stdout, stderr = client.exec_command(install, timeout=120)
    print(stdout.read().decode("utf-8", "replace"))
    err = stderr.read().decode("utf-8", "replace")
    if err.strip():
        print(err)
    client.close()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
