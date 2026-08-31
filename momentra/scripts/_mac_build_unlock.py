import os
import time

import paramiko

PASS = os.environ["MAC_SSH_PASS"]
REMOTE = "/Users/santoshmalla/momentra_v2_agent/momentra"
LOG = f"{REMOTE}/build/agent-build.log"
STATUS = f"{REMOTE}/build/agent-build.status"
UDID = "00008110-00016CAA2E29401E"
CORE = "0EADCC1C-3542-5715-8D12-AD3B00CFE369"
BUNDLE = "resolvingpoint.momentra"
KC = "/Users/santoshmalla/Library/Keychains/login.keychain-db"


def connect() -> paramiko.SSHClient:
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(
        "santosh-mac.local",
        username="santoshmalla",
        timeout=20,
        look_for_keys=True,
        allow_agent=False,
    )
    return client


def run(client: paramiko.SSHClient, cmd: str, timeout: int = 180) -> tuple[int, str]:
    print(">", cmd[:160], flush=True)
    _i, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    code = stdout.channel.recv_exit_status()
    text = (out + err).encode("ascii", errors="replace").decode("ascii")
    if text.strip():
        print(text[-2500:], flush=True)
    print("exit", code, flush=True)
    return code, text


def main() -> None:
    client = connect()
    try:
        run(client, f"security unlock-keychain -p '{PASS}' '{KC}'")
        run(client, f"security set-keychain-settings -t 3600 -u '{KC}'")
        run(
            client,
            f"security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k '{PASS}' '{KC}'",
        )
        run(client, "pkill -f 'xcodebuild -project momentra.xcodeproj' || true")
        time.sleep(1)
        start = f"""
cd {REMOTE} || exit 91
rm -f {STATUS} {LOG}
nohup bash -lc '
  security unlock-keychain -p "{PASS}" "{KC}"
  set -o pipefail
  xcodebuild -project momentra.xcodeproj -scheme momentra -configuration Debug \
    -destination "platform=iOS,id={UDID}" \
    -derivedDataPath {REMOTE}/build/DerivedData-agent \
    -allowProvisioningUpdates CODE_SIGN_STYLE=Automatic DEVELOPMENT_TEAM=TY9S2C44WR build 2>&1 | tee {LOG}
  ec=$?
  echo $ec > {STATUS}
  if [ $ec -eq 0 ]; then
    APP=$(find {REMOTE}/build/DerivedData-agent/Build/Products -name momentra.app -type d | head -n 1)
    echo APP=$APP | tee -a {LOG}
    xcrun devicectl device install app --device {CORE} "$APP" 2>&1 | tee -a {LOG}
    echo INSTALL_EC=$? | tee -a {LOG}
    xcrun devicectl device process launch --device {CORE} {BUNDLE} 2>&1 | tee -a {LOG} || true
    echo DONE | tee -a {LOG}
  fi
' >/dev/null 2>&1 &
echo STARTED
"""
        run(client, start)
        for i in range(120):
            time.sleep(15)
            _code, text = run(
                client,
                f"cat {STATUS} 2>/dev/null || echo NONE; echo ----; tail -n 12 {LOG} 2>/dev/null | tr -cd '\\11\\12\\15\\40-\\176'",
            )
            first = text.splitlines()[0].strip() if text.splitlines() else ""
            print(f"poll {i}: status={first}", flush=True)
            if first.isdigit():
                print("FINAL_STATUS", first, flush=True)
                break
    finally:
        client.close()


if __name__ == "__main__":
    main()
