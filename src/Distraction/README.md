# 🚗 Yaqazah — Distraction Detection Module (YOLOv11)

Detects some o driver distractions in real-time using YOLOv11.

---

## 📦 Detected Classes

| ID | Class      | Description              |
|----|------------|--------------------------|
| 0  | phone      | Talking/texting on phone |
| 1  | cigarette  | Smoking                  |
| 2  | drink      | Drinking                 |
| 3  | food       | Eating                   |
| 4  | safe       | Safe driving             |
| 5  | radio      | Operating radio/stereo   |
| 6  | reaching   | Reaching behind          |
| 7  | grooming   | Hair, makeup, grooming   |

---

## 📁 Project Structure

```
Distraction/
├── prepare_dataset.py      # merges & converts all datasets into unified format ( i already did this step)
├── train.py                # trains YOLOv11 on the merged dataset
├── pretrained test.py      # this is the try to use directly yolo without training on custom dataset
├── datasets/
│   └── final/              # ⚠️ download from Google Drive (see below)
│       ├── images/
│       │   ├── train/
│       │   ├── val/
│       │   └── test/
│       ├── labels/
│       │   ├── train/
│       │   ├── val/
│       │   └── test/
│       └── data.yaml
└── weights/                # ⚠️ upload best.pt here after training
    └── best.pt
```

---

## 👥 Teammate Quick-Start (Training)

> For whoever is running the training on their machine.

```bash
# 1. Activate the virtual environment
source /path/to/Yaqazah/bin/activate

# 2. Install dependencies
pip install -r requirements.txt

# 3. Download final/ from Google Drive → place inside datasets/
#    📂 Google Drive Link: https://drive.google.com/drive/folders/1qykrAAc89_cKRMpmbIC1oOaXqeD4nPC9

# 4. Train
python train.py

# 5. Upload weights/best.pt back to Google Drive when done or just commit it :)
```


## 🗃️ Datasets Used ( the claude suggestions )

| Dataset | Source | Classes Contributed |
|---|---|---|
| StateFarm Distracted Driver | [Kaggle](https://www.kaggle.com/c/state-farm-distracted-driver-detection) | phone, drink, safe, radio, reaching, grooming |
| Smoker-YOLO | [Roboflow](https://universe.roboflow.com/cigaretteple-7m0hn/smoker-yolo) | cigarette |
| Smoking and Drinking Detection | [Roboflow](https://universe.roboflow.com/yolo-dataset-rtznj/smoking-and-drinking-detection) | cigarette, drink |

> Raw datasets are **not** stored on Git or Drive.
> Only the merged `final/` output is shared to save storage.
> To regenerate `final/` from scratch, see Option B below.

---



## 📊 Model Info

| Property | Value |
|-|-|
| Architecture | YOLOv11n |
| Epochs | 50 |
| Classes | 8 |
| Total images | ~25,000 |

---

