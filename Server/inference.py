import argparse
from ast import arg
import os
import csv
import torch
import torchvision.transforms as transforms
import torch.utils.data
import numpy as np
from sklearn.metrics import average_precision_score, precision_recall_curve, accuracy_score
from torch.utils.data import Dataset
import sys
from models import get_model
import pickle
from tqdm import tqdm
from io import BytesIO
from copy import deepcopy
from dataset_paths import DATASET_PATHS
import random
import shutil
import torchvision
import csv 
import pandas as pd 
import time
import matplotlib.pyplot as plt
import face_recognition
from PIL import Image, ImageDraw

start_time = time.time()  # 记录开始时间

SEED = 0
def set_seed():
    torch.manual_seed(SEED)
    torch.cuda.manual_seed(SEED)
    np.random.seed(SEED)
    random.seed(SEED)

MEAN = {
    "imagenet":[0.485, 0.456, 0.406],
    "clip":[0.48145466, 0.4578275, 0.40821073]
}

STD = {
    "imagenet":[0.229, 0.224, 0.225],
    "clip":[0.26862954, 0.26130258, 0.27577711]
}

def inference(model, img):
    with torch.no_grad():
        y_pred = model(img).sigmoid().flatten().squeeze().cpu().numpy()
    return y_pred

def model_prepare():
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    # parser.add_argument('--image' , type=str, default='./test_images/real.png')
    # parser.add_argument('--arch', type=str, default='res50')
    # parser.add_argument('--ckpt', type=str, default='./pretrained_weights/fc_weights.pth')
    parser.add_argument('--isTrain', type=bool, default=False) 
    opt = parser.parse_args()

    model = get_model('CLIP:ViT-L/14', opt)
    state_dict = torch.load('./trained_weights/model_epoch_best.pth', map_location='cpu')
    nested_state_dict = state_dict['model']
    # print("Model keys:", model.state_dict().keys())
    # print("State_dict keys:", state_dict.keys())
    model.fc.load_state_dict(nested_state_dict)
    model.eval()
    model.cuda()
    return model

def draw_box(image,face_locations,img_preds):
    img = Image.fromarray(image, 'RGB')
    img_with_red_box = img.copy()
    img_with_red_box_draw = ImageDraw.Draw(img_with_red_box)
    for i in range(0,len(face_locations)):
        face_location = face_locations[i]
        img_pred = img_preds[i]
        if img_pred >0.4:
            img_with_red_box_draw.rectangle(
                [
                    max(0, face_location[3] - 50),
                    max(0, face_location[0] - 50),
                    min(img.width, face_location[1] + 50),
                    min(img.height, face_location[2] + 50)
                ],
                outline="red",
                width=3
            )
        else:
            img_with_red_box_draw.rectangle(
                [
                    max(0, face_location[3] - 50),
                    max(0, face_location[0] - 50),
                    min(img.width, face_location[1] + 50),
                    min(img.height, face_location[2] + 50)
                ],
                outline="green",
                width=3
            )
    output_dir = "boxed_faces"
    os.makedirs(output_dir, exist_ok=True)
    save_path = os.path.join(output_dir, f"face_with_box.jpg")
    img_with_red_box.save(save_path)

def predict(path, model):
    stat_from = "clip"

    transform = transforms.Compose([
        transforms.Resize([224,224]),
        transforms.ToTensor(),
        transforms.Normalize( mean=MEAN[stat_from], std=STD[stat_from] ),
    ])

    image = face_recognition.load_image_file(path)
    face_locations = face_recognition.face_locations(image)
    
    print(face_locations)
    if (face_locations):
        img_preds=[]
        for face_location in face_locations:
            img = Image.fromarray(image, 'RGB')
            img_cropped = img.crop((
                max(0, face_location[3] - 50),
                max(0, face_location[0] - 50),
                min(img.width, face_location[1] + 50),
                min(img.height, face_location[2] + 50)
            ))
            img_tensor=transform(img_cropped).unsqueeze(0).cuda()
            img_preds.append(inference(model, img_tensor))

        draw_box(image,face_locations,img_preds)
        y_pred = max(img_preds)
    else:
        # 打开图片并获取分辨率
        img = Image.open(path).convert("RGB")
        width, height = img.size

        # 裁剪出中间的正方形
        if width > height:  # 如果宽度大于高度，以高度为边长裁剪
            left = (width - height) // 2
            top = 0
            right = left + height
            bottom = height
        else:  # 如果高度大于宽度，以宽度为边长裁剪
            left = 0
            top = (height - width) // 2
            right = width
            bottom = top + width

        img_cropped = img.crop((left, top, right, bottom))
        img_tensor = transform(img_cropped).unsqueeze(0).cuda()
        y_pred = inference(model, img_tensor)
    
    
    # img_tensor = transform(img_cropped).unsqueeze(0)
    

    print ("Prediction: ", y_pred)
    return y_pred

def get_all_file_data(directory):
    file_names_without_extension = []  # 初始化一个空列表来存储文件路径
    file_names_with_pred_value = []  # 初始化一个空列表来存储预测数值
    file_data = []  # 初始化返回列表
    i=0
    for root, dirs, files in os.walk(directory):
        for file in files:
            i=i+1
            file_names_with_pred_value.append(perpred(os.path.join(root, file)))  # 获取预测值
            file_name_without_extension = os.path.splitext(file)[0]
            file_names_without_extension.append(file_name_without_extension)  # 获取文件名称
            if(i==10):
                i=0
                proccess_time = time.time()  # 记录时间
                print(f"程序运行时间：{proccess_time - start_time} 秒")

    file_data.append(file_names_without_extension)
    file_data.append(file_names_with_pred_value)# 整合两个列表
    return file_data

def write_to_csv(file_list, output_file_path):

    file_names, numbers = file_list

    # 确保两个子列表长度相同
    if len(file_names) != len(numbers):
        raise ValueError("文件名列表和数字列表的长度必须相同。")

    with open(output_file_path, mode='w', newline='', encoding='utf-8') as file:
        writer = csv.writer(file)

        # 写入数据行
        for file_name, number in zip(file_names, numbers):
            writer.writerow([file_name, number])

if __name__ == '__main__':
    model = model_prepare()
    print(predict('./test.jpg', model))
    # irectory_path = '/home/data/szk/face/face'
    # all_file_data = get_all_file_data(irectory_path)
    # write_to_csv(all_file_data, '/home/data/szk/face/cla_pre.csv')
    # end_time = time.time()  # 记录结束时间
    # print(f"程序运行时间：{end_time - start_time} 秒")