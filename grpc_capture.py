#!/usr/bin/env python
"""Capture emulator frames via the gRPC EmulatorController.getScreenshot RPC.

adb screencap returns black on this headless Intel-Mac emulator (HWC layer is
opaque black). The gRPC API reads the emulator's internal GPU framebuffer
directly, so it captures the real rendered frame. See HANDOFF.md.

Usage:
  grpc_capture.py snap   OUT.png
  grpc_capture.py record OUTDIR SECONDS [FPS]   # writes frame-%05d.png
"""
import os, sys, time, glob

HERE = os.path.dirname(os.path.abspath(__file__))
PB = os.path.join(HERE, ".capture", "pb")
sys.path.insert(0, PB)

import grpc
import emulator_controller_pb2 as pb
import emulator_controller_pb2_grpc as pbg

W, H = 1080, 2400
TOKEN = open(os.path.expanduser("~/.emulator_console_auth_token")).read().strip()


def _stub():
    ch = grpc.insecure_channel(
        "localhost:8554",
        options=[("grpc.max_receive_message_length", 64 * 1024 * 1024)],
    )
    return pbg.EmulatorControllerStub(ch), ch


def _fmt():
    return pb.ImageFormat(format=pb.ImageFormat.PNG, width=W, height=H)


def _md():
    return [("authorization", "Bearer " + TOKEN)]


def snap(out):
    stub, ch = _stub()
    img = stub.getScreenshot(_fmt(), metadata=_md())
    with open(out, "wb") as f:
        f.write(img.image)
    ch.close()
    print("wrote %d bytes -> %s" % (len(img.image), out))


def record(outdir, seconds, fps=None):
    # gRPC PNG grabs cap at ~2 fps on this host, so we poll at max rate for the
    # wall-clock duration and let the caller encode real-time (framerate=N/dur).
    # The measured fps is written to <outdir>/fps.txt.
    os.makedirs(outdir, exist_ok=True)
    for f in glob.glob(os.path.join(outdir, "frame-*.png")):
        os.remove(f)
    stub, ch = _stub()
    n = 0
    t0 = time.time()
    while time.time() - t0 < seconds:
        img = stub.getScreenshot(_fmt(), metadata=_md())
        with open(os.path.join(outdir, "frame-%05d.png" % n), "wb") as fh:
            fh.write(img.image)
        n += 1
    ch.close()
    elapsed = time.time() - t0
    real = n / elapsed if elapsed else 1.0
    with open(os.path.join(outdir, "fps.txt"), "w") as fh:
        fh.write("%.4f" % real)
    print("captured %d frames in %.1fs (%.2f fps) -> %s" % (n, elapsed, real, outdir))


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "snap":
        snap(sys.argv[2])
    elif cmd == "record":
        record(sys.argv[2], float(sys.argv[3]), float(sys.argv[4]) if len(sys.argv) > 4 else 10.0)
    else:
        sys.exit("unknown cmd " + cmd)
