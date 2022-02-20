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

def filterAndPrepareDataFromOI(path, select, rename = None):
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


def flatten(destination):
    all_files = []
    for root, dirs, files in os.walk(destination):
        for file in files:
            all_files.append(os.path.join(root, file))
    for file in all_files:
        shutil.move(file, destination)

