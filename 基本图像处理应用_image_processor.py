import tkinter as tk
from tkinter import filedialog, ttk, messagebox
import cv2
import numpy as np
from PIL import Image, ImageTk
import pywt
import random
import os

class ImageProcessor:
    def __init__(self, root):
        self.root = root
        self.root.title("图像处理应用")
        self.root.geometry("1200x800")

        # 创建主框架
        self.main_frame = ttk.Frame(self.root)
        self.main_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        # 创建左侧操作面板
        self.control_frame = ttk.Frame(self.main_frame, width=200)
        self.control_frame.pack(side=tk.LEFT, fill=tk.Y, padx=5, pady=5)

        # 创建右侧图像显示区域
        self.image_frame = ttk.Frame(self.main_frame)
        self.image_frame.pack(side=tk.RIGHT, fill=tk.BOTH, expand=True, padx=5, pady=5)

        # 上传按钮
        self.upload_btn = ttk.Button(self.control_frame, text="上传图片", command=self.upload_image)
        self.upload_btn.pack(fill=tk.X, padx=5, pady=5)

        # 初始化图像历史栈和当前操作图像
        self.history_stack = []
        self.current_image = None

        # 创建参数调节区域
        self.param_frame = ttk.LabelFrame(self.control_frame, text="参数调节")
        self.param_frame.pack(fill=tk.X, padx=5, pady=5)

        # 定义参数范围
        self.param_ranges = {
            "均值": "[-255, 255]",
            "标准差": "[0, 255]",
            "噪声比例": "[0, 1]",
            "阈值": "[0, 255]",
            "最小阈值": "[0, 255]",
            "最大阈值": "[0, 255]",
            "去噪强度": "[1, 30]",
            "旋转角度": "[0, 360]",
            "缩放比例": "[0.1, 5.0]",
            "最小线长": "[50, 500]"
        }

        # 创建操作按钮
        operations = [
            ("原图", self.back_to_original),
            ("高斯噪声", lambda: self.show_param_controls([("均值", -255, 255), ("标准差", 0, 255)], self.add_gaussian_noise)),
            ("椒盐噪声", lambda: self.show_param_controls([("噪声比例", 0, 1)], self.add_salt_pepper_noise)),
            ("灰度化", lambda: self.show_param_controls([], self.convert_to_gray)),
            ("反转颜色", lambda: self.show_param_controls([], self.invert_colors)),
            ("二值化", lambda: self.show_param_controls([("阈值", 0, 255)], self.threshold)),
            ("BGR转HSV", lambda: self.show_param_controls([], self.bgr_to_hsv)),
            ("边缘检测（Canny）", lambda: self.show_param_controls([("最小阈值", 0, 255), ("最大阈值", 0, 255)], self.edge_detection)),
            ("去噪（mean）", self.denoise),
            ("自定义卷积", self.show_convolution_controls),
            ("几何变换", lambda: self.show_param_controls([("旋转角度", 0, 360), ("缩放比例", 0.1, 5.0)], self.geometric_transform)),
            ("图像增强（直方图均衡化）", lambda: self.show_param_controls([], self.histogram_equalization)),
            ("线检测（HoughLine）", lambda: self.show_param_controls([("阈值", 0, 255), ("最小线长", 50, 500)], self.line_detection)),
            ("形态学操作", self.show_morphological_controls),
            ("小波变换(db1,level=3)", self.wavelet_transform)
        ]

        for text, command in operations:
            btn = ttk.Button(self.control_frame, text=text, command=command)
            btn.pack(fill=tk.X, padx=5, pady=2)

        # 图像显示标签
        self.image_label = ttk.Label(self.image_frame)
        self.image_label.pack()

        self.original_image = None
        self.param_entries = []
        self.current_operation = None

    def back_to_original(self):
        if self.original_image is not None:
            self.clear_stack()  # 清空历史栈
            self.show_image(self.original_image)

    def clear_stack(self):
        self.history_stack = []

    def save_current_state(self):
        if self.current_image is not None:
            self.history_stack.append(self.current_image.copy())

    def upload_image(self):
        file_path = filedialog.askopenfilename()
        if file_path:
            file_path = os.path.abspath(file_path)
            self.original_image = cv2.imdecode(np.fromfile(os.fspath(file_path), dtype=np.uint8), cv2.IMREAD_UNCHANGED)
            if self.original_image is not None:
                self.show_image(self.original_image)
                self.clear_stack()  # 上传新图片时清空历史栈
            else:
                messagebox.showerror("错误", "无法读取图片文件")

    def show_image(self, image):
        if image is None:
            return
        self.current_image = image.copy()
        height, width = image.shape[:2]
        max_size = 700
        if height > max_size or width > max_size:
            scale = max_size / max(height, width)
            width = int(width * scale)
            height = int(height * scale)
            image = cv2.resize(image, (width, height))

        if len(image.shape) == 3:
            image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
        image = Image.fromarray(image)
        photo = ImageTk.PhotoImage(image=image)
        self.image_label.configure(image=photo)
        self.image_label.image = photo
        self.save_current_state()

    def add_gaussian_noise(self, mean=0, sigma=25):
        if self.current_image is None:
            return
        image = self.current_image.copy()
        noise = np.random.normal(mean, sigma, image.shape).astype(np.uint8)
        noisy_image = cv2.add(image, noise)
        self.show_image(noisy_image)

    def wavelet_transform(self):
        # 清除之前的参数控件
        for widget in self.param_frame.winfo_children():
            widget.destroy()
        self.param_entries.clear()
        wavelet='db1'
        level = 3
        image = self.current_image.copy()
        # 将图像转换为浮点数并归一化到0-1范围
        image_float = image.astype(np.float64) / 255.0

        # 分别对每个颜色通道处理
        denoised_channels = []
        for channel in range(image_float.shape[2]):
            coeffs = pywt.wavedec2(image_float[:, :, channel], wavelet, level=level)
            cA = coeffs[0]
            details = coeffs[1:]

            # 计算噪声估计
            sigma = np.median(np.abs(details[0][0])) / 0.6745
            threshold = sigma * np.sqrt(2 * np.log2(image_float.shape[0] * image_float.shape[1]))

            # 应用阈值处理
            def apply_threshold(coeff):
                return pywt.threshold(coeff, threshold, mode='soft')

            coeffs_thresh = [cA]
            for i in range(level):
                coeffs_thresh.append(tuple(apply_threshold(coeff) for coeff in details[i]))

            # 重建通道
            channel_reconstructed = pywt.waverec2(coeffs_thresh, wavelet)
            denoised_channels.append(channel_reconstructed)

        # 合并所有通道并裁剪到原始大小
        image_reconstructed = np.stack(denoised_channels, axis=2)
        image_reconstructed = image_reconstructed[:image.shape[0], :image.shape[1], :]

        # 将图像恢复到0-255范围并转为整数类型
        image_reconstructed = (image_reconstructed * 255).astype(np.uint8)
    
        self.show_image(image_reconstructed)

    def add_salt_pepper_noise(self, prob=0.05):
        if self.current_image is None:
            return
        image = self.current_image.copy()
        noisy = np.zeros(image.shape, np.uint8)
        thres = 1 - prob
        for i in range(image.shape[0]):
            for j in range(image.shape[1]):
                rdn = random.random()
                if rdn < prob:
                    noisy[i][j] = 0
                elif rdn > thres:
                    noisy[i][j] = 255
                else:
                    noisy[i][j] = image[i][j]
        self.show_image(noisy)

    def convert_to_gray(self):
        if self.current_image is None:
            return
        gray = cv2.cvtColor(self.current_image, cv2.COLOR_BGR2GRAY)
        self.show_image(gray)

    def invert_colors(self):
        if self.current_image is None:
            return
        inverted = cv2.bitwise_not(self.current_image)
        self.show_image(inverted)

    def threshold(self, thresh=127):
        if self.current_image is None:
            return
        image = self.current_image.copy()
        if len(image.shape) == 3:
            image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        _, binary = cv2.threshold(image, thresh, 255, cv2.THRESH_BINARY)
        self.show_image(binary)

    def bgr_to_hsv(self):
        if self.current_image is None:
            return
        hsv = cv2.cvtColor(self.current_image, cv2.COLOR_BGR2HSV)
        self.show_image(hsv)

    def edge_detection(self, min_val=100, max_val=200):
        if self.current_image is None:
            return
        image = self.current_image.copy()
        if len(image.shape) == 3:
            image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        edges = cv2.Canny(image, min_val, max_val)
        self.show_image(edges)

    def denoise(self, strength=10):
        if self.current_image is None:
            return
        kernel = np.ones((5,5), np.float32)/25
        denoised = cv2.filter2D(self.current_image, -1, kernel)
        self.show_image(denoised)

    def geometric_transform(self, angle=45, scale=1.0):
        if self.current_image is None:
            return
        image = self.current_image.copy()
        rows, cols = image.shape[:2]
        matrix = cv2.getRotationMatrix2D((cols/2, rows/2), angle, scale)
        rotated = cv2.warpAffine(image, matrix, (cols, rows))
        self.show_image(rotated)

    def histogram_equalization(self):
        if self.current_image is None:
            return
        image = self.current_image.copy()
        if len(image.shape) == 3:
            lab = cv2.cvtColor(image, cv2.COLOR_BGR2LAB)
            l, a, b = cv2.split(lab)
            l_eq = cv2.equalizeHist(l)
            lab_eq = cv2.merge((l_eq, a, b))
            result = cv2.cvtColor(lab_eq, cv2.COLOR_LAB2BGR)
        else:
            result = cv2.equalizeHist(image)
        self.show_image(result)

    def line_detection(self, threshold=200, min_line_length=100):
        if self.current_image is None:
            return
        image = self.current_image.copy()
        if len(image.shape) == 3:
            image = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        edges = cv2.Canny(image, 50, 150)
        lines = cv2.HoughLines(edges, 1, np.pi/180, int(threshold))
        result = self.current_image.copy()
        if lines is not None:
            for rho, theta in lines[:, 0]:
                a = np.cos(theta)
                b = np.sin(theta)
                x0 = a * rho
                y0 = b * rho
                x1 = int(x0 + min_line_length*(-b))
                y1 = int(y0 + min_line_length*(a))
                x2 = int(x0 - min_line_length*(-b))
                y2 = int(y0 - min_line_length*(a))
                cv2.line(result, (x1, y1), (x2, y2), (0,0,255), 2)
        self.show_image(result)

    def show_convolution_controls(self):
        # 清除之前的参数控件
        for widget in self.param_frame.winfo_children():
            widget.destroy()
        self.param_entries.clear()

        # 创建3x3卷积核输入区域
        ttk.Label(self.param_frame, text="3×3卷积核").pack(pady=5)
        
        # 创建3x3网格输入框
        kernel_frame = ttk.Frame(self.param_frame)
        kernel_frame.pack(pady=5)
        
        self.kernel_entries = []
        for i in range(3):
            row_entries = []
            for j in range(3):
                entry = ttk.Entry(kernel_frame, width=5)
                entry.grid(row=i, column=j, padx=2, pady=2)
                entry.insert(0, "1")
                row_entries.append(entry)
            self.kernel_entries.append(row_entries)

        # 添加确认按钮
        ttk.Button(self.param_frame, text="应用",
                   command=self.apply_custom_convolution).pack(pady=5)

    def apply_custom_convolution(self):
        if self.current_image is None:
            return

        try:
            # 从输入框获取卷积核值
            kernel = []
            for row in self.kernel_entries:
                kernel_row = []
                for entry in row:
                    value = float(entry.get())
                    kernel_row.append(value)
                kernel.append(kernel_row)

            # 转换为numpy数组并归一化
            kernel = np.array(kernel, dtype=np.float32)
            kernel = kernel / kernel.sum() if kernel.sum() != 0 else kernel

            # 应用卷积
            result = cv2.filter2D(self.current_image, -1, kernel)
            self.show_image(result)

        except ValueError:
            messagebox.showerror("错误", "请输入有效的数值")

    def show_morphological_controls(self):
        for widget in self.param_frame.winfo_children():
            widget.destroy()
        self.param_entries.clear()

        frame = ttk.Frame(self.param_frame)
        frame.pack(fill=tk.X, padx=5, pady=2)
        ttk.Label(frame, text="操作类型").pack(side=tk.LEFT)
        
        operations = [
            "腐蚀", "膨胀", "开运算", "闭运算", "形态学梯度"
        ]
        
        operation_var = tk.StringVar(value=operations[0])
        operation_combo = ttk.Combobox(frame, textvariable=operation_var, values=operations, state="readonly")
        operation_combo.pack(side=tk.RIGHT)

        ttk.Button(self.param_frame, text="应用",
                   command=lambda: self.apply_morphological_operation(operation_var.get())).pack(pady=5)

    def apply_morphological_operation(self, operation_type):
        if self.current_image is None:
            return

        kernel = np.ones((5, 5), np.uint8)
        image = self.current_image.copy()

        if operation_type == "腐蚀":
            result = cv2.erode(image, kernel, iterations=1)
        elif operation_type == "膨胀":
            result = cv2.dilate(image, kernel, iterations=1)
        elif operation_type == "开运算":
            result = cv2.morphologyEx(image, cv2.MORPH_OPEN, kernel)
        elif operation_type == "闭运算":
            result = cv2.morphologyEx(image, cv2.MORPH_CLOSE, kernel)
        elif operation_type == "形态学梯度":
            result = cv2.morphologyEx(image, cv2.MORPH_GRADIENT, kernel)

        self.show_image(result)

    def show_param_controls(self, params, operation):
        for widget in self.param_frame.winfo_children():
            widget.destroy()
        self.param_entries.clear()
        
        if not params:
            operation()
            return

        for param_name, min_val, max_val in params:
            frame = ttk.Frame(self.param_frame)
            frame.pack(fill=tk.X, padx=5, pady=2)
            
            label_text = f"{param_name} {self.param_ranges.get(param_name, '')}"
            ttk.Label(frame, text=label_text).pack(side=tk.LEFT)
            
            entry = ttk.Entry(frame, width=10)
            entry.pack(side=tk.RIGHT)
            entry.insert(0, str((min_val + max_val) / 2))
            self.param_entries.append((entry, min_val, max_val))

        ttk.Button(self.param_frame, text="应用", command=lambda: self.apply_operation(operation)).pack(pady=5)

    def apply_operation(self, operation):
        try:
            params = []
            for entry, min_val, max_val in self.param_entries:
                value = float(entry.get())
                if value < min_val or value > max_val:
                    messagebox.showerror("错误", f"参数值必须在范围 [{min_val}, {max_val}] 内")
                    return
                params.append(value)
            operation(*params)
        except ValueError:
            messagebox.showerror("错误", "请输入有效的数值参数")

if __name__ == "__main__":
    root = tk.Tk()
    app = ImageProcessor(root)
    root.mainloop()