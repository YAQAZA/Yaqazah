# from roboflow import Roboflow
#
# rf = Roboflow(api_key="")
#
# # Dataset 1 — Smoker YOLO (already downloaded ✅)
# project = rf.workspace("cigaretteple-7m0hn").project("smoker-yolo")
# dataset = project.version(2).download("yolov11")
#
# # Dataset 2 — Fix: use version 2 instead of 1
# project2 = rf.workspace("yolo-dataset-rtznj").project("smoking-and-drinking-detection")
# dataset2 = project2.version(2).download("yolov11")
#
# # Dataset 3 — Driver specific: phone + smoking (bonus, good for your GP)
# project3 = rf.workspace("ai-czjl4").project("driver-phone-smoking")
# dataset3 = project3.version(1).download("yolov11")


# see the dataset structure##########################################################3
# import yaml
#
# for dataset_path in [
#     "Smoker-YOLO-2/data.yaml",
#     "Smoking-and-Drinking-Detection-2/data.yaml"
# ]:
#     with open(dataset_path) as f:
#         data = yaml.safe_load(f)
#     print(f"\n{dataset_path}")
#     print(f"Classes: {data['names']}")
##################################################################################

#train script

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
