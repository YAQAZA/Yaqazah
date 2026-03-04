import os
import shutil
import yaml
import cv2
from pathlib import Path
from sklearn.model_selection import train_test_split

# ─────────────────────────────────────────
# CONFIG — your unified classes
# ─────────────────────────────────────────
CLASS_NAMES = ['phone', 'cigarette', 'drink', 'food', 'safe', 'radio', 'reaching', 'grooming']
OUTPUT_DIR  = Path("./datasets/final")

# ─────────────────────────────────────────
# STEP 1: Convert StateFarm to YOLO format
# StateFarm is classification (no bboxes)
# We treat the whole image as the bounding box
# ─────────────────────────────────────────

# Map StateFarm folders to your classes
STATEFARM_MAP = {
    'c0': 'safe',
    'c1': 'phone',
    'c2': 'phone',
    'c3': 'phone',
    'c4': 'phone',
    'c5': 'radio',      # operating radio
    'c6': 'reaching',   # reaching behind
    'c7': 'drink',
    'c8': 'grooming',   # hair and makeup
    # c9 = talking to passenger → still skipped (no visual object to detect)
}


def convert_statefarm(statefarm_dir, output_dir, class_names):
    print("Converting StateFarm...")
    class_idx = {name: i for i, name in enumerate(class_names)}
    all_images = []
    all_labels = []

    for folder, label_name in STATEFARM_MAP.items():
        folder_path = Path(statefarm_dir) / 'imgs' / 'train' / folder
        if not folder_path.exists():
            print(f"  Skipping {folder} — not found")
            continue

        class_id = class_idx[label_name]
        for img_file in folder_path.glob('*.jpg'):
            all_images.append(img_file)
            all_labels.append(f"{class_id} 0.5 0.5 1.0 1.0\n")

    # Split into train/val/test
    train_imgs, temp_imgs, train_lbls, temp_lbls = train_test_split(
        all_images, all_labels, test_size=0.3, random_state=42)
    val_imgs, test_imgs, val_lbls, test_lbls = train_test_split(
        temp_imgs, temp_lbls, test_size=0.5, random_state=42)

    splits = {
        'train': (train_imgs, train_lbls),
        'val':   (val_imgs,   val_lbls),
        'test':  (test_imgs,  test_lbls),
    }

    for split, (imgs, lbls) in splits.items():
        img_out = output_dir / 'images' / split
        lbl_out = output_dir / 'labels' / split
        img_out.mkdir(parents=True, exist_ok=True)
        lbl_out.mkdir(parents=True, exist_ok=True)

        for img_path, label in zip(imgs, lbls):
            new_name = f"sf_{img_path.name}"
            shutil.copy(img_path, img_out / new_name)
            with open(lbl_out / (Path(new_name).stem + '.txt'), 'w') as f:
                f.write(label)

    print(f"  StateFarm done: {len(train_imgs)} train, {len(val_imgs)} val, {len(test_imgs)} test")


# ─────────────────────────────────────────
# STEP 2: Merge Roboflow datasets
# These already have bboxes in YOLO format
# We just need to remap class IDs to match ours
# ─────────────────────────────────────────

def remap_classes(source_yaml, class_names):
    """Build a mapping from source class IDs to our unified class IDs."""
    with open(source_yaml) as f:
        data = yaml.safe_load(f)

    source_classes = data['names']
    class_idx = {name: i for i, name in enumerate(class_names)}
    remap = {}

    for i, name in enumerate(source_classes):
        name_lower = name.lower()
        # fuzzy match to our classes
        if any(x in name_lower for x in ['phone', 'mobile', 'cell', 'call', 'text']):
            remap[i] = class_idx['phone']
        elif any(x in name_lower for x in ['cigar', 'smok', 'cigarette']):
            remap[i] = class_idx['cigarette']
        elif any(x in name_lower for x in ['drink', 'bottle', 'cup', 'water', 'beverage']):
            remap[i] = class_idx['drink']
        elif any(x in name_lower for x in ['food', 'eat', 'burger', 'snack']):
            remap[i] = class_idx['food']
        elif any(x in name_lower for x in ['safe', 'normal', 'distracted']):
            remap[i] = class_idx['safe']
        else:
            print(f"  WARNING: class '{name}' not mapped — skipping")
            remap[i] = None  # will be skipped

    print(f"  Class mapping: {dict(zip(source_classes, remap.values()))}")
    return remap


def merge_roboflow_dataset(source_dir, output_dir, class_names, prefix):
    print(f"Merging {prefix}...")
    source_dir = Path(source_dir)
    yaml_path = source_dir / 'data.yaml'

    remap = remap_classes(yaml_path, class_names)

    for split in ['train', 'val', 'test']:
        src_img = source_dir / split / 'images'
        src_lbl = source_dir / split / 'labels'
        dst_img = output_dir / 'images' / split
        dst_lbl = output_dir / 'labels' / split
        dst_img.mkdir(parents=True, exist_ok=True)
        dst_lbl.mkdir(parents=True, exist_ok=True)

        if not src_img.exists():
            continue

        for img_file in src_img.glob('*.*'):
            lbl_file = src_lbl / (img_file.stem + '.txt')
            new_img_name = f"{prefix}_{img_file.name}"
            new_lbl_name = f"{prefix}_{img_file.stem}.txt"

            # Copy image
            shutil.copy(img_file, dst_img / new_img_name)

            # Remap label file
            new_lines = []
            if lbl_file.exists():
                with open(lbl_file) as f:
                    for line in f:
                        parts = line.strip().split()
                        if not parts:
                            continue
                        old_id = int(parts[0])
                        new_id = remap.get(old_id)
                        if new_id is not None:
                            new_lines.append(f"{new_id} {' '.join(parts[1:])}\n")

            with open(dst_lbl / new_lbl_name, 'w') as f:
                f.writelines(new_lines)

    print(f"  {prefix} done ✅")


# ─────────────────────────────────────────
# STEP 3: Write final data.yaml
# ─────────────────────────────────────────

def write_yaml(output_dir, class_names):
    yaml_content = {
        'train': str((output_dir / 'images' / 'train').resolve()),
        'val':   str((output_dir / 'images' / 'val').resolve()),
        'test':  str((output_dir / 'images' / 'test').resolve()),
        'nc':    len(class_names),
        'names': class_names
    }
    with open(output_dir / 'data.yaml', 'w') as f:
        yaml.dump(yaml_content, f, default_flow_style=False)
    print(f"\ndata.yaml written to {output_dir / 'data.yaml'} ✅")


# ─────────────────────────────────────────
# RUN
# ─────────────────────────────────────────

if __name__ == "__main__":
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    # 1. StateFarm
    convert_statefarm(
        "./datasets/statefarm",
        OUTPUT_DIR,
        CLASS_NAMES
    )

    # 2. Smoker YOLO
    merge_roboflow_dataset(
        "Smoker-YOLO-2",
        OUTPUT_DIR,
        CLASS_NAMES,
        prefix="smoker"
    )

    # 3. Smoking + Drinking
    merge_roboflow_dataset(
        "Smoking-and-Drinking-Detection-2",
        OUTPUT_DIR,
        CLASS_NAMES,
        prefix="smokdrink"
    )

    # 4. Write data.yaml
    write_yaml(OUTPUT_DIR, CLASS_NAMES)

    # 5. Print summary
    for split in ['train', 'val', 'test']:
        count = len(list((OUTPUT_DIR / 'images' / split).glob('*.*')))
        print(f"  {split}: {count} images")