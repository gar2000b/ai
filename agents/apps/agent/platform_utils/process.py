"""Process-related OS utilities (e.g. set process name on Linux)."""
import sys


def set_process_name(name: str = "ai_code") -> None:
    """Set the process name shown by top/htop/btop (Linux only). No-op on other platforms."""
    if not sys.platform.startswith("linux"):
        return
    try:
        import ctypes

        libc = ctypes.CDLL(None)
        PR_SET_NAME = 15

        libc.prctl.argtypes = [
            ctypes.c_int,
            ctypes.c_char_p,
            ctypes.c_ulong,
            ctypes.c_ulong,
            ctypes.c_ulong,
        ]
        libc.prctl.restype = ctypes.c_int

        name_bytes = name.encode()[:15]
        libc.prctl(PR_SET_NAME, name_bytes, 0, 0, 0)

        with open("/proc/self/comm", "wb", buffering=0) as f:
            f.write(name_bytes + b"\n")
    except Exception:
        pass
