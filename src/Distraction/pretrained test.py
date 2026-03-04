"""
Driver Distraction Detection System
=====================================
Models:
  - YOLOv11n-pose  → looking away (head pose via keypoints)
                   → hand-to-mouth heuristic (eating/drinking when occluded)
  - YOLOv11n       → phone, eating, drinking, smoking, yawning

Sliding window: 5 frames
  If distraction detected in current frame AND >= 2 of previous 4 frames
  → confirmed distraction → capture frame + log with timestamp & type
"""

import cv2
import json
import logging
from datetime import datetime
from collections import deque
from pathlib import Path

from ultralytics import YOLO

# ─────────────────────────────────────────────
# CONFIG
# ─────────────────────────────────────────────

CAPTURE_DIR       = Path("captures")
LOG_FILE          = Path("distraction_log.json")
WINDOW_SIZE       = 5
CONFIRM_THRESHOLD = 2

POSE_MODEL_PATH   = "yolo11n-pose.pt"
DETECT_MODEL_PATH = "yolo11n.pt"

POSE_CONF    = 0.5
DETECT_CONF  = 0.4

# How far eye-center can drift from frame center before "looking away"
HEAD_OFFSET_RATIO = 0.18   # 18% of frame width

# How close wrist must be to nose to count as hand-to-mouth (fraction of frame height)
# Increase this value if it misses occluded cups; decrease to reduce false positives
HAND_MOUTH_RATIO  = 0.20   # 20% of frame height

# ── COCO Pose keypoint indices ───────────────
KP_NOSE         = 0
KP_LEFT_EYE     = 1
KP_RIGHT_EYE    = 2
KP_LEFT_WRIST   = 9
KP_RIGHT_WRIST  = 10

# ── COCO object labels → canonical distraction names ─────────────────────────
LABEL_TO_DISTRACTION = {
    "cell phone": "Phone Use",
    "bottle":     "Drinking",
    "cup":        "Drinking",
    "wine glass": "Drinking",
    "banana":     "Eating",
    "apple":      "Eating",
    "sandwich":   "Eating",
    "orange":     "Eating",
    "cake":       "Eating",
    "donut":      "Eating",
    "hot dog":    "Eating",
    "pizza":      "Eating",
    "carrot":     "Eating",
    "broccoli":   "Eating",
    # After fine-tuning on NTHU-DDD, uncomment:
    # "cigarette":  "Smoking",
    # "yawn":       "Yawning",
}

# UI colors per distraction type (BGR)
DISTRACTION_COLORS = {
    "Phone Use":       (0,   100, 255),
    "Drinking":        (255, 160,   0),
    "Eating":          (0,   200, 100),
    "Eating/Drinking": (180, 180,   0),   # hand-to-mouth fallback
    "Smoking":         (80,   80, 255),
    "Yawning":         (200,   0, 200),
    "Looking Away":    (0,    60, 220),
}

# ─────────────────────────────────────────────
# LOGGING SETUP
# ─────────────────────────────────────────────

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)s  %(message)s",
    handlers=[
        logging.FileHandler("detector.log"),
        logging.StreamHandler(),
    ],
)
logger = logging.getLogger(__name__)


# ─────────────────────────────────────────────
# KEYPOINT HELPERS
# ─────────────────────────────────────────────

def _kp_visible(kp) -> bool:
    """A keypoint at (0, 0) means it was not detected."""
    return not (kp[0] == 0 and kp[1] == 0)


def _dist(a, b) -> float:
    return ((a[0] - b[0]) ** 2 + (a[1] - b[1]) ** 2) ** 0.5


# ─────────────────────────────────────────────
# DETECTION FUNCTIONS
# ─────────────────────────────────────────────

