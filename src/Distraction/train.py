from ultralytics import YOLO

model = YOLO("yolo11n.pt")

model.train(
    data="./datasets/final/data.yaml",
    epochs=50,
    imgsz=640,
    batch=16,
    device=0,
    patience=10,
    augment=True,
    project="driver_distraction",
    name="yolo11_v1"
)

# Evaluate
metrics = model.val()
print(f"mAP50:     {metrics.box.map50:.3f}")
print(f"Precision: {metrics.box.p:.3f}")
print(f"Recall:    {metrics.box.r:.3f}")

# ```
#
# ---
#
# ## Final Folder Structure After Running
# ```
# datasets/
#   final/
#     images/
#       train/   ← all images merged
#       val/
#       test/
#     labels/
#       train/   ← all labels remapped to unified classes
#       val/
#       test/
#     data.yaml  ← ready for YOLO training
