import os
import xml.etree.ElementTree as ET
import shutil


def filterAndPrepareDataFromLabelMe(path, dirName, select, exclude):
    delete_list = []
    rename_dict = {}

    for rootOfDir, dirs, _ in os.walk(os.path.join(path, f"{dirName}\\Annotations")):
        for _dir in dirs:
            for rootOfFile, _, files in os.walk(os.path.join(path, f"{dirName}\\Annotations\\{_dir}")):
                for _file in files:
                    try:
                        tree = ET.parse(os.path.join(rootOfFile, _file))
                        root = tree.getroot()

                        contain_exclude = False
                        for obj in root.iter("name"):
                            if obj.text in exclude:
                                delete_list.append(os.path.join(rootOfFile, _file))
                                contain_exclude = True
                                break

                        if contain_exclude: continue

                        img = tree.find("filename")
                        newname = _dir + img.text.replace("\n", "")
                        img.text = newname

                        tree.find("folder").text = "train"

                        obj_to_delete = []
                        num_of_objects = 0
                        for obj in root.findall("object"):
                            num_of_objects += 1
                            if obj.find("name").text.replace("\n", "") not in select:
                                obj_to_delete.append(obj)

                        if len(obj_to_delete) == num_of_objects:
                            delete_list.append(os.path.join(rootOfFile, _file))
                            continue

                        for obj in obj_to_delete:
                            root.remove(obj)

                        tree.write(os.path.join(rootOfFile, _file))

                        rename_dict[os.path.join(rootOfFile, _file)] = os.path.join(rootOfFile,
                                                                                    newname.replace("jpg", "xml"))

                    except Exception:
                        delete_list.append(os.path.join(rootOfFile, _file))

    for file in delete_list:
        os.remove(file)
        os.remove(file.replace("Annotations", "Images").replace("xml", "jpg"))

    for file in rename_dict:
        os.rename(file, rename_dict[file])
        os.rename(
            file.replace("Annotations", "Images").replace("xml", "jpg"),
            rename_dict[file].replace("Annotations", "Images").replace("xml", "jpg")
        )


def filterAndPrepareDataFromOI(path, select, rename=None):
    delete_list = []

    for rootOfFile, _, files in os.walk(path):
        for _file in files:
            if not str(_file).endswith(".xml"):
                continue
            try:
                tree = ET.parse(os.path.join(rootOfFile, _file))
                root = tree.getroot()

                contain_group = False
                for obj in root.findall("object"):
                    if obj.find("group").text == '1' and obj.find("name").text in select:
                        delete_list.append(os.path.join(rootOfFile, _file))
                        contain_group = True
                        break
                if contain_group: continue

                obj_to_delete = []
                num_of_objects = 0
                for obj in root.findall("object"):
                    num_of_objects += 1
                    if obj.find("name").text not in select:
                        obj_to_delete.append(obj)
                    else:
                        if rename is not None:
                            obj.find("name").text = rename

                if len(obj_to_delete) == num_of_objects:
                    delete_list.append(os.path.join(rootOfFile, _file))
                    continue

                for obj in obj_to_delete:
                    root.remove(obj)

                tree.write(os.path.join(rootOfFile, _file))

            except Exception:
                delete_list.append(os.path.join(rootOfFile, _file))

    for file in delete_list:
        os.remove(file)
        os.remove(file.replace("xml", "jpg"))


def prepateMyDataSet(path):
    for file in os.listdir(path):
        if not str(file).endswith(".xml"):
            continue
        tree = ET.parse(os.path.join(path, file))
        root = tree.getroot()

        for obj in root.findall("object"):
            xmin, ymin, xmax, ymax = None, None, None, None
            for point in obj.iter("pt"):
                x, y = float(point.find("x").text), float(point.find("y").text)
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
            bndbox = ET.SubElement(obj, "bndbox")
            x_min = ET.SubElement(bndbox, "xmin")
            x_min.text = str(int(xmin))
            y_min = ET.SubElement(bndbox, "ymin")
            y_min.text = str(int(ymin))
            x_max = ET.SubElement(bndbox, "xmax")
            x_max.text = str(int(xmax))
            y_max = ET.SubElement(bndbox, "ymax")
            y_max.text = str(int(ymax))
            obj.remove(obj.find("polygon"))

            obj.find("name").text = "tree"

        tree.write(os.path.join(path, file))
    print("Ended for path", path)

def flatten(destination):
    all_files = []
    for root, dirs, files in os.walk(destination):
        for file in files:
            all_files.append(os.path.join(root, file))
    for file in all_files:
        shutil.move(file, destination)