def detect_looking_away(pose_results, frame_w: int) -> bool:
    """
    True if the driver's eye-center is offset more than HEAD_OFFSET_RATIO
    of the frame width from the horizontal centre.
    """
    if pose_results.keypoints is None:
        return False

    for kps in pose_results.keypoints.xy:
        if len(kps) <= KP_RIGHT_EYE:
            continue

        left_eye  = kps[KP_LEFT_EYE]
        right_eye = kps[KP_RIGHT_EYE]

        visible = [e for e in [left_eye, right_eye] if _kp_visible(e)]
        if not visible:
            continue

        eye_cx = sum(e[0] for e in visible) / len(visible)
        if abs(eye_cx - frame_w / 2.0) > frame_w * HEAD_OFFSET_RATIO:
            return True

    return False


def detect_hand_to_mouth(pose_results, frame_h: int) -> str | None:
    """
    Returns 'Eating/Drinking' if a wrist is within HAND_MOUTH_RATIO * frame_h
    pixels of the nose — catches occluded cups/glasses/food held to the mouth
    even when the object detector misses the item entirely.
    """
    if pose_results.keypoints is None:
        return None

    threshold = frame_h * HAND_MOUTH_RATIO

    for kps in pose_results.keypoints.xy:
        if len(kps) <= KP_RIGHT_WRIST:
            continue

        nose = kps[KP_NOSE]
        if not _kp_visible(nose):
            continue

        for wrist_idx in [KP_LEFT_WRIST, KP_RIGHT_WRIST]:
            wrist = kps[wrist_idx]
            if _kp_visible(wrist) and _dist(wrist, nose) < threshold:
                return "Eating/Drinking"

    return None


def detect_object_distractions(detect_results, pose_results, frame_h: int) -> list[str]:
    """
    Combines YOLO object detection with hand-to-mouth pose heuristic.
    Hand-to-mouth only fires if the object detector did NOT already catch
    eating or drinking (avoids duplicate labels).
    """
    found = set()

    # ── Object detection ──────────────────────────
    for box in detect_results.boxes:
        cls_id = int(box.cls)
        label  = detect_results.names.get(cls_id, "")
        if label in LABEL_TO_DISTRACTION:
            found.add(LABEL_TO_DISTRACTION[label])

    # ── Hand-to-mouth fallback ─────────────────────
    if "Eating" not in found and "Drinking" not in found:
        htom = detect_hand_to_mouth(pose_results, frame_h)
        if htom:
            found.add(htom)

    return list(found)


# ─────────────────────────────────────────────
# OVERLAY DRAWING
# ─────────────────────────────────────────────

def draw_overlay(frame, distractions: list[str], confirmed: bool):
    h, w = frame.shape[:2]

    if not distractions:
        cv2.rectangle(frame, (0, 0), (w, 42), (20, 20, 20), -1)
        cv2.putText(frame, "[OK]  DRIVER ATTENTIVE",
                    (12, 28), cv2.FONT_HERSHEY_DUPLEX, 0.7, (60, 220, 60), 1)
        return

    banner_color = (0, 0, 180) if confirmed else (0, 100, 220)
    banner_h     = 44 + 32 * len(distractions)
    cv2.rectangle(frame, (0, 0), (w, banner_h), banner_color, -1)

    status = "[!!] CONFIRMED DISTRACTION" if confirmed else "[!]  DISTRACTION DETECTED"
    cv2.putText(frame, status,
                (12, 28), cv2.FONT_HERSHEY_DUPLEX, 0.75, (255, 255, 255), 2)

    for i, d in enumerate(distractions):
        color = DISTRACTION_COLORS.get(d, (255, 255, 255))
        cv2.putText(frame, f"  > {d}",
                    (12, 56 + i * 32),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.68, color, 2)

    # Timestamp bottom-right corner
    ts = datetime.now().strftime("%Y-%m-%d  %H:%M:%S")
    cv2.putText(frame, ts,
                (w - 272, h - 12),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, (200, 200, 200), 1)


# ─────────────────────────────────────────────
# CAPTURE & LOG
# ─────────────────────────────────────────────

