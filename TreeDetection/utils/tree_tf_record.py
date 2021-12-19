import os

import contextlib2
from models.research.object_detection.dataset_tools import tf_record_creation_util
from models.research.object_detection.dataset_tools.create_coco_tf_record import create_tf_example
from models.research.object_detection.utils import label_map_util
import xml.etree.ElementTree as ET
import cv2


def create_tf_record_from_labelme_xml(dir, output_path, num_shards):
    annotations = []
    for file in os.listdir(dir):
        if file.endswith(".xml"):
            path = os.path.join(dir, file)
            annotations.append(ET.parse(path).getroot())

    with contextlib2.ExitStack() as tf_record_close_stack:
        output_tfrecords = tf_record_creation_util.open_sharded_output_tfrecords(
            tf_record_close_stack, output_path, num_shards)
        category_index = label_map_util.create_category_index(
            [
                {
                    "id": 1,
                    "name": "tree"
                }
            ]
        )

        total_num_annotations_skipped = 0

        for idx, image in enumerate(annotations):
            file_name = image.find("filename").text
            h, w, _ = cv2.imread(os.path.join(dir, file_name)).shape
            image_json = {
                "file_name": file_name,
                "height": h,
                "width": w,
                "id": idx + 1
            }
            annotations_json = []
            for idy, object in enumerate(image.iter("object")):
                xmin, ymin, xmax, ymax = None, None, None, None
                segmentation = []
                for point in object.iter("pt"):
                    x, y = int(point.find("x").text), int(point.find("y").text)

                    if xmin is None:
                        xmin = x
                    else:
                        xmin = min(xmin, x)
                    if ymin is None:
                        ymin = y
                    else:
                        ymin = min(ymin, y)
                    if xmax is None:
                        xmax = x
                    else:
                        xmax = max(xmax, x)
                    if ymax is None:
                        ymax = y
                    else:
                        ymax = max(ymax, y)

                    segmentation.append(x)
                    segmentation.append(y)

                annotations_json.append(
                    {
                        "bbox": [xmin, ymin, xmax-xmin, ymax-ymin],
                        "segmentation": [segmentation],
                        "iscrowd": 0,
                        "category_id": 1,
                        "area": w * h,
                        "id": idy + 1
                    }
                )

            (_, tf_example, num_annotations_skipped, _, _) = create_tf_example(
                image_json, annotations_json, dir, category_index, True
            )

            total_num_annotations_skipped += num_annotations_skipped

            shard_idx = idx % num_shards

            if tf_example:
                output_tfrecords[shard_idx].write(tf_example.SerializeToString())

        print("Finished")
        print(f"Total annotations skipped: {total_num_annotations_skipped}")