def save_capture(frame, distractions: list[str], capture_dir: Path) -> str:
    """
    Saves flagged frame as JPEG and appends a structured entry to
    distraction_log.json with timestamp, filename and distraction types.
    """
    capture_dir.mkdir(parents=True, exist_ok=True)

    now    = datetime.now()
    ts_str = now.strftime("%Y%m%d_%H%M%S_%f")[:19]
    fname  = f"distraction_{ts_str}.jpg"
    fpath  = capture_dir / fname

    cv2.imwrite(str(fpath), frame)

    entry = {
        "timestamp":    now.isoformat(timespec="seconds"),
        "filename":     fname,
        "distractions": distractions,
    }

    log_data = []
    if LOG_FILE.exists():
        try:
            with open(LOG_FILE, "r") as f:
                log_data = json.load(f)
        except json.JSONDecodeError:
            pass

    log_data.append(entry)
    with open(LOG_FILE, "w") as f:
        json.dump(log_data, f, indent=2)

    logger.warning("CAPTURED  %s  |  %s", fname, " + ".join(distractions))
    return fname


# ─────────────────────────────────────────────
# SLIDING WINDOW
# ─────────────────────────────────────────────

class SlidingWindow:
    """
    Keeps the last (WINDOW_SIZE - 1) frames of detections in a deque.

    A distraction is confirmed when it appears in the current frame AND
    in at least CONFIRM_THRESHOLD of the stored previous frames.
    This eliminates single-frame false positives.
    """

    def __init__(self, size: int = WINDOW_SIZE, threshold: int = CONFIRM_THRESHOLD):
        self.threshold = threshold
        self.history: deque[set] = deque(maxlen=size - 1)

    def push(self, current_distractions: list[str]) -> list[str]:
        """Push current detections, return list of confirmed distractions."""
        current_set = set(current_distractions)
        confirmed   = [
            d for d in current_set
            if sum(1 for past in self.history if d in past) >= self.threshold
        ]
        self.history.append(current_set)
        return confirmed


# ─────────────────────────────────────────────
# MAIN LOOP
# ─────────────────────────────────────────────

def run(source=0):
    """
    source : int  → camera index (0 = default webcam, 1 = dashcam USB)
             str  → path to a video file for offline testing
    """
    logger.info("Loading YOLOv11n-pose  (%s)", POSE_MODEL_PATH)
    pose_model = YOLO(POSE_MODEL_PATH)

    logger.info("Loading YOLOv11n detect (%s)", DETECT_MODEL_PATH)
    detect_model = YOLO(DETECT_MODEL_PATH)

    window = SlidingWindow()

    cap = cv2.VideoCapture(source)
    if not cap.isOpened():
        logger.error("Cannot open video source: %s", source)
        return

    logger.info("Detection running — press Q to quit")

    while True:
        ret, frame = cap.read()
        if not ret:
            logger.info("Stream ended.")
            break

        frame_h, frame_w = frame.shape[:2]

        # ── Run both models ──────────────────────────
        pose_res   = pose_model(frame,   conf=POSE_CONF,   verbose=False)[0]
        detect_res = detect_model(frame, conf=DETECT_CONF, verbose=False)[0]

        # ── Gather distractions this frame ───────────
        distractions = detect_object_distractions(detect_res, pose_res, frame_h)

        if detect_looking_away(pose_res, frame_w):
            distractions.append("Looking Away")

        # ── Sliding window confirmation ───────────────
        confirmed = window.push(distractions)

        # ── Draw overlay on display copy ─────────────
        display = frame.copy()
        draw_overlay(display, distractions, bool(confirmed))

        # ── Save confirmed events ─────────────────────
        if confirmed:
            save_capture(display, confirmed, CAPTURE_DIR)

        cv2.imshow("Driver Distraction Detector", display)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            logger.info("User quit.")
            break

    cap.release()
    cv2.destroyAllWindows()
    logger.info("Done. Captures in: %s", CAPTURE_DIR.resolve())


# ─────────────────────────────────────────────
# ENTRY POINT
# ─────────────────────────────────────────────

if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(description="Driver Distraction Detector")
    parser.add_argument(
        "--source", default=0,
        help="0=webcam, 1=dashcam USB, or path to a video file"
    )
    args = parser.parse_args()

    source = args.source
    try:
        source = int(source)
    except (ValueError, TypeError):
        pass

    run(source)